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
  <TemplateBase noDiagramm noTitle :templateMetaData="templateMetaData" :loader="loader">
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
        this.form.importSchema(formContent, convertedFormData)
      } catch (error) {
        console.error('Error loading form:', error)
        this.loader = false
      }
    },
    saveForm: function() {
      this.closeTask = false
      this.setVariablesAndSubmit()
    },
    setVariablesAndSubmit: function() {
      this.dataToSubmit = {}
      var result = this.form.submit()
      if (Object.keys(result.errors).length > 0 && this.closeTask) return
      Object.entries(result.data).forEach(([key, value]) => {
        this.dataToSubmit[key] = {
              name: key,
              type: typeof value,
              value: value,
              valueInfo: null
          }
      })

      return FormsService.submitVariables(this.templateMetaData.task, Object.values(this.dataToSubmit), this.closeTask).then(data => {
        if (this.closeTask) this.sendMessageToParent({ method: 'completeTask', task: data })
        else this.sendMessageToParent({ method: 'displaySuccessMessage' })
        this.loader = false
      }, e => {
        this.sendMessageToParent({ method: 'displayErrorMessage', status: e.response.status })
        this.loader = false
      }).finally(() =>{
        this.closeTask = true
      })
    }
  }
}
</script>
