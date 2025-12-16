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
  <div>
    <BWaitingBox v-if="loader" class="h-100 d-flex justify-content-center" ref="loader" styling="width:20%"></BWaitingBox>
    <div v-show="!loader" class="h-100">
      <iframe v-show="!submitForm && formFrame" tabindex="-1" class="h-100" ref="template-frame" frameBorder="0"
        src="" width="100%" height="100%" :style="fullModeStyles"></iframe>
      <div class="pt-2" v-if="!formFrame">
        <span class="small-text d-none d-sm-inline" style="vertical-align: middle">
          <strong>{{ $t('task.emptyTask') }}</strong> |
        </span>
        <IconButton icon="check" @click="completeEmptyTask()" variant="secondary" :text="$t('task.actions.submit')"></IconButton>
      </div>
      <SuccessAlert top="0" style="z-index: 1031" ref="messageSaved"> {{ $t('alert.successSaveTask') }}</SuccessAlert>
      <SuccessAlert top="0" style="z-index: 1031" ref="messageSuccess"> {{ $t('alert.successOperation') }}</SuccessAlert>
      <b-modal ref="datePickerModal">
        <b-calendar
          v-model="datePickerValue"
          value-as-date
          :start-weekday="1"
          :locale="currentLanguage()"
          block
          :label-no-date-selected="$t('cib-datepicker2.noDate')"
          :date-format-options="{ year: 'numeric', month: '2-digit', day: '2-digit' }"
          :label-reset-button="$t('cib-datepicker2.reset')"
          :label-today-button="$t('cib-datepicker2.today')"
          label-help=""
        ></b-calendar>
        <template v-slot:modal-footer>
          <b-button :title="$t('confirm.cancel')" @click="$refs.datePickerModal.hide()" variant="light">{{ $t('confirm.cancel') }}</b-button>
          <b-button :title="$t('confirm.ok')" @click="onDatePickerConfirm()" variant="primary">{{ $t('confirm.ok') }}</b-button>
        </template>
      </b-modal>
    </div>
  </div>
</template>

<script>
import { permissionsMixin } from '@/permissions.js'
import { TaskService } from '@/services.js'
import IconButton from '@/components/render-template/IconButton.vue'
import { SuccessAlert } from '@cib/common-frontend'
import { BWaitingBox } from '@cib/bootstrap-components'
import { ENGINE_STORAGE_KEY } from '@/constants.js'

export default {
  name: 'RenderTemplate',
  components: { IconButton, SuccessAlert, BWaitingBox },
  props: ['task'],
  mixins: [permissionsMixin],
  inject: ['currentLanguage', 'AuthService'],
  emits: ['complete-task'],
  data: function() {
    return {
      userInstruction: null,
      formReference: null,
      height: 0,
      submitForm: false,
      formFrame: true,
      loader: false,
      datePickerValue: null,
      datePickerRequest: null
    }
  },
  watch: {
    task: {
      handler(newVal, oldVal) {
        if (newVal && oldVal && newVal.id !== oldVal.id) {
          this.onBeforeUnload()
        }
      },
      immediate: true
    }
  },
  computed: {
    fullModeStyles: function() {
      if (this.$route.query.fullMode === 'true') {
        return 'position: fixed; top: 0; left: 0; z-index: 1030'
      }
      return ''
    }
  },
  mounted: function() {
    this.loadIframe()
    var formFrame = this.$refs['template-frame']
    window.addEventListener('message', this.processMessage)

    formFrame.setAttribute('allowfullscreen', true)
    formFrame.setAttribute('webkitallowfullscreen', true)
    formFrame.setAttribute('mozallowfullscreen', true)
    formFrame.setAttribute('oallowfullscreen', true)
    formFrame.setAttribute('msallowfullscreen', true)

    //window.addEventListener('beforeunload', this.processMessage)
    window.onbeforeunload = function() {
      this.onBeforeUnload()
    }.bind(this)
  },
  unmounted: function() {
    this.onBeforeUnload()
  },
  methods: {
    loadIframe: async function() {
      this.loader = true
      this.submitForm = false
      this.formFrame = true
      var theme = localStorage.getItem('theme') || this.$root.theme
      var themeContext = ''
      var translationContext = ''
      if (['cib', 'generic'].includes(theme) || !theme) {
        themeContext = encodeURIComponent('bootstrap/bootstrap_4.5.0.min.css?v=1.14.0')
      }
      else {
        translationContext = 'themes/' + theme + '/uiet-translations_'
        themeContext = 'themes/' + theme + '/bootstrap_4.5.0.min.css'
      }

      let formFrame = this.$refs['template-frame']
      //Startforms
      if (this.task.url) {
        formFrame.src = this.task.url + '/' + themeContext + '/' + translationContext

        this.loader = false
      } else if (this.task.isEmbedded && this.task.processDefinitionId) {
        formFrame.src = `embedded-forms.html?processDefinitionId=${this.task.processDefinitionId}&lang=${this.currentLanguage()}`
        this.loader = false
      } else if (this.task.isGenerated && this.task.processDefinitionId) {
        formFrame.src = `embedded-forms.html?generated=true&processDefinitionId=${this.task.processDefinitionId}&lang=${this.currentLanguage()}`
        this.loader = false
      } else if (this.task.id) {
        var form = this.task.formKey || await TaskService.form(this.task.id)
        if (form.key && form.key.includes('/rendered-form')) {
          // Generated forms
          this.formFrame = true
          formFrame.src = `embedded-forms.html?generated=true&taskId=${this.task.id}&lang=${this.currentLanguage()}`
          this.loader = false
        } else  if (this.task.formKey && this.task.formKey.startsWith('embedded:') && this.task.formKey !== 'embedded:/camunda/app/tasklist/ui-element-templates/template.html') {
          //Embedded forms if not "standard" ui-element-templates
          this.formFrame = true
          formFrame.src = `embedded-forms.html?taskId=${this.task.id}&lang=${this.currentLanguage()}`
          this.loader = false
        } else {
          let formReferencePromise
          //Camunda Forms
          if (this.task.camundaFormRef || (this.task.formKey && this.task.formKey.startsWith('camunda-forms:'))) {
            formReferencePromise = Promise.resolve('deployed-form')
          } else {
            formReferencePromise = TaskService.formReference(this.task.id)
          }
          formReferencePromise.then(formReference => {
            //Empty Tasks
            if (formReference === 'empty-task') {
              this.loader = false
              this.formFrame = false
              return
            }
            //Ui-element-templates

            formFrame.src = '#/' + formReference + '/' + this.currentLanguage() + '/' +
            this.task.id + '/' + this.$root.user.authToken + '/' + themeContext + '/' + translationContext

            this.loader = false
          }, () => {
            // Not needed but just in case something changes in the backend method
            this.formFrame = false
            this.loader = false
          })
        }
      }
    },
    getVariables: function() {
      return {
        data: {
          userInstruction: this.userInstruction,
          assignee: this.task.assignee
        }
      }
    },
    completeEmptyTask: function() {
      TaskService.submit(this.task.id).then(() => {
        this.completeTask()
      })
    },
    completeTask: function(task) {
      this.submitForm = true
      var data = JSON.parse(JSON.stringify(this.task))
      if (task) data.processInstanceId = task.id
      if (this.task.url) {
        this.$emit('complete-task', data)
        this.submitForm = false
      } else {
        this.$emit('complete-task', data)
        this.$router.push('/seven/auth/tasks/' + this.$route.params.filterId)
        this.submitForm = false
      }
    },
    displayErrorMessage: function(data) {
      // Show custom error messages when no HTTP status is provided
      if (!data.status) {
        this.$root.$refs.error.show(data)
        return
      }

      // Process HTTP error responses and display corresponding error messages
      var type = ''
      var errorParams = []
      switch (data.status) {
        case 404:
          if (data.type !== 'generic') {
            type = 'taskSelectedNotExist'
          } else type = 'NoObjectFoundException'
          break
        case 400:
          type = 'AccessDeniedException'
          errorParams.push(this.task.id)
          break
        default:
          type = 'errorSaveTask'
      }
      this.$root.$refs.error.show({ type: type, params: errorParams })
      if (data.status === 404 && data.type !== 'generic')
        this.$router.push('/seven/auth/tasks/' + this.$route.params.filterId)
    },
    cancelTask: function() {
      this.$router.push('.')
    },
    updateFilters: function(data) {
      this.AuthService.fetchAuths().then(permissions => {
        this.$root.user.permissions = permissions
        this.$store.dispatch('findFilters').then(response => {
          this.$store.commit('setFilters',
            { filters: this.filtersByPermissions(this.$root.config.permissions.displayFilter, response) })
          if (data.filterId) this.selectFilter(data.filterId)
        })
      })
    },
    selectFilter: function(filterId) {
      var selectedFilter = this.$store.state.filter.list.find(f => {
        return f.id === filterId
      })
      if (selectedFilter) {
        this.$store.state.filter.selected = selectedFilter
        localStorage.setItem('filter', JSON.stringify(selectedFilter))
        var path = '/seven/auth/tasks/' + selectedFilter.id +
          (this.$route.params.taskId ? '/' + this.$route.params.taskId : '')
        if (this.$route.path !== path) this.$router.replace(path)
      }
    },
    processMessage: function(e) {
      var formFrame = this.$refs['template-frame']
      if (e.source === formFrame.contentWindow && e.data.method) {
        if (e.data.method === 'completeTask') this.completeTask(e.data.task)
        else if (e.data.method === 'displaySuccessMessage') this.$refs.messageSaved.show(10)
        else if (e.data.method === 'displayGenericSuccessMessage') this.$refs.messageSuccess.show(10)
        else if (e.data.method === 'displayErrorMessage') this.displayErrorMessage(e.data.data)
        else if (e.data.method === 'cancelTask') this.cancelTask()
        else if (e.data.method === 'updateFilters') this.updateFilters(e.data)
        else if (e.data.method === 'requestConfig') {
          // Securely provide config (auth token + engine) to iframe via postMessage
          const engineName = localStorage.getItem(ENGINE_STORAGE_KEY)
          const config = {
            authToken: this.$root.user.authToken
          }
          if (engineName) {
            config.engineName = engineName
          }
          const response = {
            method: 'configResponse',
            config: config
          }
          formFrame.contentWindow.postMessage(response, '*')
        }
      else if (e.data.method === 'openDatePicker') {
          // Handle date picking request from the iframe
          const data = e.data;
          this.datePickerRequest = {
            fieldName: data.data.fieldName,
            value: data.data.value
          }
          // Parse date from dd/mm/yyyy format
          const date = e.data.data.value;
          if (typeof date === 'string') {
            const dateRegex = /^(\d{2})\/(\d{2})\/(\d{4})$/;
            const match = date.match(dateRegex);
            if (match) {
              const [, day, month, year] = match;
              var dateObject = new Date(year, month - 1, day);
              if (isNaN(dateObject.getTime())) {
                this.datePickerValue = null;
              } else {
                this.datePickerValue = dateObject;
              }
            } else {
              this.datePickerValue = null;
            }
          }

          // Show the date picker modal
          this.$refs.datePickerModal.show();
        }
      }
    },
    /**
     * Handles the date picker confirmation.
     * Formats the selected date as dd/mm/yyyy and sends it back to the iframe.
     */
    onDatePickerConfirm: function() {
      let result = null
      if (this.datePickerValue) {
        let d = new Date(this.datePickerValue)
        // Format as dd/mm/yyyy
        const day = String(d.getDate()).padStart(2, '0')
        const month = String(d.getMonth() + 1).padStart(2, '0')
        const year = d.getFullYear()
        result = `${day}/${month}/${year}`
      }

      // Send result back to iframe
      if (this.datePickerRequest && this.datePickerRequest.fieldName) {
        const response = {
          method: 'datePickerResult',
          fieldName: this.datePickerRequest.fieldName,
          value: result
        }

        // Post message to the iframe
        const formFrame = this.$refs['template-frame']
        if (formFrame && formFrame.contentWindow) {
          formFrame.contentWindow.postMessage(response, '*')
        }
      }

      // Hide the date picker modal
      if (this.$refs.datePickerModal) {
        this.$refs.datePickerModal.hide()
      }

      // Reset date picker state
      this.datePickerValue = null
      this.datePickerRequest = null
    },
    onBeforeUnload: function() {
      var formFrame = this.$refs['template-frame']
      if (formFrame) {
        formFrame.contentWindow.postMessage({ type: 'contextChanged' }, '*')
        this.loadIframe()
      }
    },
    handleScrollIframe(deltaY, x, y) {
      const frame = this.$refs['template-frame']
      const ctx = getIframeContext(frame, x, y)
      if (!ctx) return
      const el = ctx.doc.elementFromPoint(ctx.x, ctx.y)
      findAndScroll(el, deltaY, ctx.x, ctx.y, ctx.doc)
    }
  },
  beforeUnmount: function() {
    window.removeEventListener('message', this.processMessage)
  }
}
function findAndScroll(el, deltaY, iframeX, iframeY, rootDoc) {
      if (!el) return
      do {
        if (el.tagName === 'IFRAME') {
          const ctx = getIframeContext(el, iframeX, iframeY)
          if (ctx) {
            let nestedEl = ctx.doc.elementFromPoint(ctx.x, ctx.y)
            while (nestedEl) {
              const overflowY = ctx.doc.defaultView.getComputedStyle(nestedEl).overflowY
              const isScrollable = overflowY !== 'visible' && overflowY !== 'hidden'
              if ( isScrollable && nestedEl.scrollHeight > nestedEl.clientHeight) {
                nestedEl.scrollTop += deltaY
                return
              }
              nestedEl = nestedEl.parentElement
            } 
            if (ctx.doc.body && ctx.doc.body.scrollHeight > ctx.doc.body.clientHeight) {
              ctx.doc.body.scrollTop += deltaY
            }
            return
          }
        }
        const overflowY = window.getComputedStyle(el).overflowY
        const isScrollable = overflowY !== 'visible' && overflowY !== 'hidden'
        if (isScrollable && el.scrollHeight > el.clientHeight) {
          el.scrollTop += deltaY
          return
        }
        el = el.parentElement
      }  while (el)
      if (rootDoc.body && rootDoc.body.scrollHeight > rootDoc.body.clientHeight) {
        rootDoc.body.scrollTop += deltaY
      }
    }
    function getIframeContext(iframeEl, x, y) {
      if (!iframeEl || !iframeEl.contentWindow) return null
      const doc = iframeEl.contentDocument || iframeEl.contentWindow.document
      if (!doc) return null
      const rect = iframeEl.getBoundingClientRect()
      return {
        iframe: iframeEl,
        doc,
        x: x - rect.left,
        y: y - rect.top
      }
    }
</script>
