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
    </div>
    <div class="position-absolute" style="right:15px;bottom:225px;">
      <b-button size="sm" class="border" variant="light" :title="$t('bpmn-viewer.resetZoom')" @click="resetZoom()">
        <span class="mdi mdi-18px mdi-target"></span>
      </b-button>
    </div>
    <div class="btn-group-vertical" style="position:absolute;right:15px;bottom:139px;">
      <b-button size="sm" class="border" variant="light" :title="$t('bpmn-viewer.zoomIn')" @click="zoomIn()">
        <span class="mdi mdi-18px mdi-plus"></span>
      </b-button>
      <b-button size="sm" class="border" variant="light" :title="$t('bpmn-viewer.zoomOut')" @click="zoomOut()">
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
import { BWaitingBox } from 'cib-common-components'
import { mapActions, mapGetters } from 'vuex'
import { HistoryService } from '@/services.js'

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

export default {
  name: 'BpmnViewer',
  emits: ['task-selected', 'child-activity', 'overlay-click'],
  components: { BWaitingBox },
  props: {
    activityInstance: Object,
    activityInstanceHistory: Array,
    statistics: Array,
    processDefinitionId: String,
    activeTab: String,
    selectedInstance: Object,
    badgeOptions: {
      type: Object,
      default: () => ({
        showRunning: true,
        showHistory: true,
        showCanceled: true,
        showIncidents: true,
        showCalledProcesses: true,
        showJobDefinitions: true
      })
    }
  },
  data: function() {
    return {
      viewer: null,
      currentHighlight: null,
      overlayList: [],
      loader: true,
      suspendedOverlayMap: {},
      overlayClickHandler: null
    }
  },
  computed: {
    ...mapGetters(['highlightedElement', 'getHistoricActivityStatistics']),
    ...mapGetters('calledProcessDefinitions', ['getStaticCalledProcessDefinitions']),
    ...mapGetters('job', ['jobDefinitions']),
    historicActivityStatistics() {
      return this.getHistoricActivityStatistics(this.processDefinitionId)
    }
  },
  watch: {
    historicActivityStatistics: {
      handler() {
        this.drawDiagramState()
      },
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
  },
  mounted: function() {
    this.viewer = new NavigatedViewer({ container: this.$refs.diagram })
    this.viewer.on('import.done', () => {
      this.drawDiagramState()
      this.attachEventListeners()
      //Small timer so the diagram is fully rendered before setting it ready
      setTimeout(() => {
        this.setDiagramReady(true)
      }, 500)
    })
  },
  methods: {
    ...mapActions(['selectActivity', 'clearActivitySelection', 'setHighlightedElement', 'loadActivitiesInstanceHistory', 'getProcessById']),
    ...mapActions('diagram', ['setDiagramReady']),
    showDiagram: function(xml) {
      this.setDiagramReady(false)
      this.loader = true
      this.viewer.importXML(xml).then(() => {
        setTimeout(() => {
          this.viewer.get('canvas').zoom('fit-viewport')
          this.loader = false
        }, 500)
      })
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
    attachEventListeners: function() {
      // Remove existing overlay click handler if it exists
      if (this.overlayClickHandler) {
        document.removeEventListener('click', this.overlayClickHandler)
      }

      const eventBus = this.viewer.get('eventBus')
      // BPMN element click
      eventBus.on('element.click', (event) => {
        if (this.getTypeAllowed(event.element.type, interactionTypes)) {
          this.setHighlightedElement(event.element.id)
          if (this.activityInstance) {
            const childActivity = this.activityInstance.childActivityInstances.find(obj => obj.activityId === event.element.id)
            this.$emit('child-activity', childActivity || event.element)
          } else {
            this.selectActivity(event.element.id)
          }
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
          this.selectActivity(bubble.dataset.activityId)
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
      let activityId = ''
      if (typeof item === 'string') {
        activityId = item
      } else if (item && item.activityId) {
        activityId = item.activityId
      } else {
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
      elementRegistry.getAll().forEach(el => {
        const bo = el.businessObject
        if (bo?.id && bo?.name) {
          activityMap[bo.id] = bo.name
        }
      })
      this.$store.commit('setProcessActivities', activityMap)
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
      const options = this.badgeOptions || {}
      historyStatistics.forEach(stat => {
        const element = elementRegistry.get(stat.id)
        if (!element) return

        // Handle canceled activities - draw in the main position
        if (options.showCanceled && stat.canceled > 0) {
          const html = this.getBadgeOverlayHtml(stat.canceled, 'bg-warning', 'canceledInstances', stat.id)
          this.setHtmlOnDiagram(stat.id, html, { bottom: 15, right: 13 })
        }
        
        // Handle finished activities - draw above canceled ones or in main position if no canceled
        const actualFinished = stat.finished - (stat.canceled || 0)
        if (options.showHistory && actualFinished > 0) {
          const position = stat.canceled > 0 ? { bottom: 35, right: 13 } : { bottom: 15, right: 13 }
          const html = this.getBadgeOverlayHtml(actualFinished, 'bg-gray', 'activitiesHistory', stat.id)
          this.setHtmlOnDiagram(stat.id, html, position)
        }
        if (options.showRunning && stat.instances > 0) {
          const html = this.getBadgeOverlayHtml(stat.instances, 'bg-info', 'runningInstances', stat.id)
          this.setHtmlOnDiagram(stat.id, html, { bottom: 15, left: -7 })
        }
        if (options.showIncidents && stat.openIncidents > 0) {
          const position = stat.instances > 0 ? { bottom: 15, left: 18 } : { bottom: 15, left: -7 }
          const html = this.getBadgeOverlayHtml(stat.openIncidents, 'bg-danger', 'openIncidents', stat.id)
          this.setHtmlOnDiagram(stat.id, html, position)
        }
      })
    },
    drawSubprocessLinks: function(elementRegistry) {
      if (this.badgeOptions?.showCalledProcesses === false) return
      const staticCalledProcesses = this.getStaticCalledProcessDefinitions || []
      const activityStats = this.historicActivityStatistics || []

      // Map to know which activity calls which process
      const activityToProcessMap = {}
      staticCalledProcesses.forEach(proc => {
        proc.calledFromActivityIds.forEach(activityId => {
          activityToProcessMap[activityId] = proc
        })
      })

      // Map to know which activities have been executed
      // This is used to determine if a dynamic call activity can be opened
      const executedActivities = new Set(
        activityStats
          .filter(stat => (stat.finished ?? 0) > 0 || (stat.canceled ?? 0) > 0)
          .map(stat => stat.id)
      )

      const callActivitiesList = elementRegistry.getAll().filter(el => el.type === 'bpmn:CallActivity')

      callActivitiesList.forEach(ca => {
        const shape = elementRegistry.get(ca.id)
        if (!shape) return

        const calledProcess = activityToProcessMap[ca.id]
        const activityStat = activityStats.find(stat => stat.id === ca.id)
        const runningInstances = activityStat?.instances || 0

        const calledElement = ca.businessObject?.calledElement || ''
        const isDynamic = /^\$\{.+\}$/.test(calledElement)
        const isStatic = !isDynamic && !!calledProcess
        
        const inInstanceView = !!this.selectedInstance
        const wasExecuted = inInstanceView && executedActivities.has(ca.id)
        const shouldEnableDynamic = isDynamic && (runningInstances > 0 || wasExecuted)
        const isCurrentDefinitionAlreadyVisible = !!this.selectedInstance && calledProcess?.id === this.selectedInstance.processDefinitionId

        const disabled = (!isStatic && !shouldEnableDynamic) || isCurrentDefinitionAlreadyVisible

        const title = disabled
          ? this.$t('bpmn-viewer.legend.disabledSubprocess')
          : this.$t('bpmn-viewer.legend.openSubprocess')

        const wrapper = document.createElement('div')
        wrapper.title = title

        const button = document.createElement('button')
        button.type = 'button'
        button.className = 'btn btn-info btn-sm mdi mdi-link-variant px-1 py-0'

        if (disabled) {
          button.disabled = true
          wrapper.style.cursor = 'not-allowed'
        } else {
          button.addEventListener('click', () => {
            this.openSubprocess(ca.id, calledProcess, isDynamic)
          })
        }

        wrapper.appendChild(button)
        this.setHtmlOnDiagram(ca.id, wrapper, { bottom: -7, right: 15 })
      })
    },
    getBadgeOverlayHtml: function(number, classes, type, activityId) {
      var title = this.$t('bpmn-viewer.legend.' + type)
      var styleStr = "width: max-content;"
      if (activityId) {
        styleStr += " cursor: pointer;"
      }
      var overlayHtml = `
        <span data-activity-id="${activityId || ''}" data-type="${type || ''}" class="bubble position-absolute" style="${styleStr}" title="${title}">
          <span class="badge rounded-pill border border-dark px-2 py-1 me-1 ${classes}">${number}</span>
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
      let overlays = this.viewer.get('overlays')
      const overlayId = overlays.add(id, { position, html })
      this.overlayList.push(overlayId)
      return overlayId
    },
    getTypeAllowed: function(typeI, types) {
      return types.some(type => typeI.includes(type))
    },
    async openSubprocess(activityId, calledProcess, isDynamic) {
      try {
        if (this.selectedInstance) {
          const params = {
            processInstanceId: this.selectedInstance.id,
            activityType: 'callActivity',
            activityId,
            sortBy: 'startTime',
            sortOrder: 'desc',
            maxResults: 1
          }
          const calledInstance = await this.loadActivitiesInstanceHistory(params)
          const processDefinition = await HistoryService.findProcessesInstancesHistory({ processInstanceId: calledInstance[0].calledProcessInstanceId })
          if (calledInstance?.length && calledInstance[0].calledProcessInstanceId) {
            return this.navigateToSubprocess(processDefinition[0].processDefinitionKey, processDefinition[0].processDefinitionVersion, calledInstance[0].calledProcessInstanceId)
          }
        }
        // If no instance view or no instance found, try to get the called process directly
        if (isDynamic && !this.selectedInstance) {
          this.$router.push({
            path: this.$route.path,
            query: { ...this.$route.query, tab: 'calledProcessDefinitions' }
          })
        } else {          
          if (!calledProcess) {
            console.warn('No called process definition for activityId:', activityId)
            return
          }
          this.navigateToSubprocess(calledProcess.key, calledProcess.version, null)
        }
      } catch (error) {
        console.error('Failed to open subprocess:', error)
      }
    },
    async navigateToSubprocess(processKey, versionIndex, calledProcessInstanceId) {
      try {
        const params = { processKey, versionIndex }
        if (calledProcessInstanceId) {
          params.instanceId = calledProcessInstanceId
        }        
        const routeConfig = {
          name: 'process',
          params,
          query: { parentProcessDefinitionId: this.processDefinitionId, tab: params.instanceId ? 'variables' : 'instances' }
        }
        await this.$router.push(routeConfig)
      } catch (error) {
        console.error('Failed to navigate to subprocess:', error)
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
          var title = this.$t('bpmn-viewer.legend.suspendedJobDefinition')
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
