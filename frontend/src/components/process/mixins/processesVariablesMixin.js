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
        deserializeValue: false,
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
          deserializeValue: false,
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
        deserializeValue: false,
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
			try {
				this.loading = true
				const variablesToSerialize = []
				let variables = await serviceMap[service][method](this.selectedInstance.id, this.restFilter)
				variables.forEach(variable => {
					try {
						variable.value = variable.type === 'Object' ? JSON.parse(variable.value) : variable.value
					} catch {
						variablesToSerialize.push(variable.id)
					}
					variable.modify = false
				})
				if (variablesToSerialize.length > 0) {
					try {
						const dVariables = await serviceMap[service][method](this.selectedInstance.id, this.restFilter)
						dVariables.forEach(dVariable => {
							const variableToSerialize = variables.find(variable => variable.id === dVariable.id)
							if (variableToSerialize) {
								variableToSerialize.value = dVariable.value
							}
						})
					} catch (error) {
						// Handle variable deserialization errors gracefully
						// Continue with partial data rather than failing completely
						console.error('Failed to deserialize some variables:', error)
					}
				}
				variables.forEach(v => {
					v.scope = this.activityInstancesGrouped[v.activityInstanceId]
				})
				variables.sort((a, b) => a.name.localeCompare(b.name))
				this.variables = variables
				this.filteredVariables = [...variables]
			} catch (error) {
				// Handle service call failures with user feedback
				// Reset to empty state on error to prevent stale data display
				console.error('Failed to load instance variables:', error)
				if (this.$refs && this.$refs.error) {
					this.$refs.error.show()
				}
				this.variables = []
				this.filteredVariables = []
			} finally {
				this.loading = false
			}
		},
		downloadFile: function(variable) {
			try {
				if (variable.type === 'Object') {
					if (variable.value.objectTypeName.includes('FileValueDataFlowSource')) {
						TaskService.downloadFile(variable.processInstanceId, variable.name)
							.then(data => {
								this.$refs.importPopper.triggerDownload(data, variable.value.name)
							})
							.catch(error => {
								// Handle download failure with user feedback
								if (this.$refs && this.$refs.error) {
									this.$refs.error.show()
								}
								console.error('Failed to download file from service:', error)
							})
					} else {
						try {
							var blob = new Blob([Uint8Array.from(atob(variable.value.data), c => c.charCodeAt(0))], { type: variable.value.contentType })
							this.$refs.importPopper.triggerDownload(blob, variable.value.name)
						} catch (error) {
							// Handle blob creation errors (e.g., invalid base64 data)
							if (this.$refs && this.$refs.error) {
								this.$refs.error.show()
							}
							console.error('Failed to create downloadable blob from variable data:', error)
						}
					}
				} else {
					var download = this.selectedInstance.state === 'ACTIVE' ?
						ProcessService.fetchVariableDataByExecutionId(variable.executionId, variable.name) :
						HistoryService.fetchHistoryVariableDataById(variable.id)
					download
						.then(data => {
							this.$refs.importPopper.triggerDownload(data, variable.valueInfo.filename)
						})
						.catch(error => {
							// Handle download service errors with meaningful feedback
							if (this.$refs && this.$refs.error) {
								this.$refs.error.show()
							}
							console.error('Failed to download variable data:', error)
						})
				}
			} catch (error) {
				// Catch any unexpected errors in download operation
				if (this.$refs && this.$refs.error) {
					this.$refs.error.show()
				}
				console.error('Unexpected error during file download:', error)
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
					ProcessService.modifyVariableByExecutionId(this.selectedVariable.executionId, data)
						.then(() => {
							this.selectedVariable.value = fileData
							this.file = null
						})
						.catch(error => {
							// Notify user of upload failure with meaningful message
							// This prevents silent failures that could confuse users
							if (this.$refs && this.$refs.error) {
								this.$refs.error.show()
							}
							console.error('Failed to upload file:', error)
						})
				}
				// Handle file reading errors with user feedback
				// Previously this was an empty handler, causing silent failures
				reader.onerror = () => {
					if (this.$refs && this.$refs.error) {
						this.$refs.error.show()
					}
					console.error('Failed to read file:', this.file.name)
				}
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
					.catch(error => {
						// Provide meaningful error feedback for file upload failure
						// This ensures users are aware when file operations fail
						if (this.$refs && this.$refs.error) {
							this.$refs.error.show()
						}
						console.error('Failed to upload file data:', error)
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
        deserializeValue: false
      }
      this.loadSelectedInstanceVariables()
    }
	}
}
