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
        { label: 'calledProcessDefinition', key: 'name', class: 'col-4', thClass: 'border-end', tdClass: 'py-1 border-end border-top-0' },
        { label: 'state', key: 'state', class: 'col-4', thClass: 'border-end', tdClass: 'py-1 border-end border-top-0' },
        { label: 'activity', key: 'activity', class: 'col-4', thClass: 'border-end', tdClass: 'py-1 border-end border-top-0' }
      ]">
      <template v-slot:cell(name)="table">
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
      <template v-slot:cell(state)="table">
        <span :title="getCalledProcessState(table.item)" class="text-truncate">
          {{ getCalledProcessState(table.item) }}
        </span>
      </template>
      <template v-slot:cell(activity)="table">
        <button class="btn btn-link text-truncate p-0" :title="table.item.calledFromActivityIds[0]" 
          @click="setHighlightedElement(table.item.calledFromActivityIds[0])">
          {{ $store.state.activity.processActivities[table.item.calledFromActivityIds[0]] }}
      </button>
      </template>
    </FlowTable>
    <div v-else>
      <p class="text-center p-4">{{ $t('process-instance.noResults') }}</p>
    </div>
  </div>
</template>

<script>
import FlowTable from '@/components/common-components/FlowTable.vue'
import { mapActions } from 'vuex'

export default {
  name: 'CalledProcessDefinitionsTable',
  components: { FlowTable },
  props: {
    calledProcesses: Array
  },
  methods: {
    ...mapActions(['setHighlightedElement']),
    getCalledProcessState(item) {
      const instances = item.currentInstances || []
      return instances.length > 0 
        ? this.$t('process-instance.calledProcessDefinitions.runningAndReferenced')
        : this.$t('process-instance.calledProcessDefinitions.referenced')
    }
  },
}
</script>