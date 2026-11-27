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
  <div v-if="instance" class="h-100">
    <router-link
      class="btn btn-light rounded-0 border-bottom text-start w-100 align-middle d-flex align-items-center"
      :to="backLink">
      <span class="mdi mdi-18px mdi-arrow-left me-2 float-start"></span>
      <h5 class="m-0">
        {{ $t('decision.decisionInstanceId') }}: {{ instanceId }}
      </h5>
    </router-link>

    <ViewerFrame :resizerMixin="this">
      <DmnViewer ref="diagram" class="h-100" @view-changed="onViewChanged" @viewbox-changed="onViewboxChanged" :sidebar-left-open="leftOpen" />
    </ViewerFrame>

    <div class="position-absolute w-100" style="left: 0; z-index: 1" :style="'height: '+ tabsAreaHeight +'px; top: ' + (bottomContentPosition - tabsAreaHeight + 1) + 'px; ' + toggleTransition">
      <div class="d-flex align-items-end">
        <ScrollableTabsContainer :tabs-area-height="tabsAreaHeight">
          <GenericTabs :tabs="tabs" :modelValue="activeTab" @update:modelValue="changeTab" @tab-click=";"></GenericTabs>
        </ScrollableTabsContainer>
      </div>
    </div>

    <div ref="rContent" class="position-absolute w-100 border-top" style="left: 0; bottom: 0" :style="'top: ' + bottomContentPosition + 'px; ' + toggleTransition">
      <div v-if="activeTab === 'inputs'">
        <div class="overflow-auto bg-white position-absolute w-100" style="top: 0; left: 0; bottom: 0">
          <div v-if="hasDeepLinks" class="p-2">
            <DeepLinkButtons section="decisionInstance" :params="matchedDeepLinkParams" />
          </div>
          <FlowTable striped resizable thead-class="sticky-header" :items="instance.inputs" primary-key="id" :fields="[
            { label: 'decision.name', key: 'clauseName', class: 'col-4', tdClass: 'py-1' },
            { label: 'decision.type', key: 'type', class: 'col-4', tdClass: 'py-1' },
            { label: 'decision.value', key: 'value', class: 'col-4', tdClass: 'py-1' }]">
          </FlowTable>
        </div>
      </div>
      <div v-else-if="activeTab === 'outputs'">
        <div class="overflow-auto bg-white position-absolute w-100" style="top: 0; left: 0; bottom: 0">
          <div v-if="hasDeepLinks" class="p-2">
            <DeepLinkButtons section="decisionInstance" :params="matchedDeepLinkParams" />
          </div>
          <FlowTable striped resizable thead-class="sticky-header" :items="instance.outputs" primary-key="id" :fields="[
            { label: 'decision.name', key: 'clauseName', class: 'col-4', tdClass: 'py-1' },
            { label: 'decision.type', key: 'type', class: 'col-4', tdClass: 'py-1' },
            { label: 'decision.value', key: 'value', class: 'col-4', tdClass: 'py-1' }]">
          </FlowTable>
        </div>
      </div>
      <DeepLinkFrame v-else-if="matchedDeepLink" :link="matchedDeepLink" :params="matchedDeepLinkParams"></DeepLinkFrame>

      <PluginSlot name="decision-instance-tab" :only="activeTab"
        :params="{ instance: instance, decision: decisionDefinition, tenantId: instance?.tenantId }"></PluginSlot>
    </div>
  </div>
</template>

<script>
import { permissionsMixin } from '@/permissions.js'
import { DecisionService } from '@/services.js'
import DmnViewer from '@/components/decision/DmnViewer.vue'
import resizerMixin from '@/components/process/mixins/resizerMixin.js'
import bpmnViewportPersistenceMixin from '@/components/process/mixins/bpmnViewportPersistenceMixin.js'
import viewerFrameSizePersistenceMixin from '@/components/process/mixins/viewerFrameSizePersistenceMixin.js'
import ScrollableTabsContainer from '@/components/common-components/ScrollableTabsContainer.vue'
import ViewerFrame from '@/components/common-components/ViewerFrame.vue'
import DeepLinkFrame from '@/components/common-components/DeepLinkFrame.vue'
import { FlowTable, GenericTabs } from '@cib/common-frontend'
import { mapActions, mapGetters } from 'vuex'
import { getDeepLinkEntries, hasDeepLinks } from '@/utils/deepLinks.js'
import DeepLinkButtons from '@/components/common-components/DeepLinkButtons.vue'
import PluginSlot from '@/components/common/PluginSlot.vue'
import { defineTabBar } from '@/utils/tabBar.js'

const BUILTIN_TABS = [
  { id: 'inputs', text: 'decision.inputs' },
  { id: 'outputs', text: 'decision.outputs' }
]

const RESERVED_TAB_IDS = BUILTIN_TABS.map(tab => tab.id)

// Value types whose content history does not return here, see hasShownValue
const NOT_FETCHED_TYPES = ['Bytes', 'File']

const tabsFor = defineTabBar({
  deepLinkSection: 'decisionInstance',
  pluginSlot: 'decision-instance-tab',
  builtin: BUILTIN_TABS
})

export default {
  name: 'DecisionInstance',
  components: { DmnViewer, FlowTable, GenericTabs, ScrollableTabsContainer, ViewerFrame, DeepLinkFrame, DeepLinkButtons, PluginSlot },
  mixins: [permissionsMixin, resizerMixin, bpmnViewportPersistenceMixin, viewerFrameSizePersistenceMixin],
  inject: ['currentLanguage'],
  props: {
    versionIndex: String,
    instanceId: String,
    loading: Boolean,
    leftOpen: { type: Boolean, default: true }
  },
  data() {
    return {
      instance: null,
      activeTab: 'inputs',
      diagramTimer: null
    }
  },
  computed: {
    ...mapGetters('diagram', ['isDiagramReady']),
    ...mapGetters(['getSelectedDecisionVersion']),
    // Back to the version this instance belongs to, with the definition's tenant: the instance's can be its process's (CIB7-2392)
    backLink() {
      const tenantId = this.decisionDefinition?.tenantId
      return {
        path: `/seven/auth/decision/${this.instance.decisionDefinitionKey}/${this.versionIndex}`,
        query: tenantId ? { tenantId } : {}
      }
    },
    decisionDefinition() {
      return this.getSelectedDecisionVersion()
    },
    tabs() {
      return tabsFor({ config: this.$root.config, t: this.$t })
    },
    hasDeepLinks() {
      return hasDeepLinks(this.$root.config, 'decisionInstance', 'button')
    },
    matchedDeepLink() {
      return getDeepLinkEntries(this.$root.config, 'decisionInstance', RESERVED_TAB_IDS)
        .filter(entry => entry.type === 'tab')
        .find(entry => entry.id === this.activeTab)
    },
    matchedDeepLinkParams() {
      return {
        decisionInstanceId: this.instance?.id,
        decisionInstanceTenantId: this.instance?.tenantId,

        processInstanceId: this.instance?.processInstanceId,

        decisionDefinitionId: this.instance?.decisionDefinitionId,
        decisionDefinitionKey: this.instance?.decisionDefinitionKey,
        decisionDefinitionTenantId: this.decisionDefinition?.tenantId,
        decisionDefinitionVersion: this.decisionDefinition?.version,
        decisionDefinitionVersionTag: this.decisionDefinition?.versionTag,

        lang: this.currentLanguage()
      }
    }
  },
  watch: {
    isDiagramReady(isReady) {
      if (isReady && this.instance) this.applyInstanceValues()
    },
    instance: {
      handler() {
        if (this.isDiagramReady) this.applyInstanceValues()
      },
      deep: true
    }
  },
  mounted() {
    const params = {
      decisionInstanceId: this.instanceId,
      includeInputs: true,
      includeOutputs: true,
      disableBinaryFetching: true,
      disableCustomObjectDeserialization: true,
      maxResults: 1
    }
    this.instance = DecisionService.getHistoricDecisionInstances(params).then(instances => {
      this.instance = instances[0]
      this.loadDiagram()
    })
  },
  beforeUnmount() {
    clearTimeout(this.diagramTimer)
  },
  methods: {
    ...mapActions(['getXmlById']),
    changeTab(selectedTab) {
      this.activeTab = selectedTab
    },
    loadDiagram() {
      this.getXmlById(this.instance.decisionDefinitionId).then(response => {
        clearTimeout(this.diagramTimer)
        this.diagramTimer = setTimeout(() => {
          // Gone if the view was left while the diagram loaded, which unmount cannot cancel
          // A DRD holds several decisions: open the one this instance evaluated
          this.$refs.diagram?.showDiagram(response.dmnXml, this.instance.decisionDefinitionKey).then(() => this.restoreViewboxIfSaved())
        }, 100)
      })
      .catch(error => {
        console.error("Error loading diagram:", error)
      })
    },
    viewboxStorageKey() {
      return `cibseven:dmn-viewbox:${this.instance.decisionDefinitionId}`
    },
    viewerFrameStorageKey() {
      return 'cibseven:viewer-frame-size:decision'
    },
    onViewChanged() {
      this.applyInstanceValues()
    },
    applyInstanceValues() {
      const container = this.$refs.diagram?.$refs?.diagram
      if (!container) return
      const table = container.querySelector('.tjs-table') || container.querySelector('table')
      if (!table) return
      this.applyInputHeaders(table)
      this.applyOutputRows(table)
    },
    applyInputHeaders(table) {
      table.querySelectorAll('th.input-cell').forEach(th => {
        const colId = th.getAttribute('data-col-id')
        const input = this.instance?.inputs?.find(i => i.clauseId === colId)
        if (!colId || !input || !this.hasShownValue(input)) return
        const clauseDiv = th.querySelector('div.clause')
        if (!clauseDiv || clauseDiv.hasAttribute('data-original-text')) return
        const original = clauseDiv.textContent.trim()
        clauseDiv.setAttribute('data-original-text', original)
        clauseDiv.textContent = ''
        const span = document.createElement('span')
        span.textContent = `${original} `
        clauseDiv.appendChild(span)
        const bold = document.createElement('strong')
        bold.className = 'fw-bold'
        bold.textContent = `= ${this.formatValue(input.value)}`
        clauseDiv.appendChild(bold)
      })
    },
    // The engine reports the rule that produced each output (ruleId); dmn-js puts that rule's id on
    // every cell of its row (data-row-id). History records outputs only for non-empty output entries,
    // so a fired rule without any output value leaves no trace and cannot be marked.
    applyOutputRows(table) {
      const outputs = (this.instance?.outputs ?? []).filter(output => output.ruleId)
      const firedRuleIds = new Set(outputs.map(output => output.ruleId))

      table.querySelectorAll('tbody td.rule-index[data-row-id]').forEach(indexCell => {
        const ruleId = indexCell.getAttribute('data-row-id')
        const row = indexCell.closest('tr')
        if (!firedRuleIds.has(ruleId) || !row) return
        row.classList.add('fired-rule')

        const ruleCells = Array.from(row.querySelectorAll('td.output-cell'))
        outputs.filter(output => output.ruleId === ruleId && this.hasShownValue(output)).forEach(output => {
          const cell = output.clauseId
            ? ruleCells.find(td => td.getAttribute('data-col-id') === output.clauseId)
            : (ruleCells.length === 1 ? ruleCells[0] : undefined)
          if (cell) this.applyHighlightText(cell, output.value)
        })
      })
    },
    // Binary values are not fetched (disableBinaryFetching): the engine sends them with value null,
    // which must not read as a null result. A failed value carries only its error.
    hasShownValue({ type, errorMessage }) {
      return !errorMessage && !NOT_FETCHED_TYPES.includes(type)
    },
    applyHighlightText(td, value) {
      if (td.hasAttribute('data-original-text')) return
      const originalValue = td.textContent.trim()
      td.setAttribute('data-original-text', originalValue)
      td.textContent = `${originalValue} `
      const span = document.createElement('span')
      span.className = 'fw-bold'
      span.textContent = `= ${this.formatValue(value)}`
      td.appendChild(span)
    },
    formatValue(value) {
      return value !== null && typeof value === 'object' ? JSON.stringify(value) : String(value)
    }
  }
}
</script>

<style>
/* Not scoped: dmn-js renders the decision table itself. Every cell, because dmn-js colours every
   second row and the sticky rule index cell on their own. */
.dmn-decision-table-container tbody tr.fired-rule > td {
  background-color: var(--bs-success-bg-subtle);
}
</style>