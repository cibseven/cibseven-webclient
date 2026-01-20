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

    <div @mousedown="handleMouseDown" class="v-resizable position-absolute w-100" style="left: 0" :style="'height: ' + bpmnViewerHeight + 'px; ' + toggleTransition">
      <DmnViewer ref="diagram" class="h-100" />
      <span role="button" size="sm" variant="light" class="bg-white px-2 py-1 me-1 position-absolute border rounded" style="bottom: 90px; right: 11px;" @click="toggleContent">
        <span class="mdi mdi-18px" :class="toggleIcon"></span>
      </span>
    </div>

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
          <FlowTable striped resizable thead-class="sticky-header" :items="instance.inputs" primary-key="id" prefix="decision." :fields="[
            { label: 'name', key: 'clauseName', class: 'col-4', tdClass: 'py-1' },
            { label: 'type', key: 'type', class: 'col-4', tdClass: 'py-1' },
            { label: 'value', key: 'value', class: 'col-4', tdClass: 'py-1' }]">
          </FlowTable>
        </div>
      </div>
      <div v-if="activeTab === 'outputs'">
        <div ref="rContent" class="overflow-auto bg-white position-absolute w-100" style="top: 0; left: 0; bottom: 0">
          <FlowTable striped resizable thead-class="sticky-header" :items="instance.outputs" primary-key="id" prefix="decision." :fields="[
            { label: 'name', key: 'clauseName', class: 'col-4', tdClass: 'py-1' },
            { label: 'type', key: 'type', class: 'col-4', tdClass: 'py-1' },
            { label: 'value', key: 'value', class: 'col-4', tdClass: 'py-1' }]">
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
import { FlowTable, GenericTabs } from '@cib/common-frontend'
import { mapActions } from 'vuex'

export default {
  name: 'DecisionInstance',
  components: { DmnViewer, FlowTable, GenericTabs, ScrollableTabsContainer },
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
    }
  }
}
</script>
<style scoped>
.active-tab-border {
  border-bottom: 3px solid white!important;
}
</style>
