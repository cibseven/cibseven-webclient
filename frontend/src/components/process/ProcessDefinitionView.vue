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
      <WarningBox v-else-if="middlePaneError" :message="$t(middlePaneError.key, middlePaneError.params)"/>
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
      errorInstanceDefinitionId: null, // the definition of the instance, when it could not be loaded
      errorInstanceDefinitionKey: null, // its key, when it was loaded but is not the URL's
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
        // a call activity calling its own key and version changes only the parent breadcrumb
        parentProcessDefinitionId: this.$route.query.parentProcessDefinitionId,
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
    // The one error the middle pane shows when neither the definition nor the instance can be: the missing definition of
    // the instance comes first, then the instance itself. A missing version only counts when no version could be shown
    // instead; otherwise the latest one is open and only the sidebar tells the asked one was not found
    middlePaneError() {
      if (this.errorInstanceDefinitionId !== null) {
        const params = { instanceId: this.instanceId, definitionId: this.errorInstanceDefinitionId }
        // the definition was loaded, but the URL names another key: the warning points at the instance's own
        if (this.errorInstanceDefinitionKey !== null) {
          return { key: 'process.instanceOfOtherProcess', params: { ...params, definitionKey: this.errorInstanceDefinitionKey, processKey: this.processKey } }
        }
        return { key: 'process.instanceDefinitionNotFound', params }
      }
      // '' is an instance found nowhere with an empty server response: still an error, told without the response
      if (this.errorLoadingInstanceId === '') {
        return { key: 'process.instanceNotFoundNoResponse', params: { instanceId: this.instanceId } }
      }
      if (this.errorLoadingInstanceId !== null) {
        return { key: 'process.instanceNotFound', params: { instanceId: this.instanceId, error: this.errorLoadingInstanceId } }
      }
      if (this.errorVersionNotFound !== null && this.processDefinitions.length === 0) {
        return { key: 'process.definitionNotFound', params: { processKey: this.processKey, tenantId: this.definitionTenantId } }
      }
      return null
    },
  },
  beforeUnmount() {
    // Leaving the process pages: the next visit loads the called process definitions again, so a newer deployment is seen
    this.clearStaticCalledProcessDefinitions()
    // the replies of a refresh or delete still running are dropped, so a late one does not redirect away from the page
    // the user went to, e.g. after deleting the open version and opening Tasks while the cascade delete ran
    this.versionsScope = null // non-reactive
    // A route run still in progress counts as stale, so it drops its results and its failures. Its last steps check
    // the open definition and instance instead, so these go too: no statistics or activity history is requested for
    // a page that is gone, and a failure of it shows no error dialog on the page the user went to
    this.syncRun = (this.syncRun ?? 0) + 1 // non-reactive
    this.process = null
    this.selectedInstance = null
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
      // `routeState` reads the query, so every navigation re-evaluates it, e.g. a ?tab= switch: nothing to load
      // when none of its fields changed
      if (previous && Object.keys(route).every(key => route[key] === previous[key])) return
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
      } catch (error) {
        // the failure of an older navigation is dropped like its results; the current one's goes on
        if (!isStale()) throw error
      } finally {
        // a newer run clears the flag itself
        if (!isStale()) this.loading = false
      }
    },
    async loadRoute(isStale) {
      const reloadVersions = !this.keepsVersions()
      if (reloadVersions) this.clearDefinitions()
      if (this.selectedInstance && this.selectedInstance.id !== this.instanceId) this.clearInstance()

      // load the instance, the versions (if needed) and the parent breadcrumb in parallel
      const [instance, , parentProcess] = await Promise.all([
        this.lookUpInstance(this.instanceId, isStale),
        // A failure leaves no versions: an instance is still shown on its own definition (loadVersionsOfInstance), and a
        // definition is flagged by syncDefinition, so both panes tell it was not found; the error dialog tells why
        reloadVersions && this.loadVersions(isStale).catch(() => {}),
        this.lookUpParentProcess()
      ])
      if (isStale()) return
      this.parentProcess = parentProcess
      const instanceDefinition = await this.loadVersionsOfInstance(instance, isStale)
      if (isStale()) return

      await this.syncDefinition(instance, instanceDefinition)
      if (isStale()) return

      // an instance whose definition is flagged missing is not shown, so neither it nor its activities are loaded
      if (!instance || this.selectedInstance?.id === instance.id || this.errorInstanceDefinitionId) return
      this.clearInstance()
      this.selectedInstance = instance

      // The responses for an instance that is no longer selected are dropped. A failed load leaves its field
      // `null`, e.g. when the instance ended meanwhile: the instance stays shown and the error dialog tells what failed
      // a suspended instance is still running and has a runtime activity tree, like an active one
      const isRunning = 'state' in instance ? ['ACTIVE', 'SUSPENDED'].includes(instance.state) : instance.ended === false
      if (isRunning) {
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
      return this.instanceId ? this.versionsScope?.processKey === this.processKey : this.showsVersionsOfUrlTenant()
    },
    // whether the shown versions are the ones of the URL's key and tenant
    showsVersionsOfUrlTenant() {
      const shown = this.versionsScope
      return shown?.processKey === this.processKey && shown.tenantId === (this.tenantId ?? null)
    },
    /**
     * With an instance, the URL names the tenant of the instance, not the one of its definition: they differ
     * for a shared definition started for a tenant, and a link may name no tenant at all. When the versions
     * loaded with the URL's tenant miss the instance's definition, they are loaded with the definition's tenant.
     * Returns the definition when it had to be fetched, with its details, so that syncDefinition does not fetch it again.
     */
    async loadVersionsOfInstance(instance, isStale) {
      if (!instance) {
        // The instance was not found: versions of another tenant, kept for an earlier instance of the key, give way
        // to the URL tenant's. A failed first load is not repeated; a failure keeps the versions the run has
        if (this.instanceId && this.versionsScope && !this.showsVersionsOfUrlTenant()) await this.loadVersions(isStale).catch(() => {})
        return
      }
      const definitionId = definitionIdOf(instance)
      if (!definitionId || this.processDefinitions.some(definition => definition.id === definitionId)) return
      let definition
      try {
        definition = await ProcessService.findProcessById(definitionId, true)
      } catch {
        // e.g. no permission to read it: the versions the run has are kept and the route goes on loading;
        // syncDefinition flags the missing definition, the error dialog tells what failed
        return
      }
      if (isStale() || definition.key !== this.processKey) return definition
      // a failure keeps the versions the run already has, which miss the definition
      await this.loadVersions(isStale, definition.tenantId).catch(() => {})
      // The versions still miss the definition, e.g. none of its tenant could be loaded and the URL tenant's are another
      // tenant's or none: the definition was read, so the instance is shown on it as the only entry rather than flagged.
      // That entry is no tenant's versions list, so the next route loads the versions again
      if (!isStale() && !this.processDefinitions.some(entry => entry.id === definitionId)) {
        this.processDefinitions = [definition]
        this.versionsScope = null // non-reactive
      }
      return definition
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
      instance ??= this.instances?.find(i => i.id === instanceId) ?? null
      // found nowhere without a failure: the middle pane still tells it was not found, with an empty server response
      if (!instance && this.errorLoadingInstanceId === null && !isStale()) this.errorLoadingInstanceId = ''
      return instance
    },
    /**
     * The definition of the instance could not be loaded, e.g. without permission to read it, or the URL names
     * another key: rather than drawing the instance on another definition's diagram, no definition is selected
     * and the middle pane tells which definition is missing, or, when `fetched` is that definition of another key,
     * which key the instance belongs to. Returns whether it is missing.
     */
    flagMissingInstanceDefinition(instanceDefinitionId, instanceDefinition, fetched) {
      const missing = !!instanceDefinitionId && !instanceDefinition
      this.errorInstanceDefinitionId = missing ? instanceDefinitionId : null
      const otherKey = missing && fetched?.id === instanceDefinitionId && fetched.key !== this.processKey
      this.errorInstanceDefinitionKey = otherKey ? fetched.key : null
      if (missing) {
        this.process = null
        this.errorVersionNotFound = null
        // an instance already selected, e.g. when only the key of its link was edited, is not shown either
        this.clearInstance()
      }
      return missing
    },
    // Selects the definition of the instance, else the version the URL asks for, or the latest one (flagged) when there is no such version.
    // `fetched` is a definition with its details already loaded in this run, which is not requested again
    async syncDefinition(instance = this.selectedInstance, fetched = null) {
      // an instance is shown on its own definition, even when the URL names another version of it
      const instanceDefinitionId = definitionIdOf(instance)
      const instanceDefinition = this.processDefinitions.find(definition => definition.id === instanceDefinitionId)
      if (this.flagMissingInstanceDefinition(instanceDefinitionId, instanceDefinition, fetched)) return
      const requested = instanceDefinition ?? this.processDefinitions.find(definition => definition.version === this.versionIndex)
      this.errorVersionNotFound = requested ? null : this.versionIndex
      const definition = requested ?? this.processDefinitions[0]
      if (!definition) return

      if (definition !== this.process) {
        const previous = this.process
        this.process = definition
        // the entries of a reloaded versions list are new: the open version keeps the statistics its diagram shows
        if (previous?.id === definition.id) definition.statistics ??= previous.statistics

        // The responses for a definition that is no longer selected are dropped. Without its details the
        // version entry is shown as listed, so that the route goes on loading; the error dialog tells what failed
        const details = fetched?.id === definition.id ? fetched : await ProcessService.findProcessById(definition.id, true).catch(() => null)
        if (this.process !== definition) return
        if (details) this.assignDefinitionData(definition, details)
      }
      // ProcessInstancesView loads the statistics together with its instances, the instance view does not;
      // an instance that was not found is not shown, so its diagram needs none. The open version can lack them
      // too, e.g. when it was opened for a link to an instance that was not found
      if (instance && !definition.statistics) await this.loadStatistics(definition)
    },
    /**
     * Arriving from a call activity, the definition view shows the parent as a breadcrumb; the instance view
     * resolves its own. A parent that cannot be read is left out; the error dialog tells what failed.
     */
    async lookUpParentProcess() {
      const parentProcessDefinitionId = this.$route.query.parentProcessDefinitionId
      if (this.instanceId || !parentProcessDefinitionId) return null
      return this.getProcessById({ id: parentProcessDefinitionId }).catch(() => null)
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
      this.errorInstanceDefinitionId = null
      this.errorInstanceDefinitionKey = null
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
      try {
        await ProcessService.deleteProcessDefinition(processDefinition.id, true)
      } catch {
        // nothing was deleted, so the versions stay as they are; the error dialog tells what failed
        return
      }
      const lazyLoad = this.$root.config.lazyLoadHistory
      const reloaded = await ProcessService.findProcessVersionsByDefinitionKey(this.processKey, tenantId, lazyLoad).catch(() => null)
      // The user opened another key or tenant while the requests ran, e.g. deleted a version of `invoice`
      // and went to `order`: that navigation loads its own versions, so this reply must neither replace
      // them nor redirect away from the newly opened process
      if (this.versionsScope !== versionsScope) return
      // The versions cannot be reloaded: the deleted one is still dropped, so that it is neither listed nor open,
      // and the others are kept as they are; the error dialog tells what failed
      const versions = reloaded ?? this.processDefinitions.filter(version => version.id !== processDefinition.id)
      if (versions.length === 0) {
        // no more process-definitions with such key
        this.$router.replace('/seven/auth/processes')
        return
      }
      this.processDefinitions = versions
      if (reloaded) this.resetStatsLazyLoad(lazyLoad)
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
      // the definition whose instance was deleted: once another one is open, e.g. after navigating to another
      // key while the requests ran, its details are dropped and the instances view may be gone
      const definition = this.process
      if (!definition) return
      await this.loadStatistics(definition)
      const details = await ProcessService.findProcessById(definition.id, true)
      if (this.process !== definition) return
      this.assignDefinitionData(definition, details)
      this.$refs.process?.refreshDiagram()
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
