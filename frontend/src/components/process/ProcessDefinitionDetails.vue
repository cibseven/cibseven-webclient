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
    <router-link v-if="selectedDeployment" class="col-12" :to="'/seven/auth/deployments/' + version.deploymentId">{{ version.deploymentId }}</router-link>
    <span v-else class="col-12">{{ version.deploymentId }}</span>
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
    <RouterLink class="text-decoration-underline" style="cursor:pointer" :to="routeToSuperProcessInstance(selectedInstance.superProcessInstanceId)">
      {{ selectedInstance.superProcessInstanceId }}
    </RouterLink>
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
    <span class="col-12">{{ historyTimeToLiveDisplay }}</span>
  </div>
  <hr class="my-2">
  <div class="row">
    <span class="text-secondary fw-bold col-5 pe-0">{{ $t('process.details.firstStart') }}</span>
    <span class="col-7 text-end" :title="formatDateForTooltips(minTimestamp)">{{ formatDate(minTimestamp) || '-' }}</span>
  </div>
  <hr class="my-2">
  <div class="row">
    <span class="text-secondary fw-bold col-5 pe-0">{{ $t('process.details.lastStart') }}</span>
    <span class="col-7 text-end" :title="formatDateForTooltips(maxTimestamp)">{{ formatDate(maxTimestamp) || '-' }}</span>
  </div>
  <hr class="my-2">
  <div class="row align-items-center">
    <span class="text-secondary fw-bold col-8 pe-0">{{ $t('process.details.totalInstances') }}</span>
    <span class="col-4 text-end">{{ version.allInstances }}</span>
  </div>

  <EditHistoryTimeToLiveModal ref="ttlModal" :description-items="ttlDescriptionItems" @ttl-updated="onTtlUpdated" />

  <SuccessAlert ref="messageCopy"> {{ $t('process.copySuccess') }} </SuccessAlert>
</template>

<script>
import { formatDate, formatDateForTooltips } from '@/utils/dates.js'
import { ProcessService, HistoryService } from '@/services.js'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import { SuccessAlert } from '@cib/common-frontend'
import EditHistoryTimeToLiveModal from '@/components/modals/EditHistoryTimeToLiveModal.vue'
import { formatHistoryTimeToLive } from '@/utils/historyTimeToLive.js'

export default {
  name: 'ProcessDefinitionDetails',
  components: { SuccessAlert, EditHistoryTimeToLiveModal },
  mixins: [ copyToClipboardMixin ],
  props: {
    version: Object,
    selectedInstance: { type: Object, default: null },
    versionIndex: { type: String, default: '' },
    shown: { type: Boolean, default: false },
    // loaded by the sidebar, which also shows its time on the version card
    selectedDeployment: { type: Object, default: null }
  },
  data: function() {
    return {
      historyTimeToLive: null,
      minTimestamp: null,
      maxTimestamp: null,
      timestampsLoaded: false
    }
  },
  emits: ['onUpdateHistoryTimeToLive'],
  watch: {
    shown(val) {
      if (val) this.getTimestamps()
    },
    'version.id': function() {
      this.resetTimestampsCache()
      if (this.shown) {
        this.getTimestamps()
      }
    },
    'version.allInstances': function(newVal, oldVal) {
      if (newVal === oldVal) return
      this.resetTimestampsCache()
      if (this.shown) {
        this.getTimestamps()
      }
    }
  },
  computed: {
    isVersionSelected() {
      return this.version.version === this.versionIndex
    },
    ttlDescriptionItems() {
      return [
        { title: this.$t('process.details.definitionName'), value: this.version.name },
        { title: this.$t('process.details.definitionVersion'), value: this.version.version }
      ]
    },
    historyTimeToLiveDisplay() {
      return formatHistoryTimeToLive(this.$t, this.historyTimeToLive)
    }
  },
  mounted() {
    this.historyTimeToLive = this.version.historyTimeToLive
    // mounted already open when the sidebar expands a version
    if (this.shown) this.getTimestamps()
  },
  methods: {
    resetTimestampsCache() {
      this.timestampsLoaded = false
      this.minTimestamp = null
      this.maxTimestamp = null
    },
    async getTimestamps() {
      if (this.$root.config.camundaHistoryLevel === 'none') return
      if (this.timestampsLoaded) return
      this.timestampsLoaded = true
      const requestedVersionId = this.version.id
      try {
        const [first, last] = await Promise.all([
          HistoryService.findProcessesInstancesHistory({ processDefinitionId: requestedVersionId, sorting: [{ sortBy: 'startTime', sortOrder: 'asc' }] }, 0, 1),
          HistoryService.findProcessesInstancesHistory({ processDefinitionId: requestedVersionId, sorting: [{ sortBy: 'startTime', sortOrder: 'desc' }] }, 0, 1)
        ])

        if (this.version.id !== requestedVersionId) return
        this.minTimestamp = first?.[0]?.startTime ?? null
        this.maxTimestamp = last?.[0]?.startTime ?? null
      }
      catch {
        if (this.version.id !== requestedVersionId) return
        this.minTimestamp = null
        this.maxTimestamp = null
        this.timestampsLoaded = false
      }
    },
    formatDate,
    formatDateForTooltips,
    editHistoryTimeToLive: function() {
      this.$refs.ttlModal.show({ historyTimeToLive: this.historyTimeToLive })
    },
    onTtlUpdated({ ttlValue }) {
      const data = { historyTimeToLive: ttlValue }
      ProcessService.updateHistoryTimeToLive(this.version.id, data).then(() => {
        this.historyTimeToLive = ttlValue
        this.$emit('onUpdateHistoryTimeToLive', this.version.id, ttlValue)
      }).catch(error => {
        this.$root.$refs.error.show(error.response?.data)
      })
    },
    routeToSuperProcessInstance(superProcessInstanceId) {
      return {
        name: 'process-instance-id',
        params: {
          instanceId: superProcessInstanceId
        },
        query: {
          tab: 'calledProcessInstances'
        }
      }
    }
  }
}
</script>
