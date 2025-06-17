import { InfoService } from "@/services";
import { switchLanguage, i18n } from "@/i18n";
import { getTheme } from "@/utils/init";
import CamSDK from "bpm-sdk";

InfoService.getProperties().then(response => {
    const config = response.data;

    function loadTheme() {
        const css = document.createElement('link');
        css.rel = 'stylesheet';
        css.type = 'text/css';
        css.href = 'themes/' + getTheme(config) + '/styles.css';
        document.head.appendChild(css);
    }

    loadTheme();

    const searchParams = new URLSearchParams(window.location.search);
    const authorization = searchParams.get('authorization');
    const lang = searchParams.get('lang');
    const processDefinitionId = searchParams.get('processDefinitionId');
    const taskId = searchParams.get('taskId');
    config.supportedLanguages = [lang];

    switchLanguage(config, lang).then(() => {
        const submitButton = document.getElementById('submitButton');
        const saveButton = document.getElementById('saveButton');
        const loaderDiv = document.getElementById('loader');
        const contentDiv = document.getElementById('content');
        const errorDiv = document.getElementById('embeddedFormError');
        const embeddedContainer = document.getElementById('embeddedRoot');
        const formContainer = document.getElementById('embeddedFormRoot');

        const isStartForm = !!processDefinitionId;
        let embeddedForm;

        if (isStartForm) {
            submitButton.innerHTML = i18n.global.t('process.start');
        } else {
            submitButton.innerHTML = i18n.global.t('task.actions.submit');
        }
        submitButton.addEventListener('click', () => {
            blockButtons(submitButton, saveButton);
            embeddedForm.submit(err => {
                if (err) {
                    errorDiv.style.display = '';
                    errorDiv.innerHTML = i18n.global.t('task.actions.saveError', [err]);
                } else {
                    services.completeTask();
                }
                unblockButtons(submitButton, saveButton);
            });
        });

        if (isStartForm) {
            saveButton.style.display = 'none';
        }
        saveButton.innerHTML = i18n.global.t('task.actions.save');
        saveButton.addEventListener('click', () => {
            blockButtons(submitButton, saveButton);
            embeddedForm.store(err => {
                if (err) {
                    errorDiv.style.display = '';
                    errorDiv.innerHTML = i18n.global.t('task.actions.saveError', [err]);
                } else {
                    services.displaySuccessMessage();
                }
                unblockButtons(submitButton, saveButton);
            });
        });

        loadEmbeddedForm(
            isStartForm,
            processDefinitionId || taskId,
            embeddedContainer,
            formContainer,
            authorization,
            config
        ).then(
            form => {
                embeddedForm = form;
                loaderDiv.style.display = 'none';
                contentDiv.style.display = 'flex';
            },
            err => {
                console.error(err);
                services.displayErrorMessage(err);
            }
        );
    });
});

const services = {
    completeTask() {
        callParent('completeTask');
    },
    displaySuccessMessage() {
        callParent('displaySuccessMessage');
    },
    displayGenericSuccessMessage() {
        callParent('displayGenericSuccessMessage');
    },
    displayErrorMessage(data) {
        callParent('displayErrorMessage', data);
    },
    cancelTask() {
        callParent('cancelTask');
    },
    updateFilters(data) {
        callParent('updateFilters', data);
    }
};

function callParent(method, data) {
    window.parent.postMessage({
        method,
        data
    });
}

function blockButtons(...buttons) {
    buttons.forEach(button => {
        if (button) button.disabled = true;
    });
}

function unblockButtons(...buttons) {
    buttons.forEach(button => {
        if (button) button.disabled = false;
    });
}

function loadEmbeddedForm(
    isStartForm,
    referenceId,
    embeddedContainer,
    formContainer,
    authorization,
    config
) {
    var client = new CamSDK.Client({
        mock: false,
        apiUri: config.engineRestPath || '/engine-rest',
        headers: {
            authorization: authorization
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
            let formConfig = {
                client: client,
                done: err => {
                    if (err) {
                        reject(err);
                    } else {
                        resolve(embeddedForm);
                    }
                }
            };
            if (formInfo.key.includes('deployment:')) {
                let resource = await loadDeployedForm(client, isStartForm, referenceId);
                formContainer.innerHTML = resource;
                formConfig.formElement = formContainer;
                if (embeddedContainer) embeddedContainer.style.display = 'none';
            } else {
                // Start with a relative url and replace doubled slashes if necessary
                var url = formInfo.key
                    .replace('embedded:', '')
                    .replace('app:', (formInfo.contextPath || '') + '/')
                    .replace(/^(\/+|([^/]))/, '/$2')
                    .replace(/\/\/+/, '/');
                formConfig.formUrl = url;
                formConfig.containerElement = embeddedContainer;
                if (formContainer) formContainer.style.display = 'none';
            }

            if (isStartForm) {
                formConfig.processDefinitionId = referenceId;
            } else {
                formConfig.taskId = referenceId;
            }
            embeddedForm = new CamSDK.Form(formConfig);
        }
    });
}

function loadDeployedForm(client, isStartForm, referenceId) {
    return new Promise((resolve, reject) => {
        if (isStartForm) {
            client.resource('process-definition').deployedForm(
                { id: referenceId },
                (err, resource) => {
                    if (err) reject(err);
                    else {
                        resolve(resource);
                    }
                }
            );
        } else {
            client.resource('task').deployedForm(referenceId, (err, resource) => {
                if (err) reject(err);
                else {
                    resolve(resource);
                }
            });
        }
    });
}
