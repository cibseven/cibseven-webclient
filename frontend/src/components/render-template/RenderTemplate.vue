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
      <iframe v-show="!submitForm && formFrame" class="h-100" ref="template-frame" frameBorder="0"
        src="" width="100%" height="100%" :style="fullModeStyles"></iframe>
      <div class="pt-2" v-if="!formFrame">
        <span class="small-text d-none d-sm-inline" style="vertical-align: middle">
          <strong>{{ $t('task.emptyTask') }}</strong> |
        </span>
        <IconButton icon="check" @click="completeEmptyTask()" variant="secondary" :text="$t('task.actions.submit')"></IconButton>
      </div>
      <SuccessAlert top="0" style="z-index: 1031" ref="messageSaved"> {{ $t('alert.successSaveTask') }}</SuccessAlert>
      <SuccessAlert top="0" style="z-index: 1031" ref="messageSuccess"> {{ $t('alert.successOperation') }}</SuccessAlert>
    </div>
  </div>
</template>

<script>
import { permissionsMixin } from '@/permissions.js'
import { TaskService } from '@/services.js'
import IconButton from '@/components/render-template/IconButton.vue'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'
import { BWaitingBox } from 'cib-common-components'

export default {
  name: 'RenderTemplate',
  components: { IconButton, SuccessAlert, BWaitingBox },
  props: ['task'],
  mixins: [permissionsMixin],
  inject: ['currentLanguage', 'AuthService'],
  data: function() {
    return {
      userInstruction: null,
      formReference: null,
      height: 0,
      submitForm: false,
      formFrame: true,
      loader: false
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
    loadIframe: function() {
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
        formFrame.src = `embedded-forms.html?processDefinitionId=${this.task.processDefinitionId}&lang=${this.currentLanguage()}&authToken=${this.$root.user.authToken}`
        
        this.loader = false
      } else if (this.task.id) {
        
        //Embedded forms if not "standard" ui-element-templates
        if (this.task.formKey && this.task.formKey.startsWith('embedded:') && this.task.formKey !== 'embedded:/camunda/app/tasklist/ui-element-templates/template.html') {
          this.formFrame = true
          formFrame.src = `embedded-forms.html?taskId=${this.task.id}&lang=${this.currentLanguage()}&authToken=${this.$root.user.authToken}`
          this.loader = false
        } else {
          let formReferencePromise
          //Camunda Forms
          if (this.task.camundaFormRef) {
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
    displayErrorMessage: function(params) {
      var type = ''
      var errorParams = []
      switch (params.status) {
        case 404:
          if (params.type !== 'generic') {
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
      if (params.status === 404 && params.type !== 'generic')
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
        else if (e.data.method === 'displayErrorMessage') this.displayErrorMessage(e.data)
        else if (e.data.method === 'cancelTask') this.cancelTask()
        else if (e.data.method === 'updateFilters') this.updateFilters(e.data)
//					var res = this[e.data.method](e.data)
//					if (e.data.callback) {
//						formFrame.contentWindow.postMessage({
//						    'callback': e.data.callback,
//							'result': res
//						}, '*');
//					}
      }
    },
    onBeforeUnload: function() {
      var formFrame = this.$refs['template-frame']
      if (formFrame) {
        formFrame.contentWindow.postMessage({ type: 'contextChanged' }, '*');
        this.loadIframe()
      }
    }
  },
  beforeUnmount: function() {
    window.removeEventListener('message', this.processMessage)
  }
}
</script>
