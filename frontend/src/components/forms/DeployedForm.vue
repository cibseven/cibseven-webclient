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

import { convertFormDataForFormJs } from './formJsUtils.js'

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
            });
          });
        }

      } catch (error) {
        console.error('Error loading form:', error);
        this.sendMessageToParent({ method: 'displayErrorMessage', message: error.message || 'An error occurred during form loading' })
        this.loader = false;

      }
    },
    saveForm: function() {
      this.closeTask = false
      this.setVariablesAndSubmit()
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
                  type: this.determineValueTypeFromSchema(key, value),
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
        this.sendMessageToParent({ method: 'displayErrorMessage', message: error.message || 'An error occurred during form submission' })
        this.loader = false
      } finally {
        this.closeTask = true
      }
    },
    determineValueTypeFromSchema: function(fieldKey, value) {
      // Find the field definition in the form schema
      const fieldDef = this.findFieldByKey(this.formularContent, fieldKey)
      
      if (fieldDef) {
        switch (fieldDef.type) {
          case 'number':
            // Check if it has decimal digits to determine Integer vs Double
            if (fieldDef.decimalDigits && fieldDef.decimalDigits > 0) {
              return 'Double'
            } else {
              return Number.isInteger(Number(value)) ? 'Integer' : 'Double'
            }
          case 'checkbox':
            return 'Boolean'
          case 'select':
            return 'String'
          case 'textfield':
          case 'textarea':
            return 'String'
          case 'datetime':
            return 'Date'
          default:
            return 'String'
        }
      }
      
      // Fallback to original logic if field not found in schema
      return this.determineValueType(value)
    },
    
    findFieldByKey: function(schema, key) {
      if (!schema || !schema.components) return null
      
      for (const component of schema.components) {
        if (component.key === key) {
          return component
        }
        // Recursively search in nested components if they exist
        if (component.components) {
          const found = this.findFieldByKey(component, key)
          if (found) return found
        }
      }
      return null
    },
    determineValueType: function(value) {
      // Handle null and undefined
      if (value === null || value === undefined) {
        return 'String'
      }
      
      // If it's already a number, return appropriate type
      if (typeof value === 'number') {
        return Number.isInteger(value) ? 'Integer' : 'Double'
      }
      
      // If it's a boolean, return Boolean
      if (typeof value === 'boolean') {
        return 'Boolean'
      }
      
      // If it's a string, try to determine if it represents a number
      if (typeof value === 'string') {
        // Check if it's a valid number
        const numValue = Number(value)
        if (!isNaN(numValue) && value.trim() !== '') {
          // Check if it's an integer or decimal
          if (Number.isInteger(numValue)) {
            return 'Integer'
          } else {
            return 'Double'
          }
        }
        
        // Check if it's a boolean string
        if (value.toLowerCase() === 'true' || value.toLowerCase() === 'false') {
          return 'Boolean'
        }
      }
      
      // Default to String for everything else
      return 'String'
    }
  }
}
</script>
