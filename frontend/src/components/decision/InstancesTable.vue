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
  <FlowTable striped resizable thead-class="sticky-header" :items="instances" primary-key="id" prefix="decision."
    :sort-by="sortByDefaultKey" :sort-desc="sortDesc" :fields="[
    { label: 'id', key: 'id', class: 'col-2', tdClass: 'py-1 position-relative' },
    { label: 'evaluationTime', key: 'evaluationTime', class: 'col-2', tdClass: 'py-1' },
    { label: 'callingProcess', key: 'processDefinitionKey', class: 'col-3', tdClass: 'py-1 position-relative' },
    { label: 'callingInstanceId', key: 'processInstanceId', class: 'col-3', tdClass: 'py-1 position-relative' },
    { label: 'activityId', key: 'activityId', class: 'col-2', tdClass: 'py-1' }]">
    <template v-slot:cell(id)="table">
      <button :title="table.item.id" class="text-truncate w-100 btn btn-link text-start" :class="focusedCell === table.item.id ? 'pe-4': ''"
        @mouseenter="focusedCell = table.item.id" @mouseleave="focusedCell = null" @click="goToInstance(table.item)">
        {{ table.item.id }}
        <span v-if="table.item.id && focusedCell === table.item.id" @click.stop="copyValueToClipboard(table.item.id)"
          class="mdi mdi-18px mdi-content-copy px-2 position-absolute end-0 text-secondary lh-sm"></span>
      </button>
    </template>
    <template v-slot:cell(evaluationTime)="table">
        <span :title="formatDateForTooltips(table.item.evaluationTime)">{{ formatDate(table.item.evaluationTime) }}</span>
      </template>
    <template v-slot:cell(processDefinitionKey)="table">
      <button :title="table.item.processDefinitionKey" class="text-truncate w-100 btn btn-link text-start" @click="goToProcess(table.item)"
        @mouseenter="focusedCell = table.item.id + table.item.processDefinitionKey" @mouseleave="focusedCell = null">
        {{ table.item.processDefinitionKey }}
        <span v-if="table.item.id && focusedCell === (table.item.id + table.item.processDefinitionKey)"
          @click.stop="copyValueToClipboard(table.item.processDefinitionKey)" class="mdi mdi-18px mdi-content-copy px-2 position-absolute end-0 text-secondary lh-sm"></span>
      </button>
    </template>
    <template v-slot:cell(processInstanceId)="table">
      <button :title="table.item.processInstanceId" class="text-truncate w-100 btn btn-link text-start" @click="goToProcessInstance(table.item)"
        @mouseenter="focusedCell = table.item.processInstanceId" @mouseleave="focusedCell = null">
        {{ table.item.processInstanceId }}
        <span v-if="table.item.id && focusedCell === table.item.processInstanceId"
          @click.stop="copyValueToClipboard(table.item.processInstanceId)" class="mdi mdi-18px mdi-content-copy px-2 position-absolute end-0 text-secondary lh-sm"></span>
      </button>
    </template>
    <template v-slot:cell(activityId)="table">
      <div :title="table.item.activityId" class="text-truncate w-100"
        @mouseenter="focusedCell = table.item.id + table.item.activityId" @mouseleave="focusedCell = null">
        {{ table.item.activityId }}
        <span v-if="table.item.id && focusedCell === (table.item.id + table.item.activityId)"
          @click.stop="copyValueToClipboard(table.item.activityId)" class="mdi mdi-18px mdi-content-copy px-2 position-absolute end-0 text-secondary lh-sm"></span>
      </div>
    </template>
  </FlowTable>
  <SuccessAlert top="0" style="z-index: 1031" ref="success"> {{ $t('alert.successOperation') }}</SuccessAlert>
  <SuccessAlert ref="messageCopy"> {{ $t('decision.copySuccess') }} </SuccessAlert>
</template>

<script>
import { permissionsMixin } from '@/permissions.js'
import { formatDate, formatDateForTooltips } from '@/utils/dates.js'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import { FlowTable } from '@cib/common-frontend'
import { SuccessAlert } from '@cib/common-frontend'
import { HistoryService } from '@/services.js'
import { mapMutations, mapGetters } from 'vuex'

export default {
  name: 'InstancesTable',
  components: { FlowTable, SuccessAlert },
  mixins: [copyToClipboardMixin, permissionsMixin],
  props: { instances: Array, sortDesc: Boolean, sortByDefaultKey: String },
  data() {
    return {
      focusedCell: null
    }
  },
  computed: {
    ...mapGetters(['getSelectedDecisionVersion'])
  },
  methods: {
    ...mapMutations(['setSelectedInstance']),
    formatDate,
    formatDateForTooltips,
    goToInstance(instance) {
      this.setSelectedInstance(instance)
      this.$router.push({
        name: 'decision-instance',
        params: {
          versionIndex: this.getSelectedDecisionVersion.version,
          instanceId: instance.id
        }
      })
    },
    getIconState(state) {
      switch (state) {
        case 'ACTIVE': return 'mdi-chevron-triple-right text-success'
        case 'SUSPENDED': return 'mdi-close-circle-outline'
        default: return 'mdi-flag-triangle'
      }
    },
    getIconTitle(state) {
      switch (state) {
        case 'ACTIVE': return this.$t('decision.instanceRunning')
        case 'SUSPENDED': return this.$t('decision.instanceSuspended')
        default: return this.$t('decision.instanceFinished')
      }
    },
    async goToProcess(instance) {
      let processData = await HistoryService.findProcessInstance(instance.processInstanceId)
      this.$router.push({
        name: 'process',
        params: {
          processKey: processData.processDefinitionKey,
          versionIndex: processData.processDefinitionVersion
        }
      })
    },
    async goToProcessInstance(instance) {
      let processData = await HistoryService.findProcessInstance(instance.processInstanceId)
      this.$router.push({
        name: 'process',
        params: {
          processKey: processData.processDefinitionKey,
          versionIndex: processData.processDefinitionVersion,
          instanceId: instance.processInstanceId
        }
      })
    }
  }
}
</script>
