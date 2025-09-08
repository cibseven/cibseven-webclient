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
import { HistoryService, ProcessService, TaskService } from '@/services.js'

const serviceMap = {
	ProcessService: ProcessService,
	HistoryService: HistoryService
}

export default {
	props: { process: Object, selectedInstance: Object, activityInstance: Object, activityInstanceHistory: Array },
	data: function () {
		return {
			loading: true,
			filter: {
				deserializeValues: false,
			},
			variables: [],
			file: null,
			selectedVariable: null
		}
	},
	watch: {
		'selectedInstance.id': {
			immediate: true,
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
		activityInstancesGrouped: 'loadSelectedInstanceVariables'
	},
	computed: {
		activityInstancesGrouped: function () {
			if (this.activityInstanceHistory) {
				var res = []
				if (this.activityInstance) {
					res[this.activityInstance.id] = this.activityInstance.name
					this.activityInstance.childActivityInstances.forEach(ai => {
						res[ai.id] = ai.name
					})
				} else {
					res[this.selectedInstance.id] = this.selectedInstance.processDefinitionName
					this.activityInstanceHistory.forEach(ai => {
						res[ai.id] = ai.activityName
					})
				}
				return res
			}
		},
		restFilter: function () {
			let result = {
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
		loadSelectedInstanceVariables: function () {
			if (this.selectedInstance && this.activityInstancesGrouped) {
				if (this.selectedInstance.state === 'ACTIVE') {
					this.fetchInstanceVariables('ProcessService', 'fetchProcessInstanceVariables')
				} else {
					if (this.$root.config.camundaHistoryLevel === 'full') {
						this.fetchInstanceVariables('HistoryService', 'fetchProcessInstanceVariablesHistory')
					}
				}
			}
		},
		fetchInstanceVariables: async function (service, method) {
			this.loading = true
			let variables = await serviceMap[service][method](this.selectedInstance.id, this.restFilter)
			variables.forEach(v => {
				v.scope = this.activityInstancesGrouped[v.activityInstanceId]
			})
			variables.sort((a, b) => a.name.localeCompare(b.name))
			this.variables = variables
			this.filteredVariables = [...variables]
			this.loading = false
		},
		isFileValueDataSource: function(item) {
      if (item.type === 'Object') {
        const objectTypeName =
          (item.value && item.value.objectTypeName) ||
          (item.valueInfo && item.valueInfo.objectTypeName)
        if (objectTypeName && this.fileObjects.includes(objectTypeName)) return true
      }
      return false
    },
		getFileVariableName: function(item) {
			if (item.value && typeof item.value === 'object' && item.value.name) {
				return item.value.name
			}
			if (item.value && typeof item.value === 'string') {
				try {
					const parsed = JSON.parse(item.value)
					if (parsed && parsed.name) return parsed.name
				} catch { return '' }
			}
			return ''
		},
		downloadFile: function(variable) {
			if (this.isFileValueDataSource(variable)) {
				TaskService.downloadFile(variable.processInstanceId, variable.name).then(data => {
					this.$refs.importPopper.triggerDownload(data, this.getFileVariableName(variable))
				})
			} else if (variable.type === 'Object') {
				var blob = new Blob([Uint8Array.from(atob(variable.value.data), c => c.charCodeAt(0))], { type: variable.value.contentType })
				this.$refs.importPopper.triggerDownload(blob, this.getFileVariableName(variable))
			} else {
				var download = this.selectedInstance.state === 'ACTIVE' ?
					ProcessService.fetchVariableDataByExecutionId(variable.executionId, variable.name) :
					HistoryService.fetchHistoryVariableDataById(variable.id)
				download.then(data => {
					this.$refs.importPopper.triggerDownload(data, variable.valueInfo.filename)
				})
			}
		},
		uploadFile: function () {
			if (this.isFileValueDataSource(this.selectedVariable)) {
				var reader = new FileReader()
				reader.onload = event => {
					var fileData = {
						contentType: this.file.type,
						name: this.file.name,
						encoding: 'UTF-8',
						data: event.target.result.split(',')[1],
						objectTypeName: this.selectedVariable.valueInfo.objectTypeName
					}
					var valueInfo = {
						objectTypeName: this.selectedVariable.valueInfo.objectTypeName,
						serializationDataFormat: 'application/json'
					}
					var data = { fileObject: true, processDefinitionId: this.selectedVariable.processDefinitionId, modifications: {} }
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
				var formData = new FormData()
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
				var data = { modifications: {} }
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
