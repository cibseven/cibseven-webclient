<template>
  <div class="overflow-auto bg-white position-absolute container-fluid g-0" style="top: 0; bottom: 0">
    <flow-table v-if="matchedCalledList.length > 0" striped thead-class="sticky-header" :items="matchedCalledList" primary-key="id" prefix="process-instance.calledProcesses."
      sort-by="label" :sort-desc="true" :fields="[
      { label: 'id', key: 'id', class: 'col-4', tdClass: 'py-1 border-end border-top-0' },
      { label: 'process', key: 'process', class: 'col-2', tdClass: 'py-1 border-end border-top-0' },
      { label: 'version', key: 'version', class: 'col-2', tdClass: 'py-1 border-end border-top-0' },
      { label: 'callingActivity', key: 'callingActivity', class: 'col-4', tdClass: 'py-1 border-end border-top-0' }]">
      <template v-slot:cell(process)="table">
        <button :title="table.item.key" class="text-truncate btn btn-link"  @click="openSubprocess(table.item)">{{ table.item.key }}</button>
      </template>
      <template v-slot:cell(version)="table">
        <div :title="table.item.version" class="text-truncate" >{{ table.item.version }}</div>
      </template>
      <template v-slot:cell(id)="table">
        <button :title="table.item.id" class="text-truncate btn btn-link" @click="openInstance(table.item)">{{ table.item.id }}</button>
      </template>
      <template v-slot:cell(callingActivity)="table">
        <div :title="table.item.callingActivity.activityName" class="text-truncate">{{ table.item.callingActivity.activityName }}</div>
      </template>
    </flow-table>
  </div>
</template>

<script>
import { ProcessService, HistoryService } from '@/services.js'
import procesessVariablesMixin from '@/components/process/mixins/processesVariablesMixin.js'
import FlowTable from '@/components/common-components/FlowTable.vue'
export default {
  name: 'CalledProcessInstancesTable',
  components: { FlowTable},
  mixins: [procesessVariablesMixin],
  data: function() {
    return{
      calledInstanceList: [],
      matchedCalledList: []
    }
  },

  props:{
    selectedInstance: Object
  },

  created: function(){
		ProcessService.findCurrentProcessesInstances({"superProcessInstance": this.selectedInstance.id}).then(response => {
			this.calledInstanceList = response
      this.matchedCalledList = this.calledInstanceList.map(processPL => {
        let foundInst = this.activityInstanceHistory.find(processAIH => {
					if (processAIH.activityType === "callActivity"){
						if (processAIH.calledProcessInstanceId === processPL.id){
							return processAIH
						}
					}
				})
				return ({id: processPL.id, callingActivity: foundInst, key: processPL.definitionId.match(/^[^:]+/).toString(), version: processPL.definitionId.match(/(?!:)\d(?=:)/).toString()})
			})
    })
  },

  methods: {
    openSubprocess: function(event) {
      this.$router.push('/seven/auth/process/' + event.key + '/' + event.version)
    },
    openInstance: function(event) {
      this.$router.push('/seven/auth/process/' + event.key + '/' + event.version + '/' + event.id)
    }
  }
}
</script>