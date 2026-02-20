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
      :to="'/seven/auth/decision/' + instance.decisionDefinitionKey + '/' + versionIndex">
      <span class="mdi mdi-18px mdi-arrow-left me-2 float-start"></span>
      <h5 class="m-0">
        {{ $t('decision.decisionInstanceId') }}: {{ instanceId }}
      </h5>
    </router-link>

    <ViewerFrame :resizerMixin="this">
      <DmnViewer ref="diagram" class="h-100" @view-changed="onViewChanged" />
    </ViewerFrame>

    <div class="position-absolute w-100" style="left: 0; z-index: 1" :style="'height: '+ tabsAreaHeight +'px; top: ' + (bottomContentPosition - tabsAreaHeight + 1) + 'px; ' + toggleTransition">
      <div class="d-flex align-items-end">
        <ScrollableTabsContainer :tabs-area-height="tabsAreaHeight">
          <GenericTabs :tabs="tabs" :modelValue="activeTab" @update:modelValue="changeTab" @tab-click=";"></GenericTabs>
        </ScrollableTabsContainer>
      </div>
    </div>

    <div class="position-absolute w-100 border-top" style="left: 0; bottom: 0" :style="'top: ' + bottomContentPosition + 'px; ' + toggleTransition">
      <div v-if="activeTab === 'inputs'">
        <div ref="rContent" class="overflow-auto bg-white position-absolute w-100" style="top: 0; left: 0; bottom: 0">
          <FlowTable striped resizable thead-class="sticky-header" :items="instance.inputs" primary-key="id" :fields="[
            { label: 'decision.name', key: 'clauseName', class: 'col-4', tdClass: 'py-1' },
            { label: 'decision.type', key: 'type', class: 'col-4', tdClass: 'py-1' },
            { label: 'decision.value', key: 'value', class: 'col-4', tdClass: 'py-1' }]">
          </FlowTable>
        </div>
      </div>
      <div v-if="activeTab === 'outputs'">
        <div ref="rContent" class="overflow-auto bg-white position-absolute w-100" style="top: 0; left: 0; bottom: 0">
          <FlowTable striped resizable thead-class="sticky-header" :items="instance.outputs" primary-key="id" :fields="[
            { label: 'decision.name', key: 'clauseName', class: 'col-4', tdClass: 'py-1' },
            { label: 'decision.type', key: 'type', class: 'col-4', tdClass: 'py-1' },
            { label: 'decision.value', key: 'value', class: 'col-4', tdClass: 'py-1' }]">
          </FlowTable>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { permissionsMixin } from '@/permissions.js'
import { DecisionService } from '@/services.js'
import DmnViewer from '@/components/decision/DmnViewer.vue'
import resizerMixin from '@/components/process/mixins/resizerMixin.js'
import ScrollableTabsContainer from '@/components/common-components/ScrollableTabsContainer.vue'
import ViewerFrame from '@/components/common-components/ViewerFrame.vue'
import { FlowTable, GenericTabs } from '@cib/common-frontend'
import { mapActions, mapGetters } from 'vuex'

export default {
  name: 'DecisionInstance',
  components: { DmnViewer, FlowTable, GenericTabs, ScrollableTabsContainer, ViewerFrame },
  mixins: [permissionsMixin, resizerMixin],
  props: {
    versionIndex: String,
    instanceId: String,
    loading: Boolean
  },
  data() {
    return {
      instance: null,
      tabs: [
        { id: 'inputs', text: 'decision.inputs' },
        { id: 'outputs', text: 'decision.outputs' }
      ],
      activeTab: 'inputs'
    }
  },
  computed: {
    ...mapGetters('diagram', ['isDiagramReady'])
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
  methods: {
    ...mapActions(['getXmlById']),
    changeTab(selectedTab) {
      this.activeTab = selectedTab
    },
    loadDiagram() {
      this.getXmlById(this.instance.decisionDefinitionId).then(response => {
        setTimeout(() => {
          this.$refs.diagram.showDiagram(response.dmnXml)
        }, 100)
      })
      .catch(error => {
        console.error("Error loading diagram:", error)
      })
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
        if (!colId || !input) return
        const clauseDiv = th.querySelector('div.clause')
        if (!clauseDiv || clauseDiv.getAttribute('data-original-text')) return
        const original = clauseDiv.textContent.trim()
        clauseDiv.setAttribute('data-original-text', original)
        clauseDiv.textContent = ''
        const span = document.createElement('span')
        span.textContent = original + ' '
        clauseDiv.appendChild(span)
        const bold = document.createElement('strong')
        bold.className = 'fw-bold'
        bold.textContent = '= ' + String(input.value)
        clauseDiv.appendChild(bold)
      })
    },
    applyOutputRows(table) {
      const headerRow = table.querySelector('thead tr')
      if (!headerRow) return
      const headerCells = Array.from(headerRow.children)

      const tbody = table.querySelector('tbody')
      if (!tbody) return

      // Build input column map: colIndex → expected input value
      const inputColumns = []
      table.querySelectorAll('th.input-cell').forEach(th => {
        const colId = th.getAttribute('data-col-id')
        const input = this.instance?.inputs?.find(i => i.clauseId === colId)
        if (colId && input) {
          inputColumns.push({
            colIndex: headerCells.indexOf(th),
            value: String(input.value).trim()
          })
        }
      })
      table.querySelectorAll('th.output-cell').forEach(th => {
        const colId = th.getAttribute('data-col-id')
        const output = colId
          ? this.instance?.outputs?.find(o => o.clauseId === colId)
          : this.instance?.outputs?.[0]
        if (!output || !output.value) return

        const outputColIndex = headerCells.indexOf(th)
        const outputValue = String(output.value).trim()

        Array.from(tbody.rows).forEach(row => {
          const td = row.children[outputColIndex]
          if (!td) return

          const tdText = td.getAttribute('data-original-text') ?? td.textContent
          if (this.normalizeCell(tdText) !== outputValue) return

          // DMN string literals ("value") can be compared exactly; expressions (< 250) cannot
          const disqualified = inputColumns.some(({ colIndex, value }) => {
            const cell = row.children[colIndex]
            if (!cell) return false
            const raw = (cell.getAttribute('data-original-text') ?? cell.textContent).trim()
            if (!this.isDmnStringLiteral(raw)) return false
            return this.normalizeCell(raw) !== value
          })

          if (!disqualified) this.applyHighlightText(row, td)
        })
      })
    },
    applyHighlightText(row, td) {
      if (td.getAttribute('data-original-text')) return
      td.setAttribute('data-original-text', td.textContent.trim())
      const originalValue = td.getAttribute('data-original-text')
      td.textContent = originalValue + ' '
      const span = document.createElement('span')
      span.className = 'fw-bold'
      span.textContent = '= ' + String(originalValue)
      td.appendChild(span)
    },
    // Strips surrounding DMN double-quotes and trims whitespace for exact comparison
    normalizeCell(text) {
      return text.trim().replace(/^"|"$/g, '').trim()
    },
    // Returns true if the cell text is a DMN string literal (wrapped in double quotes)
    isDmnStringLiteral(text) {
      return /^".*"$/.test(text.trim())
    }
  }
}
</script>