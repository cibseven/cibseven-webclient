<template>
  <TemplateBase noDiagramm noTitle :templateMetaData="templateMetaData" :loader="loader">
    <div ref="customForm" v-html="bodyContent"></div>
    <template v-slot:button-row>
      <IconButton icon="check" :disabled="disabled" @click="setVariablesAndSubmit()" variant="secondary" :text="$t('task.actions.submit')"></IconButton>
    </template>
    <div class="h-100 d-flex flex-column overflow-auto" id="form"></div>
  </TemplateBase>
</template>

<script>

import TemplateBase from '@/components/forms/TemplateBase.vue'

import postMessageMixin from '@/components/forms/postMessage.js'

import { FormsService } from '@/services.js'

import IconButton from '@/components/forms/IconButton.vue'

export default {
  name: "CustomForm",
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
      formularContent: null,
      loader: true,
      disabled: false,
      form: null,
      dataToSubmit: {},
      closeTask: true,
      bodyContent: null
    }
  },
  created: function() {
    this.loadForm()
  },
  methods: {
    loadForm: function() {
      this.loader = false
      fetch('forms/start-form.html')
              .then(res => res.text())
              .then(html => {

            // Usamos DOMParser para parsear el HTML de manera eficiente
            const parser = new DOMParser()
            const doc = parser.parseFromString(html, 'text/html')

            console.log(doc.body.innerHTML)
            this.bodyContent = doc.body.innerHTML
      })
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
