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
  <div class="h-100 d-flex flex-column bg-light">
    <div class="overflow-auto h-100">
      <div v-for="(group, groupIndex) in groups" :key="groupIndex" class="m-3 bg-white shadow">
        <div v-for="item in group" :key="item.label" class="p-2 ps-3 border-bottom">
          <div :class="item.value ? 'fw-bold' : 'text-muted'">
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
            <CopyableActionButton
              :displayValue="item.value"
              :clickable="item.link !== undefined"
              :to="item.link"
              :title="$t(item.label) + ':\n' + item.value"
              @copy="copyValueToClipboard"
            />
          </div>
        </div>
      </div>
    </div>
  </div>

  <SuccessAlert ref="messageCopy" style="z-index: 9999"> {{ $t('process.copySuccess') }} </SuccessAlert>
</template>

<script>
import { SuccessAlert, CopyableActionButton } from '@cib/common-frontend'
import { permissionsMixin } from '@/permissions.js'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'

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
          { label: 'process.details.tenantId', value: this.instance.tenantId },
          { label: 'process-instance.businessKey', value: this.instance.businessKey },
        ])
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
          { label: 'process.details.definitionName', value: this.processDefinition.name, link },
          { label: 'process.details.definitionId', value: this.processDefinition.id, link },
          { label: 'process.details.definitionKey', value: this.processDefinition.key, link },
          { label: 'process.details.definitionVersion', value: this.processDefinition.version, suspended: this.processDefinition.suspended === 'true', link },
          { label: 'process.details.tenantId', value: this.processDefinition.tenantId, link },
        ])
      }

      return groups
    }
  },
  methods: {
    getIconState: function(state) {
      switch(state) {
        case 'ACTIVE':
          return 'mdi-chevron-triple-right text-success'
        case 'SUSPENDED':
          return 'mdi-close-circle-outline'
      }
      return 'mdi-flag-triangle'
    },
    getIconTitle: function(state) {
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
