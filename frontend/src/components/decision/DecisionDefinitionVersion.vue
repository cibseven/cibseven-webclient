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
    <div @mousedown="handleMouseDown" class="v-resizable position-absolute w-100" style="left: 0" :style="'height: ' + bpmnViewerHeight + 'px; ' + toggleTransition">
      <DmnViewer ref="diagram" class="h-100" />
    </div>

    <ul class="nav nav-tabs position-absolute border-0 bg-light" style="left: -1px" :style="'top: ' + (bottomContentPosition - toggleButtonHeight) + 'px; ' + toggleTransition">
      <span role="button" size="sm" variant="light" class="border-bottom-0 bg-white rounded-top border py-1 px-2 me-1" @click="toggleContent">
        <span class="mdi mdi-18px" :class="toggleIcon"></span>
      </span>
      <li class="nav-item m-0" v-for="(tab, index) in tabs" :key="index">
        <a role="button" @click="changeTab(tab)" class="nav-link py-2" :class="{ 'active': tab.active, 'bg-light border border-bottom-0': !tab.active }">
          {{ $t('decision.' + tab.id) }}
        </a>
      </li>
    </ul>

    <div class="position-absolute w-100" style="left: 0; bottom: 0" :style="'top: ' + bottomContentPosition + 'px; ' + toggleTransition">
      <div v-if="activeTab === 'instances'">
        <div ref="filterTable" class="bg-light d-flex position-absolute w-100">
          <div class="col-3 p-3">
            <b-input-group size="sm">
              <template #prepend>
                <b-button :title="$t('searches.search')" aria-hidden="true" size="sm" class="rounded-left" variant="secondary"><span class="mdi mdi-magnify" style="line-height: initial"></span></b-button>
              </template>
              <b-form-input :title="$t('searches.search')" size="sm" :placeholder="$t('searches.search')" @input="search"></b-form-input>
            </b-input-group>
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
import { BWaitingBox } from 'cib-common-components'
import { mapGetters, mapActions } from 'vuex'
import { debounce } from '@/utils/debounce.js'

export default {
  name: 'DecisionDefinitionVersion',
  components: { DmnViewer, InstancesTable, BWaitingBox },
  mixins: [permissionsMixin, resizerMixin],
  props: {
    versionIndex: String,
    loading: Boolean,
    decisionKey: String
  },
  data: function() {
    return {
      topBarHeight: 0,
      tabs: [ { id: 'instances', active: true } ],
      activeTab: 'instances',
      sortByDefaultKey: 'startTimeOriginal',
      sorting: false,
      sortDesc: true,
      decisionInstances: [],
      firstResult: 0,
      maxResults: this.$root.config.maxProcessesResults
    }
  },
  computed: {
    ...mapGetters(['getSelectedDecisionVersion']),
    decision: function() {
      return this.getSelectedDecisionVersion(this.decisionKey)
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
      this.tabs.forEach((tab) => {
        tab.active = tab.id === selectedTab.id
      })
      this.activeTab = selectedTab.id
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
        let data = {
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
    search: debounce(800, function(evt) { 
      this.filter = evt.target.value
      this.firstResult = 0
      this.loadInstances()
    })
  }
}
</script>
