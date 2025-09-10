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
      <b-button style="top: 2px; right: 60px" class="border-0 position-absolute"
        variant="btn-outline-primary" size="sm" :title="$t('process.loadVersions')"
        @click="onRefreshProcessDefinitions(false)">
        <span class="mdi mdi-18px mdi-cog-refresh-outline"></span>
      </b-button>
      <b-button style="top: 2px; right: 30px" class="border-0 position-absolute"
        variant="btn-outline-primary" size="sm" :title="$t('process.refreshVersions')"
        @click="onRefreshProcessDefinitions($root.config.lazyLoadHistory)">
        <span class="mdi mdi-18px mdi-refresh"></span>
      </b-button>
      <div class="list-group mx-3 mb-3" role="list">
        <router-link v-for="version of processDefinitions" :key="version.id"
          :to="{
            path: `/seven/auth/process/${version.key}/${version.version}`,
            query: {
              ...Object.fromEntries(
                Object.entries($route.query).filter(([key]) => key !== 'tab')
              ),
              tab: 'instances'
            }
          }" class="btn border-0">
          <div
            class="rounded-0 mt-3 p-2 bg-white border-0 list-group-item-action btn active"
            :class="version.version === versionIndex ? 'list-group-item shadow' : ''"
          >
            <div class="d-flex align-items-center">
              <h6 style="font-size: 1rem">
                <span class="fw-bold">{{ $t('process.details.definitionVersion') + ': ' + version.version }}</span>
              </h6>
              <div class="d-flex ms-auto" :id="version.id">
                <span class="mdi mdi-18px mdi-information-outline text-info"></span>
              </div>
            </div>
            <div class="d-flex">
              <div class="mb-1">
                <div class="text-start">{{ $t('process.details.unfinishedInstances') + ': ' + version.runningInstances }}</div>
                <div class="text-start">{{ $t('process.details.totalInstances') + ': ' + version.allInstances }}</div>
              </div>
              <div class="d-flex ms-auto my-auto mb-0">
                <b-button v-if="processByPermissions($root.config.permissions.deleteProcessDefinition, version)"
                  @click.stop.prevent="onDeleteClicked(version)"
                  :disabled="false"
                  variant="link"
                  class="shadow-none p-0 text-danger"
                  :title="$t('process.deleteProcessDefinition.tooltip')">
                  <span class="mdi mdi-18px mdi-delete-outline"></span>
                </b-button>
              </div>
            </div>
            <b-popover :target="version.id" triggers="hover" placement="right" boundary="viewport" max-width="350px">
              <ProcessDefinitionDetails :version="version" :instances="instances" :version-index="versionIndex" 
                :selected-instance="selectedInstance" @onUpdateHistoryTimeToLive="onUpdateHistoryTimeToLive"></ProcessDefinitionDetails>
            </b-popover>
          </div>
        </router-link>
      </div>
    </div>
    <SuccessAlert ref="successOperation"> {{ $t('alert.successOperation') }}</SuccessAlert>
    <DeleteProcessDefinitionModal ref="confirm"></DeleteProcessDefinitionModal>
  </div>
</template>

<script>
import { permissionsMixin } from '@/permissions.js'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'
import DeleteProcessDefinitionModal from '@/components/process/modals/DeleteProcessDefinitionModal.vue'
import ProcessDefinitionDetails from '@/components/process/ProcessDefinitionDetails.vue'

export default {
  name: 'ProcessDetailsSidebar',
  components: { SuccessAlert, DeleteProcessDefinitionModal, ProcessDefinitionDetails },
  mixins: [copyToClipboardMixin, permissionsMixin],
  props: {
    processDefinitions: {
      type: Array,
      required: true
    },
    instances: Array,
    processKey: String,
    versionIndex: { type: String, default: '' },
    selectedInstance: { type: Object, default: null }
  },
  emits: ['onDeleteProcessDefinition', 'onRefreshProcessDefinitions'],
  methods: {
    onRefreshProcessDefinitions: function(lazyLoadHistory) {
      this.$emit('onRefreshProcessDefinitions', lazyLoadHistory)
    },
    onDeleteClicked: function(processDefinition) {
      this.$refs.confirm.show({ ok: this.deleteProcessDefinition, processDefinition: processDefinition })
    },
    deleteProcessDefinition: function(processDefinition) {
      this.$emit('onDeleteProcessDefinition', { processDefinition: processDefinition })
    },
    onUpdateHistoryTimeToLive(versionId, historyTimeToLive) {
      this.processDefinitions.forEach(processDefinition => {
        if (processDefinition.id === versionId) {
          processDefinition.historyTimeToLive = historyTimeToLive
          this.$refs.successOperation.show()
        }
      })
    }
  }
}
</script>
