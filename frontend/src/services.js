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

let servicesBasePath = ''

function getServicesBasePath() {
  return servicesBasePath
}

function setServicesBasePath(basePath) {
  servicesBasePath = basePath
}

var TaskService = {
  findIdentityLinks: function(taskId) {
    return axios.get(getServicesBasePath() + '/task/' + taskId + '/identity-links')
  },
  addIdentityLink: function(taskId, data) {
    return axios.post(getServicesBasePath() + '/task/' + taskId + '/identity-links', data)
  },
  removeIdentityLink: function(taskId, data) {
    return axios.post(getServicesBasePath() + '/task/' + taskId + '/identity-links/delete', data)
  },
  findTasks: function(filters) {
    return axios.get(getServicesBasePath() + '/task', { params: { filter: filterToUrlParams(filters) } })
  },
  findTasksPost: function(filters) {
    return axios.post(getServicesBasePath() + '/task', filters)
  },
  findTasksByProcessInstance: function(processInstanceId) {
    return axios.get(getServicesBasePath() + "/task/by-process-instance/" + processInstanceId)
  },
  findTasksByProcessInstanceAsignee: function(processInstanceId, createdAfter) {
    const url = getServicesBasePath() + "/task/by-process-instance-asignee"
    const params = {}
    if (processInstanceId) params.processInstanceId = processInstanceId
    if (createdAfter) params.createdAfter = createdAfter
    return axios.get(url, { params })
  },
  findTaskById: function(taskId) { return axios.get(getServicesBasePath() + "/task/" + taskId) },
  findTasksByFilter: function(filterId, filters, pagination) {
    var queryPagination = '?firstResult=' + pagination.firstResult + '&maxResults=' + pagination.maxResults
    return axios.post(getServicesBasePath() + "/task/by-filter/" + filterId + queryPagination, filters)
  },
  submit: function(taskId) { return axios.post(getServicesBasePath() + "/task/submit/" + taskId) },
  formReference: function(taskId) { return axios.get(getServicesBasePath() + "/task/" + taskId + "/form-reference") },
  setAssignee: function(taskId, userId) { return axios.post(getServicesBasePath() + "/task/" + taskId + "/assignee/" + userId) },
  update: function(task) { return axios.put(getServicesBasePath() + "/task/update", task) },
  fetchActivityVariables: function(activityInstanceId) {
	return axios.get(getServicesBasePath() + "/task/" + activityInstanceId + "/variables")
  },
  findTasksCountByFilter: function(filterId, filters) { return axios.post(getServicesBasePath() + "/task/by-filter/" + filterId + '/count', filters) },
  checkActiveTask: function(id, auth) {
    return axios.create().get(getServicesBasePath() + '/task/' + id, { headers: { authorization: auth } })
  },
  downloadFile: function(processInstanceId, fileVariable) {
    return axios.get(getServicesBasePath() + '/task/' + processInstanceId + '/variable/download/' + fileVariable, { responseType: 'blob' })
  },
  findHistoryTaksCount: function(filters) {
    return axios.post(getServicesBasePath() + '/task-history/count', filters)
  },
  getTaskCountByCandidateGroup: function() {
    return axios.get(getServicesBasePath() + '/task/report/candidate-group-count')
  }
}

var FilterService = {
  findFilters: function() { return axios.get(getServicesBasePath() + "/filter") },
  createFilter: function(filter) { return axios.post(getServicesBasePath() + "/filter", filter) },
  updateFilter: function(filter) { return axios.put(getServicesBasePath() + "/filter", filter) },
  deleteFilter: function(filterId) { return axios.delete(getServicesBasePath() + "/filter/" + filterId) }
}

var ProcessService = {
  findProcesses: function() { return axios.get(getServicesBasePath() + "/process") },
  findProcessesWithInfo: function() { return axios.get(getServicesBasePath() + "/process/extra-info") },
  findProcessesWithFilters: function(filters) { return axios.post(getServicesBasePath() + "/process", filters) },
  findProcessByDefinitionKey: function(key, tenantId) {
    let url = `${getServicesBasePath()}/process/${key}`
    if (tenantId) url += `?tenantId=${tenantId}`
    return axios.get(url)
  },
  findProcessVersionsByDefinitionKey: function(key, tenantId, lazyLoad = false) {
    let url = `${getServicesBasePath()}/process/process-definition/versions/${key}?lazyLoad=${lazyLoad}`
    if (tenantId) url += `&tenantId=${tenantId}`
    return axios.get(url)
  },
  findProcessById: function(id, extraInfo = false) {
	return axios.get(getServicesBasePath() + "/process/process-definition-id/" + id + '?extraInfo=' + extraInfo)
  },
  findProcessesInstances: function(key) {
	  return axios.get(getServicesBasePath() + "/process/instances/by-process-key/" + key)
  },
  findProcessInstance: function(processInstanceId) {
	  return axios.get(getServicesBasePath() + "/process/process-instance/" + processInstanceId)
  },
  findCurrentProcessesInstances: function(filter) {
    return axios.post(getServicesBasePath() + "/process/instances", filter)
  },
  findCurrentProcessesInstancesBySuperIds: function(superProcessInstanceIds) {
    return axios.post(getServicesBasePath() + "/process/instances/by-super-ids", superProcessInstanceIds)
  },
  findActivityInstance: function(processInstanceId) {
    return axios.get(getServicesBasePath() + "/process/activity/by-process-instance/" + processInstanceId)
  },
  findCalledProcessDefinitions: function(processDefinitionId) {
	  return axios.get(getServicesBasePath() + "/process/called-process-definitions/" + processDefinitionId)
  },
  fetchDiagram: function(processId) { return axios.get(getServicesBasePath() + "/process/" + processId + "/diagram") },
  startProcess: function(key, tenantId, locale) {
    let url = `${getServicesBasePath()}/process/${key}/start`
    if (tenantId)  url += `?tenantId=${tenantId}`
    return axios.post(url, {
      variables: {
        _locale: { value: locale, type: String }
        // initiator: { value: userId, type: String }
      }
    })
  },
  startForm: function(processDefinitionId) { return axios.get(getServicesBasePath() + "/process/" + processDefinitionId + "/start-form") },
  suspendInstance: function(processInstanceId, suspend) {
    return axios.put(getServicesBasePath() + "/process/instance/" + processInstanceId + "/suspend", null, { params: { suspend: suspend } })
  },
  suspendProcess: function(processId, suspend, includeProcessInstances, executionDate) {
    return axios.put(getServicesBasePath() + "/process/" + processId + "/suspend", null, {
      params: {
        suspend: suspend,
        includeProcessInstances: includeProcessInstances,
        executionDate: executionDate
      }
    })
  },
  stopInstance: function(processInstanceId) {
    return axios.delete(getServicesBasePath() + "/process/instance/" + processInstanceId + "/delete")
  },
  findDeployments: function() {
    return axios.get(getServicesBasePath() + "/process/deployments")
  },
  findDeploymentResources: function(deploymentId) {
    return axios.get(getServicesBasePath() + "/process/deployments/" + deploymentId + "/resources")
  },
  deleteDeployment: function(deploymentId, cascade) {
    return axios.delete(getServicesBasePath() + "/process/deployments/" + deploymentId, { params: { cascade: cascade } })
  },
  fetchVariableDataByExecutionId: function(executionId, varName) {
    return axios.get(getServicesBasePath() + "/process/execution/" + executionId + "/localVariables/" + varName + "/data", { responseType: "blob" })
  },
  findProcessStatistics: function(processId) {
    return axios.get(getServicesBasePath() + "/process/process-definition/" + processId + "/statistics")
  },
  fetchProcessInstanceVariables: function(processInstanceId, deserialize) {
    return axios.get(getServicesBasePath() + "/process/variable-instance/process-instance/" + processInstanceId + "/variables",
      { params: {
        deserialize: deserialize
      }
    })
  },
  modifyVariableByExecutionId: function(executionId, data) {
    return axios.post(getServicesBasePath() + "/process/execution/" + executionId + "/localVariables", data)
  },
  modifyVariableDataByExecutionId: function(executionId, varName, data) {
    return axios.post(getServicesBasePath() + "/process/execution/" + executionId + "/localVariables/" + varName + "/data", data)
  },
  fetchChatComments: function(processInstanceId, processDefinitionKey, deserialize) {
    return axios.get(getServicesBasePath() + '/process/process-instance/' + processInstanceId + '/chat-comments',
      { params: {
        deserialize: deserialize,
        processDefinitionKey: processDefinitionKey
      }
    })
  },
  fetchStatusDataset: function(processInstanceId, processDefinitionKey, deserialize) {
    return axios.get(getServicesBasePath() + '/process/process-instance/' + processInstanceId + '/status-dataset', {
      params: {
        deserialize: deserialize,
        processDefinitionKey: processDefinitionKey
      }
    })
  },
  submitVariables: function(processInstanceId, variables) {
    return axios.post(getServicesBasePath() + '/process/process-instance/' + processInstanceId + '/submit-variables', variables)
  },
  updateHistoryTimeToLive: function(id, data) {
    return axios.put(getServicesBasePath() + "/process/" + id + "/history-time-to-live", data)
  },
  deleteProcessDefinition: function(id, cascade) {
    return axios.delete(getServicesBasePath() + '/process/' + id + '/delete', {
      params: {
        cascade: cascade
      }
    })
  },
  putLocalExecutionVariable: function(executionId, varName, data) {
    return axios.put(getServicesBasePath() + '/process/execution/' + executionId + '/localVariables/' + varName, data)
  },
  deleteVariableByExecutionId: function(executionId, varName) {
    return axios.delete(getServicesBasePath() + "/process/execution/" + executionId + "/localVariables/" + varName)
  }
}

var VariableInstanceService = {
  getVariableInstance: function(id, deserializeValue) {
    let url = `${getServicesBasePath()}/variable-instance/${id}`
    if (deserializeValue !== null && deserializeValue !== undefined) {
      url += `?deserializeValue=${deserializeValue}`
    }
    return axios.get(url)
  }
}

var HistoricVariableInstanceService = {
  getHistoricVariableInstance: function(id, deserializeValue) {
    let url = `${getServicesBasePath()}/history/variable-instance/${id}`
    if (deserializeValue !== null && deserializeValue !== undefined) {
      url += `?deserializeValue=${deserializeValue}`
    }
    return axios.get(url)
  }
}

var AdminService = {
  findUsers: function(filter) {
    // id, firstName, firstNameLike, lastName, lastNameLike, email, emailLike, memberOfGroup, memberOfTenant, idIn, firstResult, maxResults
    return axios.get(getServicesBasePath() + "/admin/user", {
      params: filter,
    })
  },
  createUser: function (user) { return axios.post(getServicesBasePath() + "/admin/user/create", user) },
  updateUserProfile: function (userId, user) { return axios.put(getServicesBasePath() + "/admin/user/" + userId + "/profile", user) },
  updateUserCredentials: function (userId, password, authenticatedUserPassword) {
    return axios.put(getServicesBasePath() + "/admin/user/" + userId + "/credentials", {
        password: password,
        authenticatedUserPassword: authenticatedUserPassword
    })
  },
  deleteUser: function (userId) { return axios.delete(getServicesBasePath() + "/admin/user/" + userId) },
  findGroups: function (filter) {
    // id, name, nameLike, type, member, memberOfTenant, firstResult, maxResults
    return axios.get(getServicesBasePath() + "/admin/group", {
      params: filter
    })
  },
  createGroup: function (group) { return axios.post(getServicesBasePath() + "/admin/group/create", group) },
  updateGroup: function (groupId, group) { return axios.put(getServicesBasePath() + "/admin/group/" + groupId, group) },
  deleteGroup: function (groupId) { return axios.delete(getServicesBasePath() + "/admin/group/" + groupId) },
  addMember: function (groupId, userId) { return axios.put(getServicesBasePath() + "/admin/group/" + groupId + "/members/" + userId) },
  deleteMember: function (groupId, userId) { return axios.delete(getServicesBasePath() + "/admin/group/" + groupId + "/members/" + userId) },
  findAuthorizations: function (filter) {
      // id, type, userIdIn, lastName, groupIdIn, resourceType, resourceId
    return axios.get(getServicesBasePath() + "/admin/authorization", {
      params: filter
    })
  },
  createAuthorization: function (authorization) { return axios.post(getServicesBasePath() + "/admin/authorization/create", authorization) },
  updateAuthorization: function (authorizationId, authorization)
    { return axios.put(getServicesBasePath() + "/admin/authorization/" + authorizationId, authorization) },
  deleteAuthorization: function (authorizationId) { return axios.delete(getServicesBasePath() + "/admin/authorization/" + authorizationId) }

}

var HistoryService = {
  findTasksByDefinitionKeyHistory: function(taskDefinitionKey, processInstanceId) {
    return axios.get(getServicesBasePath() + "/task-history/by-process-key", {
      params: {
        taskDefinitionKey: taskDefinitionKey,
        processInstanceId: processInstanceId
      }
    })
  },
  findTasksByProcessInstanceHistory: function(processInstanceId) {
    return axios.get(getServicesBasePath() + "/task-history/by-process-instance/" + processInstanceId)
  },
  fetchActivityVariablesHistory: function(activityInstanceId) {
    return axios.get(getServicesBasePath() + "/task-history/" + activityInstanceId + "/variables")
  },
  findProcessesInstancesHistory: function(filters, firstResult, maxResults) {
    const params = {}
    if (firstResult != null) params.firstResult = firstResult
    if (maxResults != null) params.maxResults = maxResults
    return axios.post(getServicesBasePath() + '/process-history/instance', filters, { params })
  },
  findProcessesInstancesHistoryById: function(id, activityId, firstResult, maxResults, text, active, sortingCriteria = [], fetchIncidents = false) {
    const requestBody = {
      processDefinitionId: id
    }
    
    // Add incident fetching if requested
    if (fetchIncidents) {
      requestBody.fetchIncidents = true
    }
    
    // Add activity filter
    if (activityId) {
      requestBody.activeActivityIdIn = [activityId]
    }
    
    // Add text search with OR logic (business key LIKE or exact process instance ID)
    if (text && text.trim() !== '') {
      const trimmedText = text.trim()
      requestBody.orQueries = [
        {
          processInstanceBusinessKeyLike: `%${trimmedText}%`,
          processInstanceId: trimmedText
        }
      ]
    }
    
    // Add active/finished filter
    if (active !== undefined && active !== null) {
      if (active) {
        requestBody.unfinished = true
      } else {
        requestBody.finished = true
      }
    }
    
    // Add sorting criteria
    if (sortingCriteria && sortingCriteria.length > 0) {
      requestBody.sorting = sortingCriteria.map(criteria => ({
        sortBy: criteria.field,
        sortOrder: criteria.order
      }))
    } else {
      // Default sorting by start time descending
      requestBody.sorting = [{ sortBy: 'startTime', sortOrder: 'desc' }]
    }
    
    const params = {}
    if (firstResult !== null && firstResult !== undefined) params.firstResult = firstResult
    if (maxResults !== null && maxResults !== undefined) params.maxResults = maxResults
    
    return axios.post(getServicesBasePath() + "/process-history/instance", requestBody, { params })
  },
  findActivitiesInstancesHistory: function(processInstanceId) {
    return axios.get(getServicesBasePath() + "/process-history/activity/by-process-instance/" + processInstanceId)
  },
  findActivitiesProcessDefinitionHistory: function(processDefinitionId) {
    return axios.get(getServicesBasePath() + "/process-history/activity/by-process-definition/" + processDefinitionId)
  },
  findActivitiesInstancesHistoryWithFilter(filter){
	return axios.get(getServicesBasePath() + "/process-history/activity", 	{ params: filter })
  },
  fetchProcessInstanceVariablesHistory: function(processInstanceId, deserialize) {
    return axios.get(getServicesBasePath() + "/process-history/instance/by-process-instance/" + processInstanceId + "/variables",
      { params: {
        deserialize: deserialize
      }
    })
  },
  fetchHistoryVariableDataById: function(id) {
    return axios.get(getServicesBasePath() + "/process-history/variable/" + id + "/data", { responseType: "blob" })
  },
  findProcessInstance: function(id) {
    return axios.get(getServicesBasePath() + "/process-history/instance/" + id)
  },
  findTasksByTaskIdHistory: function(taskId) {
    return axios.get(getServicesBasePath() + "/task-history/by-task-id/" + taskId)
  },
  deleteProcessInstanceFromHistory: function(id) {
    return axios.delete(getServicesBasePath() + "/process-history/instance/" + id)
  },
  deleteVariableHistoryInstance: function(id) {
    return axios.delete(getServicesBasePath() + "/process-history/instance/" + id + "/variables")
  }
}

var JobDefinitionService = {
  findJobDefinitions: function(params) {
    return axios.post(getServicesBasePath() + "/job-definition", params)
  },
  suspendJobDefinition: function(jobDefinitionId, params) {
    return axios.put(getServicesBasePath() + "/job-definition/" + jobDefinitionId + "/suspend", params)
  },
  overrideJobDefinitionPriority: function(jobDefinitionId, params) {
    return axios.put(getServicesBasePath() + "/job-definition/" + jobDefinitionId + "/job-priority", params)
  },
  findJobDefinition: function(id) {
    return axios.get(getServicesBasePath() + "/job-definition/" + id)
  },
  retryJobDefinitionById: function(id, params) {
    return axios.put(getServicesBasePath() + "/job-definition/" + id + '/retries', params)
  }
}

var IncidentService = {
  fetchIncidentStacktraceByJobId: function(id) {
    return axios.get(getServicesBasePath() + "/incident/" + id + "/stacktrace")
  },
  fetchIncidentStacktraceByExternalTaskId: function(id) {
    return axios.get(getServicesBasePath() + "/incident/external-task/" + id + "/errorDetails")
  },
  retryJobById: function(id, params) {
    return axios.put(getServicesBasePath() + "/incident/job/" + id + "/retries", params)
  },
  retryExternalTaskById: function(id, params) {
    return axios.put(getServicesBasePath() + "/incident/external-task/" + id + "/retries", params)
  },
  findIncidents: function(params) {
    const queryParams = new URLSearchParams()
    Object.keys(params).forEach(key => {
      if (params[key] !== undefined && params[key] !== null) {
        queryParams.append(key, params[key])
      }
    })
    return axios.get(getServicesBasePath() + "/incident?" + queryParams.toString())
  },
  setIncidentAnnotation: function(id, params) {
    return axios.put(getServicesBasePath() + "/incident/" + id + "/annotation", params)
  }
}

var AuthService = {
  fetchAuths: function() {return axios.get(getServicesBasePath() + "/auth/authorizations") },
  passwordRecover: function(data) { return axios.post(getServicesBasePath() + "/auth/password-recover", data) },
  passwordRecoverCheck: function(recoverToken) { return axios.get(getServicesBasePath() + "/auth/password-recover-check",
    { headers: { authorization: recoverToken } }
  ) },
  passwordRecoverUpdatePassword: function(userId, password, authenticatedUserPassword, recoverToken) {
    return axios.put(getServicesBasePath() + "/auth/password-recovery-update-password/" + userId,
    { password: password, authenticatedUserPassword: authenticatedUserPassword },
    { headers: { authorization: recoverToken } }
  ) },
  login: function(params, remember) {
    return axios.create().post(getServicesBasePath() + '/auth/login', params).then(function(user) {
      axios.defaults.headers.common.authorization = user.data.authToken
      ;(remember ? localStorage : sessionStorage).setItem('token', user.data.authToken)
      return user.data
    })
  }
}

var InfoService = {
  getProperties: function() {
    return axios.get('info/properties')
  },
  getVersion: function() {
    return axios.get(getServicesBasePath() + '/info')
  }
}

var FormsService = {
  submitVariables: function(task, formResult, close) {
    return axios.post(getServicesBasePath() + '/task/' + task.id + '/submit-variables', formResult, {
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
    return axios.post(getServicesBasePath() + '/process/' + processDefinitionId + '/submit-startform-variables', formResult)
  },
  downloadFiles: function(processInstanceId, documentsList) {
    return axios.post(getServicesBasePath() + '/task/' + processInstanceId + '/download', documentsList, { responseType: 'blob' } )
  },
  downloadFile: function(processInstanceId, fileVariable) {
    return axios.get(getServicesBasePath() + '/task/' + processInstanceId + '/variable/download/' + fileVariable, { responseType: 'blob' })
  },
  fetchVariable: function(taskId, variableName, deserialize) {
    return axios.get(getServicesBasePath() + '/task/' + taskId + '/variable/' + variableName, { params: { deserialize: deserialize } })
  },
  sendMessage: function(data) {
    return axios.post(getServicesBasePath() + '/process/message', data)
  },
  fetchVariables: function(taskId, deserialize) {
    return axios.post(getServicesBasePath() + '/task/' + taskId, null, { params: { deserialize: deserialize } } )
  },
  deleteVariable: function(taskId, variableName) {
    return axios.delete(getServicesBasePath() + '/task/' + taskId + '/variable/' + variableName)
  },
  handleBpmnError: function(taskId, data) {
    return axios.post(getServicesBasePath() + '/task/' + taskId + '/bpmnError', data)
  }
}

var TemplateService = {
  getTemplate: function(element, taskId, locale, token) {
	return axios.get(getServicesBasePath() + '/template/' + element + '/' + taskId + '?locale=' + locale, {
	    headers: {
        Authorization: `${token}`
	    }
	  })
  },
  getStartFormTemplate: function(element, processDefinitionId, locale, token) {
    return axios.get(getServicesBasePath() + '/template/' + element + '/key/' + processDefinitionId + '?locale=' + locale, {
	    headers: {
        Authorization: `${token}`
	    }
	  })
  }
}

var DecisionService = {
  getDecisionList: function(params) {
    return axios.get(getServicesBasePath() + "/decision", { params })
  },
  getDecisionVersionsByKey: function(key, lazyLoad = false) {
    return axios.get(getServicesBasePath() + "/decision/key/" + key + "/versions" + '?lazyLoad=' + lazyLoad)
  },
  getDecisionDefinitionById: function(id, extraInfo = false) {
    return axios.get(getServicesBasePath() + "/decision/id/" + id + '?extraInfo=' + extraInfo)
  },
  getDecisionByKey: function(key) {
    return axios.get(getServicesBasePath() + "/decision/key/" + key)
  },
  getDecisionByKeyAndTenant: function (key, tenant) {
    return axios.get(getServicesBasePath() + "/decision/key/" + key + "/tenant/" + tenant)
  },
  getDiagramByKey: function (key) {
    return axios.get(getServicesBasePath() + "/decision/key/" + key + "/diagram")
  },
  getDiagramById: function (id) {
    return axios.get(getServicesBasePath() + "/decision/id/" + id + "/diagram")
  },
  getDiagramByKeyAndTenant: function (key, tenant) {
    return axios.get(getServicesBasePath() + "/decision/key/" + key + "/tenant/" + tenant + "/diagram")
  },
  getXmlByKey: function (key) {
    return axios.get(getServicesBasePath() + "/decision/key/" + key + "/xml")
  },
  getXmlByKeyAndTenant: function (key, tenant) {
    return axios.get(getServicesBasePath() + "/decision/key/" + key + "/tenant/" + tenant + "/xml")
  },
  getXmlById: function (id) {
    return axios.get(getServicesBasePath() + "/decision/id/" + id + "/xml")
  },
  evaluateByKey: function (key, data) {
    return axios.post(getServicesBasePath() + "/decision/key/" + key + "/evaluate", data)
  },
  evaluateByKeyAndTenant: function (key, tenant, data) {
    return axios.post(getServicesBasePath() + "/decision/key/" + key + "/tenant/" + tenant + "/evaluate", data)
  },
  evaluateById: function (id, data) {
    return axios.post(getServicesBasePath() + "/decision/id/" + id + "/evaluate", data)
  },
  updateHistoryTTLByKey: function (key, data) {
    return axios.put(getServicesBasePath() + "/decision/key/" + key + "/history-ttl", data)
  },
  updateHistoryTTLByKeyAndTenant: function (key, tenant, data) {
    return axios.put(getServicesBasePath() + "/decision/key/" + key + "/tenant/" + tenant + "/history-ttl", data)
  },
  updateHistoryTTLById: function (id, data) {
    return axios.put(getServicesBasePath() + "/decision/id/" + id + "/history-ttl", data)
  },
  getHistoricDecisionInstances: function (params) {
    return axios.get(getServicesBasePath() + "/decision/history/instances", { params })
  },
  getHistoricDecisionInstanceCount: function (params) {
    return axios.get(getServicesBasePath() + "/decision/history/instances/count", { params })
  },
  getHistoricDecisionInstanceById: function (id, params) {
    return axios.get(getServicesBasePath() + "/decision/history/instances/" + id, { params })
  },
  deleteHistoricDecisionInstances: function (payload) {
    return axios.post(getServicesBasePath() + "/decision/history/instances/delete", payload)
  },
  setHistoricDecisionInstanceRemovalTime: function (payload) {
    return axios.post(getServicesBasePath() + "/decision/history/instances/set-removal-time", payload)
  }
}

var JobService = {
  getJobs: function(params) {
    return axios.post(getServicesBasePath() + '/job', params)
  },
  setSuspended: function(id, data) {
    return axios.put(getServicesBasePath() + '/job/' + id + '/suspended', data)
  },
  deleteJob: function(id) {
    return axios.delete(getServicesBasePath() + '/job/' + id)
  },
  getHistoryJobLog: function(params) {
    return axios.get(getServicesBasePath() + '/job/history/job-log', { params })
  },
  getHistoryJobLogStacktrace: function(id) {
    return axios.get(getServicesBasePath() + '/job/history/job-log/' + id + '/stacktrace')
  }
}

var BatchService = {
  getBatches: function(params) {
    return axios.get(getServicesBasePath() + '/batch', { params })
  },
  getBatchStatistics: function(params) {
    return axios.get(getServicesBasePath() + '/batch/statistics', { params })
  },
  setBatchSuspensionState: function(id, params) {
    return axios.put(getServicesBasePath() + '/batch/' + id + '/suspended', params)
  },
  deleteBatch: function(id, params) {
    return axios.delete(getServicesBasePath() + '/batch/' + id, params)
  },
  getHistoricBatches: function(params) {
    return axios.get(getServicesBasePath() + '/history/batch', { params })
  },
  getHistoricBatchCount(params) {
    return axios.get('/history/batch/count', { params })
  },
  getHistoricBatchById: function(id) {
    return axios.get(getServicesBasePath() + '/history/batch/' + id)
  },
  deleteHistoricBatch: function(id) {
    return axios.delete(getServicesBasePath() + '/history/batch/' + id)
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
    return axios.get(getServicesBasePath() + '/analytics')
  }
}

var SystemService = {
  getTelemetryData() {
    return axios.get(getServicesBasePath() + '/system/telemetry/data')
  },
  getMetricsData(params) {
    return axios.get(getServicesBasePath() + '/system/metrics/data', { params })
  }
}

var TenantService = {  
  getTenants(params) {
    return axios.get(getServicesBasePath() + '/tenant', { params })
  },
  getTenantById: function (tenantId) {
    return axios.get(getServicesBasePath() + '/tenant/' + tenantId)
  },
  createTenant: function (tenant) {
    return axios.post(getServicesBasePath() + '/tenant', tenant)
  },
  updateTenant: function (tenant) {
    return axios.put(getServicesBasePath() + '/tenant/' + tenant.id, tenant)
  },
  deleteTenant: function (tenantId) {
    return axios.delete(getServicesBasePath() + '/tenant/' + tenantId)
  },
  removeUserFromTenant(tenantId, userId) {
    return axios.delete(`${getServicesBasePath()}/tenant/${tenantId}/users/${userId}`)
  },
  addUserToTenant(tenantId, userId) {
    return axios.post(`${getServicesBasePath()}/tenant/${tenantId}/users/${userId}`)
  },
  removeGroupFromTenant(tenantId, groupId) {
    return axios.delete(`${getServicesBasePath()}/tenant/${tenantId}/groups/${groupId}`)
  },
  addGroupToTenant(tenantId, groupId) {
    return axios.post(`${getServicesBasePath()}/tenant/${tenantId}/groups/${groupId}`)
  }
}

var ExternalTaskService = {
  fetchExternalTasks(params) {
    return axios.get(getServicesBasePath() + '/external-tasks', { params })
  }
}

export { TaskService, FilterService, ProcessService, VariableInstanceService, HistoricVariableInstanceService, AdminService, JobService, JobDefinitionService, SystemService,
  HistoryService, IncidentService, AuthService, InfoService, FormsService, TemplateService, DecisionService, 
  AnalyticsService, BatchService, TenantService, ExternalTaskService, getServicesBasePath, setServicesBasePath }
