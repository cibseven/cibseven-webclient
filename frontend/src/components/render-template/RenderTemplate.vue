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
    <RenderIframe v-if="['camundaForm', 'embedded'].includes(taskType)" :task="task" @complete-task="completeTask" @saved="$refs.messageSaved.show(10)" @generic-success="$refs.messageSuccess.show(10)" @error="displayErrorMessage"
      @cancel="cancelTask" @filter-update="updateFilters"></RenderIframe>
    <RenderEuit v-else-if="taskType === 'euit'" :task="task" @complete-task="completeTask" @error="displayErrorMessage"
      @cancel="cancelTask"></RenderEuit>
    <div v-else class="h-100">
      <div class="pt-2">
        <span class="small-text d-none d-sm-inline" style="vertical-align: middle">
          <strong>{{ $t('task.emptyTask') }}</strong> |
        </span>
        <IconButton icon="check" @click="completeEmptyTask()" variant="secondary" :text="$t('task.actions.submit')"></IconButton>
      </div>
      <SuccessAlert top="0" style="z-index: 1031" ref="messageSaved"> {{ $t('alert.successSaveTask') }}</SuccessAlert>
      <SuccessAlert top="0" style="z-index: 1031" ref="messageSuccess"> {{ $t('alert.successOperation') }}</SuccessAlert>
      <b-modal ref="datePickerModal" :title="$t('deployed-form.selectDate')">
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
          <b-button @click="$refs.datePickerModal.hide()" variant="light">{{ $t('confirm.cancel') }}</b-button>
          <b-button @click="onDatePickerConfirm()" variant="primary">{{ $t('confirm.ok') }}</b-button>
        </template>
      </b-modal>
    </div>
  </div>
</template>

<script>
import { permissionsMixin } from '@/permissions.js'
import { TaskService } from '@/services.js'
import IconButton from '@/components/forms/IconButton.vue'
import { SuccessAlert } from '@cib/common-frontend'
import RenderIframe from '@/components/render-template/RenderIframe.vue'
import RenderEuit from '@/components/render-template/RenderEuit.vue'

export default {
  name: 'RenderTemplate',
  components: { IconButton, SuccessAlert, RenderIframe, RenderEuit },
  props: ['task'],
  emits: ['complete-task'],
  mixins: [permissionsMixin],
  inject: ['currentLanguage', 'AuthService'],
  computed: {
    taskType: function() {
      if (!this.task.formKey) return 'empty'
      if (this.task.formKey.startsWith('euit:')) return 'euit'
      if (this.task.formKey.startsWith('embedded:')) return 'embedded'
      if (this.task.camundaFormRef) return 'camundaForm'
      return 'empty'
    }
  },
  methods: {
    completeEmptyTask: function() {
      TaskService.submit(this.task.id).then(() => {
        this.completeTask()
      })
    },
    completeTask: function(task) {
      this.submitForm = true
      const data = JSON.parse(JSON.stringify(this.task))
      if (task) data.processInstanceId = task.id
      if (this.task.isStartform) {
        this.$emit('complete-task', data)
        this.submitForm = false
      } else {
        this.$emit('complete-task', data)
        this.$router.push('/seven/auth/tasks/' + this.$route.params.filterId)
        this.submitForm = false
      }
    },
    displayErrorMessage: function(params) {
      let type = ''
      const errorParams = []
      switch (params && params.status) {
        case 404:
          if (params.type !== 'generic') {
            type = 'taskSelectedNotExist'
          } else type = 'NoObjectFoundException'
          break
        case 400:
          type = 'AccessDeniedException'
          errorParams.push(this.task.id)
          break
        case 500:
          type = 'EUiTNotFoundException'
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
      const selectedFilter = this.$store.state.filter.list.find(f => {
        return f.id === filterId
      })
      if (selectedFilter) {
        this.$store.state.filter.selected = selectedFilter
        localStorage.setItem('filter', JSON.stringify(selectedFilter))
        const path = '/seven/auth/tasks/' + selectedFilter.id +
          (this.$route.params.taskId ? '/' + this.$route.params.taskId : '')
        if (this.$route.path !== path) this.$router.replace(path)
      }
    }
  }
}
</script>
