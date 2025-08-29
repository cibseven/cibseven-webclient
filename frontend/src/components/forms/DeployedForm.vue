<!--

    Copyright CIB software GmbH and/or licensed to CIB software GmbH
    under one or more contributor license agreements. See the NOTICE file
    distributed with this work for additional information regarding copyright
    ownership. CIB software licenses this file to you under the Apache License,
    Version 2.0; you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.

-->
<template>
  <TemplateBase ref="templateBase" noDiagramm noTitle :templateMetaData="templateMetaData" :loader="loader">
    <template v-slot:button-row>
      <IconButton icon="check" :disabled="disabled" @click="setVariablesAndSubmit()" variant="secondary" :text="$t('task.actions.submit')"></IconButton>
    </template>
    <div class="h-100 d-flex flex-column overflow-auto" id="form"></div>
  </TemplateBase>
</template>

<script>

import TemplateBase from '@/components/forms/TemplateBase.vue'

import postMessageMixin from '@/components/forms/postMessage.js'

import { FormsService, ProcessService, TemplateService, TaskService, getServicesBasePath } from '@/services.js'

import IconButton from '@/components/forms/IconButton.vue'

import { Form } from '@bpmn-io/form-js'
import '@bpmn-io/form-js/dist/assets/form-js.css'

import { convertFormDataForFormJs, updateEndpointCredentials } from './formJsUtils.js'

/**
 * Property key used to bind file picker components to document preview components
 * 
 * When a file picker component has this property, it creates a document reference variable
 * that can be consumed by document preview components. The workflow is:
 * 
 * 1. File picker with document-reference-key uploads a file
 * 2. A document reference variable is created with the name specified in document-reference-key
 * 3. Document preview components can bind to this variable to show the uploaded file
 * 
 * Example:
 * - File picker has property: document-reference-key = "myDocumentRef"
 * - After file upload, variable "myDocumentRef" contains document reference data
 * - Document preview component binds to variable "myDocumentRef" to display the file
 */
const DOCUMENT_REFERENCE_KEY = 'document-reference-key'

/**
 * Component type identifier for file picker components
 */
const FILEPICKER_TYPE = 'filepicker'

export default {
  name: "DeployedForm",
  mixins: [postMessageMixin],
  components: { TemplateBase, IconButton },
  props: {
    locale: { type: String, default: 'en' },
    taskId: String,
    token: String,
    theme: String, // unused
    translation: String // unused
  },
  data: function() {
    return {
      templateMetaData: null,
      loader: true,
      disabled: false,
      form: null,
      dataToSubmit: {},
      closeTask: true,
      pendingDocumentReferences: {} // Store document reference data to be saved during submission
    }
  },
  created: function() {
    this.loadForm()
  },
  methods: {
    /**
     * Check if form has file picker components with document-reference-key property
     * @param {Object} formContent - Form content object
     * @returns {boolean} True if form has file picker with document-reference-key
     */
    hasFilePickerWithDocumentReferenceKey(formContent) {
      if (!formContent || !Array.isArray(formContent.components)) return false;
      return formContent.components.some(component => 
        component.type === FILEPICKER_TYPE && 
        component.properties && 
        component.properties[DOCUMENT_REFERENCE_KEY]
      );
    },
    /**
     * Creates a document reference object for file preview components
     * 
     * This method generates a document reference structure that can be consumed by
     * document preview components in form-js. The reference includes the endpoint
     * URL for accessing the file data and metadata about the file.
     * 
     * @param {string} variableName - The variable name used to store the file
     * @param {File} file - The uploaded file object containing type and name properties
     * @returns {Array} An array containing a single document reference object with:
     *   - documentId: The variable name identifier
     *   - endpoint: Full URL to access the file data via REST API
     *   - metadata: Object containing contentType and fileName
     */
    createDocumentReference(variableName, file) {
      return [{
        documentId: variableName,
        endpoint: `${window.location.origin}/${getServicesBasePath()}/process/process-instance/${this.templateMetaData.task.processInstanceId}/variables/${variableName}/data`,
        metadata: {
          contentType: file.type,
          fileName: file.name
        }
      }];
    },
    loadForm: async function() {
      this.loader = false
      try {
        const template = await TemplateService.getTemplate('CibsevenFormUiTask', this.taskId, this.locale, this.token)
        this.templateMetaData = template
        
        // Load form content
        const formContent = await TaskService.getDeployedForm(this.taskId)
        
        // Load form variables
        const formData = await FormsService.fetchVariables(this.taskId, false)

        // Convert the service response format to the format expected by form-js
        const convertedFormData = convertFormDataForFormJs(formData)
        
        // Inject authentication tokens into document reference endpoints to enable secure file preview
        // This ensures that document preview components can access files without authentication errors
        updateEndpointCredentials(convertedFormData, this.$root.user.authToken)

        this.form = new Form({
            container: document.querySelector('#form'),
        })
        await this.form.importSchema(formContent, convertedFormData)

        // Wait for DOM to be updated after form import
        await this.$nextTick()

        // Find all file input fields in the form to attach change listeners for file upload handling
        const fileInputs = document.querySelectorAll('#form input[type="file"]');
        if (fileInputs.length > 0) {
          fileInputs.forEach(fileInput => {
            fileInput.addEventListener('change', async (e) => {
              this.$refs.templateBase.handleFileSelection(e, fileInput, formContent);

              for (const [variableName, file] of Object.entries(this.$refs.templateBase.formFiles)) {

                // Check if this file picker has document-reference-key property
                const filePickerComponent = formContent.components.find(component =>
                  component.type === FILEPICKER_TYPE &&
                  component.key === variableName &&
                  component.properties &&
                  component.properties[DOCUMENT_REFERENCE_KEY]
                );

                const hasDocumentReferenceKey = !!filePickerComponent;
                if (hasDocumentReferenceKey) {
                  // Upload the selected file to the backend and associate it with the process variable
                  await FormsService.uploadVariableFileData(this.taskId, variableName, file, 'File');

                  // Create a new variable with file data source information
                  const documentReference = this.createDocumentReference(variableName, file);

                  // Get the document reference variable name
                  const documentReferenceVariableName = filePickerComponent.properties[DOCUMENT_REFERENCE_KEY];
                  // Store for later submission (will be saved during setVariablesAndSubmit)
                  this.pendingDocumentReferences[documentReferenceVariableName] = {
                    name: documentReferenceVariableName,
                    type: 'Json',
                    value: JSON.stringify(documentReference), // Store the file data source array as JSON
                    valueInfo: {
                      objectTypeName: 'java.lang.String',
                      serializationDataFormat: 'application/json'
                    }
                  };;

                  // Update fileDataSource endpoint with authentication token and cache bust
                  updateEndpointCredentials({ documentReference }, this.$root.user.authToken);

                  // Direct state update (more reliable for document previews)
                  const formState = this.form._getState();
                  formState.data[documentReferenceVariableName] = documentReference;
                  this.form._setState(formState);
                }
              }
            });
          });
        }

      } catch (error) {
        console.error('Error loading form:', error);
        this.sendMessageToParent({ method: 'displayErrorMessage', message: error.message || 'An error occurred during form loading' })
        this.loader = false;

      }
    },
    setVariablesAndSubmit: async function() {
      try {
        this.dataToSubmit = {}
        var result = this.form.submit()
        if (Object.keys(result.errors).length > 0 && this.closeTask) return

        // To submit file variables, we must use separate endpoint before or after submitting non-file form data.
        for (const [variableName, file] of Object.entries(this.$refs.templateBase.formFiles)) {
          await FormsService.uploadVariableFileData(this.taskId, variableName, file, 'File');
        }

        // Process and submit non-file form fields (files have been uploaded separately)
        Object.entries(result.data).forEach(([key, value]) => {
          if (!this.$refs.templateBase.formFiles[key]) {
            this.dataToSubmit[key] = {
                  name: key,
                  type: typeof value,
                  value: value,
                  valueInfo: null
              }
          }
        })
        
        // Save pending document reference variables to bind file picker and document preview components
        // These variables contain the endpoint URLs and metadata needed for document preview functionality
        for (const [variableName, documentReferenceData] of Object.entries(this.pendingDocumentReferences)) {
          await ProcessService.putLocalExecutionVariable(
            this.templateMetaData.task.processInstanceId,
            variableName,
            documentReferenceData
          );
        }
        
        const data = await FormsService.submitVariables(this.templateMetaData.task, Object.values(this.dataToSubmit), this.closeTask);
        if (this.closeTask) this.sendMessageToParent({ method: 'completeTask', task: data })
        else this.sendMessageToParent({ method: 'displaySuccessMessage' })
        this.loader = false
      } catch (error) {
        console.error('Error during form submission:', error)
        this.sendMessageToParent({ method: 'displayErrorMessage', message: error.message || 'An error occurred during form submission' })
        this.loader = false
      } finally {
        this.closeTask = true
      }
    }
  }
}
</script>
