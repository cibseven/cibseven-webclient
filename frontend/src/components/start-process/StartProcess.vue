<template>
  <GlobalEvents v-if="!hideProcessSelection" @keydown.ctrl.alt.p.prevent="$refs.startProcess.show()"></GlobalEvents>
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
            <label class="btn border-end-0 border-light" style="cursor: default"><span class="mdi mdi-magnify"
              style="line-height: initial"></span></label>
          </div>
          <input class="form-control border-start-0 border-light ps-0" type="text" :placeholder="$t('searches.search')" v-model="processesFilter" :disabled="isStartingProcess">
        </div>
      </div>
      <b-list-group v-if="startableProcesses.length > 0" >
        <b-list-group-item v-for="process of startableProcesses" :key="process.key" class="p-1 d-flex align-items-center justify-content-between">
          <b-button :disabled="isStartingProcess" variant="link" @click="startProcess(process)">
            <HighlightedText :text="process.name ? process.name : process.key" :keyword="processesFilter"></HighlightedText>
          </b-button>
          <BWaitingBox v-if="isStartingProcess && process.loading" class="d-inline ms-auto me-1" styling="width: 24px"></BWaitingBox>
        </b-list-group-item>
      </b-list-group>
      <div v-else class="container">
        <h5 class="text-secondary text-center">{{ $t(this.textEmptyProcessesList()) }}</h5>
      </div>
    </div>
    <template v-slot:modal-footer>
      <b-button v-if="startParamUrl && !hideProcessSelection" @click="startParamUrl = ''" class="text-secondary" variant="link">{{ $t('process.back') }}</b-button>
    </template>
  </b-modal>
</template>

<script>
import { ProcessService } from '@/services.js'
import RenderTemplate from '@/components/render-template/RenderTemplate.vue'
import { BWaitingBox } from 'cib-common-components'
import HighlightedText from '@/components/common-components/HighlightedText.vue'

export default {
  components: { RenderTemplate, BWaitingBox, HighlightedText },
  inject: ['currentLanguage'],
  emits: ['process-started', 'display-popover'],
  props: { hideProcessSelection: Boolean },
  data: function() {
    return {
      isStartingProcess: false,
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
      this.isStartingProcess = true
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
              this.isStartingProcess = false
            })

          } else {

            var templateType

            if (url.camundaFormRef) {
              templateType = 'start-deployed-form'
            } else {
              templateType = url.key.split('?template=')[1]
            }

            this.startParamUrl = window.location.origin + '/webapp/#' +
              '/' + templateType +
              '/' + this.currentLanguage() +
              '/' + processLatest.id +
              '/' + this.$root.user.authToken

            if (this.hideProcessSelection) this.$refs.startProcess.show()
            process.loading = false
            this.isStartingProcess = false
          }
        })
      })
    },
    textEmptyProcessesList: function() {
      return this.processesFilter === '' ? 'process.emptyProcessList' : 'process.emptyProcessListFiltered'
    }
  },
  beforeUnmount: function() {
    this.$eventBus.off('openStartProcess', this.show)
  }
}
</script>
