<template>
  <div>
    <BWaitingBox v-if="loader" class="h-100" ref="loader" styling="width:25%;height:25%;align-items:center;justify-content:center;position:absolute;top:36%;right:36%"></BWaitingBox>
    <div :class="loader ? 'invisible' : 'visible'" class="h-100">
      <div class="h-100" ref="diagram"></div>
    </div>
    <div class="d-none d-md-block position-absolute" style="right:30px; top:15px">
      <div v-if="activityInstanceHistory" class="row g-0 border rounded ms-1">
        <div class="h-100">
          <div class="bg-white border-light rounded-top text-center" style="opacity:0.8">
            <span>{{ $t('bpmn-viewer.legend.title') }}</span>
          </div>
          <div class="bg-light border-light rounded-bottom" style="opacity:0.8">
            <span class="px-2"><span class="mdi mdi-arrow-right-drop-circle mdi-24px text-success align-middle me-1"></span>{{ $t('bpmn-viewer.legend.currentTask') }}</span>
            <span class="pe-2"><span class="mdi mdi-card-outline mdi-24px text-info align-middle me-1"></span>{{ $t('bpmn-viewer.legend.selectedTask') }}</span>
          </div>
        </div>
      </div>
    </div>
    <div class="position-absolute" style="right:15px;bottom:100px;">
      <b-button size="sm" class="border" variant="light" :title="$t('bpmn-viewer.resetZoom')" @click="resetZoom()">
        <span class="mdi mdi-18px mdi-target"></span>
      </b-button>
    </div>
    <div class="btn-group-vertical" style="position:absolute;right:15px;bottom:15px;">
      <b-button size="sm" class="border" variant="light" :title="$t('bpmn-viewer.zoomIn')" @click="zoomIn()">
        <span class="mdi mdi-18px mdi-plus"></span>
      </b-button>
      <b-button size="sm" class="border" variant="light" :title="$t('bpmn-viewer.zoomOut')" @click="zoomOut()">
        <span class="mdi mdi-18px mdi-minus"></span>
      </b-button>
    </div>
  </div>
</template>

<script>
import NavigatedViewer from 'bpmn-js/lib/NavigatedViewer'
import { ProcessService } from '@/services.js'
import { BWaitingBox } from 'cib-common-components'

const interactionTypes = ['bpmn:UserTask', 'bpmn:CallActivity', 'bpmn:ScriptTask']
const drawedTypes = ['userTask', 'serviceTask', 'scriptTask', 'callActivity', 'exclusiveGateway', 'endEvent', 'startEvent']

function getActivitiesToMark(treeObj) {
  let result = []
  if (!treeObj.childTransitionInstances.length && !treeObj.childActivityInstances.length) {
    result.push(treeObj.activityId)
  } else {
    treeObj.childTransitionInstances.forEach(transitionInstance => {
      result.push(transitionInstance.activityId)
    })
    treeObj.childActivityInstances.forEach(activityInstance => {
      result = result.concat(getActivitiesToMark(activityInstance))
    })
  }
  return result
}

export default {
  name: 'BpmnViewer',
  emits: ['activity-id'],
  components: { BWaitingBox },
  props: {
    activityInstance: Object,
    activityInstanceHistory: Array,
    statistics: Array,
    activityId: String,
    processDefinitionId: String,
    activitiesHistory: Array
  },
  data: function() {
    return {
      viewer: null,
      currentHighlight: null,
      overlayList: [],
      loader: true,
      runningActivities: [],
      suspendedOverlayMap: {}
    }
  },
  computed: {
    jobDefinitions: function() {
      return this.$store.getters['jobDefinition/getJobDefinitions']
    }
  },
  watch: {
    activityInstanceHistory: function() {
      setTimeout(() => {
        this.drawDiagramState()
        if (this.currentHighlight) this.currentHighlight.shape.classList.remove('bpmn-highlight')
        this.currentHighlight = null
        this.$emit('task-selected', null)
      }, 100)
    },
    jobDefinitions: {
      handler() {
        if (this.viewer) {
          this.drawJobDefinitionBadges()
        }
      },
      immediate: true,
      deep: true
    }
  },
  mounted: function() {
    this.viewer = new NavigatedViewer({ container: this.$refs.diagram })
    this.viewer.on('import.done', event => {
      if (event.error) {
         this.$emit('error', event.error)
      } else {
         this.$emit('shown', event.warnings)
      }
      this.drawDiagramState()
      this.attachEventListeners()
    })
  },
  methods: {
    showDiagram: function(xml) {
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
      const eventBus = this.viewer.get('eventBus')
      eventBus.on('element.click', (event) => {
        if (this.getTypeAllowed(event.element.type, interactionTypes)) {
          this.highlightElement(event.element.id)
          if (this.activityInstance) {
            const childActivity = this.activityInstance.childActivityInstances.find(obj => obj.activityId === event.element.id)
            this.$emit('child-activity', childActivity || event.element)
          } else {
            this.$emit('activity-id', event.element.id)
          }
        } else {
          if (this.currentHighlight) {
            this.currentHighlight.shape.classList.remove('bpmn-highlight')
            this.currentHighlight = null
            this.$emit('activity-id', null)
          }
          this.$emit('task-selected', null)
        }
      })
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
      const overlaysContainers = document.querySelectorAll('.djs-overlays')
      if (overlaysContainers.length) {
        overlaysContainers.forEach((container) => {
          container.addEventListener('click', (event) => {
            const bubble = event.target.closest('.bubble')
            if (bubble && bubble.dataset.activityId) {
              this.highlightElement(bubble.dataset.activityId)
              this.$emit('activity-id', bubble.dataset.activityId)
            }
          })
        })
      }
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
      const overlays = this.viewer.get('overlays')
      const elementRegistry = this.viewer.get('elementRegistry')
      this.cleanDiagramState()
      if (this.activityInstance != null) {
        getActivitiesToMark(this.activityInstance).forEach(activityId => {
          const htmlTemplate = '<div><i class="mdi mdi-arrow-right-drop-circle mdi-24px text-success"/></div>'
          this.setHtmlOnDiagram(overlays, activityId, htmlTemplate, { top: -5, right: 25 })
          this.runningActivities.push(activityId)
        })
      }
      if (this.activityInstanceHistory != null) {
        this.drawActivitiesHistory(this.activityInstanceHistory, elementRegistry, overlays)
      }
      if (!this.activityInstance && !this.activityInstanceHistory) {
        if (this.statistics) {
          this.statistics.forEach(item => {
            const shape = elementRegistry.get(item.id)
            if (shape) {
              const htmlTemplate = this.getBadgeOverlayHtml(item.instances, 'bg-info', 'runingInstances', item.id)
              this.setHtmlOnDiagram(overlays, item.id, htmlTemplate, { bottom: 15, left: -7 })
              if (item.failedJobs > 0) {
                const failedHtml = this.getBadgeOverlayHtml(item.failedJobs, 'bg-danger', 'openIncidents', item.id)
                this.setHtmlOnDiagram(overlays, item.id, failedHtml, { top: -7, right: 15 })
              }
              shape.nInstances = item.instances
            }
          })
        }
        if (this.activitiesHistory) {
          this.drawActivitiesHistory(this.activitiesHistory, elementRegistry, overlays)
        }
      }
      const callActivitiesList = elementRegistry.getAll().filter(element => element.type === 'bpmn:CallActivity')
      callActivitiesList.forEach(ca => {
        const shape = elementRegistry.get(ca.id)
        if (shape) {
          const disabled = (ca.nInstances === undefined && this.runningActivities.indexOf(ca.id) === -1)
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
              this.openSubprocess(ca.id)
            })
          }
          wrapper.appendChild(button)
          this.setHtmlOnDiagram(overlays, ca.id, wrapper, { bottom: -7, right: 15 })
        }
      })
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
    drawActivitiesHistory: function(activities, elementRegistry, overlays) {
      const filledActivities = {}
      activities.forEach(item => {
        const typeAllowed = this.getTypeAllowed(item.activityType, drawedTypes)
        if (!typeAllowed || !item.endTime) return
        filledActivities[item.activityId] = (filledActivities[item.activityId] || 0) + 1
      })
      Object.keys(filledActivities).forEach(key => {
        const shape = elementRegistry.get(key)
        if (shape) {
          this.setHtmlOnDiagram(overlays, key, this.getBadgeOverlayHtml(filledActivities[key], 'bg-gray', 'activitiesHistory', null),
            { bottom: 15, right: 13 })
        }
      })
    },
    getBadgeOverlayHtml: function(number, classes, type, activityId) {
      var title = this.$t('bpmn-viewer.legend.' + type)
      var styleStr = "width: max-content;"
      if (activityId) {
        styleStr += " cursor: pointer;"
      }
      var overlayHtml = `
        <span data-activity-id="${activityId || ''}" class="bubble position-absolute" style="${styleStr}" title="${title}">
          <span class="badge rounded-pill border border-dark px-2 py-1 me-1 ${classes}">${number}</span>
        </span>
      `
      return overlayHtml
    },
    cleanDiagramState: function() {
      const overlays = this.viewer.get('overlays')
      this.overlayList.forEach(overlayId => overlays.remove(overlayId))
      this.overlayList = []
    },
    setHtmlOnDiagram: function(overlays, id, html, position) {
      const overlayId = overlays.add(id, { position, html })
      this.overlayList.push(overlayId)
    },
    getTypeAllowed: function(typeI, types) {
      return types.some(type => typeI.includes(type))
    },
    openSubprocess: function(activityId) {
      const childInstance = this.activityInstanceHistory?.find(
        ai => ai.activityId === activityId && ai.calledProcessInstanceId
      )
      if (childInstance && childInstance.calledProcessInstanceId) {
        ProcessService.findProcessInstance(childInstance.calledProcessInstanceId).then(subprocess => {
          const [processKey, versionIndex] = subprocess.definitionId.split(':')
          this.$router.push({ name: 'process', params: { processKey, versionIndex, instanceId: subprocess.id } })
        })
      } else {
        ProcessService.findCalledProcessDefinitions(this.processDefinitionId).then(subprocess => {
          const process = subprocess.find(item => item.calledFromActivityIds.includes(activityId))
          if (process) {
            this.$router.push({ name: 'process', params: { processKey: process.key } })
          }
        })
      }
    },
    drawJobDefinitionBadges: function() {
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
            position: { top: -10, left: -15 },
            html: suspendedBadge
          })
          this.suspendedOverlayMap[jobDefinition.activityId] = overlayId
        }
      })
    }
  }
}
</script>
