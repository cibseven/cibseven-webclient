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
  <div v-if="decision" class="h-100">
    <ViewerFrame :resizerMixin="this">
      <DmnViewer ref="diagram" class="h-100" />
    </ViewerFrame>

    <div class="position-absolute w-100" style="left: 0; z-index: 1" :style="'height: '+ tabsAreaHeight +'px; top: ' + (bottomContentPosition - tabsAreaHeight + 1) + 'px; ' + toggleTransition">
      <div class="d-flex align-items-end">
        <ScrollableTabsContainer :tabs-area-height="tabsAreaHeight">
          <GenericTabs :tabs="tabs" :modelValue="activeTab" @update:modelValue="changeTab" @tab-click=";"></GenericTabs>
        </ScrollableTabsContainer>
      </div>
    </div>

    <div class="position-absolute w-100 overflow-hidden border-top" style="left: 0; bottom: 0" :style="'top: ' + bottomContentPosition + 'px; ' + toggleTransition">
      <div v-if="activeTab === 'instances'">
        <div ref="filterTable" class="bg-white d-flex position-absolute w-100">
          <div class="container-fluid p-2">
            <div class="row align-items-center pb-1">
              <div class="col-8">
                <div class="border rounded d-flex flex-fill align-items-center">
                  <b-button @click.stop="search(filter)"
                    size="sm" class="mdi mdi-magnify mdi-24px text-secondary" variant="link"
                    :title="$t('searches.refreshAndFilter')"></b-button>
                  <div class="flex-grow-1">
                    <label class="visually-hidden" for="filter-decision-instances">{{ $t('searches.filter') }}</label>
                    <input
                      id="filter-decision-instances"
                      type="text"
                      :placeholder="$t('searches.filter')"
                      class="form-control-plaintext w-100"
                      @input="search($event.target.value)"
                    />
                  </div>
                </div>
              </div>
              <div class="col-4">
                <component :is="DecisionDefinitionVersionActionsPlugin" v-if="DecisionDefinitionVersionActionsPlugin" :decision="decision" :decision-key="decisionKey"></component>
              </div>
            </div>
          </div>
        </div>
        <div ref="rContent" class="overflow-auto bg-white position-absolute w-100" style="top: 60px; left: 0; bottom: 0" @scroll="handleScrollDecisions">
          <InstancesTable ref="instancesTable" v-if="!loading && decisionInstances.length > 0 && !sorting" :instances="decisionInstances" :sortByDefaultKey="sortByDefaultKey" :sortDesc="sortDesc"></InstancesTable>
          <div v-else-if="loading" class="py-3 text-center w-100">
            <BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('admin.loading') }}
          </div>
          <div v-else>
            <p class="text-center p-4">{{ $t('process-instance.noResults') }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>

import { permissionsMixin } from '@/permissions.js'
import DmnViewer from '@/components/decision/DmnViewer.vue'
import InstancesTable from '@/components/decision/InstancesTable.vue'
import resizerMixin from '@/components/process/mixins/resizerMixin.js'
import ScrollableTabsContainer from '@/components/common-components/ScrollableTabsContainer.vue'
import ViewerFrame from '@/components/common-components/ViewerFrame.vue'
import { BWaitingBox, GenericTabs } from '@cib/common-frontend'
import { mapGetters, mapActions } from 'vuex'
import { debounce } from '@/utils/debounce.js'

export default {
  name: 'DecisionDefinitionVersion',
  components: { DmnViewer, InstancesTable, ViewerFrame, BWaitingBox, GenericTabs, ScrollableTabsContainer },
  mixins: [permissionsMixin, resizerMixin],
  props: {
    versionIndex: String,
    loading: Boolean,
    decisionKey: String
  },
  data: function() {
    return {
      topBarHeight: 0,
      tabs: [ { id: 'instances', text: 'decision.instances' } ],
      activeTab: 'instances',
      sortByDefaultKey: 'evaluationTime',
      sorting: false,
      sortDesc: true,
      decisionInstances: [],
      firstResult: 0,
      maxResults: this.$root.config.maxProcessesResults,
      filter: null
    }
  },
  computed: {
    ...mapGetters(['getSelectedDecisionVersion']),
    decision: function() {
      return this.getSelectedDecisionVersion()
    },
    DecisionDefinitionVersionActionsPlugin: function() {
      return this.$options.components && this.$options.components.DecisionDefinitionVersionActionsPlugin
        ? this.$options.components.DecisionDefinitionVersionActionsPlugin
        : null
    }
  },
  mounted: function() {
    if (this.decision) {
      this.loadDiagram()
      this.loadInstances()
    }
  },
  methods: {
    ...mapActions(['getXmlById', 'getHistoricDecisionInstances']),
    changeTab: function(selectedTab) {
      this.activeTab = selectedTab
    },
    loadDiagram() {
      this.getXmlById(this.decision.id)
        .then(response => {
          setTimeout(() => {
            this.$refs.diagram.showDiagram(response.dmnXml)
          }, 100)
        })
        .catch(error => {
          console.error("Error loading diagram:", error)
        })
    },
    handleScrollDecisions: function(el) {
      // TODO: Check method
      if (this.decisionInstances.length < this.firstResult) return
      if (Math.ceil(el.target.scrollTop + el.target.clientHeight) >= el.target.scrollHeight) {
        this.showMore()
      }
    },
    loadInstances(showMore) {
      if (this.$root.config.camundaHistoryLevel !== 'none') {
        const data = {
          key: this.decision.key,
          version: this.versionIndex,
          params: {
            decisionDefinitionKey: this.decision.key,
            decisionDefinitionId: this.decision.id,
            includeInputs: true,
            includeOutputs: true,
            firstResult: this.firstResult,
            maxResults: this.maxResults
          }
        }
        if (this.filter) data.params.decisionInstanceId = this.filter
        this.getHistoricDecisionInstances(data).then((instances) => {
          if (!showMore) this.decisionInstances = instances
          else this.decisionInstances = !this.decisionInstances ? instances : this.decisionInstances.concat(instances)
        })
      } else {
        // TODO: Implement
        console.error("Not implemented for seven history level: none")
      }
    },
    showMore() {
      this.firstResult += this.$root.config.maxProcessesResults
      this.loadInstances(true)
    },
    search: debounce(800, function(filter) {
      this.filter = filter
      this.firstResult = 0
      this.loadInstances()
    })
  }
}
</script>
