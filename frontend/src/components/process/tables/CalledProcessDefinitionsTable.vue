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
    <div v-if="showSpinner">
      <p class="text-center p-4"><BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('admin.loading') }}</p>
    </div>
    <FlowTable v-else-if="calledProcessDefinitions.length > 0" resizable striped thead-class="sticky-header" :items="calledProcessDefinitions" primary-key="id" prefix="process-instance.calledProcessDefinitions."
      :fields="[
        { label: 'calledProcessDefinition', key: 'label', class: 'col-4', tdClass: 'py-1' },
        { label: 'state', key: 'state', class: 'col-4', tdClass: 'py-1' },
        { label: 'activity', key: 'activity', class: 'col-4', tdClass: 'py-1' }
      ]">
      <template v-slot:cell(label)="table">
        <CopyableActionButton
            :display-value="table.item.label || table.item.key"
            :title="table.item.name"
            @copy="copyValueToClipboard"
            :to="{
              name: 'process', 
              params: {
                processKey: table.item.definitionKey,
                versionIndex: table.item.version
              },
              query: { parentProcessDefinitionId: process.id, tab: 'instances' }
            }"
          />
      </template>
      <template v-slot:cell(state)="table">
        <span v-if="table.item.activities.length" class="text-truncate">
          {{ $t(getCalledProcessState(table.item)) }}
        </span>
      </template>
      <template v-slot:cell(activity)="table">
        <div class="w-100">
          <CopyableActionButton
            v-for="(act, index) in table.item.activities" :key="index" 
            :display-value="act.activityName"
            :title="act.activityName"
            @click="selectActivity(act.activityId)"
            @copy="copyValueToClipboard"
          />
        </div>
      </template>
    </FlowTable>
    <div v-if="loading && calledProcessDefinitions.length > 0">
      <p class="text-center p-4"><BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('admin.loadingMoreData') }}</p>
    </div>
    <div v-else-if="!initialLoading && calledProcessDefinitions.length === 0">
      <p class="text-center p-4">{{ $t('process-instance.noResults') }}</p>
    </div>
    <SuccessAlert ref="messageCopy" style="z-index: 9999"> {{ $t('process.copySuccess') }} </SuccessAlert>
  </div>
</template>

<script>
import FlowTable from '@/components/common-components/FlowTable.vue'
import { BWaitingBox } from 'cib-common-components'
import { mapActions, mapGetters } from 'vuex'
import CopyableActionButton from '@/components/common-components/CopyableActionButton.vue'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'


export default {
  name: 'CalledProcessDefinitionsTable',
  components: { FlowTable, CopyableActionButton, SuccessAlert, BWaitingBox },
  mixins: [copyToClipboardMixin],
  props: {
    process: Object
  },
  data() {
    return {
      loading: false,
      initialLoading: false
    }
  },
  computed: {
    ...mapGetters('calledProcessDefinitions', [
      'calledProcessDefinitions',
      'getCalledProcessState'
    ]),
    ...mapGetters(['selectedActivityId']),
    ...mapGetters('diagram', ['isDiagramReady']),
    showSpinner() {
      return this.initialLoading && this.calledProcessDefinitions.length === 0
    }
  },
  watch: {
    selectedActivityId() {
      this.setHighlightedElement(this.selectedActivityId)
      this.filterByActivity(this.selectedActivityId)
    },
    'process.id': {
      handler(id) {
        if (id && this.isDiagramReady) {
          this.loadCalledProcessDefinitionsData()
        }
      },
      immediate: true
    },
    isDiagramReady(ready) {
      if (ready && this.process?.id && !this.loading) {
        this.loadCalledProcessDefinitionsData()
      }
    }
  },
  methods: {
    ...mapActions(['setHighlightedElement', 'selectActivity']),
    ...mapActions('calledProcessDefinitions', [
      'loadCalledProcessDefinitions', 
      'filterByActivity'
    ]),
    async loadCalledProcessDefinitionsData() {
      // Only proceed if we have the required data
      if (!this.process?.id) {
        return
      }
      
      this.initialLoading = true
      this.loading = true
      try {
        await this.loadCalledProcessDefinitions({ 
          processId: this.process.id,
          chunkSize: this.$root?.config?.maxProcessesResults || 50
        })
      } catch (error) {
        console.error('Error loading called processes:', error)
      } finally {
        this.loading = false
        this.initialLoading = false
      }
    }
  }
}
</script>