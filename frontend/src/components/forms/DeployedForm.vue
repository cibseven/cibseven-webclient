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

import { FormsService, TemplateService, TaskService } from '@/services.js'

import IconButton from '@/components/forms/IconButton.vue'

import { Form } from '@bpmn-io/form-js'
import '@bpmn-io/form-js/dist/assets/form-js.css'

import { convertFormDataForFormJs, findDocumentPreviewComponents, getDocumentReferenceVariableName, determineValueTypeFromSchema } from './formJsUtils.js'

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
      formSchema: null,
      dataToSubmit: {},
      closeTask: true
    }
  },
  created: function() {
    this.loadForm()
  },
  methods: {
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

        // Find all documentPreview components in the form schema
        const documentPreviewComponents = findDocumentPreviewComponents(formContent)
        documentPreviewComponents.forEach(component => {
          const dataSource = component.dataSource

          // Check if dataSource starts with "=" (indicating a variable reference)
          if (dataSource && dataSource.startsWith('=')) {
            const variableName = dataSource.substring(1) // Remove the "=" prefix
            let documentReferenceKey = variableName

            // Check if there's a file picker component with the same variable name
            const hasFilePicker = formContent.components && formContent.components.some(comp =>
              comp.type === 'filepicker' && comp.key === variableName
            );

            if (hasFilePicker) {
              // Add postfix to create a new variable name for document reference
              documentReferenceKey = getDocumentReferenceVariableName(variableName)

              // Update the component's dataSource to point to the new variable name
              component.dataSource = `=${documentReferenceKey}`
            }

            // Check if this variable exists in original form data and is of type "File"
            if (formData[variableName] && formData[variableName].type === 'File') {
              const fileVariable = formData[variableName]
              // Extract file type and name from the file variable metadata
              const fileType = fileVariable.valueInfo?.mimeType || 'application/octet-stream'
              const fileName = fileVariable.valueInfo?.filename || 'document'

              // Use the unified createDocumentReference method from TemplateBase
              const documentReference = this.$refs.templateBase.createDocumentReference(variableName, fileType, fileName)

              // Store the document reference with the postfix in converted form data
              convertedFormData[documentReferenceKey] = documentReference
            }
          }
        })

        this.form = new Form({
            container: document.querySelector('#form'),
        })
        await this.form.importSchema(formContent, convertedFormData)
        this.formSchema = formContent

        // Wait for DOM to be updated after form import
        await this.$nextTick()

        // Find all file input fields in the form to attach change listeners for file upload handling
        const fileInputs = document.querySelectorAll('#form input[type="file"]');
        if (fileInputs.length > 0) {
          fileInputs.forEach(fileInput => {
            fileInput.addEventListener('change', async (e) => {
              this.$refs.templateBase.handleFileSelection(e, fileInput, formContent, this.taskId, this.form);
            });
          });
        }

      } catch (error) {
        console.error('Error loading form:', error);
        this.sendMessageToParent({ method: 'displayErrorMessage', data: error.message || 'An error occurred during form loading' })
        this.loader = false;

      }
    },
    setVariablesAndSubmit: async function() {
      try {
        this.dataToSubmit = {}
        const result = this.form.submit()
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
                  type: determineValueTypeFromSchema(this.formSchema, key),
                  value: value,
                  valueInfo: null
              }
          }
        })
        
        const data = await FormsService.submitVariables(this.templateMetaData.task, Object.values(this.dataToSubmit), this.closeTask);
        if (this.closeTask) this.sendMessageToParent({ method: 'completeTask', task: data })
        else this.sendMessageToParent({ method: 'displaySuccessMessage' })
        this.loader = false
      } catch (error) {
        console.error('Error during form submission:', error)
        this.sendMessageToParent({ method: 'displayErrorMessage', data: error.message || 'An error occurred during form submission' })
        this.loader = false
      } finally {
        this.closeTask = true
      }
    }
  }
}
</script>
