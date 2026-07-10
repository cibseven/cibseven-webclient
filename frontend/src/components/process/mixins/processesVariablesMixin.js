/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
import { HistoryService, ProcessService } from '@/services.js'
import variableUtils from '@/components/process/mixins/variableUtils.js'

const serviceMap = {
	ProcessService: ProcessService,
	HistoryService: HistoryService
}

export default {
	props: { process: Object, selectedInstance: Object, activityInstance: Object, activityInstanceHistory: Array },
	data: function () {
		return {
			loading: true,
			fetching: false,
			filter: {
				deserializeValues: false,
			},
			variables: [],
			variablesSource: null,
			file: null,
			selectedVariable: null
		}
	},
	watch: {
		'selectedInstance.id': {
			handler: function () {
				this.filter = {
					deserializeValues: false,
				}
				this.variables = []
				this.filteredVariables = []
				this.file = null
				this.selectedVariable = null
				this.loadSelectedInstanceVariables()
			}
		},
		activityInstancesGrouped: 'onScopeMapChanged'
	},
	computed: {
		isActiveInstance: function() {
			if (this.selectedInstance?.state) {
				// 'state' is available from historic process instances
				const activeStates = ['ACTIVE', 'SUSPENDED']
				return this.selectedInstance && activeStates.includes(this.selectedInstance.state)
			}
			else {
				// use runtime instance
				// they have 'ended' and 'suspended' states
				return this.selectedInstance && this.selectedInstance.ended === false
			}
		},
		historyAvailable: function () {
			return ['full', 'audit'].includes(this.$root.config.camundaHistoryLevel)
		},
		liveActivityInstanceIds: function () {
			const ids = new Set()
			if (this.activityInstance) {
				const collect = ai => {
					ids.add(ai.id)
					ai.childActivityInstances?.forEach(collect)
					ai.childTransitionInstances?.forEach(ti => ids.add(ti.id))
				}
				collect(this.activityInstance)
			}
			return ids
		},
		activityInstanceData: function () {
				const names = {}
				const activityIds = {}
				const flatten = instances => {
					instances.forEach(ai => {
						names[ai.id] = ai.name || ai.activityId
						activityIds[ai.id] = ai.activityId
						if (ai.childActivityInstances?.length > 0) flatten(ai.childActivityInstances)
					})
				}
				// finished scopes first, so the runtime tree overrides entries for scopes present in both
				if (this.activityInstanceHistory) {
					this.activityInstanceHistory.forEach(ai => {
						names[ai.id] = ai.activityName || ai.activityId
						activityIds[ai.id] = ai.activityId
					})
				}
				if (this.activityInstance) {
					names[this.activityInstance.id] = this.activityInstance.name
					flatten(this.activityInstance.childActivityInstances)
				} else if (this.selectedInstance) {
					names[this.selectedInstance.id] = this.selectedInstance.processDefinitionName
				}
				return { names, activityIds }
		},
		activityInstancesGrouped: function () {
			return this.activityInstanceData.names
		},
		activityInstanceIdToActivityId: function () {
			return this.activityInstanceData.activityIds
		},
		restFilter: function () {
			const result = {
				...this.filter,
				deserializeValues: false,
				sortBy: 'variableName',
				sortOrder: 'asc'
			}
			// https://docs.cibseven.org/rest/cibseven/2.0/#tag/Variable-Instance/operation/getVariableInstances
			if (result.activityInstanceIdIn) {
				result.activityInstanceIdIn = result.activityInstanceIdIn.join(',')
			}
			if (result.variableValues) {
				result.variableValues = result.variableValues.map((v) => `${v.name}_${v.operator}_${v.value}`).join(',')
			}
			return result
		}
	},
	methods: {
		loadSelectedInstanceVariables: function() {
			if (this.fetching) return // Prevent concurrent requests
			if (this.selectedInstance && this.activityInstancesGrouped) {
				if (this.historyAvailable) {
					// the history query is a superset of the runtime query (same ids, current values)
					// and also contains variables whose scope has already finished
					this.fetchInstanceVariables('HistoryService', 'fetchProcessInstanceVariablesHistory')
				} else if (this.isActiveInstance) {
					this.fetchInstanceVariables('ProcessService', 'fetchProcessInstanceVariables')
				} else {
					// no variables available for finished process instances if history level is 'activity' or 'none'
					this.variables = []
					this.filteredVariables = []
					this.loading = false
					this.fetching = false
				}
			}
		},
		onScopeMapChanged: function () {
			// scope maps (runtime tree / historic activity instances) can arrive after the
			// variables were already fetched - re-annotate in place instead of re-fetching
			if (this.variables.length && !this.fetching) {
				this.annotateVariables(this.variables)
				// in-place annotation does not trigger the 'variables' watcher, so refresh the filtered list
				if (typeof this.applyActivityFilter === 'function') this.applyActivityFilter()
				else this.filteredVariables = [...this.variables]
			} else {
				this.loadSelectedInstanceVariables()
			}
		},
		annotateVariables: function (variables) {
			variables.forEach(v => {
				v.scope = this.activityInstancesGrouped[v.activityInstanceId] || v.activityInstanceId
				v.scopeActivityId = this.activityInstanceIdToActivityId[v.activityInstanceId] || null
				v.isLive = this.isVariableScopeLive(v)
			})
		},
		isVariableScopeLive: function (v) {
			if (!this.isActiveInstance) return false
			// runtime rows are live by definition
			if (this.variablesSource === 'runtime') return true
			if (this.activityInstance) return this.liveActivityInstanceIds.has(v.activityInstanceId)
			// no runtime tree available (e.g. suspended instances, external embedders)
			return true
		},
		fetchInstanceVariables: async function (service, method) {
			this.fetching = true
			this.loading = true
			this.variablesSource = service === 'ProcessService' ? 'runtime' : 'history'
			const variables = await serviceMap[service][method](this.selectedInstance.id, this.restFilter)
			this.annotateVariables(variables)
			variables.sort((a, b) => a.name.localeCompare(b.name))

			this.variables = variables
			this.filteredVariables = [...variables]
			this.loading = false
			this.fetching = false
		},
    displayValue(variable) {
	  return variableUtils.displayValue(variable)
    },
    displayValueTooltip(item) {
      if (this.isFile(item)) {
        return this.$t('process-instance.download') + ': ' + this.displayValue(item)
      }
      else {
        return this.displayValue(item)
      }
    },
    isFile(variable) {
      return variableUtils.isFile(variable)
    },
    isFileValueDataSource(variable) {
      return variableUtils.isFileValueDataSource(variable)
    },
    getFileVariableName(variable) {
      return variableUtils.getFileVariableName(variable)
    },
		downloadFile: function(variable) {
			if (this.isFileValueDataSource(variable)) {
				const filter = { variableName: variable.name, deserializeValues: true }
				HistoryService.fetchProcessInstanceVariablesHistory(variable.processInstanceId, filter).then(result => {
					if (result && result.length > 0) {
						const value = result[0].value
						const fileData = typeof value === 'string' ? JSON.parse(value) : value
						const blob = new Blob([Uint8Array.from(atob(fileData.data), c => c.codePointAt(0))], { type: fileData.contentType })
						this.$refs.importPopper.triggerDownload(blob, fileData.name)
					}
				})
			} else if (variable.type === 'Object') {
				const blob = new Blob([Uint8Array.from(atob(variable.value.data), c => c.codePointAt(0))], { type: variable.value.contentType })
				this.$refs.importPopper.triggerDownload(blob, this.getFileVariableName(variable))
			} else {
				const download = variable.isLive ?
					ProcessService.fetchVariableDataByExecutionId(variable.executionId, variable.name) :
					HistoryService.fetchHistoryVariableDataById(variable.id)
				download.then(data => {
					this.$refs.importPopper.triggerDownload(data, variable.valueInfo.filename)
				})
			}
		},
		uploadFile: function () {
			if (this.isFileValueDataSource(this.selectedVariable)) {
				const reader = new FileReader()
				reader.onload = event => {
					const fileData = {
						contentType: this.file.type,
						name: this.file.name,
						encoding: 'UTF-8',
						data: event.target.result.split(',')[1],
						objectTypeName: this.selectedVariable.valueInfo.objectTypeName
					}
					const valueInfo = {
						objectTypeName: this.selectedVariable.valueInfo.objectTypeName,
						serializationDataFormat: 'application/json'
					}
					const data = { fileObject: true, processDefinitionId: this.selectedVariable.processDefinitionId, modifications: {} }
					data.modifications[this.selectedVariable.name] = {
						value: JSON.stringify(fileData),
						valueInfo: valueInfo, type: this.selectedVariable.type
					}
					ProcessService.modifyVariableByExecutionId(this.selectedVariable.executionId, data).then(() => {
						this.selectedVariable.value = fileData
					})
				}
				reader.onerror = () => { }
				reader.readAsDataURL(this.file)
			} else {
				const formData = new FormData()
				formData.append('data', this.file)
				formData.append('valueType', 'File')
				const fileObj = { name: this.file.name, type: this.file.type }
				ProcessService.modifyVariableDataByExecutionId(this.selectedVariable.executionId, this.selectedVariable.name, formData)
					.then(() => {
						this.selectedVariable.valueInfo.filename = fileObj.name
						this.selectedVariable.valueInfo.mimeType = fileObj.type
						this.file = null
					})
			}
		},
		saveOrEditVariable: function (variable) {
			if (variable.modify) {
				const data = { modifications: {} }
				data.modifications[variable.name] = { value: variable.value }
				ProcessService.modifyVariableByExecutionId(variable.executionId, data).then(() => {
					variable.modify = false
				})
			} else variable.modify = true
		},
		changeFilter: function(queryObject) {
			this.filter = {
				...queryObject,
				deserializeValues: false
			}
			this.loadSelectedInstanceVariables()
		}
	}
}
