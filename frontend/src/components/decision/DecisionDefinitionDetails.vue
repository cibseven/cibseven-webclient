<template>
  <div class="row">
    <span class="text-secondary font-weight-bold col-8 pe-0">{{ $t('decision.details.versionTag') }}</span>
    <span class="col-4 text-end">{{ version.versionTag }}</span>
  </div>
  <hr class="my-2">
  <div class="row">
    <span class="text-secondary font-weight-bold col-12">
      {{ $t('decision.details.definitionId') }}
      <button @click="copyValueToClipboard(version.id)" class="btn btn-sm mdi mdi-content-copy float-end"
        :title="$t('decision.details.copyValue')"></button>
    </span>
    <span class="col-12">{{ version.id }}</span>
  </div>
  <hr class="my-2">
  <div class="row">
    <span class="text-secondary font-weight-bold col-12">
      {{ $t('decision.details.definitionKey') }}
      <button @click="copyValueToClipboard(version.key)" class="btn btn-sm mdi mdi-content-copy float-end"
        :title="$t('decision.details.copyValue')"></button>
    </span>
    <span class="col-12">{{ version.key }}</span>
  </div>
  <hr class="my-2">
  <div class="row">
    <span class="text-secondary font-weight-bold col-12">
      {{ $t('decision.details.definitionName') }}
      <button @click="copyValueToClipboard(version.name)" class="btn btn-sm mdi mdi-content-copy float-end"
        :title="$t('decision.details.copyValue')"></button>
    </span>
    <span class="col-12">{{ version.name }}</span>
  </div>
  <hr class="my-2">
  <div class="row">
    <span class="text-secondary font-weight-bold col-12">
      {{ $t('decision.details.deploymentId') }}
      <button @click="copyValueToClipboard(version.deploymentId)" class="btn btn-sm mdi mdi-content-copy float-end"
        :title="$t('decision.details.copyValue')"></button>
    </span>
    <router-link class="col-12" :to="'/seven/auth/deployments/' + version.deploymentId">{{ version.deploymentId }}</router-link>
  </div>
  <hr class="my-2">
  <div class="row">
    <span class="text-secondary font-weight-bold col-12">
      {{ $t('decision.details.historyTimeToLive') }}
      <button @click="copyValueToClipboard(historyTimeToLive)" class="btn btn-sm mdi mdi-content-copy float-end"
        :title="$t('decision.details.copyValue')"></button>
      <button @click="editHistoryTimeToLive()" class="btn btn-sm mdi mdi-pencil float-end"
        :title="$t('decision-instance.edit')"></button>
    </span>
    <span class="col-12">{{ historyTimeToLive + ' ' + $t('decision.days') }}</span>
  </div>
  <hr class="my-2">
  <div class="row align-items-center">
    <span class="text-secondary font-weight-bold col-8 pe-0">{{ $t('decision.details.unfinishedInstances') }}</span>
    <span class="col-4 text-end">{{ version.runningInstances }}</span>
  </div>
  <hr class="my-2">
  <div class="row align-items-center">
    <span class="text-secondary font-weight-bold col-8 pe-0">{{ $t('decision.details.totalInstances') }}</span>
    <span class="col-4 text-end">{{ version.allInstances }}</span>
  </div>
  <hr class="my-2">
  <div class="row align-items-center">
    <span class="text-secondary font-weight-bold col-8 pe-0">{{ $t('decision.details.finishedInstances') }}</span>
    <span class="col-4 text-end">{{ version.completedInstances }}</span>
  </div>

  <b-modal ref="historyTimeToLive" :title="$t('decision.details.historyTimeToLive')">
    <div class="d-flex col-6 align-items-center ps-0">
      <input class="form-control" type="number" v-model="historyTimeToLiveChanged">
      <span class="ms-2">{{ $t('decision.days') }}</span>
    </div>
    <template v-slot:modal-footer>
      <b-button @click="$refs.historyTimeToLive.hide()" variant="link">{{ $t('confirm.cancel') }}</b-button>
      <b-button @click="updateHistoryTimeToLive()" variant="primary">{{ $t('decision-instance.save') }}</b-button>
    </template>
  </b-modal>

  <SuccessAlert ref="messageCopy"> {{ $t('decision.copySuccess') }} </SuccessAlert>
</template>

<script>

import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'

export default {
  name: 'DecisionDefinitionDetails',
  components: { SuccessAlert },
  mixins: [ copyToClipboardMixin ],
  props: {
    version: Object
  },
  data: function() {
    return {
      historyTimeToLive: '',
      historyTimeToLiveChanged: ''
    }
  },
  emits: ['onUpdateHistoryTimeToLive'],
  mounted: function() {
    this.historyTimeToLive = this.version.historyTimeToLive
  },
  methods: {
    editHistoryTimeToLive: function() {
      this.historyTimeToLiveChanged = this.historyTimeToLive
      this.$refs.historyTimeToLive.show()
    },
    updateHistoryTimeToLive: function() {
      // TODO: Implement
    }
  }
}
</script>
