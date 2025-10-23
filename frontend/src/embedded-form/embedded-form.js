/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
import { InfoService } from "@/services";
import { switchLanguage, i18n } from "@/i18n";
import { getTheme } from "@/utils/init";
import CamSDK from "bpm-sdk";
// Import jQuery to wrap DOM elements for BPM SDK compatibility - the SDK expects jQuery objects for .find() method calls
import $ from 'jquery';

/**
 * Initialize the embedded form application
 * @param {Object} options - Configuration options
 * @param {HTMLElement} options.submitButton - Submit button element
 * @param {HTMLElement} options.saveButton - Save button element
 * @param {HTMLElement} options.loaderDiv - Loader div element
 * @param {HTMLElement} options.contentDiv - Content div element
 * @param {HTMLElement} options.errorDiv - Error div element
 * @param {HTMLElement} options.embeddedContainer - Embedded container element
 * @param {HTMLElement} options.formContainer - Form container element
 */
export function initEmbeddedForm(options = {}) {
    const {
        submitButton,
        saveButton,
        loaderDiv,
        contentDiv,
        errorDiv,
        embeddedContainer,
        formContainer
    } = options;

    return InfoService.getProperties().then(response => {
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
        const lang = searchParams.get('lang');
        const processDefinitionId = searchParams.get('processDefinitionId');
        const taskId = searchParams.get('taskId');
        // Check if this is a Camunda generated form (vs embedded form)
        const generated = searchParams.get('generated');
        config.supportedLanguages = [lang];

        // Request configuration from parent window via postMessage
        return services.requestConfig().then(parentConfig => {
            return switchLanguage(config, lang).then(() => {
                const isStartForm = !!processDefinitionId;
                const isGeneratedForm = !!generated;
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

                return loadEmbeddedForm(
                    isStartForm,
                    isGeneratedForm,
                    processDefinitionId || taskId,
                    embeddedContainer,
                    formContainer,
                    parentConfig,
                    config
                ).then(
                    form => {
                        embeddedForm = form;
                        loaderDiv.style.display = 'none';
                        contentDiv.style.display = 'flex';
                        // Setup date picker handlers for generated forms to use Vue date picker from parent window
                        if (isGeneratedForm) {
                            setupDatePickerHandlers(formContainer);
                        }
                    },
                    err => {
                        console.error(err);
                        services.displayErrorMessage(err);
                        throw err;
                    }
                ).catch(err => {
                    console.error('Error initializing embedded form:', err);
                    errorDiv.style.display = '';
                    errorDiv.innerHTML = i18n.global.t('task.actions.initError', [err]);
                    loaderDiv.style.display = 'none';
                    throw err;
                });
            });
        });
    });
}

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
    },
    requestConfig() {
        return requestFromParent({
            requestMethod: 'requestConfig',
            responseMethod: 'configResponse',
            extractData: (eventData) => eventData.config,
            validateData: (config) => !!config && !!config.authToken,
            errorMessage: 'No config received from parent window',
            timeout: 5000
        });
    }
};

function callParent(method, data) {
    window.parent.postMessage({
        method,
        data
    });
}

function requestFromParent(options) {
    const {
        requestMethod,
        responseMethod,
        extractData,
        validateData,
        errorMessage,
        timeout = 5000,
        requestData
    } = options;

    return new Promise((resolve, reject) => {
        const messageHandler = createMessageHandler({
            responseMethod,
            extractData,
            validateData,
            errorMessage,
            resolve,
            reject
        });
        const timeoutId = setTimeout(() => {
            window.removeEventListener('message', messageHandler);
            reject(new Error(`Timeout waiting for response to '${requestMethod}' from parent window`));
        }, timeout);
        // Pass timeoutId to messageHandler
        messageHandler.timeoutId = timeoutId;

        window.addEventListener('message', messageHandler);
        callParent(requestMethod, requestData);
    });
}

function createMessageHandler(config) {
    const {
        responseMethod,
        extractData,
        validateData,
        errorMessage,
        resolve,
        reject
    } = config;

    function messageHandler(event) {
        if (event.data && event.data.method === responseMethod) {
            if (messageHandler.timeoutId) {
                clearTimeout(messageHandler.timeoutId);
            }
            window.removeEventListener('message', messageHandler);

            const data = extractData(event.data);
            if (validateData(data)) {
                resolve(data);
            } else {
                window.removeEventListener('message', messageHandler);
                reject(new Error(errorMessage));
            }
        }
    }
    return messageHandler;
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

function setupDatePickerHandlers(formContainer) {
    // Find Angular date inputs with uib-datepicker-popup attribute from Camunda generated forms
    // These inputs are normalized during HTML processing to remove the original Angular calendar functionality
    const dateInputs = formContainer.querySelectorAll('input[uib-datepicker-popup]');

    dateInputs.forEach(input => {
        // Remove any existing listeners to prevent duplicates
        input.removeEventListener('click', handleDateInputClick);
        input.removeEventListener('focus', handleDateInputClick);

        // Add both click and focus handlers for better UX
        input.addEventListener('click', handleDateInputClick);
        input.addEventListener('focus', handleDateInputClick);
    });
}

function handleDateInputClick(e) {
    e.preventDefault();

    const input = e.target;
    const fieldName = input.name;

    try {
        // Create a unique message handler to listen for the date picker result from the parent window
        // This ensures we only respond to the correct date picker response for this specific field
        const messageHandler = function(event) {
            if (event.data &&
                event.data.method === 'datePickerResult' &&
                event.data.fieldName === fieldName) {

                window.removeEventListener('message', messageHandler);

                if (event.data.value !== null) {
                    input.value = event.data.value;
                    // Move cursor to end of input
                    input.setSelectionRange(input.value.length, input.value.length);
                }
            }
        };

        // Add the listener to handle the date picker result
        window.addEventListener('message', messageHandler);

        // Send the request to parent window to open the date picker
        callParent('openDatePicker', {
            fieldName: fieldName,
            value: input.value
        });

    } catch (err) {
        console.error('Date picker error:', err);
        services.displayErrorMessage(err);
    }
}

function loadEmbeddedForm(
    isStartForm,
    isGeneratedForm,
    referenceId,
    embeddedContainer,
    formContainer,
    parentConfig,
    config
) {
    const headers = {
        authorization: parentConfig.authToken
    };
    
    // Add X-Process-Engine header if engine is specified
    // Note: The CamSDK.Client will handle the engine path internally based on the engine parameter
    if (parentConfig.engineName) {
        headers['X-Process-Engine'] = parentConfig.engineName;
    }
    
    var client = new CamSDK.Client({
        mock: false,
        apiUri: config.engineRestPath || '/engine-rest',
        headers: headers,
        engine: parentConfig.engineName || 'default'
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
            let formConfig = {
                client: client,
                done: function(err, form) {
                  if (err) {
                      reject(err);
                    } else if (form) {
                        resolve(form);
                    }
                }
            };
            if (formInfo.key.includes('deployment:')) {
                let resource = await loadDeployedForm(client, isStartForm, referenceId);
                formContainer.innerHTML = resource;
                formConfig.formElement = $(formContainer);
                if (embeddedContainer) embeddedContainer.style.display = 'none';
            } else if (formInfo.key.includes('/rendered-form')) {
                // Load Camunda generated form HTML and normalize it for Vue integration
                let resource = await loadGeneratedForm(isStartForm, referenceId, formContainer, client, config);
                formContainer.innerHTML = resource;
                formConfig.formElement = $(formContainer);
                if (embeddedContainer) embeddedContainer.style.display = 'none';
            } else {
                // Start with a relative url and replace doubled slashes if necessary
                var url = formInfo.key
                    .replace('embedded:', '')
                    .replace('app:', (formInfo.contextPath || '') + '/')
                    .replace(/^(\/+|([^/]))/, '/$2')
                    .replace(/\/\/+/, '/');
                formConfig.formUrl = url;
                formConfig.containerElement = $(embeddedContainer);
                if (formContainer) formContainer.style.display = 'none';
            }

            if (isStartForm) {
                formConfig.processDefinitionId = referenceId;
            } else {
                formConfig.taskId = referenceId;
            }
            new CamSDK.Form(formConfig);
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

function loadGeneratedForm(isStartForm, referenceId, formContainer, client, config) {
    // Fetches Camunda generated form HTML from the engine REST API and normalizes it by removing unsupported Angular components
    return new Promise((resolve, reject) => {
        if (isStartForm) {
            client.http.get(`process-definition/${referenceId}/rendered-form`, {
            data: {
                noCache: Date.now(),
                userId: config.userId,
                engineName: config.engineName
            },
            headers: {
                'Accept': '*/*'
            },
            done: function(err, renderedFormHtml) {
                if (err) {
                    console.error('Error getting rendered form:', err);
                    reject(err);
                } else {
                    var updatedHtml = normalizeGeneratedFormHtml(renderedFormHtml);
                    resolve(updatedHtml);
                }
            }
        });
        }
        else {
            const Task = client.resource('task');
            Task.http.get(`task/${referenceId}/rendered-form`, {
                data: {
                    noCache: Date.now(),
                    userId: config.userId,
                    engineName: config.engineName,
                    taskId: referenceId
                },
                headers: {
                    'Accept': '*/*'
                },
                done: function (err, renderedFormHtml) {
                    if (err) {
                        console.error('Error getting rendered form:', err);
                        reject(err);
                    } else {
                        var updatedHtml = normalizeGeneratedFormHtml(renderedFormHtml);
                        resolve(updatedHtml);
                    }
                }
            });
        }
    });
}

function normalizeGeneratedFormHtml(htmlString) {
    const parser = new DOMParser();
    const doc = parser.parseFromString(htmlString, 'text/html');
    const selects = doc.querySelectorAll('select.form-control');
    selects.forEach(select => {
        select.classList.add('form-select');
    });

    // Remove all elements with class="help-block"
    const helpBlocks = doc.querySelectorAll('.help-block');
    helpBlocks.forEach(el => el.remove());

    // Add asterisk (*) to labels of required fields
    const requiredInputs = doc.querySelectorAll('input[required="true"], select[required="true"]');
    requiredInputs.forEach(input => {
        const fieldId = input.id || input.name;
        if (fieldId) {
            const label = doc.querySelector(`label[for="${fieldId}"]`);
            if (label && !label.textContent.trim().endsWith('*')) {
                label.innerHTML = label.innerHTML.trim() + ' <span style="color: red;">*</span>';
            }
        }
    });

    // Select all Angular date inputs (with uib-datepicker-popup)
    const dateInputs = doc.querySelectorAll('input[uib-datepicker-popup]');

    // Patch date inputs by removing the calendar button and associated scripts
    dateInputs.forEach(originalInput => {
        const fieldName = originalInput.name;
        // Remove adjacent .input-group-btn (calendar button)
        const inputGroup = originalInput.closest('.input-group');
        if (inputGroup) {
            const btn = inputGroup.querySelector('.input-group-btn');
            if (btn) btn.remove();
        }

        // Remove <script cam-script> associated with the field
        const scripts = doc.querySelectorAll('script[cam-script]');
        scripts.forEach(script => {
            if (script.textContent.includes(`open${fieldName}`)) {
                script.remove();
            }
        });
    });

    return doc.body.innerHTML;
}
