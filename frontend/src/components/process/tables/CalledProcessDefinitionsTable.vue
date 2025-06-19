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
    <div v-if="loading">
      <p class="text-center p-4"><BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('admin.loading') }}</p>
    </div>
    <FlowTable v-else-if="calledProcessDefinitions.length > 0" resizable striped thead-class="sticky-header" :items="calledProcessDefinitions" primary-key="id" prefix="process-instance.calledProcessDefinitions."
      sort-by="label" :sort-desc="true" :fields="[
        { label: 'calledProcessDefinition', key: 'name', class: 'col-4', thClass: 'border-end', tdClass: 'py-1 border-end border-top-0' },
        { label: 'state', key: 'state', class: 'col-4', thClass: 'border-end', tdClass: 'py-1 border-end border-top-0' },
        { label: 'activity', key: 'activity', class: 'col-4', thClass: 'border-end', tdClass: 'py-1 border-end border-top-0' }
      ]">
      <template v-slot:cell(name)="table">
        <CopyableActionButton
            :display-value="table.item.name"
            :title="table.item.name"
            @copy="copyValueToClipboard"
            :to="{
              name: 'process', 
              params: {
                processKey: table.item.key,
                versionIndex: table.item.version
              }
            }"
          />
      </template>
      <template v-slot:cell(state)="table">
        <span v-if="table.item.activities.length" class="text-truncate">
          {{ getCalledProcessState(table.item.activities) }}
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
    <div v-else-if="!loading">
      <p class="text-center p-4">{{ $t('process-instance.noResults') }}</p>
    </div>
  </div>
  <SuccessAlert ref="messageCopy" style="z-index: 9999"> {{ $t('process.copySuccess') }} </SuccessAlert>
</template>

<script>
import FlowTable from '@/components/common-components/FlowTable.vue'
import { BWaitingBox } from 'cib-common-components'
import { mapActions, mapGetters } from 'vuex'
import { HistoryService } from '@/services.js'
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
  computed: {
    ...mapGetters('calledProcessDefinitions', ['calledProcessDefinitions']),
    ...mapGetters(['diagramXml', 'selectedActivityId']),
  },
  data() {
    return {
      loading: true
    }
  },
  watch: {
    selectedActivityId() {
      this.setHighlightedElement(this.selectedActivityId)
      this.filterByActivity()
    },
    'process.id': {
      handler(id) {
        if (id) {
          this.loadCalledProcessDefinitionsData(id)
        }
      },
      immediate: true
    }
  },
  created() {
    this.loadCalledProcesses()
  },
  methods: {
    ...mapActions(['setHighlightedElement', 'selectActivity']),
    ...mapActions('calledProcessDefinitions', ['loadCalledProcessDefinitions']),
    async loadCalledProcessDefinitionsData(processId) {
      this.loading = true
      try {
        await this.loadCalledProcessDefinitions(processId)
      } finally {
        this.loading = false
      }
    },
    loadCalledProcesses: function () {
      this.calledProcesses = []

      HistoryService.findActivitiesInstancesHistoryWithFilter({
        processDefinitionId: this.process.id,
        activityType: 'callActivity'
      }).then(activities => {
        if (activities.length === 0) return

        const staticIds = this.getStaticCallActivityIdsFromXml(this.diagramXml)
        const activityList = this.markStaticOrDynamic(activities, staticIds)

        const filteredActivities = activityList.filter(activity => {
          return activity.isStatic || (!activity.endTime && activity.canceled !== true && activity.deleted !== true)
        })

        const instancesIdList = [...new Set(filteredActivities.map(a => a.calledProcessInstanceId))]

        if (instancesIdList.length > 0) {
          HistoryService.findProcessesInstancesHistory(
            { processInstanceIds: instancesIdList }
          ).then(processInstances => {
            const groupedProcesses = this.groupCalledProcesses(filteredActivities, processInstances)
            this.calledProcesses = Object.values(groupedProcesses)
            this.allCalledProcesses = Object.values(groupedProcesses)
          })
        }
      })
    },

    groupCalledProcesses(activities, processInstances) {
      const grouped = {}
      processInstances.forEach(instance => {
        const relatedActivities = activities.filter(
          act => act.calledProcessInstanceId === instance.id
        )
        const processDefId = instance.processDefinitionId
        const key = processDefId.split(':')[0]
        const version = processDefId.split(':')[1]
        const foundProcess = this.$store.state.process.list.find(p => p.key === key)
        relatedActivities.forEach(activity => {
          const groupKey = processDefId
          if (!grouped[groupKey]) {
            grouped[groupKey] = {
              id: groupKey,
              key,
              version,
              name: foundProcess ? foundProcess.name : key,
              activities: [],
              instances: []
            }
          }
          if (!grouped[groupKey].activities.some(a => a.activityId === activity.activityId)) {
            grouped[groupKey].activities.push({
              ...activity,
              isStatic: activity.isStatic
            })
          }
          if (!grouped[groupKey].instances.some(i => i.id === instance.id)) {
            grouped[groupKey].instances.push(instance)
          }
        })
      })
      return grouped
    },

    getStaticCallActivityIdsFromXml(xmlString) {
      const parser = new DOMParser()
      const xmlDoc = parser.parseFromString(xmlString, 'application/xml')
      const callActivities = xmlDoc.getElementsByTagName('bpmn:callActivity')
      const staticIds = []
      for (let i = 0; i < callActivities.length; i++) {
        const el = callActivities[i]
        const calledElement = el.getAttribute('calledElement')
        const id = el.getAttribute('id')
        if (calledElement && !calledElement.trim().startsWith('${')) {
          staticIds.push(id)
        }
      }
      return staticIds
    },
    markStaticOrDynamic(activitiesList, staticActivityIds) {
      return activitiesList.map(activity => ({
        ...activity,
        isStatic: staticActivityIds.includes(activity.activityId)
      }))
    },
    getCalledProcessState(activities) {
      const hasRunning = activities.some(act => act.endTime == null && act.canceled !== true)
      const hasReferenced = activities.some(act => act.isStatic)
      if (hasRunning && hasReferenced) return this.$t('process-instance.calledProcessDefinitions.runningAndReferenced')
      if (hasRunning) return this.$t('process-instance.calledProcessDefinitions.running')
      if (hasReferenced) return this.$t('process-instance.calledProcessDefinitions.referenced')
      return null
    },
    filterByActivity() {
      if (this.selectedActivityId) {
        this.calledProcesses = this.allCalledProcesses
          .filter(cp => cp.activities.some(act => act.activityId === this.selectedActivityId))
          .map(cp => this.mapSelectedActivity(cp, this.selectedActivityId))
      } else {
        this.calledProcesses = [...this.allCalledProcesses]
      }
    },
    mapSelectedActivity(cp, activityId) {
      const selectedActivity = cp.activities.find(act => act.activityId === activityId)
      return {
        ...cp,
        activities: [selectedActivity]
      }
    }
  }
}
</script>