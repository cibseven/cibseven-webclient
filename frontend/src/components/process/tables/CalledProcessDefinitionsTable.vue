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
  <div class="overflow-auto bg-white container-fluid g-0">
    <FlowTable v-if="calledProcesses.length > 0" resizable striped thead-class="sticky-header" :items="calledProcesses" primary-key="id" prefix="process-instance.calledProcessDefinitions."
      sort-by="label" :sort-desc="true" :fields="[
      { label: 'process', key: 'process', class: 'col-2', thClass: 'border-end', tdClass: 'py-1 border-end border-top-0' },
      { label: 'version', key: 'version', class: 'col-4', thClass: 'border-end', tdClass: 'py-1 border-end border-top-0' },
      { label: 'activities', key: 'activities', class: 'col-6', thClass: 'border-end', tdClass: 'py-1 border-end border-top-0' }
]">
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
      {{ table.item.process.name }}
</router-link>
      </template>
      <template v-slot:cell(version)="table">
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
      {{ table.item.process.name }}
      </router-link>
      </template>
      <template v-slot:cell(activities)="table">
        <div class="d-flex flex-column">
        <span v-for="(act, index) in table.item.activities" :key="index" :title="act.activityName" class="d-block">{{ act.activityName }}</span>
      </div>
      </template>
    </FlowTable>
    <div v-else>
      <p class="text-center p-4">{{ $t('process-instance.noResults') }}</p>
    </div>
  </div>
</template>

<script>
import processesVariablesMixin from '@/components/process/mixins/processesVariablesMixin.js'
import FlowTable from '@/components/common-components/FlowTable.vue'

export default {
  name: 'CalledProcessDefinitionsTable',
  components: {FlowTable},
  mixins: [processesVariablesMixin],
  props: { 
    process: Object,
    instances: Array,
    calledProcesses: Array
  }
}
</script>