import { InfoService } from "@/services"
import { switchLanguage, i18n } from "@/i18n"
import { getTheme } from "@/utils/init"

InfoService.getProperties().then(config => {
    function loadTheme() {

        var css = document.createElement('Link')
        css.setAttribute('rel', 'stylesheet')
        css.setAttribute('type', 'text/css')
        css.setAttribute('href', 'themes/' + getTheme(config) + '/styles.css')

        document.head.appendChild(css)
    }

    loadTheme()

    const searchParams = new URLSearchParams(location.search)
    const authorization = searchParams.get('authorization')
    const userId = searchParams.get('userId')
    const lang = searchParams.get('lang')
    const processDefinitionId = searchParams.get('processDefinitionId')
    const taskId = searchParams.get('taskId')
    config.supportedLanguages = [lang]

    switchLanguage(config, lang).then(() => {
        const embeddedFormRoot = document.getElementById('embeddedFormRoot')
        const submitButton = document.getElementById('submitButton')
        const saveButton = document.getElementById('saveButton')
        const loaderDiv = document.getElementById('loader')
        const contentDiv = document.getElementById('content')
        const errorDiv = document.getElementById('embeddedFormError')

        const isStartForm = !!processDefinitionId
        let embeddedForm

        if (isStartForm) {
            submitButton.innerHTML = i18n.global.t('process.start')
        } else {
            submitButton.innerHTML = i18n.global.t('task.actions.submit')
        }
        submitButton.addEventListener('click', () => {
            blockButtons(submitButton, saveButton)
            embeddedForm.submit(err => {
                if (err) {
                    errorDiv.style.display = 'block'
                    errorDiv.innerHTML = i18n.global.t('task.actions.saveError', [err])
                } else {
                    services.completeTask()
                }
                unblockButtons(submitButton, saveButton)
            })
        })

        if (isStartForm) {
            saveButton.style.display = 'none'
        }
        saveButton.innerHTML = i18n.global.t('task.actions.save')
        saveButton.addEventListener('click', () => {
            blockButtons(submitButton, saveButton)
            embeddedForm.store(err => {
                if (err) {
                    errorDiv.style.display = 'block'
                    errorDiv.innerHTML = i18n.global.t('task.actions.saveError', [err])
                } else {
                    services.displaySuccessMessage()
                }
                unblockButtons(submitButton, saveButton)
            })
        })

        loadEmbeddedForm(isStartForm, processDefinitionId || taskId, embeddedFormRoot, authorization, userId).then(form => {
            embeddedForm = form
            loaderDiv.style.display = 'none'
            contentDiv.style.display = 'flex'
        }, err => {
            services.displayErrorMessage(err)
        })
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
        button.disabled = true
    })
}

function unblockButtons(...buttons) {
    buttons.forEach(button => {
        button.disabled = false
    })
}

function loadEmbeddedForm(isStartForm, referenceId, container, authorization, userId) {
    var client = new window.CamSDK.Client({
        mock: false,
        apiUri: '/engine-rest',
        headers: {
            'authorization': authorization,
            'Context-User-ID': userId
        }
    })
    return new Promise((resolve, reject) => {
        if (isStartForm) {
            let processService = client.resource('process-definition')
            processService.startForm({ id: referenceId }, (err, taskFormInfo) => {
                if (err) reject(err)
                else loadForm(taskFormInfo)
            })
        } else {
            let taskService = client.resource('task')
            // loads the task form using the task ID provided
            taskService.form(referenceId, (err, taskFormInfo) => {
                if (err) reject(err)
                else loadForm(taskFormInfo)
            })
        }
        function loadForm(formInfo) {
            var url = formInfo.key.replace('embedded:', '').replace('app:', (formInfo.contextPath || '') + '/')
            //Start with a relative url and replace doubled slashes if necessary
                .replace(/^(\/+|([^/]))/, '/$2').replace(/\/\/+/, '/')

            let embeddedForm
            let config = {
                client: client,
                formUrl: url,
                containerElement: container,
                // continue the logic with the callback
                done: (err) => {
                    if (err) {
                        reject(err)
                    } else {
                        resolve(embeddedForm)
                    }
                }
            }
            if (isStartForm) {
                config.processDefinitionId = referenceId
            } else {
                config.taskId = referenceId
            }
            embeddedForm = new window.CamSDK.Form(config)
        }
    })
}
