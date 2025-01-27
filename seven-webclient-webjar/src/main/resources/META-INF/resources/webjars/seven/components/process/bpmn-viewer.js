/* globals BpmnJS, setTimeout, document */
	
	import { ProcessService } from '../../services.js';	

	var interactionTypes = ['bpmn:UserTask', 'bpmn:CallActivity']
	var drawedTypes = ['userTask', 'serviceTask', 'scriptTask', 'callActivity', 'exclusiveGateway', 'endEvent', 'startEvent']
	var events = ['element.click', 'element.hover', 'element.out']
	
	function getActivitiesToMark(treeObj) {
		var result = []
		if ((treeObj.childTransitionInstances.length === 0 && treeObj.childActivityInstances.length === 0)) {
			result.push(treeObj.activityId)
		}
		else {
			treeObj.childTransitionInstances.forEach(transitionInstance => {
				result.push(transitionInstance.activityId)
			})
			treeObj.childActivityInstances.forEach(activityInstance => {
				result = result.concat(getActivitiesToMark(activityInstance))
			})
		}
		return result
	}
	
	const BpmnViewer = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/process/bpmn-viewer.html').then(function (html) {
            return {
                template: html,
                props: { activityInstance: Object, activityInstanceHistory: Array, 
					statistics: Array, activityId: String, processDefinitionId: String, activitiesHistory: Array },
				data: function() { 
					return { 
						viewer: null,
						currentElement: null,
						overlayList: [],
						loader: true,
						runningActivities: [] 
					} 
				},
				watch: {
					activityInstanceHistory: function() {
						setTimeout(() => {
							this.drawDiagramState()
							if (this.currentElement) this.currentElement.gfx.classList.remove('selectable', 'highlight')
							this.currentElement = null
							this.$emit('task-selected', null)
						}, 100)
					}
				},
				mounted: function() {
					this.viewer = new BpmnJS({ container: this.$refs.diagram })
					this.viewer.on('import.done', event => {
				        var error = event.error;
				        var warnings = event.warnings;
				        if (error) { this.$emit('error', error) } 
				        else { this.$emit('shown', warnings) }
						this.drawDiagramState()
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
					setEvents: function() {
						var eventBus = this.viewer.get('eventBus')
						events.forEach(event => {
							eventBus.on(event, function(e) {
								this.setDiagramInteraction(event, e)
							})
						})
					},
					setDiagramInteraction: function(evt, e) {
						var element = e.element
						if (this.getTypeAllowed(element.type, interactionTypes)) {
							if (evt === 'element.hover') {
								e.gfx.classList.add('selectable', 'highlight')
							}
							if (evt === 'element.out') {
								if (!this.currentElement || this.currentElement.element.id !== element.id) 
									e.gfx.classList.remove('selectable', 'highlight')
							}
							if (evt === 'element.click') {
								if (this.currentElement) this.currentElement.gfx.classList.remove('selectable', 'highlight')
								this.currentElement = e
								e.gfx.classList.add('selectable', 'highlight')
								this.$emit('task-selected', element)
							}
						}
					},
					drawDiagramState: function() {
						var overlays = this.viewer.get('overlays')
				        var elementRegistry = this.viewer.get('elementRegistry')
				        var self = this
				        this.cleanDiagramState()
				        
				        if (this.activityInstance != null) {
							getActivitiesToMark(this.activityInstance).forEach(activityId => {
								var htmlTemplate = '<div><i class="mdi mdi-arrow-right-drop-circle mdi-24px text-success"/></div>'
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
									var shape = elementRegistry.get(item.id)
									if (shape) {
										var htmlTemplate = this.getBadgeOverlayHtml(item.instances, 'bg-info')
										if (item.failedJobs > 0) {
											htmlTemplate += '<span class="badge bg-danger rounded-pill text-white border border-dark px-2 py-1">'
											htmlTemplate += item.failedJobs + '</span>'
										}
										this.setHtmlOnDiagram(overlays, item.id, htmlTemplate, { bottom: 15, left: -7 })
										shape.nInstances = item.instances
									} 
								})
							}
							if (this.activitiesHistory) {
								filledActivities = {}
								this.drawActivitiesHistory(this.activitiesHistory, elementRegistry, overlays)
							}
						}
						
						var callActivitiesList = elementRegistry.getAll().filter(function(element) {
					  		return element.type === 'bpmn:CallActivity'
						})
						
						if (callActivitiesList.length > 0) {
							callActivitiesList.forEach(ca => {
								var shape = elementRegistry.get(ca.id)
								if (shape) {
									var disabled = (ca.nInstances === undefined && this.runningActivities.indexOf(ca.id) === -1)
									var htmlTemplate = '<button type="button" class="btn btn-info btn-sm mdi mdi-link-variant px-1 py-0" '
									htmlTemplate += (disabled ? 'disabled' : '') + '></button>'
									var tempDiv = document.createElement('div')
									tempDiv.innerHTML = htmlTemplate
									var button = tempDiv.firstChild
									button.addEventListener('click', () => {
										this.openSubprocess(ca.id)
									})
									this.setHtmlOnDiagram(overlays, ca.id, button, { bottom: -7, right: 15 })									
								} 
							})
						}
						
						this.viewer.on('element.click', function(event) {
							if (self.currentElement) self.currentElement.gfx.classList.remove('selectable', 'highlight')
							if (self.getTypeAllowed(event.element.type, interactionTypes) && 
								self.statistics.some(obj => obj.id === event.element.id) && 
								event.element.id !== self.activityId) {
								self.currentElement = event
								event.gfx.classList.add('selectable', 'highlight')
								if (self.activityInstance) {
									var childActivity = self.activityInstance.childActivityInstances.find(obj => obj.activityId === event.element.id)
									self.$emit('child-activity', childActivity)
								} else self.$emit('activity-id', event.element.id)
							} else if(event.element.id !== self.activityId && self.activityId !== '') {
								if (self.activityInstance) self.$emit('child-activity', null)
								else self.$emit('activity-id', '')
							}
					  	})
					  	
					  	this.viewer.on('element.hover', function(event) {
							if (self.getTypeAllowed(event.element.type, interactionTypes) && self.statistics.some(obj => obj.id === event.element.id)) 
								event.gfx.classList.add('selectable', 'highlight')
								
							event.gfx.addEventListener('mouseleave', function() {
								if (!self.currentElement || self.currentElement.element.id !== event.element.id) 
									event.gfx.classList.remove('selectable', 'highlight')
					        })
						})						
					  	
					},
					drawActivitiesHistory: function(activities, elementRegistry, overlays) {
						var filledActivities = {}
						activities.forEach(item => {
							var typeAllowed = this.getTypeAllowed(item.activityType, drawedTypes)
							if (!typeAllowed || !item.endTime) return
							if (!filledActivities[item.activityId]) filledActivities[item.activityId] = 1
							else filledActivities[item.activityId]++
						})
						Object.keys(filledActivities).forEach(key => {
							var shape = elementRegistry.get(key)
							if (shape) {
								this.setHtmlOnDiagram(overlays, key, this.getBadgeOverlayHtml(filledActivities[key], 'bg-gray'), 
									{ bottom: 15, right: 13 })
							}
						})
					},
					getBadgeOverlayHtml: function(number, classes) {
						var overlayHtml = `<span class="position-absolute" style="width: max-content">
							<span class="badge rounded-pill border border-dark px-2 py-1 me-1 ${classes}">${number}</span>`
						return overlayHtml
					},
					cleanDiagramState: function() {
						var overlays = this.viewer.get('overlays')
						if (this.overlayList.length > 0) {
							this.overlayList.forEach(overlayId => {
								overlays.remove(overlayId)
							})
							this.overlayList = []
						}
					},
					clearEvents: function() {
						var eventBus = this.viewer.get('eventBus')
						events.forEach(event => {
							eventBus.off(event)
						})
					},
					setHtmlOnDiagram: function(overlays, id, html, position) {
						var overlayId = overlays.add(id, {
							position: position, html: html
						})
						this.overlayList.push(overlayId)
					},
					getTypeAllowed: function(typeI, types) {
						return types.some(type => {
							return typeI.indexOf(type) !== -1
						})
					},
					openSubprocess: function(activityId) {
						ProcessService.findCalledProcessDefinitions(this.processDefinitionId).then(subprocess => {
							var process = subprocess.find(item => item.calledFromActivityIds.includes(activityId))							
							if (process) {
							  this.$emit('open-subprocess', process)
							}
						})
					}
				}
            }
        })
	})
	
	export { BpmnViewer }