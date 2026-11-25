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
          <WarningBox v-if="processDefinitions.length === 0" :message="$t('process.definitionNotFound', { processKey: processKey, tenantId: definitionTenantId })"/>
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
        :process="process"
        :process-key="processKey"
        :version-index="computedVersionIndex"
        :activity-instance="activityInstance"
        :activity-instance-history="activityInstanceHistory"
        :tenant-id="definitionTenantId"
        :filter="filter"
        :parent-process="parentProcess"
        @instance-deleted="onInstanceDeleted()"
        @filter-instances="filterInstances($event)"
      />
      <ProcessInstanceView ref="navbar-variables" v-else-if="process && selectedInstance && instanceId"
        :process="process"
        :activity-instance="activityInstance"
        :activity-instance-history="activityInstanceHistory"
        :selected-instance="selectedInstance"
        :tenant-id="instanceTenantId"
      />
      <!-- the route is loading and neither the definition nor the instance can be shown yet -->
      <div v-else-if="loading" class="h-100 d-flex justify-content-center align-items-center">
        <BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('admin.loading') }}
      </div>
      <WarningBox v-else-if="errorLoadingInstanceId !== null" :message="$t('process.instanceNotFound', { instanceId: instanceId, error: errorLoadingInstanceId })"/>
      <WarningBox v-else-if="errorVersionNotFound !== null && processDefinitions.length === 0" :message="$t('process.definitionNotFound', { processKey: processKey, tenantId: definitionTenantId })"/>
    </SidebarsFlow>
    <TaskPopper ref="importPopper"></TaskPopper>
  </div>
</template>

<script>
import { moment } from '@/globals.js'
import { ProcessService, HistoryService } from '@/services.js'
import ProcessInstancesView from '@/components/process/ProcessInstancesView.vue'
import ProcessDetailsSidebar from '@/components/process/ProcessDetailsSidebar.vue'
import ProcessInstanceView from '@/components/process/ProcessInstanceView.vue'
import ProcessInstanceDetailsSidebar from '@/components/process/ProcessInstanceDetailsSidebar.vue'
import WarningBox from '@/components/common-components/WarningBox.vue'
import { SidebarsFlow, TaskPopper, BWaitingBox } from '@cib/common-frontend'
import { mapGetters, mapActions } from 'vuex'
import { formatDate } from '@/utils/dates.js'
import sidebarOpenPersistenceMixin from '@/mixins/sidebarOpenPersistenceMixin.js'

const UNFINISHED_FILTER_PREFERENCE_KEY = 'cibseven:preferences:unfinishedInstancesFilter'

// The id of the definition an instance was started from; runtime instances carry `definitionId` rather than `processDefinitionId`
function definitionIdOf(instance) {
  return instance?.processDefinitionId ?? instance?.definitionId
}

function getStringObjByKeys(keys, obj) { // TODO rewrite to use join()
  let result = ''
  keys.forEach(key => {
    result += obj[key] + ';'
  })
  return result.slice(0, -1)
}

export default {
  name: 'ProcessDefinitionView',
  components: { ProcessInstancesView, ProcessDetailsSidebar, ProcessInstanceView, ProcessInstanceDetailsSidebar, SidebarsFlow, TaskPopper, WarningBox, BWaitingBox },
  mixins: [sidebarOpenPersistenceMixin],
  props: {
    processKey: { type: String, required: true },
    versionIndex: { type: String, required: true },
    instanceId: { type: String },
    // The tenant ID of the process definition (when instanceId is not provided) or
    // the tenant ID of the process instance (when instanceId is provided).
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
    // The tenant of the open instance: its own once loaded, as a link may name no tenant;
    // until then the `tenantId` of the URL, which is the instance's for an instance link
    instanceTenantId() {
      if (!this.instanceId) return null
      return this.selectedInstance ? (this.selectedInstance.tenantId ?? null) : this.tenantId
    },
    // The tenant of the open definition and its versions. It differs from the instance's for a shared
    // definition started for a tenant; until the definition is open, the URL's tenant is assumed
    definitionTenantId() {
      return this.process ? this.process.tenantId : this.tenantId
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
     * 1) the versions of the key and the instance lookup, in parallel, 2) the definition version the URL asks for, 3) the instance.
     * A newer navigation makes an older run drop its results. `loading` is set while the newest run works.
     */
    async syncWithRoute() {
      const run = this.syncRun = (this.syncRun ?? 0) + 1 // non-reactive
      const isStale = () => run !== this.syncRun
      this.loading = true
      try {
        await this.loadRoute(isStale)
      } finally {
        // a newer run clears the flag itself
        if (!isStale()) this.loading = false
      }
    },
    async loadRoute(isStale) {
      const reloadVersions = !this.keepsVersions()
      if (reloadVersions) this.clearDefinitions()
      if (this.selectedInstance && this.selectedInstance.id !== this.instanceId) this.clearInstance()

      // load the instance and the versions in parallel, if needed
      const [instance] = await Promise.all([
        this.lookUpInstance(this.instanceId, isStale),
        reloadVersions && this.loadVersions(isStale)
      ])
      if (isStale()) return
      await this.loadVersionsOfInstance(instance, isStale)
      if (isStale()) return

      await this.syncDefinition(instance)
      if (isStale()) return

      if (!instance || this.selectedInstance?.id === instance.id) return
      this.clearInstance()
      this.selectedInstance = instance

      // The responses for an instance that is no longer selected are dropped. A failed load leaves its field
      // `null`, e.g. when the instance ended meanwhile: the instance stays shown and the error dialog tells what failed
      const isActive = 'state' in instance ? instance.state === 'ACTIVE' : instance.ended === false
      if (isActive) {
        const activityInstance = await ProcessService.findActivityInstance(instance.id).catch(() => null)
        if (this.selectedInstance?.id !== instance.id) return
        this.activityInstance = activityInstance
      }
      // without history the engine keeps no activity history, also not for an active instance
      if (this.$root.config.camundaHistoryLevel !== 'none') {
        const activityInstanceHistory = await HistoryService.findActivitiesInstancesHistory(instance.id).catch(() => null)
        if (this.selectedInstance?.id !== instance.id) return
        this.activityInstanceHistory = activityInstanceHistory
      }
    },
    /**
     * Whether the shown versions can stay for the route. With an instance, any versions of the key will do:
     * loadVersionsOfInstance reloads them when they miss the instance's definition. Without one, they must be
     * the versions of the URL's tenant, which may not be the tenant an instance had them loaded with.
     */
    keepsVersions() {
      const shown = this.versionsScope
      return shown?.processKey === this.processKey && (!!this.instanceId || shown.tenantId === (this.tenantId ?? null))
    },
    /**
     * With an instance, the URL names the tenant of the instance, not the one of its definition: they differ
     * for a shared definition started for a tenant, and a link may name no tenant at all. When the versions
     * loaded with the URL's tenant miss the instance's definition, they are loaded with the definition's tenant.
     */
    async loadVersionsOfInstance(instance, isStale) {
      const definitionId = definitionIdOf(instance)
      if (!definitionId || this.processDefinitions.some(definition => definition.id === definitionId)) return
      let definition
      try {
        definition = await ProcessService.findProcessById(definitionId)
      } catch {
        // e.g. no permission to read it: the versions loaded with the URL's tenant are kept
        // and the route goes on loading; the error dialog tells what failed
        return
      }
      if (isStale() || definition.key !== this.processKey) return
      await this.loadVersions(isStale, definition.tenantId)
    },
    async loadVersions(isStale, tenantId = this.tenantId) {
      const processKey = this.processKey
      const lazyLoad = this.$root.config.lazyLoadHistory
      const versions = await ProcessService.findProcessVersionsByDefinitionKey(processKey, tenantId, lazyLoad)
      if (isStale()) return
      this.processDefinitions = versions
      this.resetStatsLazyLoad(lazyLoad)
      // the key and tenant the shown versions were loaded with, which can differ from the URL's tenant
      this.versionsScope = { processKey, tenantId: tenantId ?? null } // non-reactive
    },
    async findProcessInstance(instanceId) {
      return (this.$root.config.camundaHistoryLevel !== 'none') ?
        HistoryService.findProcessInstance(instanceId) :
        ProcessService.findProcessInstance(instanceId)
    },
    async lookUpInstance(instanceId, isStale) {
      this.errorLoadingInstanceId = null
      if (!instanceId) return null
      if (this.selectedInstance?.id === instanceId) return this.selectedInstance

      let instance = null
      try {
        instance = await this.findProcessInstance(instanceId)
      } catch (error) {
        // the failure of an older navigation must not show up on the newer one
        if (!isStale()) this.errorLoadingInstanceId = error.message
      }
      // an instance listed in the table can still be shown when history has purged it
      return instance ?? this.instances?.find(i => i.id === instanceId) ?? null
    },
    // Selects the definition of the instance, else the version the URL asks for, or the latest one (flagged) when there is no such version
    async syncDefinition(instance = this.selectedInstance) {
      // an instance is shown on its own definition, even when the URL names another version of it
      const instanceDefinitionId = definitionIdOf(instance)
      const requested = this.processDefinitions.find(definition => definition.id === instanceDefinitionId) ??
        this.processDefinitions.find(definition => definition.version === this.versionIndex)
      this.errorVersionNotFound = requested ? null : this.versionIndex
      const definition = requested ?? this.processDefinitions[0]
      if (!definition || definition === this.process) return

      const previous = this.process
      this.process = definition
      // the entries of a reloaded versions list are new: the open version keeps the statistics its diagram shows
      if (previous?.id === definition.id) definition.statistics ??= previous.statistics

      // the responses for a definition that is no longer selected are dropped
      const details = await ProcessService.findProcessById(definition.id, true)
      if (this.process !== definition) return
      this.assignDefinitionData(definition, details)
      // ProcessInstancesView loads the statistics together with its instances, the instance view does not
      if (this.instanceId && !definition.statistics) await this.loadStatistics(definition)
      if (this.process !== definition) return

      // arriving from a call activity, the parent is shown as a breadcrumb
      const parentProcessDefinitionId = this.$route.query.parentProcessDefinitionId
      const parentProcess = parentProcessDefinitionId ? await this.getProcessById({ id: parentProcessDefinitionId }) : null
      if (this.process !== definition) return
      this.parentProcess = parentProcess
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
    },
    async onDeleteProcessDefinition({ processDefinition }) {
      // the key and tenant whose versions are shown when the deletion starts
      const versionsScope = this.versionsScope
      const tenantId = this.definitionTenantId
      await ProcessService.deleteProcessDefinition(processDefinition.id, true)
      const lazyLoad = this.$root.config.lazyLoadHistory
      const versions = await ProcessService.findProcessVersionsByDefinitionKey(this.processKey, tenantId, lazyLoad)
      // The user opened another key or tenant while the requests ran, e.g. deleted a version of `invoice`
      // and went to `order`: that navigation loads its own versions, so this reply must neither replace
      // them nor redirect away from the newly opened process
      if (this.versionsScope !== versionsScope) return
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
          // the definition view names the tenant of the definition, not the one of an instance the URL showed
          ...Object.fromEntries(Object.entries(this.$route.query).filter(([key]) => key !== 'tenantId')),
          ...(tenantId ? { tenantId } : {}),
        }
      })
    },
    // "refresh process definitions" button: 'all' when every version's data is loaded, shown on the sidebar's load button
    async onRefreshProcessDefinitions(lazyLoad) {
      this.refreshingVersions = lazyLoad ? 'refresh' : 'all'
      // the key and tenant whose versions are shown when the refresh starts
      const versionsScope = this.versionsScope
      try {
        const versions = await ProcessService.findProcessVersionsByDefinitionKey(this.processKey, this.definitionTenantId, lazyLoad)
        // The user opened another key or tenant while the request ran, e.g. refreshed the versions of `invoice`
        // and went to `order`: that navigation loads its own versions, so this reply must not replace them
        if (this.versionsScope !== versionsScope) return
        // Every version was deleted meanwhile, e.g. by another user: the deleted definition is not shown
        // any more, and syncDefinition flags it so that both panes tell the definition was not found
        if (versions.length === 0) this.clearDefinitions()
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
      this.assignDefinitionData(this.process, await ProcessService.findProcessById(this.process.id, true))
      this.$refs.process.refreshDiagram()
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
