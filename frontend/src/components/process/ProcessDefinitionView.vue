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
  <div class="d-flex flex-column">
    <div class="d-flex ps-3 py-2">
      <b-button :title="$t('start.cockpit.processes.title')" variant="outline-secondary" href="#/seven/auth/processes/list" class="mdi mdi-18px mdi-arrow-left border-0"></b-button>
      <h4 class="ps-1 m-0 align-items-center d-flex" style="border-width: 3px !important">{{ processName }}</h4>
      <b-button :disabled="!instances || instances.length === 0" :title="$t('process.exportInstances')" variant="outline-secondary" @click="exportCSV()"
        class="ms-auto me-3 mdi mdi-18px mdi-download-outline border-0"></b-button>
    </div>
    <SidebarsFlow ref="sidebars" class="border-top overflow-auto fixed-left-sidebar" :left-open="leftOpen" @update:left-open="onLeftOpenChanged" :left-caption="shortendLeftCaption">
      <template v-slot:left>
        <template v-if="errorVersionNotFound !== null">
          <WarningBox :message="$t('process.definitionVersionNotFound', [errorVersionNotFound])"/>
          <WarningBox v-if="processDefinitions.length === 0" :message="$t('process.definitionNotFound', { processKey: processKey, tenantId: tenantId })"/>
        </template>
        <ProcessDetailsSidebar ref="navbar" v-if="process && (!selectedInstance && !instanceId)"
          :process-key="processKey"
          :process-definitions="processDefinitions"
          :version-index="computedVersionIndex"
          :selected-process="process"
          :refreshing="refreshingVersions"
          @on-refresh-process-definitions="onRefreshProcessDefinitions"
          @on-delete-process-definition="onDeleteProcessDefinition"
          :selected-instance="selectedInstance"
        ></ProcessDetailsSidebar>
        <ProcessInstanceDetailsSidebar v-else-if="(selectedInstance || instanceId)"
          :instance="selectedInstance"
          :process-definition="process"
        ></ProcessInstanceDetailsSidebar>
      </template>
      <ProcessInstancesView ref="process" v-if="process && !selectedInstance && !instanceId"
        :loading="loading"
        :process="process"
        :process-key="processKey"
        :version-index="computedVersionIndex"
        :activity-instance="activityInstance"
        :activity-instance-history="activityInstanceHistory"
        :tenant-id="tenantId"
        :filter="filter"
        :parent-process="parentProcess"
        @instance-deleted="onInstanceDeleted()"
        @task-selected="setSelectedTask($event)"
        @filter-instances="filterInstances($event)"
      />
      <ProcessInstanceView ref="navbar-variables" v-else-if="process && selectedInstance && instanceId"
        :process="process"
        :activity-instance="activityInstance"
        :activity-instance-history="activityInstanceHistory"
        :selected-instance="selectedInstance"
        :tenant-id="tenantId"
        @task-selected="setSelectedTask($event)"
      />
      <WarningBox v-else-if="errorLoadingInstanceId !== null" :message="$t('process.instanceNotFound', { instanceId: instanceId, error: errorLoadingInstanceId })"/>
      <WarningBox v-else-if="errorVersionNotFound !== null && processDefinitions.length === 0" :message="$t('process.definitionNotFound', { processKey: processKey, tenantId: tenantId })"/>
    </SidebarsFlow>
    <TaskPopper ref="importPopper"></TaskPopper>
  </div>
</template>

<script>
import { moment } from '@/globals.js'
import { TaskService, ProcessService, HistoryService } from '@/services.js'
import ProcessInstancesView from '@/components/process/ProcessInstancesView.vue'
import ProcessDetailsSidebar from '@/components/process/ProcessDetailsSidebar.vue'
import ProcessInstanceView from '@/components/process/ProcessInstanceView.vue'
import ProcessInstanceDetailsSidebar from '@/components/process/ProcessInstanceDetailsSidebar.vue'
import WarningBox from '@/components/common-components/WarningBox.vue'
import { SidebarsFlow, TaskPopper } from '@cib/common-frontend'
import { mapGetters, mapActions } from 'vuex'
import { formatDate } from '@/utils/dates.js'
import sidebarOpenPersistenceMixin from '@/mixins/sidebarOpenPersistenceMixin.js'

const UNFINISHED_FILTER_PREFERENCE_KEY = 'cibseven:preferences:unfinishedInstancesFilter'

function getStringObjByKeys(keys, obj) { // TODO rewrite to use join()
  let result = ''
  keys.forEach(key => {
    result += obj[key] + ';'
  })
  return result.slice(0, -1)
}

export default {
  name: 'ProcessDefinitionView',
  components: { ProcessInstancesView, ProcessDetailsSidebar, ProcessInstanceView, ProcessInstanceDetailsSidebar, SidebarsFlow, TaskPopper, WarningBox },
  mixins: [sidebarOpenPersistenceMixin],
  props: {
    processKey: { type: String, required: true },
    versionIndex: { type: String, required: true },
    instanceId: { type: String },
    tenantId: { type: String }
  },
  watch: {
    routeState: { handler: 'onRouteChanged', immediate: true },
    sidebarScope(newScope) {
      this.leftOpen = this.getSavedLeftOpen(newScope)
    }
  },
  data() {
    return {
      leftOpen: this.getSavedLeftOpen(this.instanceId ? 'process-instance' : 'process-definition'),
      process: null, // selected process definition: always an entry of processDefinitions
      processDefinitions: [],
      refreshingVersions: null,
      errorVersionNotFound: null,
      errorLoadingInstanceId: null,
      selectedInstance: null,
      task: null,
      activityInstance: null,
      activityInstanceHistory: null,
      filter: localStorage.getItem(UNFINISHED_FILTER_PREFERENCE_KEY) === 'false' ? {} : { unfinished: true },
      loading: false, // the route's definition and instance are being loaded
      parentProcess: null
    }
  },
  computed: {
    ...mapGetters('instances', ['instances']),
    // One watcher for all route props: a navigation that changes several of them is handled once
    routeState() {
      return {
        processKey: this.processKey,
        tenantId: this.tenantId,
        versionIndex: this.versionIndex,
        instanceId: this.instanceId,
      }
    },
    sidebarScope() {
      return (this.selectedInstance || this.instanceId) ? 'process-instance' : 'process-definition'
    },
    shortendLeftCaption() {
      if (this.selectedInstance || this.instanceId) {
        return this.$t('process-instance.info')
      }
      return this.$t('process.details.historyVersions')
    },
    processName() {
      if (!this.process) return this.processKey
      return this.process.name || this.process.key
    },
    computedVersionIndex() {
      return this.process?.version || this.versionIndex
    },
  },
  // Leaving the process pages: the next visit loads the called process definitions again, so a newer deployment is seen
  beforeUnmount: function() {
    this.clearStaticCalledProcessDefinitions()
  },
  methods: {
    ...mapActions(['clearActivitySelection', 'getProcessById']),
    ...mapActions('calledProcessDefinitions', ['clearStaticCalledProcessDefinitions']),
    formatDate,
    onLeftOpenChanged(isOpen) {
      this.leftOpen = isOpen
      this.saveLeftOpen(this.sidebarScope, isOpen)
    },
    onRouteChanged(route, previous) {
      // a different process or instance starts without the activity picked in the previous diagram
      if (route.processKey !== previous?.processKey || route.instanceId !== previous?.instanceId) {
        this.clearActivitySelection()
      }
      return this.syncWithRoute()
    },
    /**
     * Brings the view in line with the route props. Each step only runs when its input changed:
     * 1) the versions of the key, 2) the definition version the URL asks for, 3) the instance.
     * A newer navigation makes an older run drop its results. `loading` is set while the newest run works.
     */
    async syncWithRoute() {
      const run = this.syncRun = (this.syncRun ?? 0) + 1 // non-reactive
      const isStale = () => run !== this.syncRun
      // instances and definitions fetched in this navigation, by id; a redirect hands them on to the next run
      const fetched = this.handover ?? new Map()
      this.handover = null
      this.loading = true
      try {
        await this.loadRoute(isStale, fetched)
      } finally {
        // a newer run, or the one the redirect starts, clears the flag itself
        if (!isStale() && !this.handover) this.loading = false
      }
    },
    async loadRoute(isStale, fetched) {
      const versionsScope = `${this.processKey}\n${this.tenantId ?? ''}`
      if (versionsScope !== this.versionsScope) this.clearDefinitions()
      if (this.selectedInstance && this.selectedInstance.id !== this.instanceId) this.clearInstance()

      const instance = await this.lookUpInstance(this.instanceId, fetched)
      if (isStale()) return
      // the instance's definition tells the version it belongs to and the tenant of the versions
      const instanceDefinition = instance && await this.findInstanceDefinition(instance, fetched)
      if (isStale()) return
      if (instanceDefinition && this.redirectToInstanceDefinition(instanceDefinition, fetched)) return

      if (versionsScope !== this.versionsScope) {
        const lazyLoad = this.$root.config.lazyLoadHistory
        const tenantId = instanceDefinition ? instanceDefinition.tenantId : this.tenantId
        const versions = await ProcessService.findProcessVersionsByDefinitionKey(this.processKey, tenantId, lazyLoad)
        if (isStale()) return
        this.processDefinitions = versions
        this.resetStatsLazyLoad(lazyLoad)
        this.versionsScope = versionsScope // non-reactive
      }

      await this.syncDefinition(fetched)
      if (isStale()) return

      if (instance) await this.selectInstance(instance)
    },
    async findProcessInstance(instanceId) {
      return (this.$root.config.camundaHistoryLevel !== 'none') ?
        HistoryService.findProcessInstance(instanceId) :
        ProcessService.findProcessInstance(instanceId)
    },
    async lookUpInstance(instanceId, fetched = new Map()) {
      this.errorLoadingInstanceId = null
      if (!instanceId) return null
      if (this.selectedInstance?.id === instanceId) return this.selectedInstance
      if (fetched.has(instanceId)) return fetched.get(instanceId)

      let instance = null
      try {
        instance = await this.findProcessInstance(instanceId)
      } catch (error) {
        this.errorLoadingInstanceId = error.message
      }
      // an instance listed in the table can still be shown when history has purged it
      instance ??= this.instances?.find(i => i.id === instanceId) ?? null
      if (instance) fetched.set(instanceId, instance)
      return instance
    },
    async findInstanceDefinition(instance, fetched) {
      // runtime instances carry `definitionId` rather than `processDefinitionId`
      const definitionId = instance.processDefinitionId ?? instance.definitionId
      return this.processDefinitions.find(definition => definition.id === definitionId) ??
        this.fetchDefinition(definitionId, fetched)
    },
    // The URL names a version (or key) the instance does not belong to: show it with its own definition
    redirectToInstanceDefinition(definition, fetched) {
      if (!definition.key || definition.version == null) return false
      const versionIndex = String(definition.version)
      if (definition.key === this.processKey && versionIndex === this.versionIndex) return false
      this.handover = fetched // non-reactive
      this.$router.replace({
        name: 'process',
        params: { processKey: definition.key, versionIndex, instanceId: this.instanceId },
        query: this.$route.query
      })
      return true
    },
    // Selects the version the URL asks for, or the latest one (flagged) when there is no such version
    async syncDefinition(fetched) {
      const requested = this.processDefinitions.find(definition => definition.version === this.versionIndex)
      this.errorVersionNotFound = requested ? null : this.versionIndex
      const definition = requested ?? this.processDefinitions[0]
      if (definition && definition !== this.process) await this.selectDefinition(definition, fetched)
    },
    async selectDefinition(definition, fetched = new Map()) {
      const previous = this.process
      this.process = definition
      // the entries of a reloaded versions list are new: the open version keeps the statistics its diagram shows
      if (previous?.id === definition.id) definition.statistics ??= previous.statistics
      this.assignDefinitionData(definition, await this.fetchDefinition(definition.id, fetched))
      // ProcessInstancesView loads the statistics together with its instances, the instance view does not
      if (this.instanceId && !definition.statistics) await this.loadStatistics(definition)

      // arriving from a call activity, the parent is shown as a breadcrumb
      const parentProcessDefinitionId = this.$route.query.parentProcessDefinitionId
      this.parentProcess = parentProcessDefinitionId ? await this.getProcessById({ id: parentProcessDefinitionId }) : null
    },
    async fetchDefinition(id, fetched = new Map()) {
      if (!fetched.has(id)) fetched.set(id, await ProcessService.findProcessById(id, true))
      return fetched.get(id)
    },
    // the definition never fills in the incidents and the deployment time: the version entry keeps its own
    assignDefinitionData(entry, definition) {
      Object.assign(entry, { ...definition, incidents: entry.incidents, deploymentTime: entry.deploymentTime })
    },
    clearDefinitions() {
      this.versionsScope = null // non-reactive
      this.processDefinitions = []
      this.process = null
      this.parentProcess = null
      this.errorVersionNotFound = null
    },
    clearInstance() {
      this.selectedInstance = null
      this.activityInstance = null
      this.activityInstanceHistory = null
      this.task = null
    },
    async selectInstance(instance) {
      if (this.selectedInstance?.id === instance.id) return
      this.clearInstance()
      this.selectedInstance = instance

      // the responses for an instance that is no longer selected are dropped
      const isActive = 'state' in instance ? instance.state === 'ACTIVE' : instance.ended === false
      if (isActive) {
        const activityInstance = await ProcessService.findActivityInstance(instance.id)
        if (this.selectedInstance?.id !== instance.id) return
        this.activityInstance = activityInstance
      }
      if (isActive || this.$root.config.camundaHistoryLevel !== 'none') {
        const activityInstanceHistory = await HistoryService.findActivitiesInstancesHistory(instance.id)
        if (this.selectedInstance?.id !== instance.id) return
        this.activityInstanceHistory = activityInstanceHistory
      }
    },
    async onDeleteProcessDefinition({ processDefinition }) {
      await ProcessService.deleteProcessDefinition(processDefinition.id, true)
      const lazyLoad = this.$root.config.lazyLoadHistory
      const versions = await ProcessService.findProcessVersionsByDefinitionKey(this.processKey, this.tenantId, lazyLoad)
      if (versions.length === 0) {
        // no more process-definitions with such key
        this.$router.replace('/seven/auth/processes')
        return
      }
      this.processDefinitions = versions
      this.resetStatsLazyLoad(lazyLoad)
      if (processDefinition.version !== this.computedVersionIndex) {
        // a version other than the open one was deleted: the open one is selected from the new list
        await this.syncDefinition()
        return
      }
      // The open version was deleted: navigate to the nearest remaining one.
      //
      // 5 4 3 2 1
      //     ^      - deleted
      //   ^   ^    - one of final nextVersionIndex
      //
      const deleted = Number(processDefinition.version)
      const distance = version => Math.abs(Number(version.version) - deleted)
      const nearest = versions.reduce((best, version) => distance(version) < distance(best) ? version : best)
      this.$router.replace({
        name: 'process',
        params: {
          processKey: processDefinition.key,
          versionIndex: nearest.version,
        },
        query: {
          ...this.$route.query,
          ...(this.tenantId ? { tenantId: this.tenantId } : {}),
        }
      })
    },
    // "refresh process definitions" button: 'all' when every version's data is loaded, shown on the sidebar's load button
    async onRefreshProcessDefinitions(lazyLoad) {
      this.refreshingVersions = lazyLoad ? 'refresh' : 'all'
      try {
        const versions = await ProcessService.findProcessVersionsByDefinitionKey(this.processKey, this.tenantId, lazyLoad)
        this.processDefinitions = versions
        this.resetStatsLazyLoad(lazyLoad)
        await this.syncDefinition()
        return versions
      } finally {
        this.refreshingVersions = null
      }
    },
    resetStatsLazyLoad(lazyLoad) {
      if (lazyLoad) {
        this.processDefinitions.forEach(v => {
          v.runningInstances = '-'
          v.allInstances = '-'
          v.completedInstances = '-'
          v.incidents = '-'
        })
      }
    },
    async loadStatistics(definition = this.process) {
      await this.$store.dispatch('loadStatistics', definition)
    },
    async onInstanceDeleted() {
      await this.loadStatistics()
      this.assignDefinitionData(this.process, await this.fetchDefinition(this.process.id))
      this.$refs.process.refreshDiagram()
    },
    async setSelectedTask(selectedTask) {
      if (!this.selectedInstance || !selectedTask) return
      const [task] = await HistoryService.findTasksByDefinitionKeyHistory(selectedTask.id, this.selectedInstance.id)
      if (!task) {
        this.task = null
        return
      }
      this.task = task
      const fetchVariables = task.endTime ? HistoryService.fetchActivityVariablesHistory : TaskService.fetchActivityVariables
      const variables = await fetchVariables(task.activityInstanceId)
      variables.forEach(variable => {
        variable.value = variable.type === 'Object' ? JSON.stringify(variable.value) : variable.value
      })
      this.task.variables = variables
    },
    filterInstances(filter) {
      this.filter = filter
      localStorage.setItem(UNFINISHED_FILTER_PREFERENCE_KEY, filter.unfinished === true)
      // InstancesTable will automatically reload when filter changes
    },
    exportCSV() {
      const headers = [
        { text: 'state', key: 'state' },
        { text: 'businessKey', key: 'businessKey' },
        { text: 'startTime', key: 'startTime' },
        { text: 'endTime', key: 'endTime' },
        { text: 'id', key: 'id' },
        { text: 'startUserId', key: 'startUserId' },
        { text: 'details.definitionName', key: 'processDefinitionName' },
        { text: 'details.definitionVersion', key: 'processDefinitionVersion' }
      ]
      headers.forEach(h => h.text = this.$t('process.' + h.text))
      let csvContent = headers.map(h => h.text).join(';') + '\n'
      const keys = headers.map(h => h.key)
      this.instances.forEach(v => {
        const formattedValues = {
          ...v,
          startTime: this.formatDate(v.startTime),
          endTime: this.formatDate(v.endTime)
        }
        csvContent += getStringObjByKeys(keys, formattedValues) + '\n'
      })
      const csvBlob = new Blob([csvContent], { type: 'text/csv' })
      const filename = 'Management_Instances_' + moment().format('YYYYMMDD_HHmm') + '.csv'
      this.$refs.importPopper.triggerDownload(csvBlob, filename)
    }
  }
}
</script>

<style scoped>
/* fixed sidebar width outside phones, so the load button's label fits; the middle column follows it */
@media (min-width: 576px) {
  .fixed-left-sidebar {
    --left-sidebar-width: 22.5rem;
  }
  .fixed-left-sidebar > :deep(.border-end.position-absolute) {
    width: var(--left-sidebar-width);
  }
  .fixed-left-sidebar > :deep(.sidebars-middle[class*="offset-"]) {
    margin-left: var(--left-sidebar-width);
    width: calc(100% - var(--left-sidebar-width));
  }
}
</style>
