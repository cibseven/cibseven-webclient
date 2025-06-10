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
  <div class="overflow-auto bg-white container-fluid g-0 h-100" >
    <flow-table v-if="!loading && matchedCalledList.length > 0" striped thead-class="sticky-header" :items="matchedCalledList" primary-key="id" prefix="process-instance.calledProcesses."
      sort-by="label" :sort-desc="true" :fields="[
      { label: 'id', key: 'id', class: 'col-4', tdClass: 'py-1 border-end border-top-0' },
      { label: 'process', key: 'process', class: 'col-2', tdClass: 'py-1 border-end border-top-0' },
      { label: 'version', key: 'version', class: 'col-2', tdClass: 'py-1 border-end border-top-0' },
      { label: 'callingActivity', key: 'callingActivity', class: 'col-4', tdClass: 'py-1 border-end border-top-0' }]">
      <template v-slot:cell(process)="table">
        <router-link
          :to="{
            name: 'process', 
            params: {
              processKey: table.item.key,
              versionIndex: table.item.version
            }
          }"
          :title="table.item.name"
          class="text-truncate"
      >
      {{ table.item.name }}
</router-link>
      </template>
      <template v-slot:cell(version)="table">
        <div :title="table.item.version" class="text-truncate" >{{ table.item.version }}</div>
      </template>
      <template v-slot:cell(id)="table">
        <router-link
          :to="{
            name: 'process', 
            params: {
              processKey: table.item.key,
              versionIndex: table.item.version,
              instanceId: table.item.id
            }
          }"
          :title="table.item.name"
          class="text-truncate"
        >
        {{ table.item.id }}
        </router-link>
      </template>
      <template v-slot:cell(callingActivity)="table">
        <div :title="table.item.callingActivity.activityName" class="text-truncate">{{ table.item.callingActivity.activityName }}</div>
      </template>
    </flow-table>
    <div v-else-if="loading">
      <p class="text-center p-4"><BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('admin.loading') }}</p>
    </div>
    <div v-else>
      <p class="text-center p-4">{{ $t('process-instance.noResults') }}</p>
    </div>
  </div>
</template>

<script>
import { ProcessService } from '@/services.js'
import processesVariablesMixin from '@/components/process/mixins/processesVariablesMixin.js'
import FlowTable from '@/components/common-components/FlowTable.vue'
import { BWaitingBox } from 'cib-common-components'
export default {
  name: 'CalledProcessInstancesTable',
  components: { FlowTable, BWaitingBox},
  mixins: [processesVariablesMixin],
  data: function() {
    return{
      calledInstanceList: [],
      matchedCalledList: [],
      loading: true
    }
  },
  watch: {
    activityInstanceHistory: 'loadCalledProcesses'
  },

  props:{
    selectedInstance: Object
  },

  created: function(){
    if (this.activityInstanceHistory) {
      this.loadCalledProcesses()
    }
  },

  methods: {
    loadCalledProcesses: function() {
      ProcessService.findCurrentProcessesInstances({"superProcessInstance": this.selectedInstance.id}).then(response => {
			this.calledInstanceList = response
      let key = null
      this.matchedCalledList = this.calledInstanceList.map(processPL => {
        key = processPL.definitionId.match(/^[^:]+/).at(0)                    
        let foundInst = this.activityInstanceHistory.find(processAIH => {
					if (processAIH.activityType === "callActivity"){
						if (processAIH.calledProcessInstanceId === processPL.id){
							return processAIH
						}
					}
				})
        let foundProcess = this.$store.state.process.list.find(processSPL => {
          if (key === processSPL.key){
            return processSPL
          }
        })
				return ({
          id: processPL.id,
          callingActivity: foundInst,
          key: key,
          version: processPL.definitionId.match(/(?!:)\d(?=:)/).at(0),
          name: foundProcess ? foundProcess.name : key
        })
			})
      this.loading = false
    })
    }
  }
}
</script>
