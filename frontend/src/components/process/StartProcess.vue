<template>
  <GlobalEvents v-if="!hideProcessSelection" @keydown.ctrl.x.prevent="$refs.startProcess.show()"></GlobalEvents>
  <b-modal body-class="pt-0" size="lg" scrollable ref="startProcess" :hide-footer="!(startParamUrl && !hideProcessSelection)"
    :title="!hideProcessSelection ? $t('start.startProcesses') : processName" @shown="$emit('display-popover', false)" no-close-on-backdrop
    @hidden="$emit('display-popover', true); startParamUrl = ''" :footer-class="{ 'justify-content-between': startParamUrl && !hideProcessSelection }"
    dialog-class="h-100">
    <div v-if="startParamUrl" class="mh-100 h-100" style="height: 70vh">
      <RenderTemplate class="h-100" :task="{ url: startParamUrl, assignee: $root.user, id: selectedProcess.id, processInstanceId: selectedProcess.processInstanceId }"
      @complete-task="$refs.startProcess.hide(); $emit('process-started', $event)"></RenderTemplate>
    </div>
    <div v-else-if="!hideProcessSelection">
      <div class="form-group mt-3">
        <div class="input-group">
          <div class="input-group-append">
            <label class="btn border-end-0 border-light "><span class="mdi mdi-magnify"
              style="line-height: initial"></span></label>
          </div>
          <input class="form-control border-start-0 border-light ps-0" type="text" :placeholder="$t('searches.search')" v-model="processesFilter">
        </div>
      </div>
      <b-list-group>
        <b-list-group-item v-for="process of startableProcesses" :key="process.key" class="p-1">
          <b-button variant="link" @click="startProcess(process)">{{ process.name ? process.name : process.key }}</b-button>
        </b-list-group-item>
      </b-list-group>
    </div>
    <template v-slot:modal-footer>
      <b-button v-if="startParamUrl && !hideProcessSelection" @click="startParamUrl = ''" class="text-secondary" variant="link">{{ $t('process.back') }}</b-button>
    </template>
  </b-modal>
</template>

<script>
import { ProcessService } from '@/services.js'
import RenderTemplate from '@/components/render-template/RenderTemplate.vue'
export default {
  components: { RenderTemplate },
  inject: ['currentLanguage'],
  emits: ['process-started', 'display-popover'],
  props: { hideProcessSelection: Boolean },
  data: function() {
    return {
      processesFilter: '',
      startParamUrl: '',
      selectedProcess: {}
    }
  },
  computed: {
    startableProcesses: function() {
      return this.$store.state.process.list.filter(process => {
        return process.startableInTasklist === true && !process.revoked && process.suspended !== 'true' &&
          (process.name ? process.name.toLowerCase().indexOf(this.processesFilter.toLowerCase()) !== -1 :
          process.key.toLowerCase().indexOf(this.processesFilter.toLowerCase()) !== -1)
      }).sort((objA, objB) => {
        var nameA = objA.name ? objA.name.toUpperCase() : objA.key.toUpperCase()
          var nameB = objB.name ? objB.name.toUpperCase() : objB.key.toUpperCase()
          return nameA < nameB ? -1 : nameA > nameB ? 1 : 0
        })
    },
    processName: function() {
      return this.selectedProcess.name !== null ? this.selectedProcess.name : this.selectedProcess.key
    }
  },
  mounted: function() {
    this.$eventBus.on('openStartProcess', this.show)
  },
  methods: {
    show: function() {
      this.$refs.startProcess.show()
    },
    startProcess: function(process) {
      process.loading = true
      ProcessService.findProcessByDefinitionKey(process.key).then(processLatest => {
        this.selectedProcess = processLatest
        ProcessService.startForm(processLatest.id).then(url => {
          if (!url.key && !url.camundaFormRef) {
            ProcessService.startProcess(processLatest.key, this.currentLanguage()).then(task => {
              this.$refs.startProcess.hide()
              task.processInstanceId = task.id
              this.$emit('process-started', task)
              process.loading = false
            })
          }
          else {
            if (url.camundaFormRef) {
              this.startParamUrl = this.$root.config.uiElementTemplateUrl + '/startform/camunda-form-template' +
                '?processDefinitionKey=' + processLatest.id
            } else {
              this.startParamUrl = this.$root.config.uiElementTemplateUrl + '/startform/' +
              url.key.split('?template=')[1] + '?processDefinitionId=' + processLatest.id +
              '&processDefinitionKey=' + processLatest.key
            }
            if (this.hideProcessSelection) this.$refs.startProcess.show()
            process.loading = false
          }
        })
      })
    }
  },
  beforeUnmount: function() {
    this.$eventBus.off('openStartProcess', this.show)
  }
}
</script>
