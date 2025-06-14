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
import appConfig from './appConfig.js'
import { axios } from './globals.js'

function filterToUrlParams(filters) {
  var filter = ''
  if (Array.isArray(filters)) {
    filters.forEach(item => {
      filter += "&" + item.key + "=" + item.value
    })
  }
  return filter
}

var TaskService = {
  findIdentityLinks: function(taskId) {
    return axios.get(appConfig.servicesBasePath + '/task/' + taskId + '/identity-links')
  },
  addIdentityLink: function(taskId, data) {
    return axios.post(appConfig.servicesBasePath + '/task/' + taskId + '/identity-links', data)
  },
  removeIdentityLink: function(taskId, data) {
    return axios.post(appConfig.servicesBasePath + '/task/' + taskId + '/identity-links/delete', data)
  },
  findTasks: function(filters) {
    return axios.get(appConfig.servicesBasePath + '/task', { params: { filter: filterToUrlParams(filters) } })
  },
  findTasksPost: function(filters) {
    return axios.post(appConfig.servicesBasePath + '/task', filters)
  },
  findTasksByProcessInstance: function(processInstanceId) {
    return axios.get(appConfig.servicesBasePath + "/task/by-process-instance/" + processInstanceId)
  },
  findTasksByProcessInstanceAsignee: function(processInstanceId, createdAfter) {
    const url = appConfig.servicesBasePath + "/task/by-process-instance-asignee"
    const params = {}
    if (processInstanceId) params.processInstanceId = processInstanceId
    if (createdAfter) params.createdAfter = createdAfter
    return axios.get(url, { params })
  },
  findTaskById: function(taskId) { return axios.get(appConfig.servicesBasePath + "/task/" + taskId) },
  findTasksByFilter: function(filterId, filters, pagination) {
    var queryPagination = '?firstResult=' + pagination.firstResult + '&maxResults=' + pagination.maxResults
    return axios.post(appConfig.servicesBasePath + "/task/by-filter/" + filterId + queryPagination, filters)
  },
  submit: function(taskId) { return axios.post(appConfig.servicesBasePath + "/task/submit/" + taskId) },
  formReference: function(taskId) { return axios.get(appConfig.servicesBasePath + "/task/" + taskId + "/form-reference") },
  setAssignee: function(taskId, userId) { return axios.post(appConfig.servicesBasePath + "/task/" + taskId + "/assignee/" + userId) },
  update: function(task) { return axios.put(appConfig.servicesBasePath + "/task/update", task) },
  fetchActivityVariables: function(activityInstanceId) {
    return axios.get(appConfig.servicesBasePath + "/task/" + activityInstanceId + "/variables")
  },
  findTasksCountByFilter: function(filterId, filters) { return axios.post(appConfig.servicesBasePath + "/task/by-filter/" + filterId + '/count', filters) },
  checkActiveTask: function(id, auth) {
    return axios.create().get(appConfig.servicesBasePath + '/task/' + id, { headers: { authorization: auth } })
  },
  downloadFile: function(processInstanceId, fileVariable) {
    return axios.get(appConfig.servicesBasePath + '/task/' + processInstanceId + '/variable/download/' + fileVariable, { responseType: 'blob' })
  },
  findHistoryTaksCount: function(filters) {
    return axios.post(appConfig.servicesBasePath + '/task-history/count', filters)
  },
  getTaskCountByCandidateGroup: function() {
    return axios.get(appConfig.servicesBasePath + '/task/report/candidate-group-count')
  }
}

var FilterService = {
  findFilters: function() { return axios.get(appConfig.servicesBasePath + "/filter") },
  createFilter: function(filter) { return axios.post(appConfig.servicesBasePath + "/filter", filter) },
  updateFilter: function(filter) { return axios.put(appConfig.servicesBasePath + "/filter", filter) },
  deleteFilter: function(filterId) { return axios.delete(appConfig.servicesBasePath + "/filter/" + filterId) }
}

var ProcessService = {
  findProcesses: function() { return axios.get(appConfig.servicesBasePath + "/process") },
  findProcessesWithInfo: function() { return axios.get(appConfig.servicesBasePath + "/process/extra-info") },
  findProcessesWithFilters: function(filters) { return axios.post(appConfig.servicesBasePath + "/process", filters) },
  findProcessByDefinitionKey: function(key, tenantId) {
    let url = `${appConfig.servicesBasePath}/process/${key}`
    if (tenantId) url += `?tenantId=${tenantId}`
    return axios.get(url)
  },
  findProcessVersionsByDefinitionKey: function(key, tenantId, lazyLoad = false) {
    let url = `${appConfig.servicesBasePath}/process/process-definition/versions/${key}?lazyLoad=${lazyLoad}`
    if (tenantId) url += `&tenantId=${tenantId}`
    return axios.get(url)
  },
  findProcessById: function(id, extraInfo = false) {
    return axios.get(appConfig.servicesBasePath + "/process/process-definition-id/" + id + '?extraInfo=' + extraInfo)
  },
  findProcessesInstances: function(key) {
    return axios.get(appConfig.servicesBasePath + "/process/instances/by-process-key/" + key)
  },
  findProcessInstance: function(processInstanceId) {
    return axios.get(appConfig.servicesBasePath + "/process/process-instance/" + processInstanceId)
  },
  findCurrentProcessesInstances: function(filter) {
    return axios.post(appConfig.servicesBasePath + "/process/instances", filter)
  },
  findActivityInstance: function(processInstanceId) {
    return axios.get(appConfig.servicesBasePath + "/process/activity/by-process-instance/" + processInstanceId)
  },
  findCalledProcessDefinitions: function(processDefinitionId) {
    return axios.get(appConfig.servicesBasePath + "/process/called-process-definitions/" + processDefinitionId)
  },
  fetchDiagram: function(processId) { return axios.get(appConfig.servicesBasePath + "/process/" + processId + "/diagram") },
  startProcess: function(key, tenantId, locale) {
    let url = `${appConfig.servicesBasePath}/process/${key}/start`
    if (tenantId)  url += `?tenantId=${tenantId}`
    return axios.post(url, {
      variables: {
        _locale: { value: locale, type: String }
        // initiator: { value: userId, type: String }
      }
    })
  },
  startForm: function(processDefinitionId) { return axios.get(appConfig.servicesBasePath + "/process/" + processDefinitionId + "/start-form") },
  suspendInstance: function(processInstanceId, suspend) {
    return axios.put(appConfig.servicesBasePath + "/process/instance/" + processInstanceId + "/suspend", null, { params: { suspend: suspend } })
  },
  suspendProcess: function(processId, suspend, includeProcessInstances, executionDate) {
    return axios.put(appConfig.servicesBasePath + "/process/" + processId + "/suspend", null, {
      params: {
        suspend: suspend,
        includeProcessInstances: includeProcessInstances,
        executionDate: executionDate
      }
    })
  },
  stopInstance: function(processInstanceId) {
    return axios.delete(appConfig.servicesBasePath + "/process/instance/" + processInstanceId + "/delete")
  },
  findDeployments: function() {
    return axios.get(appConfig.servicesBasePath + "/process/deployments")
  },
  findDeploymentResources: function(deploymentId) {
    return axios.get(appConfig.servicesBasePath + "/process/deployments/" + deploymentId + "/resources")
  },
  deleteDeployment: function(deploymentId, cascade) {
    return axios.delete(appConfig.servicesBasePath + "/process/deployments/" + deploymentId, { params: { cascade: cascade } })
  },
  fetchVariableDataByExecutionId: function(executionId, varName) {
    return axios.get(appConfig.servicesBasePath + "/process/execution/" + executionId + "/localVariables/" + varName + "/data", { responseType: "blob" })
  },
  findProcessStatistics: function(processId) {
    return axios.get(appConfig.servicesBasePath + "/process/process-definition/" + processId + "/statistics")
  },
  fetchProcessInstanceVariables: function(processInstanceId, deserialize) {
    return axios.get(appConfig.servicesBasePath + "/process/variable-instance/process-instance/" + processInstanceId + "/variables",
      { params: {
        deserialize: deserialize
      }
    })
  },
  modifyVariableByExecutionId: function(executionId, data) {
    return axios.post(appConfig.servicesBasePath + "/process/execution/" + executionId + "/localVariables", data)
  },
  modifyVariableDataByExecutionId: function(executionId, varName, data) {
    return axios.post(appConfig.servicesBasePath + "/process/execution/" + executionId + "/localVariables/" + varName + "/data", data)
  },
  fetchChatComments: function(processInstanceId, processDefinitionKey, deserialize) {
    return axios.get(appConfig.servicesBasePath + '/process/process-instance/' + processInstanceId + '/chat-comments',
    { params: {
      deserialize: deserialize,
      processDefinitionKey: processDefinitionKey
    } } )
  },
  fetchStatusDataset: function(processInstanceId, processDefinitionKey, deserialize) {
    return axios.get(appConfig.servicesBasePath + '/process/process-instance/' + processInstanceId + '/status-dataset', {
      params: {
        deserialize: deserialize,
        processDefinitionKey: processDefinitionKey
      }
    })
  },
  submitVariables: function(processInstanceId, variables) {
    return axios.post(appConfig.servicesBasePath + '/process/process-instance/' + processInstanceId + '/submit-variables', variables)
  },
  updateHistoryTimeToLive: function(id, data) {
    return axios.put(appConfig.servicesBasePath + "/process/" + id + "/history-time-to-live", data)
  },
  deleteProcessDefinition: function(id, cascade) {
    return axios.delete(appConfig.servicesBasePath + '/process/' + id + '/delete', {
      params: {
        cascade: cascade
      }
    })
  },
  putLocalExecutionVariable: function(executionId, varName, data) {
    return axios.put(appConfig.servicesBasePath + '/process/execution/' + executionId + '/localVariables/' + varName, data)
  },
  deleteVariableByExecutionId: function(executionId, varName) {
    return axios.delete(appConfig.servicesBasePath + "/process/execution/" + executionId + "/localVariables/" + varName)
  }
}

var AdminService = {
  findUsers: function(filter) {
    // id, firstName, firstNameLike, lastName, lastNameLike, email, emailLike, memberOfGroup, memberOfTenant, idIn, firstResult, maxResults
    return axios.get(appConfig.servicesBasePath + "/admin/user", {
      params: filter,
    })
  },
  createUser: function (user) { return axios.post(appConfig.servicesBasePath + "/admin/user/create", user) },
  updateUserProfile: function (userId, user) { return axios.put(appConfig.servicesBasePath + "/admin/user/" + userId + "/profile", user) },
  updateUserCredentials: function (userId, password, authenticatedUserPassword) {
    return axios.put(appConfig.servicesBasePath + "/admin/user/" + userId + "/credentials", {
        password: password,
        authenticatedUserPassword: authenticatedUserPassword
    })
  },
  deleteUser: function (userId) { return axios.delete(appConfig.servicesBasePath + "/admin/user/" + userId) },
  findGroups: function (filter) {
    // id, name, nameLike, type, member, memberOfTenant, firstResult, maxResults
    return axios.get(appConfig.servicesBasePath + "/admin/group", {
      params: filter
    })
  },
  createGroup: function (group) { return axios.post(appConfig.servicesBasePath + "/admin/group/create", group) },
  updateGroup: function (groupId, group) { return axios.put(appConfig.servicesBasePath + "/admin/group/" + groupId, group) },
  deleteGroup: function (groupId) { return axios.delete(appConfig.servicesBasePath + "/admin/group/" + groupId) },
  addMember: function (groupId, userId) { return axios.put(appConfig.servicesBasePath + "/admin/group/" + groupId + "/members/" + userId) },
  deleteMember: function (groupId, userId) { return axios.delete(appConfig.servicesBasePath + "/admin/group/" + groupId + "/members/" + userId) },
  findAuthorizations: function (filter) {
      // id, type, userIdIn, lastName, groupIdIn, resourceType, resourceId
    return axios.get(appConfig.servicesBasePath + "/admin/authorization", {
      params: filter
    })
  },
  createAuthorization: function (authorization) { return axios.post(appConfig.servicesBasePath + "/admin/authorization/create", authorization) },
  updateAuthorization: function (authorizationId, authorization)
    { return axios.put(appConfig.servicesBasePath + "/admin/authorization/" + authorizationId, authorization) },
  deleteAuthorization: function (authorizationId) { return axios.delete(appConfig.servicesBasePath + "/admin/authorization/" + authorizationId) }

}

var HistoryService = {
  findTasksByDefinitionKeyHistory: function(taskDefinitionKey, processInstanceId) {
    return axios.get(appConfig.servicesBasePath + "/task-history/by-process-key", {
      params: {
        taskDefinitionKey: taskDefinitionKey,
        processInstanceId: processInstanceId
      }
    })
  },
  findTasksByProcessInstanceHistory: function(processInstanceId) {
    return axios.get(appConfig.servicesBasePath + "/task-history/by-process-instance/" + processInstanceId)
  },
  fetchActivityVariablesHistory: function(activityInstanceId) {
    return axios.get(appConfig.servicesBasePath + "/task-history/" + activityInstanceId + "/variables")
  },
  findProcessesInstancesHistory: function(filters, firstResult, maxResults) {
  return axios.post(appConfig.servicesBasePath + "/process-history/instance", {
	params: {
		filters: filters,
		firstResult: firstResult,
		maxResults: maxResults
	}
  })
  },
  
  findProcessesInstancesHistoryById: function(id, activityId, firstResult, maxResults, text, active) {
    return axios.get(appConfig.servicesBasePath + "/process-history/instance/by-process-id/" + id, {
      params: {
        activityId: activityId,
        active: active,
        firstResult: firstResult,
        maxResults: maxResults,
        text: text
      }
    })
  },
  findActivitiesInstancesHistory: function(processInstanceId) {
    return axios.get(appConfig.servicesBasePath + "/process-history/activity/by-process-instance/" + processInstanceId)
  },
  findActivitiesProcessDefinitionHistory: function(processDefinitionId) {
    return axios.get(appConfig.servicesBasePath + "/process-history/activity/by-process-definition/" + processDefinitionId)
  },
  findActivitiesInstancesHistoryWithFilter(filter){
	return axios.get(appConfig.servicesBasePath + "/process-history/activity", 	{ params: filter })
  },
  fetchProcessInstanceVariablesHistory: function(processInstanceId, deserialize) {
    return axios.get(appConfig.servicesBasePath + "/process-history/instance/by-process-instance/" + processInstanceId + "/variables",
      { params: {
        deserialize: deserialize
      }
    })
  },
  fetchHistoryVariableDataById: function(id) {
    return axios.get(appConfig.servicesBasePath + "/process-history/variable/" + id + "/data", { responseType: "blob" })
  },
  findProcessInstance: function(id) {
    return axios.get(appConfig.servicesBasePath + "/process-history/instance/" + id)
  },
  findTasksByTaskIdHistory: function(taskId) {
    return axios.get(appConfig.servicesBasePath + "/task-history/by-task-id/" + taskId)
  },
  deleteProcessInstanceFromHistory: function(id) {
    return axios.delete(appConfig.servicesBasePath + "/process-history/instance/" + id)
  },
  deleteVariableHistoryInstance: function(id) {
    return axios.delete(appConfig.servicesBasePath + "/process-history/instance/" + id + "/variables")
  }
}

var JobDefinitionService = {
  findJobDefinitions: function(params) {
    return axios.post(appConfig.servicesBasePath + "/job-definition", params)
  },
  suspendJobDefinition: function(jobDefinitionId, params) {
    return axios.put(appConfig.servicesBasePath + "/job-definition/" + jobDefinitionId + "/suspend", params)
  },
  overrideJobDefinitionPriority: function(jobDefinitionId, params) {
    return axios.put(appConfig.servicesBasePath + "/job-definition/" + jobDefinitionId + "/job-priority", params)
  },
  findJobDefinition: function(id) {
    return axios.get(appConfig.servicesBasePath + "/job-definition/" + id)
  },
  retryJobDefinitionById: function(id, params) {
    return axios.put(appConfig.servicesBasePath + "/job-definition/" + id + '/retries', params)
  }
}

var IncidentService = {
  fetchIncidentStacktraceByJobId: function(id) {
    return axios.get(appConfig.servicesBasePath + "/incident/" + id + "/stacktrace")
  },
  retryJobById: function(id, params) {
    return axios.put(appConfig.servicesBasePath + "/incident/job/" + id + "/retries", params)
  },
  findIncidents: function(processDefinitionId) {
    return axios.get(appConfig.servicesBasePath + "/incident?processDefinitionId=" + processDefinitionId)
  },
  setIncidentAnnotation: function(id, params) {
    return axios.put(appConfig.servicesBasePath + "/incident/" + id + "/annotation", params)
  }
}

var AuthService = {
  fetchAuths: function() {return axios.get(appConfig.servicesBasePath + "/auth/authorizations") },
  passwordRecover: function(data) { return axios.post(appConfig.servicesBasePath + "/auth/password-recover", data) },
  passwordRecoverCheck: function(recoverToken) { return axios.get(appConfig.servicesBasePath + "/auth/password-recover-check",
    { headers: { authorization: recoverToken } }
  ) },
  passwordRecoverUpdatePassword: function(userId, password, authenticatedUserPassword, recoverToken) {
    return axios.put(appConfig.servicesBasePath + "/auth/password-recovery-update-password/" + userId,
    { password: password, authenticatedUserPassword: authenticatedUserPassword },
    { headers: { authorization: recoverToken } }
  ) },
  login: function(params, remember) {
    return axios.create().post(appConfig.servicesBasePath + '/auth/login', params).then(function(user) {
      axios.defaults.headers.common.authorization = user.data.authToken
      ;(remember ? localStorage : sessionStorage).setItem('token', user.data.authToken)
      return user.data
    })
  }
}

var InfoService = {
  getProperties: function() {
    return axios.get(appConfig.servicesBasePath + '/info/properties')
  },
  getVersion: function() {
    return axios.get(appConfig.servicesBasePath + '/info')
  }
}

var FormsService = {
  submitVariables: function(task, formResult, close) {
    return axios.post(appConfig.servicesBasePath + '/task/' + task.id + '/submit-variables', formResult, {
      params: {
        name: task.name,
        processInstanceId: task.processInstanceId,
        processDefinitionId: task.processDefinitionId,
        assignee: task.assignee,
        close: close
      }
    })
  },
  submitStartFormVariables: function(processDefinitionId, formResult, locale) {
    formResult.push({ name: '_locale', type: 'String', value: locale })
    return axios.post(appConfig.servicesBasePath + '/process/' + processDefinitionId + '/submit-startform-variables', formResult)
  },
  downloadFiles: function(processInstanceId, documentsList) {
    return axios.post(appConfig.servicesBasePath + '/task/' + processInstanceId + '/download', documentsList, { responseType: 'blob' } )
  },
  downloadFile: function(processInstanceId, fileVariable) {
    return axios.get(appConfig.servicesBasePath + '/task/' + processInstanceId + '/variable/download/' + fileVariable, { responseType: 'blob' })
  },
  fetchVariable: function(taskId, variableName, deserialize) {
    return axios.get(appConfig.servicesBasePath + '/task/' + taskId + '/variable/' + variableName, { params: { deserialize: deserialize } })
  },
  sendMessage: function(data) {
    return axios.post(appConfig.servicesBasePath + '/process/message', data)
  },
  fetchVariables: function(taskId, deserialize) {
    return axios.post(appConfig.servicesBasePath + '/task/' + taskId, null, { params: { deserialize: deserialize } } )
  },
  deleteVariable: function(taskId, variableName) {
    return axios.delete(appConfig.servicesBasePath + '/task/' + taskId + '/variable/' + variableName)
  },
  handleBpmnError: function(taskId, data) {
    return axios.post(appConfig.servicesBasePath + '/task/' + taskId + '/bpmnError', data)
  }
}

var TemplateService = {
  getTemplate: function(element, taskId, locale, token) {
	return axios.get(appConfig.servicesBasePath + '/template/' + element + '/' + taskId + '?locale=' + locale, {
	    headers: {
        Authorization: `${token}`
	    }
	  })
  },
  getStartFormTemplate: function(element, processDefinitionId, locale, token) {
    return axios.get(appConfig.servicesBasePath + '/template/' + element + '/key/' + processDefinitionId + '?locale=' + locale, {
	    headers: {
        Authorization: `${token}`
	    }
	  })
  }
}

var DecisionService = {
  getDecisionList: function(params) {
    return axios.get(appConfig.servicesBasePath + "/decision", { params })
  },
  getDecisionVersionsByKey: function(key, lazyLoad = false) {
    return axios.get(appConfig.servicesBasePath + "/decision/key/" + key + "/versions" + '?lazyLoad=' + lazyLoad)
  },
  getDecisionDefinitionById: function(id, extraInfo = false) {
    return axios.get(appConfig.servicesBasePath + "/decision/id/" + id + '?extraInfo=' + extraInfo)
  },
  getDecisionByKey: function(key) {
    return axios.get(appConfig.servicesBasePath + "/decision/key/" + key)
  },
  getDecisionByKeyAndTenant: function (key, tenant) {
    return axios.get(appConfig.servicesBasePath + "/decision/key/" + key + "/tenant/" + tenant)
  },
  getDiagramByKey: function (key) {
    return axios.get(appConfig.servicesBasePath + "/decision/key/" + key + "/diagram")
  },
  getDiagramById: function (id) {
    return axios.get(appConfig.servicesBasePath + "/decision/id/" + id + "/diagram")
  },
  getDiagramByKeyAndTenant: function (key, tenant) {
    return axios.get(appConfig.servicesBasePath + "/decision/key/" + key + "/tenant/" + tenant + "/diagram")
  },
  getXmlByKey: function (key) {
    return axios.get(appConfig.servicesBasePath + "/decision/key/" + key + "/xml")
  },
  getXmlByKeyAndTenant: function (key, tenant) {
    return axios.get(appConfig.servicesBasePath + "/decision/key/" + key + "/tenant/" + tenant + "/xml")
  },
  getXmlById: function (id) {
    return axios.get(appConfig.servicesBasePath + "/decision/id/" + id + "/xml")
  },
  evaluateByKey: function (key, data) {
    return axios.post(appConfig.servicesBasePath + "/decision/key/" + key + "/evaluate", data)
  },
  evaluateByKeyAndTenant: function (key, tenant, data) {
    return axios.post(appConfig.servicesBasePath + "/decision/key/" + key + "/tenant/" + tenant + "/evaluate", data)
  },
  evaluateById: function (id, data) {
    return axios.post(appConfig.servicesBasePath + "/decision/id/" + id + "/evaluate", data)
  },
  updateHistoryTTLByKey: function (key, data) {
    return axios.put(appConfig.servicesBasePath + "/decision/key/" + key + "/history-ttl", data)
  },
  updateHistoryTTLByKeyAndTenant: function (key, tenant, data) {
    return axios.put(appConfig.servicesBasePath + "/decision/key/" + key + "/tenant/" + tenant + "/history-ttl", data)
  },
  updateHistoryTTLById: function (id, data) {
    return axios.put(appConfig.servicesBasePath + "/decision/id/" + id + "/history-ttl", data)
  },
  getHistoricDecisionInstances: function (params) {
    return axios.get(appConfig.servicesBasePath + "/decision/history/instances", { params })
  },
  getHistoricDecisionInstanceCount: function (params) {
    return axios.get(appConfig.servicesBasePath + "/decision/history/instances/count", { params })
  },
  getHistoricDecisionInstanceById: function (id, params) {
    return axios.get(appConfig.servicesBasePath + "/decision/history/instances/" + id, { params })
  },
  deleteHistoricDecisionInstances: function (payload) {
    return axios.post(appConfig.servicesBasePath + "/decision/history/instances/delete", payload)
  },
  setHistoricDecisionInstanceRemovalTime: function (payload) {
    return axios.post(appConfig.servicesBasePath + "/decision/history/instances/set-removal-time", payload)
  }
}

var JobService = {
  getJobs: function(params) {
    return axios.post(appConfig.servicesBasePath + '/job', params)
  },
  setSuspended: function(id, data) {
    return axios.put(appConfig.servicesBasePath + '/job/' + id + '/suspended', data)
  },
  deleteJob: function(id) {
    return axios.delete(appConfig.servicesBasePath + '/job/' + id)
  },
  getHistoryJobLog: function(params) {
    return axios.get(appConfig.servicesBasePath + '/job/history/job-log', { params })
  },
  getHistoryJobLogStacktrace: function(id) {
    return axios.get(appConfig.servicesBasePath + '/job/history/job-log/' + id + '/stacktrace')
  }
}

var BatchService = {
  getBatches: function(params) {
    return axios.get(appConfig.servicesBasePath + '/batch', { params })
  },
  getBatchStatistics: function(params) {
    return axios.get(appConfig.servicesBasePath + '/batch/statistics', { params })
  },
  setBatchSuspensionState: function(id, params) {
    return axios.put(appConfig.servicesBasePath + '/batch/' + id + '/suspended', params)
  },
  deleteBatch: function(id, params) {
    return axios.delete(appConfig.servicesBasePath + '/batch/' + id, params)
  },
  getHistoricBatches: function(params) {
    return axios.get(appConfig.servicesBasePath + '/history/batch', { params })
  },
  getHistoricBatchCount(params) {
    return axios.get('/history/batch/count', { params })
  },
  getHistoricBatchById: function(id) {
    return axios.get(appConfig.servicesBasePath + '/history/batch/' + id)
  },
  deleteHistoricBatch: function(id) {
    return axios.delete(appConfig.servicesBasePath + '/history/batch/' + id)
  },
  setRemovalTime(payload) {
    return axios.post('/history/batch/set-removal-time', payload)
  },
  getCleanableBatchReport(params) {
    return axios.get('/history/batch/cleanable-batch-report', { params })
  },
  getCleanableBatchReportCount() {
    return axios.get('/history/batch/cleanable-batch-report/count')
  }
}

var AnalyticsService = {
  async getAnalytics() {
    return axios.get(appConfig.servicesBasePath + '/analytics')
  }
}

var SystemService = {
  getTelemetryData() {
    return axios.get(appConfig.servicesBasePath + '/system/telemetry/data')
  },
  getMetricsData(params) {
    return axios.get(appConfig.servicesBasePath + '/system/metrics/data', { params })
  }
}

var TenantService = {  
  getTenants(params) {
    return axios.get(appConfig.servicesBasePath + '/tenant', { params })
  },
  getTenantById: function (tenantId) {
    return axios.get(appConfig.servicesBasePath + '/tenant/' + tenantId)
  },
  createTenant: function (tenant) {
    return axios.post(appConfig.servicesBasePath + '/tenant', tenant)
  },
  updateTenant: function (tenant) {
    return axios.put(appConfig.servicesBasePath + '/tenant/' + tenant.id, tenant)
  },
  deleteTenant: function (tenantId) {
    return axios.delete(appConfig.servicesBasePath + '/tenant/' + tenantId)
  },
  removeUserFromTenant(tenantId, userId) {
    return axios.delete(`${appConfig.servicesBasePath}/tenant/${tenantId}/users/${userId}`)
  },
  addUserToTenant(tenantId, userId) {
    return axios.post(`${appConfig.servicesBasePath}/tenant/${tenantId}/users/${userId}`)
  },
  removeGroupFromTenant(tenantId, groupId) {
    return axios.delete(`${appConfig.servicesBasePath}/tenant/${tenantId}/groups/${groupId}`)
  },
  addGroupToTenant(tenantId, groupId) {
    return axios.post(`${appConfig.servicesBasePath}/tenant/${tenantId}/groups/${groupId}`)
  }
}

export { TaskService, FilterService, ProcessService, AdminService, JobService, JobDefinitionService, SystemService,
  HistoryService, IncidentService, AuthService, InfoService, FormsService, TemplateService, DecisionService, 
  AnalyticsService, BatchService, TenantService }
