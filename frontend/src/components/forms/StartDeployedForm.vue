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

import { FormsService, ProcessService } from '@/services.js'
import IconButton from '@/components/forms/IconButton.vue'

import { Form } from '@bpmn-io/form-js'
import '@bpmn-io/form-js/dist/assets/form-js.css'

import { determineValueTypeFromSchema } from './formJsUtils.js'

import { extractErrorMessage } from '@/utils/error.js'

export default {
  name: "StartDeployedForm",
  mixins: [postMessageMixin],
  components: { TemplateBase, IconButton},
  props: {
    locale: { type: String, default: 'en' },
    processDefinitionId: String,
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
      formSchema: null,
      closeTask: true
    }
  },
  created: function() {
    this.loadForm()
  },
  methods: {
    loadForm: async function() {
      try {
        // Load form content
        const formContent = await ProcessService.getDeployedStartForm(this.processDefinitionId)
        
        this.form = new Form({
          container: document.querySelector('#form'),
        })
        await this.form.importSchema(formContent)
        this.formSchema = formContent

        // Wait for DOM to be updated after form import
        await this.$nextTick()

        // Find all file input fields in the form to attach change listeners for file upload handling
        const fileInputs = document.querySelectorAll('#form input[type="file"]');
        if (fileInputs.length > 0) {
          fileInputs.forEach(fileInput => {
            fileInput.addEventListener('change', async (e) => {
              this.$refs.templateBase.handleFileSelection(e, fileInput, formContent, null, this.form);
            });
          });
        }

        this.loader = false
      } catch (error) {
        console.error('Error loading start form:', error)
        this.sendMessageToParent({ method: 'displayErrorMessage', data: extractErrorMessage(error) })
        this.loader = false
      }
    },
    setVariablesAndSubmit: async function() {
      try {
        this.dataToSubmit = {}
        const result = this.form.submit()
        if (Object.keys(result.errors).length > 0) return

        // Process and submit non-file form fields (files were already uploaded)
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

        // Add files from formFiles to dataToSubmit
        const fileVariables = await this.$refs.templateBase.convertFilesToVariables();
        Object.keys(fileVariables).forEach(key => {
          this.dataToSubmit[key] = fileVariables[key];
        });

        this.dataToSubmit.initiator = { name: 'initiator', type: 'string', value: this.$root.user.userID }
        
        const data = await FormsService.submitStartFormVariables(this.processDefinitionId,
          Object.values(this.dataToSubmit), this.locale);

        this.sendMessageToParent({ method: 'completeTask', task: data })
        this.loader = false
      } catch (error) {
        console.error('Error during form submission:', error)
        this.sendMessageToParent({ method: 'displayErrorMessage', data: extractErrorMessage(error) })
        this.loader = false
      }
    }
  }
}
</script>
