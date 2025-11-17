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
  <div class="row">
    <span class="text-secondary fw-bold col-8 pe-0">{{ $t('process.details.versionTag') }}</span>
    <span class="col-4 text-end">{{ version.versionTag }}</span>
  </div>
  <hr class="my-2">
  <div class="row">
    <span class="text-secondary fw-bold col-12">
      {{ $t('process.details.definitionId') }}
      <button @click="copyValueToClipboard(version.id)" class="btn btn-sm mdi mdi-content-copy float-end border-0"
        :title="$t('process.details.copyValue')"></button>
    </span>
    <span class="col-12">{{ version.id }}</span>
  </div>
  <hr class="my-2">
  <div class="row">
    <span class="text-secondary fw-bold col-12">
      {{ $t('process.details.definitionKey') }}
      <button @click="copyValueToClipboard(version.key)" class="btn btn-sm mdi mdi-content-copy float-end border-0"
        :title="$t('process.details.copyValue')"></button>
    </span>
    <span class="col-12">{{ version.key }}</span>
  </div>
  <hr class="my-2">
  <div class="row">
    <span class="text-secondary fw-bold col-12">
      {{ $t('process.details.definitionName') }}
      <button @click="copyValueToClipboard(version.name)" class="btn btn-sm mdi mdi-content-copy float-end border-0"
        :title="$t('process.details.copyValue')"></button>
    </span>
    <span class="col-12">{{ version.name }}</span>
  </div>
  <hr class="my-2">
  <div class="row">
    <span class="text-secondary fw-bold col-12">
      {{ $t('process.details.deploymentId') }}
      <button @click="copyValueToClipboard(version.deploymentId)" class="btn btn-sm mdi mdi-content-copy float-end border-0"
        :title="$t('process.details.copyValue')"></button>
    </span>
    <router-link class="col-12" :to="'/seven/auth/deployments/' + version.deploymentId">{{ version.deploymentId }}</router-link>
  </div>
  <hr class="my-2">
  <div class="row">
    <span class="text-secondary fw-bold col-12">
      {{ $t('process.details.tenantId') }}
      <button v-if="version.tenantId" @click="copyValueToClipboard(version.tenantId)" class="btn btn-sm mdi mdi-content-copy float-end border-0"
        :title="$t('process.details.copyValue')"></button>
    </span>
    <span class="col-12">{{ version.tenantId ? version.tenantId : '-' }}</span>
  </div>
  <hr v-if="selectedInstance?.superProcessInstanceId && isVersionSelected" class="my-2">
  <div v-if="selectedInstance?.superProcessInstanceId && isVersionSelected" class="row">
    <span class="text-secondary fw-bold col-12">
      {{ $t('process.details.superProcessInstanceId') }}
      <button @click="copyValueToClipboard(selectedInstance.superProcessInstanceId)" class="btn btn-sm mdi mdi-content-copy float-end border-0"
        :title="$t('process.details.copyValue')"></button>
    </span>
    <a class="text-decoration-underline" style="cursor:pointer" @click.prevent="navigateToSuperProcessInstance(selectedInstance.superProcessInstanceId)">
      {{ selectedInstance.superProcessInstanceId }}
    </a>
  </div>
  <hr class="my-2">
  <div class="row">
    <span class="text-secondary fw-bold col-12">
      {{ $t('process.details.historyTimeToLive') }}
      <button @click="copyValueToClipboard(historyTimeToLive)" class="btn btn-sm mdi mdi-content-copy float-end border-0"
        :title="$t('process.details.copyValue')"></button>
      <button @click="editHistoryTimeToLive()" class="btn btn-sm mdi mdi-pencil float-end border-0"
        :title="$t('process-instance.edit')"></button>
    </span>
    <span class="col-12">{{ historyTimeToLive + ' ' + $t('process.days') }}</span>
  </div>
  <hr class="my-2">
  <div class="row">
    <span class="text-secondary fw-bold col-5 pe-0">{{ $t('process.details.firstStart') }}</span>
    <span class="col-7 text-end">{{ getDate('min') }}</span>
  </div>
  <hr class="my-2">
  <div class="row">
    <span class="text-secondary fw-bold col-5 pe-0">{{ $t('process.details.lastStart') }}</span>
    <span class="col-7 text-end">{{ getDate('max') }}</span>
  </div>
  <hr class="my-2">
  <div class="row align-items-center">
    <span class="text-secondary fw-bold col-8 pe-0">{{ $t('process.details.unfinishedInstances') }}</span>
    <span class="col-4 text-end">{{ version.runningInstances }}</span>
  </div>
  <hr class="my-2">
  <div class="row align-items-center">
    <span class="text-secondary fw-bold col-8 pe-0">{{ $t('process.details.totalInstances') }}</span>
    <span class="col-4 text-end">{{ version.allInstances }}</span>
  </div>
  <hr class="my-2">
  <div class="row align-items-center">
    <span class="text-secondary fw-bold col-8 pe-0">{{ $t('process.details.finishedInstances') }}</span>
    <span class="col-4 text-end">{{ version.completedInstances }}</span>
  </div>

  <b-modal ref="historyTimeToLive" :title="$t('process.details.historyTimeToLive')">
    <div class="d-flex col-6 align-items-center ps-0">
      <input class="form-control" type="number" v-model="historyTimeToLiveChanged">
      <span class="ms-2">{{ $t('process.days') }}</span>
    </div>
    <template v-slot:modal-footer>
      <b-button @click="$refs.historyTimeToLive.hide()" variant="light">{{ $t('confirm.cancel') }}</b-button>
      <b-button @click="updateHistoryTimeToLive()" variant="primary">{{ $t('process-instance.save') }}</b-button>
    </template>
  </b-modal>

  <SuccessAlert ref="messageCopy"> {{ $t('process.copySuccess') }} </SuccessAlert>
</template>

<script>
import { moment } from '@/globals.js'
import { formatDate } from '@/utils/dates.js'
import { ProcessService, HistoryService } from '@/services.js'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import { SuccessAlert } from '@cib/common-frontend'

export default {
  name: 'ProcessDefinitionDetails',
  components: { SuccessAlert },
  mixins: [ copyToClipboardMixin ],
  props: {
    instances: Array,
    version: Object,
    selectedInstance: { type: Object, default: null },
    versionIndex: { type: String, default: '' }
  },
  data: function() {
    return {
      versions: [],
      historyTimeToLive: '',
      historyTimeToLiveChanged: ''
    }
  },
  emits: ['onUpdateHistoryTimeToLive'],
  computed: {
    isVersionSelected() {
      return this.version.version === this.versionIndex
    }
  },
  mounted: function() {
    this.historyTimeToLive = this.version.historyTimeToLive
  },
  methods: {
    getDate: function(type) {
      const timestamps = this.instances.filter(i => i.processDefinitionVersion === this.version.version).map(i => moment(i.startTime).valueOf())
      if (timestamps.length === 0) return '-'
      const date = type === 'min' ? Math.min(...timestamps) : Math.max(...timestamps)
      return formatDate(date)
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
    },
    navigateToSuperProcessInstance: async function(superProcessInstanceId) {
      if (!superProcessInstanceId) return
      try {
        const processInstance = await HistoryService.findProcessInstance(superProcessInstanceId)
        const processKey = processInstance.processDefinitionKey
        const versionIndex = processInstance.processDefinitionVersion
        const params = { processKey, versionIndex, instanceId: processInstance.id }

        const routeConfig = {
          name: 'process',
          params,
          query: {
            tab: 'variables'
          }
        }
        await this.$router.push(routeConfig)
      } catch (error) {
        console.error('Failed to navigate to super process instance:', error)
      }
    }
  }
}
</script>
