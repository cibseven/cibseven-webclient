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
			variables: [],
			file: null,
			selectedVariable: null
		}
	},
	watch: {
		'selectedInstance.id': {
			immediate: true,
			handler: function () {
				this.variables = []
				this.filteredVariables = []
				this.file = null
				this.selectedVariable = null
				this.loadSelectedInstanceVariables()
			}
		}
	},
	computed: {
		activityInstancesGrouped: function () {
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
	methods: {
		loadSelectedInstanceVariables: function () {
			if (this.selectedInstance) {
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
			const variablesToSerialize = []
			let variables = await serviceMap[service][method](this.selectedInstance.id, false)
			variables.forEach(variable => {
				try {
					variable.value = variable.type === 'Object' ? JSON.parse(variable.value) : variable.value
				} catch {
					variablesToSerialize.push(variable.id)
				}
				variable.modify = false
			})
			if (variablesToSerialize.length > 0) {
				const dVariables = await serviceMap[service][method](this.selectedInstance.id, true)
				dVariables.forEach(dVariable => {
					const variableToSerialize = variables.find(variable => variable.id === dVariable.id)
					if (variableToSerialize) {
						variableToSerialize.value = dVariable.value
					}
				})
			}
			variables.forEach(v => {
				v.scope = this.activityInstancesGrouped[v.activityInstanceId]
			})
			variables.sort((a, b) => a.name.localeCompare(b.name))
			this.variables = variables
			this.filteredVariables = [...variables]
			this.loading = false
		},
		downloadFile: function(variable) {
			if (variable.type === 'Object') {
				if (variable.value.objectTypeName.includes('FileValueDataFlowSource')) {
					TaskService.downloadFile(variable.processInstanceId, variable.name).then(data => {
						this.$refs.importPopper.triggerDownload(data, variable.value.name)
					})
				} else {
					var blob = new Blob([Uint8Array.from(atob(variable.value.data), c => c.charCodeAt(0))], { type: variable.value.contentType })
					this.$refs.importPopper.triggerDownload(blob, variable.value.name)
				}
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
				formData.append('file', this.file)
				var fileObj = { name: this.file.name, type: this.file.type }
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
		}
	}
}
