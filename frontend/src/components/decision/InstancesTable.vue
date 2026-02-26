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
  <FlowTable striped resizable thead-class="sticky-header" :items="instances" primary-key="id"
    :sort-by="sortByDefaultKey" :sort-desc="sortDesc" :fields="[
    { label: 'decision.id', key: 'id', class: 'col-2', tdClass: 'py-1 position-relative' },
    { label: 'decision.evaluationTime', key: 'evaluationTime', class: 'col-2', tdClass: 'py-1' },
    { label: 'decision.callingProcess', key: 'processDefinitionKey', class: 'col-3', tdClass: 'py-1 position-relative' },
    { label: 'decision.callingInstanceId', key: 'processInstanceId', class: 'col-3', tdClass: 'py-1' },
    { label: 'decision.activityId', key: 'activityId', class: 'col-2', tdClass: 'py-1' }]">

    <template v-slot:cell(id)="table">
      <CopyableActionButton
        :display-value="table.item.id"
        :title="$t('decision.showInstance') + '\n' + $t('decision.id') + ': ' + table.item.id"
        @copy="copyValueToClipboard"
        @click="goToInstance(table.item)"
      />
    </template>

    <template v-slot:cell(evaluationTime)="table">
      <div class="text-truncate">
        <span :title="formatDateForTooltips(table.item.evaluationTime)">{{ formatDate(table.item.evaluationTime) }}</span>
      </div>
    </template>

    <template v-slot:cell(processDefinitionKey)="table">
      <CopyableActionButton
        :display-value="table.item.processDefinitionKey"
        :title="$t('decision.callingProcess') + ':\n' + table.item.processDefinitionKey"
        @copy="copyValueToClipboard"
        @click="goToProcessDefinition(table.item)"
      />
    </template>

    <template v-slot:cell(processInstanceId)="table">
      <CopyableActionButton
        :display-value="table.item.processInstanceId"
        :title="$t('decision.callingInstanceId') + ':\n' + table.item.processInstanceId"
        @copy="copyValueToClipboard"
        :to="`/seven/auth/processes/instance/${table.item.processInstanceId}`"
      />
    </template>

    <template v-slot:cell(activityId)="table">
      <CopyableActionButton
        :display-value="table.item.activityId"
        :title="$t('decision.activityId') + ':\n' + table.item.activityId"
        :clickable="false"
        @copy="copyValueToClipboard"
      />
    </template>
  </FlowTable>
  <SuccessAlert top="0" style="z-index: 1031" ref="success"> {{ $t('alert.successOperation') }}</SuccessAlert>
  <SuccessAlert ref="messageCopy"> {{ $t('decision.copySuccess') }} </SuccessAlert>
</template>

<script>
import { permissionsMixin } from '@/permissions.js'
import { formatDate, formatDateForTooltips } from '@/utils/dates.js'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import { FlowTable, SuccessAlert, CopyableActionButton } from '@cib/common-frontend'
import { HistoryService } from '@/services.js'
import { mapMutations, mapGetters } from 'vuex'

export default {
  name: 'InstancesTable',
  components: { FlowTable, SuccessAlert, CopyableActionButton },
  mixins: [copyToClipboardMixin, permissionsMixin],
  props: {
    instances: Array,
    sortDesc: Boolean,
    sortByDefaultKey: String,
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
    async goToProcessDefinition(instance) {
      const processData = await HistoryService.findProcessInstance(instance.processInstanceId)
      this.$router.push({
        name: 'process',
        params: {
          processKey: processData.processDefinitionKey,
          versionIndex: processData.processDefinitionVersion
        },
        query: {
          ...(processData.tenantId ? { tenantId: processData.tenantId } : {}),
          tab: 'instances',
        }
      })
    }
  }
}
</script>
