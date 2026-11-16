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
  <div>
    <BWaitingBox v-if="loader" class="h-100" ref="loader" styling="width:25%;height:25%;align-items:center;justify-content:center;position:absolute;top:36%;right:36%"></BWaitingBox>
    <div :class="loader ? 'invisible' : 'visible'" class="h-100">
      <div class="h-100" ref="diagram"></div>
      <slot v-if="!loader" name="viewer-overlay" :viewer="viewer" :viewer-parent="$refs.diagram"></slot>
    </div>
    <div class="position-absolute" style="right:15px;bottom:225px;">
      <b-button size="sm" variant="light" :title="$t('bpmn-viewer.resetZoom')" @click="resetZoom()">
        <span class="mdi mdi-18px mdi-target"></span>
      </b-button>
    </div>
    <div class="btn-group-vertical" style="position:absolute;right:15px;bottom:139px;">
      <b-button size="sm" variant="light" :title="$t('bpmn-viewer.zoomIn')" @click="zoomIn()">
        <span class="mdi mdi-18px mdi-plus"></span>
      </b-button>
      <b-button size="sm" variant="light" :title="$t('bpmn-viewer.zoomOut')" @click="zoomOut()">
        <span class="mdi mdi-18px mdi-minus"></span>
      </b-button>
    </div>
  </div>
</template>

<style>
@import "bpmn-js/dist/assets/bpmn-js.css";
@import "bpmn-js/dist/assets/diagram-js.css";
</style>

<script>
import NavigatedViewer from 'bpmn-js/lib/NavigatedViewer'
import { BWaitingBox } from '@cib/common-frontend'
import { mapActions, mapGetters } from 'vuex'
import { HistoryService } from '@/services.js'
import { abbreviateNumber } from 'js-abbreviation-number'
import ElementTemplateIconRendererModule from '@/components/process/ElementTemplateIconRenderer.js'
import { isModelerEnabled } from '@/utils/config.js'

const interactionTypes = [
  // Tasks
  'bpmn:UserTask', 'bpmn:ServiceTask', 'bpmn:ScriptTask', 'bpmn:SendTask',
  'bpmn:ReceiveTask', 'bpmn:ManualTask', 'bpmn:BusinessRuleTask',
  // Activities
  'bpmn:CallActivity', 'bpmn:SubProcess', 'bpmn:Transaction',
  // Gateways
  'bpmn:ExclusiveGateway', 'bpmn:InclusiveGateway', 'bpmn:ParallelGateway',
  'bpmn:EventBasedGateway', 'bpmn:ComplexGateway',
  // Events
  'bpmn:StartEvent', 'bpmn:EndEvent', 'bpmn:IntermediateThrowEvent',
  'bpmn:IntermediateCatchEvent', 'bpmn:BoundaryEvent'
]

// camunda: attributes of a call activity that, next to calledElement, pick the called definition
const calledElementReferenceAttributes = ['calledElementVersion', 'calledElementVersionTag', 'calledElementTenantId']

export default {
  name: 'BpmnViewer',
  emits: ['task-selected', 'child-activity', 'overlay-click', 'viewbox-changed'],
  components: { BWaitingBox },
  props: {
    activityInstance: Object,
    activityInstanceHistory: Array,
    statistics: Array,
    processDefinitionId: String,
    activeTab: String,
    selectedInstance: Object,
    useRuntimeStats: {
      type: Boolean,
      default: false
    },
    runtimeActivityId: String,
    badgeOptions: {
      type: Object,
      default: () => ({
        showRunning: true,
        showHistory: true,
        showCanceled: true,
        showIncidents: true,
        showCalledProcesses: true,
        showJobDefinitions: true,
        showClosedIncidents: true
      })
    }
  },
  data: function() {
    return {
      viewer: null,
      currentHighlight: null,
      overlayList: [],
      // The call activity link overlays, also in overlayList, so they can be redrawn on their own
      subprocessLinkOverlays: [],
      loader: true,
      suspendedOverlayMap: {},
      overlayClickHandler: null,
      viewboxChangeTimer: null
    }
  },
  computed: {
    ...mapGetters(['highlightedElement', 'getHistoricActivityStatistics']),
    ...mapGetters('calledProcessDefinitions', ['getStaticCalledProcessDefinitionsFor']),
    ...mapGetters('job', ['jobDefinitions']),
    ...mapGetters('elementTemplates', { allElementTemplateContents: 'allElementTemplateContents' }),
    historicActivityStatistics() {
      return this.getHistoricActivityStatistics(this.processDefinitionId)
    },
    staticCalledProcessDefinitions() {
      return this.getStaticCalledProcessDefinitionsFor(this.processDefinitionId)
    },
    // The selected instance's own entries: the previous instance's history can still be passed in while the new one loads
    selectedInstanceActivityHistory() {
      const processInstanceId = this.selectedInstance?.id
      return (this.activityInstanceHistory || []).filter(activity => activity.processInstanceId === processInstanceId)
    }
  },
  watch: {
    historicActivityStatistics: {
      handler() { this.drawDiagramState() },
      deep: true
    },
    staticCalledProcessDefinitions: function() {
      if (this.viewer) this.redrawSubprocessLinks()
    },
    // It tells which call activities started a called instance, and can arrive after the diagram was drawn
    selectedInstanceActivityHistory: function() {
      if (this.viewer) this.redrawSubprocessLinks()
    },
    statistics: {
      handler() { if (this.viewer) this.drawDiagramState() },
      deep: true
    },
    useRuntimeStats: function() {
      if (this.viewer) this.drawDiagramState()
    },
    runtimeActivityId: function() {
      if (this.viewer) this.drawDiagramState()
    },
    activityInstance: {
      handler() { if (this.viewer) this.drawDiagramState() },
      deep: true
    },
    jobDefinitions: {
      handler() {
        if (this.viewer) {
          this.drawJobDefinitionBadges()
        }
      },
      immediate: true,
      deep: true
    },
    highlightedElement: function(newVal) {
      this.highlightElement(newVal)
    },
    allElementTemplateContents: {
      handler() { this.applyElementTemplateIcons() },
      deep: true
    }
  },
  mounted: function() {
    this.viewer = new NavigatedViewer({
      container: this.$refs.diagram,
      additionalModules: [ElementTemplateIconRendererModule]
    })
    this.viewer.on('import.done', () => {
      this.drawDiagramState()
      this.attachEventListeners()
      this.applyElementTemplateIcons()
      //Small timer so the diagram is fully rendered before setting it ready
      setTimeout(() => {
        this.setDiagramReady(true)
      }, 500)
    })
    this.ensureElementTemplatesLoaded()
  },
  methods: {
    ...mapActions(['selectActivity', 'clearActivitySelection', 'setHighlightedElement', 'getProcessById']),
    ...mapActions('diagram', ['setDiagramReady']),
    ...mapActions('elementTemplates', ['fetchAllElementTemplates']),
    ensureElementTemplatesLoaded: function() {
      if (!isModelerEnabled(this.$root?.config)) return
      if (this.allElementTemplateContents && this.allElementTemplateContents.length > 0) return
      this.fetchAllElementTemplates().catch(err => {
        console.warn('BpmnViewer: failed to load element templates for icon rendering', err)
      })
    },
    applyElementTemplateIcons: function() {
      if (!this.viewer) return
      let registry
      try {
        registry = this.viewer.get('elementTemplateIcons')
      } catch {
        return
      }
      const contents = this.allElementTemplateContents || []
      const iconMap = new Map()
      contents.forEach(template => {
        const id = template && template.id
        const icon = template && template.icon && template.icon.contents
        if (id && icon) iconMap.set(id, icon)
      })
      registry.setIcons(iconMap)
    },
    showDiagram: function(xml, selectedActivityId = null) {
      this.setDiagramReady(false)
      this.loader = true
      return this.viewer.importXML(xml).then(() => {
        return new Promise(resolve => {
          setTimeout(() => {
            if (!this.viewer || !this.$refs.diagram) return
            try {
              this.viewer.get('canvas').zoom('fit-viewport')
              this.loader = false
              this.highlightElement(selectedActivityId)
            } catch {
              console.warn('BpmnViewer: Unable to zoom diagram, component may have been unmounted')
            }
            resolve()
          }, 500)
        })
      })
    },
    setViewbox: function(viewbox) {
      if (!this.viewer) return
      this.viewer.get('canvas').viewbox(viewbox)
    },
    zoomIn: function() {
      this.viewer.get('zoomScroll').stepZoom(1)
    },
    zoomOut: function() {
      this.viewer.get('zoomScroll').stepZoom(-1)
    },
    resetZoom: function() {
      this.viewer.get('zoomScroll').reset()
    },
    // What clicking an activity does: highlight it and narrow the lists below the diagram to it
    selectDiagramElement: function(element) {
      this.setHighlightedElement(element.id)
      if (this.activityInstance) {
        const childActivity = this.activityInstance.childActivityInstances.find(obj => obj.activityId === element.id)
        this.$emit('child-activity', childActivity || element)
      } else {
        const listMode = this.$root.config.camundaHistoryLevel === 'none' ? 'active' : 'all'
        this.selectActivity({ activityId: element.id, listMode })
      }
    },
    attachEventListeners: function() {
      // Remove existing overlay click handler if it exists
      if (this.overlayClickHandler) {
        document.removeEventListener('click', this.overlayClickHandler)
      }

      const eventBus = this.viewer.get('eventBus')
      eventBus.on('canvas.viewbox.changed', () => {
        clearTimeout(this.viewboxChangeTimer)
        this.viewboxChangeTimer = setTimeout(() => {
          this.$emit('viewbox-changed', this.viewer.get('canvas').viewbox())
        }, 300)
      })
      // BPMN element click
      eventBus.on('element.click', (event) => {
        if (this.getTypeAllowed(event.element.type, interactionTypes)) {
          this.selectDiagramElement(event.element)
        } else {
          if (this.currentHighlight) {
            this.currentHighlight.shape.classList.remove('bpmn-highlight')
            this.currentHighlight = null
            this.clearActivitySelection()
          }
          this.$emit('task-selected', null)
        }
      })
      // BPMN element hover
      eventBus.on('element.hover', (event) => {
        if (this.getTypeAllowed(event.element.type, interactionTypes)) {
          const gfx = event.gfx
          const shape = gfx.querySelector('rect, path') || gfx
          if (!this.currentHighlight || this.currentHighlight.id !== event.element.id) {
            shape.classList.add('bpmn-highlight')
          }
          gfx.addEventListener('mouseleave', () => {
            if (!this.currentHighlight || this.currentHighlight.id !== event.element.id) {
              shape.classList.remove('bpmn-highlight')
            }
          }, { once: true })
        }
      })

      // Generic overlay click delegation - using event delegation on document
      // Store the handler reference so we can remove it later
      this.overlayClickHandler = (event) => {
        // Only handle clicks within BPMN overlay containers
        if (!event.target.closest('.djs-overlays')) return

        const bubble = event.target.closest('.bubble')
        if (bubble && bubble.dataset.activityId) {
          this.setHighlightedElement(bubble.dataset.activityId)
          const listMode = ['activitiesHistory', 'canceledInstances'].includes(bubble.dataset.type) ? 'executed' : 'active'
          this.selectActivity({ activityId: bubble.dataset.activityId, listMode })
        }

        // Generic: emit overlay-click for any overlay element with a data-overlay-type attribute
        const overlay = event.target.closest('[data-overlay-type]')
        if (overlay) {
          this.$emit('overlay-click', {
            type: overlay.dataset.overlayType,
            activityId: overlay.dataset.activityId || null,
            event
          })
        }
      }

      document.addEventListener('click', this.overlayClickHandler)
    },
    highlightElement: function(item) {
      let activityId
      if (typeof item === 'string') {
        activityId = item
      } else if (item && item.activityId) {
        activityId = item.activityId
      } else {
        return
      }

      // Clear previous highlight if no activityId is provided
      if (!activityId) {
        if (this.currentHighlight) {
          this.currentHighlight.shape.classList.remove('bpmn-highlight')
          this.currentHighlight = null
        }
        return
      }

      const elementRegistry = this.viewer.get('elementRegistry')
      const canvas = this.viewer.get('canvas')
      const element = elementRegistry.get(activityId)
      if (!element) return

      if (this.currentHighlight && this.currentHighlight.id !== activityId) {
        this.currentHighlight.shape.classList.remove('bpmn-highlight')
      }

      const gfx = elementRegistry.getGraphics(element)
      if (!gfx) return

      let shape = gfx.querySelector('rect, path')
      if (!shape) {
        shape = gfx
      }
      shape.classList.add('bpmn-highlight')
      this.currentHighlight = { id: activityId, shape }
      canvas.scrollToElement(activityId)
    },
    drawDiagramState: function() {
      const elementRegistry = this.viewer.get('elementRegistry')
      this.cleanDiagramState()
      this.drawActivitiesBadges(elementRegistry)
      this.drawSubprocessLinks(elementRegistry)
      if (this.jobDefinitions) {
        this.drawJobDefinitionBadges()
      }
      this.setSelectableOnAllowedElements()
      this.buildActivityMap(elementRegistry)
    },
    buildActivityMap: function(elementRegistry) {
      const activityMap = {}
      const externalTaskActivityIds = []
      elementRegistry.getAll().forEach(el => {
        const bo = el.businessObject
        if (bo?.id && bo?.name) {
          activityMap[bo.id] = bo.name
        }
        // same check as the engine (BpmnParse)
        if (bo?.id && bo.get?.('camunda:type')?.toLowerCase() === 'external') {
          externalTaskActivityIds.push(bo.id)
        }
      })
      this.$store.commit('setProcessActivities', activityMap)
      this.$store.commit('setExternalTaskActivityIds', externalTaskActivityIds)
    },
    setSelectableOnAllowedElements: function() {
      const elementRegistry = this.viewer.get('elementRegistry')
      elementRegistry.getAll().forEach(el => {
        if (this.getTypeAllowed(el.type, interactionTypes)) {
          const gfx = elementRegistry.getGraphics(el)
          if (gfx && !gfx.classList.contains('selectable')) {
            gfx.classList.add('selectable')
          }
        }
      })
    },
    drawActivitiesBadges: function(elementRegistry) {
      const historyStatistics = this.historicActivityStatistics
      const historyLevel = this.$root.config.camundaHistoryLevel
      const mergedStatistics = this.getMergedStatistics(historyStatistics, historyLevel)

      mergedStatistics.forEach(stat => {
        this.drawActivityBadges(stat, elementRegistry)
      })
    },
    getMergedStatistics: function(historyStatistics, historyLevel) {
      if (this.selectedInstance) {
        return this.getInstanceMergedStatistics(historyStatistics, historyLevel)
      } else if (historyLevel !== 'full') {
        return this.getProcessMergedStatistics(historyStatistics)
      } else {
        return this.getProcessMergedStatisticsFullHistory(historyStatistics)
      }
    },
    getInstanceMergedStatistics(historyStatistics, historyLevel) {

      if (!this.activityInstance) return historyStatistics

      const isFull = historyLevel === 'full'
      const activitiesMap = new Map()

      const addInstance = (activityId, incidentCount = 0) => {
        const existing = activitiesMap.get(activityId) || { instances: 0, openIncidents: 0 }
        activitiesMap.set(activityId, {
          instances: existing.instances + 1,
          openIncidents: isFull ? undefined : existing.openIncidents + incidentCount
        })
      }

      // Transition instances: activities currently between sequence flows/gateways
      this.activityInstance.childTransitionInstances?.forEach(trans =>
        addInstance(trans.activityId, trans.incidents?.length || 0)
      )

      // On non-full history levels the history API has no incident data, so we
      // read incidents directly from the runtime activity tree instead.
      if (!isFull) {
        this.activityInstance.childActivityInstances?.forEach(act =>
          addInstance(act.activityId, act.incidents?.length || 0)
        )
      }

      // Merge runtime data into history entries.
      // Full history: only override instance count. Otherwise merge all runtime fields.
      const merged = historyStatistics.map(stat => {
        const runtime = activitiesMap.get(stat.id)
        if (!runtime) return stat
        return isFull ? { ...stat, instances: runtime.instances } : { ...stat, ...runtime }
      })

      // Append runtime-only activities not present in history
      const knownIds = new Set(historyStatistics.map(stat => stat.id))
      activitiesMap.forEach((data, activityId) => {
        if (knownIds.has(activityId)) return
        merged.push({
          id: activityId,
          instances: data.instances,
          finished: 0,
          canceled: 0,
          openIncidents: isFull ? 0 : (data.openIncidents || 0),
          resolvedIncidents: 0,
          deletedIncidents: 0
        })
      })

      return merged
    },
    getProcessMergedStatistics: function(historyStatistics) {
      if (!this.statistics || this.statistics.length === 0) {
        return historyStatistics || []
      }

      const historyMap = new Map((historyStatistics || []).map(hs => [hs.id, hs]))

      const merged = this.statistics.map(stat => {
        const historyStat = historyMap.get(stat.id)
        return {
          id: stat.id,
          instances: this.useRuntimeStats || stat.id === this.runtimeActivityId
            ? (stat.instances || 0)
            : historyStat?.instances ?? (stat.instances || 0),
          finished: historyStat?.finished || 0,
          canceled: historyStat?.canceled || 0,
          openIncidents: this.countRuntimeIncidents(stat),
          resolvedIncidents: historyStat?.resolvedIncidents || 0,
          deletedIncidents: historyStat?.deletedIncidents || 0
        }
      })

      // Add historyStatistics not in this.statistics
      historyStatistics?.forEach(hs => {
        if (!this.statistics.some(s => s.id === hs.id)) {
          merged.push(hs)
        }
      })

      return merged
    },
    getProcessMergedStatisticsFullHistory: function(historyStatistics) {
      // Unfiltered, runtime and history agree on open incidents, so the same merge applies
      if (this.useRuntimeStats) return this.getProcessMergedStatistics(historyStatistics)
      const merged = historyStatistics.map(hs => {
        const stat = this.statistics?.find(s => s.id === hs.id)
        if (!stat) return hs
        return { ...hs, instances: hs.id === this.runtimeActivityId ? (stat.instances || 0) : hs.instances ?? stat.instances }
      })
      // Tokens waiting in an async job of the selected activity have no history entry yet;
      // runtimeActivityId is only set unfiltered, so its runtime incidents can be trusted
      const selected = this.statistics?.find(s => s.id === this.runtimeActivityId)
      if (selected && !historyStatistics.some(hs => hs.id === selected.id)) {
        merged.push({ id: selected.id, instances: selected.instances || 0, finished: 0, canceled: 0, openIncidents: this.countRuntimeIncidents(selected), resolvedIncidents: 0, deletedIncidents: 0 })
      }
      return merged
    },
    countRuntimeIncidents: function(stat) {
      return stat.incidents?.reduce((sum, inc) => sum + (inc.incidentCount || 0), 0) || 0
    },
    drawActivityBadges: function(stat, elementRegistry) {
      const element = elementRegistry.get(stat.id)
      if (!element) return

      const options = this.badgeOptions || {}

      this.drawCanceledBadge(stat, options)
      this.drawFinishedBadge(stat, options)
      this.drawClosedIncidentsBadge(stat, options)
      this.drawRunningInstancesBadge(stat, options)
      this.drawOpenIncidentsBadge(stat, options)
    },
    drawCanceledBadge: function(stat, options) {
      if (options.showCanceled && stat.canceled > 0) {
        const html = this.getBadgeOverlayHtml(stat.canceled, 'bg-warning', 'canceledInstances', stat.id)
        this.setHtmlOnDiagram(stat.id, html, { bottom: 15, right: 13 })
      }
    },
    drawFinishedBadge: function(stat, options) {
      const actualFinished = stat.finished - (stat.canceled || 0)
      if (options.showHistory && actualFinished > 0) {
        const position = stat.canceled > 0 ? { bottom: 35, right: 13 } : { bottom: 15, right: 13 }
        const html = this.getBadgeOverlayHtml(actualFinished, 'bg-gray text-dark', 'activitiesHistory', stat.id)
        this.setHtmlOnDiagram(stat.id, html, position)
      }
    },
    drawClosedIncidentsBadge: function(stat, options) {
      if (!options.showClosedIncidents || !this.selectedInstance) return
      if (stat.resolvedIncidents === 0 && stat.deletedIncidents === 0) return

      const mainOccupied =
        (options.showCanceled && stat.canceled > 0) ||
        ((!stat.canceled || stat.canceled === 0) && (options.showHistory && (stat.finished - (stat.canceled || 0)) > 0))

      const position = mainOccupied ? { bottom: 15, right: 36 } : { bottom: 15, right: 13 }
      const html = this.getBadgeOverlayHtml('!', 'bg-danger', 'closedIncidents', stat.id)
      this.setHtmlOnDiagram(stat.id, html, position)
    },
    drawRunningInstancesBadge: function(stat, options) {
      if (options.showRunning && stat.instances > 0) {
        const html = this.getBadgeOverlayHtml(stat.instances, 'bg-info', 'runningInstances', stat.id)
        this.setHtmlOnDiagram(stat.id, html, { bottom: 15, left: -7 })
      }
    },
    drawOpenIncidentsBadge: function(stat, options) {
      if (options.showIncidents && stat.openIncidents > 0) {
        const position = stat.instances > 0 ? { bottom: 15, left: 18 } : { bottom: 15, left: -7 }
        const html = this.getBadgeOverlayHtml(stat.openIncidents, 'bg-danger', 'openIncidents', stat.id)
        this.setHtmlOnDiagram(stat.id, html, position)
      }
    },
    redrawSubprocessLinks: function() {
      const removed = new Set(this.subprocessLinkOverlays)
      this.cleanDiagramState(this.subprocessLinkOverlays)
      this.overlayList = this.overlayList.filter(overlayId => !removed.has(overlayId))
      this.drawSubprocessLinks(this.viewer.get('elementRegistry'))
    },
    drawSubprocessLinks: function(elementRegistry) {
      this.subprocessLinkOverlays = []
      if (this.badgeOptions?.showCalledProcesses === false) return
      // Without a process definition (e.g. a deployment's resource preview) there is nothing to resolve the links against
      if (!this.processDefinitionId) return
      const staticCalledProcesses = this.staticCalledProcessDefinitions || []
      const activityStats = this.historicActivityStatistics || []

      // Map to know which activity calls which process
      const activityToProcessMap = {}
      staticCalledProcesses.forEach(proc => {
        proc.calledFromActivityIds.forEach(activityId => {
          activityToProcessMap[activityId] = proc
        })
      })

      // Call activities of the selected instance that started a called instance, the target of their link
      const activitiesWithCalledInstance = new Set(
        this.selectedInstanceActivityHistory
          .filter(activity => activity.calledProcessInstanceId)
          .map(activity => activity.activityId)
      )

      const callActivitiesList = elementRegistry.getAll().filter(el => el.type === 'bpmn:CallActivity')

      callActivitiesList.forEach(ca => {
        const shape = elementRegistry.get(ca.id)
        if (!shape) return
        // A case call (camunda:caseRef) names no process, so there is no process to link to
        if (!ca.businessObject?.calledElement) return

        const calledProcess = activityToProcessMap[ca.id]
        const activityStat = activityStats.find(stat => stat.id === ca.id)
        const runningInstances = activityStat?.instances || 0

        // A definition the engine resolved is static; without one, an expression in the reference means it is only known at runtime
        const isStatic = !!calledProcess
        const isDynamic = !isStatic && this.hasExpressionReference(ca.businessObject)

        const inInstanceView = !!this.selectedInstance
        const hasCalledInstance = inInstanceView && activitiesWithCalledInstance.has(ca.id)
        // Without a resolved definition: in an instance only a called instance it started is a target;
        // in the definition view a running expression opens the called process definitions tab
        const canOpenCalledInstance = inInstanceView ? hasCalledInstance : (isDynamic && runningInstances > 0)
        const callsDisplayedDefinition = !!calledProcess &&
          calledProcess.id === (this.selectedInstance?.processDefinitionId ?? this.processDefinitionId)
        // In an instance a recursive call opens the called instance, which is another page
        const isCurrentDefinitionAlreadyVisible = callsDisplayedDefinition && !hasCalledInstance

        const disabled = (!isStatic && !canOpenCalledInstance) || isCurrentDefinitionAlreadyVisible

        const title = this.$t(this.getSubprocessLinkTitleKey({
          disabled, isDynamic, isCurrentDefinitionAlreadyVisible,
          staticDefinitionsLoaded: !!this.staticCalledProcessDefinitions
        }))

        const wrapper = document.createElement('div')
        wrapper.title = title

        const button = document.createElement('button')
        button.type = 'button'
        button.className = 'btn btn-info btn-sm mdi mdi-link-variant px-1 py-0 text-white'

        if (disabled) {
          button.disabled = true
          wrapper.style.cursor = 'not-allowed'
        } else {
          button.addEventListener('click', () => {
            this.openSubprocess(ca.id, calledProcess)
          })
        }

        wrapper.appendChild(button)
        this.subprocessLinkOverlays.push(this.setHtmlOnDiagram(ca.id, wrapper, { bottom: -7, right: 15 }))
      })
    },
    // The engine resolves a call activity at runtime when its process key, version, version tag or tenant is an expression
    hasExpressionReference: function(businessObject) {
      const references = [
        businessObject?.calledElement,
        ...calledElementReferenceAttributes.map(name => businessObject?.get?.(`camunda:${name}`))
      ]
      return references.some(value => /[$#]\{.+\}/.test(value || ''))
    },
    getSubprocessLinkTitleKey: function({ disabled, isDynamic, isCurrentDefinitionAlreadyVisible, staticDefinitionsLoaded }) {
      if (!disabled) return 'bpmn-viewer.legend.openSubprocess'
      if (isCurrentDefinitionAlreadyVisible) return 'bpmn-viewer.legend.currentSubprocess'
      if (isDynamic) return 'bpmn-viewer.legend.disabledSubprocess'
      // "Not deployed" only holds once the static definitions of this diagram are known
      return staticDefinitionsLoaded ? 'bpmn-viewer.legend.notFoundSubprocess' : 'bpmn-viewer.legend.unknownSubprocess'
    },
    getBadgeOverlayHtml: function(number, classes, type, activityId) {
      const title = this.$t('bpmn-viewer.legend.' + type)
      let styleStr = "width: max-content;"
      if (activityId) {
        styleStr += " cursor: pointer;"
      }

      

      const formattedNumber = localStorage?.getItem('cibseven:preferences:shortenBadgeNumbers') !== 'false' ? abbreviateNumber(number,0,{symbols:["", "K", "M"]}) : number

      const overlayHtml = `
        <span data-activity-id="${activityId || ''}" data-type="${type || ''}" class="bubble position-absolute" style="${styleStr}" title="${title}">
          <span class="badge rounded-pill border border-dark px-2 py-1 me-1 ${classes}">${formattedNumber}</span>
        </span>
      `
      return overlayHtml
    },
    cleanDiagramState: function(overlayList) {
      const overlays = this.viewer.get('overlays')
      const list = overlayList || this.overlayList

      list.forEach(overlayId => overlays.remove(overlayId))

      list.splice(0, list.length)
    },
    setHtmlOnDiagram: function(id, html, position) {
      const overlays = this.viewer.get('overlays')
      const overlayId = overlays.add(id, { position, html })
      this.overlayList.push(overlayId)
      return overlayId
    },
    getTypeAllowed: function(typeI, types) {
      return types.some(type => typeI.includes(type))
    },
    async openSubprocess(activityId, calledProcess) {
      try {
        if (this.selectedInstance) {
          const calledProcessInstance = await this.findCalledProcessInstance(activityId)
          if (calledProcessInstance) {
            return this.navigateToSubprocess(calledProcessInstance.processDefinitionKey, calledProcessInstance.processDefinitionVersion, calledProcessInstance.id)
          }
        }
        // Without a called instance, open the called definition
        if (calledProcess) return this.navigateToSubprocess(calledProcess.key, calledProcess.version, null)
        // No known target (an expression, or a name the engine did not resolve): show what was called instead.
        // Guessing the latest version of the name could open a definition that is not deployed, or the wrong version or tenant
        const tab = this.selectedInstance ? 'calledProcessInstances' : 'calledProcessDefinitions'
        this.$router.push({ path: this.$route.path, query: { ...this.$route.query, tab } })
        // Narrows that tab to this call activity, so the click shows something even when the tab is already open
        this.selectDiagramElement(this.viewer.get('elementRegistry').get(activityId))
      } catch (error) {
        console.error('Failed to open subprocess:', error)
      }
    },
    // The latest instance the call activity started in the selected instance, or null
    findCalledProcessInstance: async function(activityId) {
      // From the history the viewer already has; a later run that started nothing (cancelled, waiting on an
      // async job) must not hide an earlier run's called instance
      const calledProcessInstanceId = this.selectedInstanceActivityHistory
        .filter(activity => activity.activityId === activityId && activity.calledProcessInstanceId)
        .sort((a, b) => (b.startTime || '').localeCompare(a.startTime || ''))[0]?.calledProcessInstanceId
      if (!calledProcessInstanceId) return null
      const instances = await HistoryService.findProcessesInstancesHistory({ processInstanceId: calledProcessInstanceId })
      // Its history can be gone (cleanup, deletion) while the call activity still names it
      return instances?.[0] ?? null
    },
    async navigateToSubprocess(processKey, versionIndex, calledProcessInstanceId) {
      if (calledProcessInstanceId) {
        await this.$router.push({
          name: 'process-instance-id',
          params: {
            instanceId: calledProcessInstanceId
          },
          query: {
            parentProcessDefinitionId: this.processDefinitionId,
            tab: 'variables',
          }
        })
      }
      else {
        await this.$router.push({
          name: 'process',
          params: {
            processKey,
            versionIndex,
            // TODO: consider passing tenantId if multi-tenancy is used
          },
          query: {
            parentProcessDefinitionId: this.processDefinitionId,
            tab: 'instances',
          }
        })
      }
    },
    drawJobDefinitionBadges: function() {
      if (this.badgeOptions?.showJobDefinitions === false) return
      const overlays = this.viewer?.get('overlays')
      const elementRegistry = this.viewer?.get('elementRegistry')
      if (!overlays || !elementRegistry || !Array.isArray(this.jobDefinitions)) return
      Object.values(this.suspendedOverlayMap).forEach((overlayId) => {
        overlays.remove(overlayId)
      })
      this.suspendedOverlayMap = {}
      this.jobDefinitions.forEach(jobDefinition => {
        const shape = elementRegistry.get(jobDefinition.activityId)
        if (shape && jobDefinition.suspended) {
          const title = this.$t('bpmn-viewer.legend.suspendedJobDefinition')
          const suspendedBadge = `
            <span class="badge bg-warning rounded-pill text-white border border-dark px-2 py-1" title="${title}">
              <span class="mdi mdi-pause"></span>
            </span>`
          const overlayId = overlays.add(jobDefinition.activityId, {
            position: { top: -10, right: 15 },
            html: suspendedBadge
          })
          this.suspendedOverlayMap[jobDefinition.activityId] = overlayId
        }
      })
    }
  },
  beforeUnmount: function() {
    this.setDiagramReady(false)
    clearTimeout(this.viewboxChangeTimer)
    // Clean up document event listener to prevent memory leaks
    if (this.overlayClickHandler) {
      document.removeEventListener('click', this.overlayClickHandler)
      this.overlayClickHandler = null
    }
    // Clean up viewer if it exists
    if (this.viewer) {
      this.viewer.destroy()
    }
  }
}
</script>
