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

import { FormsService, TemplateService, ProcessService } from '@/services.js'
import IconButton from '@/components/forms/IconButton.vue'

import { Form } from '@bpmn-io/form-js'
import '@bpmn-io/form-js/dist/assets/form-js.css'

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
      formularContent: null,
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
      const template = await TemplateService.getStartFormTemplate('CibsevenFormUiTask', this.processDefinitionId, this.locale, this.token)

      this.loader = false
      this.templateMetaData = template
      var formContent = JSON.parse(template.variables.formularContent.value)
      this.formularContent = formContent
      
      this.form = new Form({
        container: document.querySelector('#form'),
      })
      await this.form.importSchema(this.formularContent)

      // Find all file input fields in the form to attach change listeners for file upload handling
      const fileInputs = document.querySelectorAll('#form input[type="file"]');
      fileInputs.forEach(fileInput => {
        fileInput.addEventListener('change', async (e) => {
          this.$refs.templateBase.handleFileSelection(e, fileInput, this.formularContent);
        });
      });
    },
    setVariablesAndSubmit: async function() {
      this.dataToSubmit = {}
      var result = this.form.submit()
      if (Object.keys(result.errors).length > 0) return

      // Process and submit non-file form fields (files were already uploaded)
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
      this.dataToSubmit.initiator = { name: 'initiator', type: 'string', value: this.$root.user.userID }
      Object.keys(this.dataToSubmit).forEach(key => {
        if (this.dataToSubmit[key].value === null) delete this.dataToSubmit[key]
      })
      
      try {
        const data = await FormsService.submitStartFormVariables(this.processDefinitionId,
          Object.values(this.dataToSubmit), this.locale);

        // To submit a file variable, we must use separate endpoint after starting the process.
        for (const [variableName, file] of Object.entries(this.$refs.templateBase.formFiles)) {
          try {
            await ProcessService.uploadProcessInstanceVariableFileData(data.id, variableName, file, 'File');
          } catch (error) {
            console.error(`Error uploading file for variable ${variableName}:`, error);
          }
        }

        this.sendMessageToParent({ method: 'completeTask', task: data })
        this.loader = false
      } catch (e) {
        this.sendMessageToParent({ method: 'displayErrorMessage', status: e.response.status })
        this.loader = false
      }
    }
  }
}
</script>
