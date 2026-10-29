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
      <b-button ref="loadData" :style="loadDataStyle"
        class="load-data position-absolute d-inline-flex align-items-center"
        :class="{ 'border-0': compactLoadData }" :variant="compactLoadData ? 'btn-outline-primary' : 'outline-secondary'" size="sm"
        :title="$t('process.loadVersions')" :aria-label="compactLoadData ? $t('process.loadData') : null"
        :disabled="!!refreshing" :aria-busy="refreshing === 'all' ? 'true' : null"
        @click="onRefreshProcessDefinitions(false)">
        <!-- the spinner takes the place of the icon while every version's data is loading -->
        <span v-if="refreshing === 'all'" class="load-data-spinner spinner-border" aria-hidden="true"></span>
        <span v-else class="mdi mdi-18px mdi-poll" :class="{ 'lh-1': !compactLoadData }" aria-hidden="true"></span>
        <span v-if="!compactLoadData" class="ms-1">{{ $t('process.loadData') }}</span>
      </b-button>
      <b-button style="top: 2px; right: 30px" class="border-0 position-absolute"
        variant="btn-outline-primary" size="sm" :title="$t('process.refreshVersions')"
        :disabled="!!refreshing"
        @click="onRefreshProcessDefinitions($root.config.lazyLoadHistory)">
        <span class="mdi mdi-18px mdi-refresh"></span>
      </b-button>
      <div class="list-group mx-3 mb-3" role="list">
        <!-- the shadow wraps the card and its open details, so none falls between them -->
        <div v-for="version of processDefinitions" :key="version.id" class="version-item rounded mt-3" role="listitem"
          :class="{ shadow: version.version === versionIndex }">
          <router-link
            :to="{
              path: `/seven/auth/process/${version.key}/${version.version}`,
              query: {
                ...Object.fromEntries(
                  Object.entries($route.query).filter(([key]) => !['tab', 'tenantId'].includes(key))
                ),
                tenantId: version.tenantId ? version.tenantId : undefined,
                tab: 'instances'
              }
            }" class="btn border-0 p-0 d-block">
            <div
              class="position-relative p-2 bg-white border-0 list-group-item-action btn active"
              :class="[{ 'list-group-item': version.version === versionIndex }, expandedVersionId === version.id ? 'rounded-top rounded-bottom-0' : 'rounded']"
            >
              <div class="d-flex align-items-baseline">
                <span class="version-number h4 fw-bold mb-0 me-2" :title="$t('process.details.definitionVersion')">{{ version.version }}.</span>
                <div class="flex-grow-1 min-width-0">
                  <div class="text-start fw-normal">
                    <div v-if="version.suspended === 'true'" class="text-warning small">
                      <span class="mdi mdi-pause-circle-outline me-1" aria-hidden="true"></span>{{ $t('process-instance.jobDefinitions.suspended') }}
                    </div>
                    <div class="text-truncate">
                      {{ $t('process.details.versionTag') }}: <span class="fw-bold">{{ version.versionTag || '-' }}</span>
                    </div>
                    <div v-if="deploymentTimeOf(version)" class="text-truncate deployment-time"
                      :title="formatDateForTooltips(deploymentTimeOf(version))">
                      {{ $t('deployment.deploymentTime') }}: {{ formatDate(deploymentTimeOf(version), 'L HH:mm') }}
                    </div>
                    <div v-else class="text-truncate deployment-time">{{ $t('deployment.deploymentTime') }}: -</div>
                  </div>
                  <div class="d-flex align-items-center gap-3 fw-bold mt-2">
                    <span class="running-instances" :title="$t('process.details.unfinishedInstances')">
                      <span class="mdi mdi-18px mdi-chevron-triple-right text-success" aria-hidden="true"></span>
                      <span class="visually-hidden">{{ $t('process.details.unfinishedInstances') }}:</span>
                      {{ countOrDash(version.runningInstances) }}
                    </span>
                    <span class="open-incidents" :title="$t('bpmn-viewer.legend.openIncidents')">
                      <span class="mdi mdi-18px mdi-alert-outline text-warning" aria-hidden="true"></span>
                      <span class="visually-hidden">{{ $t('bpmn-viewer.legend.openIncidents') }}:</span>
                      {{ countOrDash(incidentsOf(version)) }}
                    </span>
                    <span v-if="hasHistory" class="finished-instances" :title="$t('process.details.finishedInstances')">
                      <span class="mdi mdi-18px mdi-flag-checkered text-secondary" aria-hidden="true"></span>
                      <span class="visually-hidden">{{ $t('process.details.finishedInstances') }}:</span>
                      {{ countOrDash(version.completedInstances) }}
                    </span>
                  </div>
                </div>
                <b-button class="details-toggle d-flex align-self-center ms-2 p-0 border-0 shadow-none" variant="link" size="sm"
                  @click.stop.prevent="toggleDetails(version.id)"
                  :title="$t('process.info')"
                  :aria-label="$t('process.info')"
                  :aria-expanded="String(expandedVersionId === version.id)"
                  :aria-controls="`version-details-${version.id}`">
                  <span class="mdi mdi-24px text-secondary" :class="expandedVersionId === version.id ? 'mdi-chevron-up' : 'mdi-chevron-down'" aria-hidden="true"></span>
                </b-button>
              </div>
            </div>
          </router-link>
          <div v-if="expandedVersionId === version.id" :id="`version-details-${version.id}`" class="version-details bg-white border-top rounded-bottom p-3">
            <ProcessDefinitionDetails :version="version" :version-index="versionIndex"
              :selected-deployment="deployments[version.deploymentId] ?? null"
              :shown="true"
              :selected-instance="selectedInstance" @onUpdateHistoryTimeToLive="onUpdateHistoryTimeToLive"></ProcessDefinitionDetails>
            <div class="row g-2 mt-2">
              <!-- the left half is kept for the enterprise edition (e.g. Migrate); alone, Delete keeps its own width on the right -->
              <div class="version-actions-plugin col-6 d-flex">
                <component :is="ProcessDefinitionVersionActionsPlugin" v-if="ProcessDefinitionVersionActionsPlugin" :process="version" :process-key="processKey"></component>
              </div>
              <div class="col-6 d-flex">
                <b-button v-if="processByPermissions($root.config.permissions.deleteProcessDefinition, version)" variant="outline-danger" class="delete-version d-inline-flex align-items-center justify-content-center fw-bold"
                  :class="ProcessDefinitionVersionActionsPlugin ? 'w-100' : 'ms-auto'" :title="$t('process.deleteProcessDefinition.tooltip')" @click="onDeleteClicked(version)">
                  <span class="mdi mdi-trash-can-outline me-1" aria-hidden="true"></span>{{ $t('confirm.delete') }}
                </b-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <SuccessAlert ref="successOperation"> {{ $t('alert.successOperation') }}</SuccessAlert>
    <DeleteProcessDefinitionModal ref="confirm"></DeleteProcessDefinitionModal>
  </div>
</template>

<script>
import { permissionsMixin } from '@/permissions.js'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import { SuccessAlert } from '@cib/common-frontend'
import DeleteProcessDefinitionModal from '@/components/process/modals/DeleteProcessDefinitionModal.vue'
import ProcessDefinitionDetails from '@/components/process/ProcessDefinitionDetails.vue'
import { ProcessService } from '@/services.js'
import { formatDate, formatDateForTooltips } from '@/utils/dates.js'

// distance of the labelled load button from the right edge, clear of the refresh button
const LOAD_DATA_RIGHT = 65

export default {
  name: 'ProcessDetailsSidebar',
  components: { SuccessAlert, DeleteProcessDefinitionModal, ProcessDefinitionDetails },
  mixins: [copyToClipboardMixin, permissionsMixin],
  props: {
    processDefinitions: {
      type: Array,
      required: true
    },
    processKey: String,
    versionIndex: { type: String, default: '' },
    selectedInstance: { type: Object, default: null },
    // the selected definition, with the activity statistics the diagram already loaded
    selectedProcess: { type: Object, default: null },
    // the versions reload in progress: 'all' (every version's data) or 'refresh'
    refreshing: { type: String, default: null }
  },
  emits: ['onDeleteProcessDefinition', 'onRefreshProcessDefinitions'],
  data() {
    return {
      expandedVersionId: null,
      // what was loaded for the versions selected before, so their cards keep it
      deployments: {},
      incidentsByVersionId: {},
      // only the icon, without border, when the label does not fit beside the sidebar caption
      compactLoadData: false,
      loadDataWidth: 0
    }
  },
  mounted() {
    if (typeof ResizeObserver === 'undefined') return
    this.resizeObserver = new ResizeObserver(() => this.fitLoadData())
    this.resizeObserver.observe(this.$el)
  },
  beforeUnmount() {
    this.resizeObserver?.disconnect()
  },
  computed: {
    hasHistory() {
      return this.$root.config.camundaHistoryLevel !== 'none'
    },
    // compact it sits like the refresh button; labelled it is centred in the header
    loadDataStyle() {
      return this.compactLoadData ? 'top: 2px; right: 60px' : `top: 6px; right: ${LOAD_DATA_RIGHT}px`
    },
    ProcessDefinitionVersionActionsPlugin: function() {
      return this.$options.components && this.$options.components.ProcessDefinitionVersionActionsPlugin
        ? this.$options.components.ProcessDefinitionVersionActionsPlugin
        : null
    },
    selectedDeploymentId() {
      return this.processDefinitions.find(v => v.version === this.versionIndex)?.deploymentId ?? null
    },
    selectedIncidents() {
      const statistics = this.selectedProcess?.statistics
      if (!statistics) return null
      return statistics.reduce((sum, activity) =>
        sum + (activity.incidents ?? []).reduce((count, incident) => count + (incident.incidentCount ?? 0), 0), 0)
    }
  },
  watch: {
    selectedDeploymentId: {
      handler: 'loadSelectedDeployment',
      immediate: true
    },
    selectedIncidents: {
      handler: 'rememberSelectedIncidents',
      immediate: true
    },
    'selectedProcess.id': 'rememberSelectedIncidents',
    // a reloaded list starts over, like its running and finished counts
    processDefinitions() {
      this.incidentsByVersionId = {}
    }
  },
  methods: {
    formatDate,
    formatDateForTooltips,
    // lazy loaded counts come as '-' until the version is selected
    countOrDash(value) {
      return typeof value === 'number' ? value : '-'
    },
    fitLoadData() {
      const button = this.$refs.loadData?.$el ?? this.$refs.loadData
      if (!button) return
      // the width with the label, kept to know when it fits again
      if (!this.compactLoadData) this.loadDataWidth = button.offsetWidth
      // the caption is drawn by SidebarsFlow, in the header button of the panel holding this content
      let caption = null
      for (let panel = this.$el.parentElement, level = 0; panel && !caption && level < 3; panel = panel.parentElement, level++) {
        caption = panel.querySelector(':scope > button h2')
      }
      let captionEnd = this.$el.getBoundingClientRect().left
      if (caption) {
        const text = document.createRange()
        text.selectNodeContents(caption)
        captionEnd = text.getBoundingClientRect().right
      }
      const labelStart = this.$el.getBoundingClientRect().right - LOAD_DATA_RIGHT - this.loadDataWidth
      this.compactLoadData = labelStart < captionEnd + 8
    },
    // the selected version shows its live count, the others the one they had when selected or the loaded data
    incidentsOf(version) {
      if (version.id === this.selectedProcess?.id) return this.selectedIncidents
      return this.incidentsByVersionId[version.id] ?? version.incidents
    },
    // from the deployment read when the version was selected, or the loaded data
    deploymentTimeOf(version) {
      return this.deployments[version.deploymentId]?.deploymentTime ?? version.deploymentTime ?? null
    },
    rememberSelectedIncidents() {
      if (this.selectedIncidents !== null) this.incidentsByVersionId[this.selectedProcess.id] = this.selectedIncidents
    },
    async loadSelectedDeployment(deploymentId) {
      if (!deploymentId || this.deployments[deploymentId] || !this.canReadDeployment(deploymentId)) return
      this.deployments[deploymentId] = await ProcessService.findDeployment(deploymentId)
    },
    // one version's details open at a time
    toggleDetails(versionId) {
      this.expandedVersionId = this.expandedVersionId === versionId ? null : versionId
    },
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

<style scoped>
.min-width-0 {
  min-width: 0;
}
/* the size of the icon it replaces, so the button keeps its size */
.load-data-spinner {
  width: 18px;
  height: 18px;
  border-width: 2px;
}
/* danger 20% darker, so its text reaches the contrast needed on white */
.delete-version {
  --delete-version-color: color-mix(in srgb, var(--bs-danger) 80%, #000);
  --bs-btn-color: var(--delete-version-color);
  --bs-btn-border-color: var(--delete-version-color);
  /* hover and press only tint the background, the text keeps its color */
  --bs-btn-hover-color: var(--delete-version-color);
  --bs-btn-hover-bg: color-mix(in srgb, var(--delete-version-color) 10%, transparent);
  --bs-btn-hover-border-color: var(--delete-version-color);
  --bs-btn-active-color: var(--delete-version-color);
  --bs-btn-active-bg: color-mix(in srgb, var(--delete-version-color) 20%, transparent);
  --bs-btn-active-border-color: var(--delete-version-color);
}
</style>
