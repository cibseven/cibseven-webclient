<template>
  <div class="row">
    <span class="text-secondary font-weight-bold col-8 pe-0">{{ $t('process.details.versionTag') }}</span>
    <span class="col-4 text-end">{{ version.versionTag }}</span>
  </div>
  <hr class="my-2">
  <div class="row">
    <span class="text-secondary font-weight-bold col-12">
      {{ $t('process.details.definitionId') }}
      <button @click="copyValueToClipboard(version.id)" class="btn btn-sm mdi mdi-content-copy float-end"
        :title="$t('process.details.copyValue')"></button>
    </span>
    <span class="col-12">{{ version.id }}</span>
  </div>
  <hr class="my-2">
  <div class="row">
    <span class="text-secondary font-weight-bold col-12">
      {{ $t('process.details.definitionKey') }}
      <button @click="copyValueToClipboard(version.key)" class="btn btn-sm mdi mdi-content-copy float-end"
        :title="$t('process.details.copyValue')"></button>
    </span>
    <span class="col-12">{{ version.key }}</span>
  </div>
  <hr class="my-2">
  <div class="row">
    <span class="text-secondary font-weight-bold col-12">
      {{ $t('process.details.definitionName') }}
      <button @click="copyValueToClipboard(version.name)" class="btn btn-sm mdi mdi-content-copy float-end"
        :title="$t('process.details.copyValue')"></button>
    </span>
    <span class="col-12">{{ version.name }}</span>
  </div>
  <hr class="my-2">
  <div class="row">
    <span class="text-secondary font-weight-bold col-12">
      {{ $t('process.details.deploymentId') }}
      <button @click="copyValueToClipboard(version.deploymentId)" class="btn btn-sm mdi mdi-content-copy float-end"
        :title="$t('process.details.copyValue')"></button>
    </span>
    <router-link class="col-12" :to="'/seven/auth/deployments/' + version.deploymentId">{{ version.deploymentId }}</router-link>
  </div>
  <hr class="my-2">
  <div class="row">
    <span class="text-secondary font-weight-bold col-12">
      {{ $t('process.details.tenantId') }}
      <button v-if="version.tenantId" @click="copyValueToClipboard(version.tenantId)" class="btn btn-sm mdi mdi-content-copy float-end"
        :title="$t('process.details.copyValue')"></button>
    </span>
    <span class="col-12">{{ version.tenantId ? version.tenantId : '-' }}</span>
  </div>
  <hr class="my-2">
  <div class="row">
    <span class="text-secondary font-weight-bold col-12">
      {{ $t('process.details.historyTimeToLive') }}
      <button @click="copyValueToClipboard(historyTimeToLive)" class="btn btn-sm mdi mdi-content-copy float-end"
        :title="$t('process.details.copyValue')"></button>
      <button @click="editHistoryTimeToLive()" class="btn btn-sm mdi mdi-pencil float-end"
        :title="$t('process-instance.edit')"></button>
    </span>
    <span class="col-12">{{ historyTimeToLive + ' ' + $t('process.days') }}</span>
  </div>
  <hr class="my-2">
  <div class="row">
    <span class="text-secondary font-weight-bold col-5 pe-0">{{ $t('process.details.firstStart') }}</span>
    <span class="col-7 text-end">{{ getDate('min') }}</span>
  </div>
  <hr class="my-2">
  <div class="row">
    <span class="text-secondary font-weight-bold col-5 pe-0">{{ $t('process.details.lastStart') }}</span>
    <span class="col-7 text-end">{{ getDate('max') }}</span>
  </div>
  <hr class="my-2">
  <div class="row align-items-center">
    <span class="text-secondary font-weight-bold col-8 pe-0">{{ $t('process.details.unfinishedInstances') }}</span>
    <span class="col-4 text-end">{{ version.runningInstances }}</span>
  </div>
  <hr class="my-2">
  <div class="row align-items-center">
    <span class="text-secondary font-weight-bold col-8 pe-0">{{ $t('process.details.totalInstances') }}</span>
    <span class="col-4 text-end">{{ version.allInstances }}</span>
  </div>
  <hr class="my-2">
  <div class="row align-items-center">
    <span class="text-secondary font-weight-bold col-8 pe-0">{{ $t('process.details.finishedInstances') }}</span>
    <span class="col-4 text-end">{{ version.completedInstances }}</span>
  </div>

  <b-modal ref="historyTimeToLive" :title="$t('process.details.historyTimeToLive')">
    <div class="d-flex col-6 align-items-center ps-0">
      <input class="form-control" type="number" v-model="historyTimeToLiveChanged">
      <span class="ms-2">{{ $t('process.days') }}</span>
    </div>
    <template v-slot:modal-footer>
      <b-button @click="$refs.historyTimeToLive.hide()" variant="link">{{ $t('confirm.cancel') }}</b-button>
      <b-button @click="updateHistoryTimeToLive()" variant="primary">{{ $t('process-instance.save') }}</b-button>
    </template>
  </b-modal>

  <SuccessAlert ref="messageCopy"> {{ $t('process.copySuccess') }} </SuccessAlert>
</template>

<script>
import moment from 'moment'
import { ProcessService } from '@/services.js'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'

export default {
  name: 'ProcessDefinitionDetails',
  components: { SuccessAlert },
  mixins: [ copyToClipboardMixin ],
  props: {
    instances: Array,
    version: Object
  },
  data: function() {
    return {
      versions: [],
      historyTimeToLive: '',
      historyTimeToLiveChanged: ''
    }
  },
  emits: ['onUpdateHistoryTimeToLive'],
  mounted: function() {
    this.historyTimeToLive = this.version.historyTimeToLive
  },
  methods: {
    getDate: function(type) {
      if (this.instances.length === 0) return null
      var date = type === 'min' ?
          Math.min.apply(Math, this.instances.map(i => { return moment(i.startTimeOriginal) })) :
          Math.max.apply(Math, this.instances.map(i => { return moment(i.startTimeOriginal) }))
      return moment(date).format('LL HH:mm')
    },
    editHistoryTimeToLive: function() {
      this.historyTimeToLiveChanged = this.historyTimeToLive
      this.$refs.historyTimeToLive.show()
    },
    updateHistoryTimeToLive: function() {
      var data = { historyTimeToLive: this.historyTimeToLiveChanged || null }
      ProcessService.updateHistoryTimeToLive(this.version.id, data).then(() => {
        this.historyTimeToLive = data.historyTimeToLive
        this.$refs.historyTimeToLive.hide()
        this.$emit('onUpdateHistoryTimeToLive', this.version.id, data.historyTimeToLive);
      })
    }
  }
}
</script>
