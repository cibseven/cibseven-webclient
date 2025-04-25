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

import { FormsService, TemplateService } from '@/services.js'
import IconButton from '@/components/forms/IconButton.vue'

import { Form } from '@bpmn-io/form-js'

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
    loadForm: function() {
      TemplateService.getStartFormTemplate('CibsevenFormUiTask', this.processDefinitionId,
        this.locale, this.token).then(template => {
          this.loader = false
          this.templateMetaData = template
          var formContent = JSON.parse(template.variables.formularContent.value)
          this.formularContent = formContent
          this.form = new Form({
              container: document.querySelector('#form'),
          })
          this.form.importSchema(this.formularContent)
      })
    },
    setVariablesAndSubmit: function() {
      this.dataToSubmit = {}
      var result = this.form.submit()
      if (Object.keys(result.errors).length > 0) return
      Object.entries(result.data).forEach(([key, value]) => {
        this.dataToSubmit[key] = {
              name: key,
              type: typeof value,
              value: value,
              valueInfo: null
          }
      })
      this.dataToSubmit.initiator = { name: 'initiator', type: 'string', value: this.$root.user.userID }
      Object.keys(this.dataToSubmit).forEach(key => {
        if (this.dataToSubmit[key].value === null) delete this.dataToSubmit[key]
      })
      FormsService.submitStartFormVariables(this.processDefinitionId,
        Object.values(this.dataToSubmit), this.locale)
      .then(data => {
        this.sendMessageToParent({ method: 'completeTask', task: data })
        this.loader = false
      }, e => {
        this.sendMessageToParent({ method: 'displayErrorMessage', status: e.response.status })
        this.loader = false
      })
    }
  }
}
</script>
