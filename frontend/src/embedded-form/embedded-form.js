import $ from 'jquery';
import { InfoService } from "@/services"
import { switchLanguage, i18n } from "@/i18n"
import { getTheme } from "@/utils/init"
import CamSDK from "./camunda-bpm-sdk/index-browser.js"

InfoService.getProperties().then(response => {
    const config = response.data
    function loadTheme() {

        var css = $('<link>', {
            rel: 'stylesheet',
            type: 'text/css',
            href: 'themes/' + getTheme(config) + '/styles.css'
        });
        $('head').append(css);
    }

    loadTheme()

    const searchParams = new URLSearchParams(location.search)
    const authorization = searchParams.get('authorization')
    const lang = searchParams.get('lang')
    const processDefinitionId = searchParams.get('processDefinitionId')
    const taskId = searchParams.get('taskId')
    config.supportedLanguages = [lang]

    switchLanguage(config, lang).then(() => {
        const $submitButton = $('#submitButton');
        const $saveButton = $('#saveButton');
        const $loaderDiv = $('#loader');
        const $contentDiv = $('#content');
        const $errorDiv = $('#embeddedFormError');
        const $embeddedContainer = $('#embeddedRoot');
        const $formContainer = $('#embeddedFormRoot');

        const isStartForm = !!processDefinitionId
        let embeddedForm

        if (isStartForm) {
            $submitButton.html(i18n.global.t('process.start'));
        } else {
            $submitButton.html(i18n.global.t('task.actions.submit'));
        }
        $submitButton.on('click', function () {
            blockButtons($submitButton, $saveButton);
            embeddedForm.submit(err => {
                if (err) {
                    $errorDiv.show().html(i18n.global.t('task.actions.saveError', [err]));
                } else {
                    services.completeTask();
                }
                unblockButtons($submitButton, $saveButton);
            });
        });
        
        if (isStartForm) {
            $saveButton.hide();
        }
        $saveButton.html(i18n.global.t('task.actions.save'));
        $saveButton.on('click', function () {
            blockButtons($submitButton, $saveButton);
            embeddedForm.store(err => {
                if (err) {
                    $errorDiv.show().html(i18n.global.t('task.actions.saveError', [err]));
                } else {
                    services.displaySuccessMessage();
                }
                unblockButtons($submitButton, $saveButton);
            });
        });
        
        loadEmbeddedForm(isStartForm, processDefinitionId || taskId, $embeddedContainer, $formContainer, authorization, config).then(form => {
            embeddedForm = form;
            $loaderDiv.hide();
            $contentDiv.css('display', 'flex');
        }, err => {
            console.error(err);
            services.displayErrorMessage(err);
        });
    })
})

const services = {
    completeTask() {
        callParent('completeTask')
    },
    displaySuccessMessage() {
        callParent('displaySuccessMessage')
    },
    displayGenericSuccessMessage() {
        callParent('displayGenericSuccessMessage')
    },
    displayErrorMessage(data) {
        callParent('displayErrorMessage', data)
    },
    cancelTask() {
        callParent('cancelTask')
    },
    updateFilters(data) {
        callParent('updateFilters', data)
    }
}

function callParent(method, data) {
    window.parent.postMessage({
        method,
        data
    })
}

function blockButtons(...buttons) {
    buttons.forEach(button => {
        $(button).prop('disabled', true);
    });
}

function unblockButtons(...buttons) {
    buttons.forEach(button => {
        $(button).prop('disabled', false);
    });
}

function loadEmbeddedForm(isStartForm, referenceId, embeddedContainer, formContainer, authorization, config) {
    var client = new CamSDK.Client({
        mock: false,
        apiUri: config.engineRestPath || '/engine-rest',
        headers: {
            'authorization': authorization
        }
    });
    return new Promise((resolve, reject) => {
        if (isStartForm) {
            let processService = client.resource('process-definition');
            processService.startForm({ id: referenceId }, (err, taskFormInfo) => {
                if (err) reject(err);
                else loadForm(taskFormInfo);
            });
        } else {
            let taskService = client.resource('task');
            // loads the task form using the task ID provided
            taskService.form(referenceId, (err, taskFormInfo) => {
                if (err) reject(err);
                else loadForm(taskFormInfo);
            });
        }
        async function loadForm(formInfo) {
            let embeddedForm;
            let config = {
                client: client,
                // continue the logic with the callback
                done: (err) => {
                    if (err) {
                        reject(err);
                    } else {
                        resolve(embeddedForm);
                    }
                }
            };
            if (formInfo.key.includes('deployment:')) {
                let resource = await loadDeployedForm(client, isStartForm, referenceId);
                formContainer.html(resource);
                config.formElement = formContainer;
                embeddedContainer.hide()
            } else {
                // Start with a relative url and replace doubled slashes if necessary
                var url = formInfo.key.replace('embedded:', '').replace('app:', (formInfo.contextPath || '') + '/')
                    .replace(/^(\/+|([^/]))/, '/$2').replace(/\/\/+/, '/')
                config.formUrl = url
                config.containerElement = embeddedContainer
                formContainer.hide()
            }

            if (isStartForm) {
                config.processDefinitionId = referenceId
            } else {
                config.taskId = referenceId
            }
            embeddedForm = new CamSDK.Form(config)
        }
    })
}

function loadDeployedForm(client, isStartForm, referenceId) {
    return new Promise((resolve, reject) => {
        if (isStartForm) {
            client.resource('process-definition').deployedForm({ id: referenceId }, (err, resource) => {
                if (err) reject(err)
                else {
                    resolve(resource)
                }
            })
        } else {
            client.resource('task').deployedForm(referenceId, (err, resource) => {
                if (err) reject(err)
                else {
                    resolve(resource)
                }
            })
        }
    })
}
