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
  <div class="h-100 d-flex flex-column bg-light" v-if="instance && processDefinition">
    <div class="overflow-auto h-100">
      <div v-for="(group, groupIndex) in groups" :key="groupIndex" class="m-3 bg-white shadow-sm rounded">

        <template v-if="group[0].chain !== undefined">
          <div class="p-2 ps-3 pb-0 fw-bold">
            {{ $t('process-instance.details.instancesChain') }}
          </div>

          <div class="p-2 ps-2 pt-0">
            <div v-for="(item, itemIndex) in group" :key="itemIndex" class="d-flex align-items-center">

              <template v-if="itemIndex === 0">
                <span class="mdi mdi-18px mdi-circle-small"></span>
              </template>
              <template v-if="itemIndex > 0">
                <span :style="{ width: ((itemIndex) * 23) + 'px' }"></span>
                <span class="mdi mdi-18px mdi-subdirectory-arrow-right"></span>
              </template>

              <CopyableActionButton
                :displayValue="item.value"
                :clickable="item.link !== undefined"
                :to="item.link"
                :title="$t(item.label) + ':\n' + item.value"
                @copy="copyValueToClipboard"
              />
            </div>
          </div>
        </template>

        <template v-else>
          <div v-for="(item, itemIndex) in group" :key="item.label" :class="itemIndex === group.length - 1 ? 'p-2 ps-3' : 'p-2 ps-3 border-bottom'">
            <div class="fw-bold">
              {{ $t(item.label) }}
            </div>
            <div class="d-flex align-items-center" v-if="item.value">
              <template v-if="item.state">
                <span :title="getIconTitle(item.state)" class="mdi mdi-18px me-1" :class="getIconState(item.state)"></span>
              </template>
              <template v-if="item.incidents">
                <span :title="$t('process.instanceIncidents')" class="mdi mdi-18px mdi-alert-outline text-warning me-1"></span>
              </template>
              <template v-if="item.suspended">
                <span class="mdi mdi-18px mdi-pause-circle-outline text-warning me-1" :title="$t('process-instance.jobDefinitions.suspended')"></span>
              </template>

              <span v-if="item.date === true" :title="formatDateForTooltips(item.value)" class="text-truncate d-block">{{ formatDate(item.value) }}</span>
              <span v-else-if="item.duration === true" :title="formatDuration(item.value)" class="text-truncate d-block">{{ formatDuration(item.value) }}</span>
              <CopyableActionButton v-else
                :displayValue="item.value"
                :clickable="item.link !== undefined"
                :to="item.link"
                :title="$t(item.label) + ':\n' + item.value"
                @copy="copyValueToClipboard"
              />

            </div>
          </div>
        </template>

      </div>
    </div>
  </div>

  <SuccessAlert ref="messageCopy" style="z-index: 9999"> {{ $t('process.copySuccess') }} </SuccessAlert>
</template>

<script>
import { SuccessAlert, CopyableActionButton } from '@cib/common-frontend'
import { permissionsMixin } from '@/permissions.js'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import { formatDate, formatDateForTooltips, formatDuration } from '@/utils/dates.js'

export default {
  name: 'ProcessInstanceDetailsSidebar',
  components: { SuccessAlert, CopyableActionButton },
  mixins: [copyToClipboardMixin, permissionsMixin],
  props: {
    instance: { type: Object, default: null },
    processDefinition: { type: Object, default: null },
  },
  computed: {
    groups() {
      const groups = []

      if (this.instance) {
        groups.push([
          { label: 'process-instance.processInstanceId', value: this.instance.id, state: this.instance.state, incidents: this.instance.incidents?.length > 0 },
          { label: 'process-instance.details.startUser', value: this.instance.startUserId },
          { label: 'process-instance.details.start', value: this.instance.startTime, date: true },
          ...(this.instance.endTime ? [
            { label: 'process-instance.details.finish', value: this.instance.endTime, date: true },
            { label: 'process-instance.details.duration', value: this.instance.durationInMillis, duration: true },
          ] : []),
          { label: 'process.details.tenantId', value: this.instance.tenantId },
          { label: 'process-instance.businessKey', value: this.instance.businessKey },
        ])
      }

      // instances chain
      if (this.instance) {
        let chain = [
          this.instance.rootProcessInstanceId,
          this.instance.superProcessInstanceId,
          this.instance.id,
        ].filter(Boolean)

        // remove consecutive duplicates (not all duplicates globally)
        chain = chain.filter((item, index) => {
          // Always keep the first element (index === 0)
          // For every next element, keep it only if it's different from the previous one
          return index === 0 || item !== chain[index - 1];
        })

        if (chain.length > 1) {
          groups.push(chain.map(id => ({
            chain: true,
            label: id === this.instance.id ? 'process-instance.processInstanceId' : 'process-instance.details.superProcessInstanceId',
            value: id,
            link: id === this.instance.id ? undefined : { name: 'process-instance-id', params: { instanceId: id } }
          })))
        }
      }

      if (this.processDefinition) {
        const link = {
            path: `/seven/auth/process/${this.processDefinition.key}/${this.processDefinition.version}`,
            query: {
              ...Object.fromEntries(
                Object.entries(this.$route.query).filter(([key]) => !['tab', 'tenantId'].includes(key))
              ),
              ...(this.processDefinition.tenantId ? { tenantId: this.processDefinition.tenantId } : {}),
              tab: 'instances',
            }
          }

        groups.push([
          { label: 'process.details.definitionId', value: this.processDefinition.id, link },
          { label: 'process.details.definitionKey', value: this.processDefinition.key, link },
          { label: 'process.details.definitionName', value: this.processDefinition.name || this.processDefinition.id, link },
          { label: 'process.details.versionTag', value: this.processDefinition.versionTag, link },
          { label: 'process.details.definitionVersion', value: this.processDefinition.version, suspended: this.processDefinition.suspended === 'true', link },
          { label: 'process.details.deploymentId', value: this.processDefinition.deploymentId, link: { name: 'deployments', params: { deploymentId: this.processDefinition.deploymentId } } },
          { label: 'process.details.tenantId', value: this.processDefinition.tenantId, link },
        ])
      }

      return groups
    }
  },
  methods: {
    formatDate,
    formatDateForTooltips,
    formatDuration,

    getIconState(state) {
      switch(state) {
        case 'ACTIVE':
          return 'mdi-chevron-triple-right text-success'
        case 'SUSPENDED':
          return 'mdi-close-circle-outline'
      }
      return 'mdi-flag-triangle'
    },

    getIconTitle(state) {
      switch(state) {
        case 'ACTIVE':
          return this.$t('process.instanceRunning')
        case 'SUSPENDED':
          return this.$t('process.instanceSuspended')
      }
      return this.$t('process.instanceFinished')
    },
  }
}
</script>
