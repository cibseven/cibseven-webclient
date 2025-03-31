import appConfig from './appConfig.js'
import moment from 'moment'
import { axios } from './globals.js'

function patchProcess(process) {
  if (Array.isArray(process)) process.forEach(patchProcess)
  else {
    process.startTimeOriginal = process.startTime
    process.endTimeOriginal = process.endTime
    if (process.startTime) process.startTime = moment(process.startTime).format('LL HH:mm')
    if (process.endTime) process.endTime = moment(process.endTime).format('LL HH:mm')
    if (process.removalTime) process.removalTime = moment(process.removalTime).format('LL HH:mm')
  }
  return process
}

function patchTask(task) {
  if (Array.isArray(task)) task.forEach(patchTask)
  else {
    task.createdOriginal = task.created
    if (task.created) task.created = moment(task.created).format('LL HH:mm')
    if (task.startTime) task.startTime = moment(task.startTime).format('LL HH:mm')
    if (task.endTime) task.endTime = moment(task.endTime).format('LL HH:mm')
  }
  return task
}

function patchDeployment(deployment) {
  if (Array.isArray(deployment)) deployment.forEach(patchDeployment)
  else {
    deployment.originalTime = deployment.deploymentTime
    if (deployment.deploymentTime) {
      deployment.deploymentTime = moment(deployment.deploymentTime).utc().format('LL HH:mm')
    }
  }
  return deployment
}

function patchJob(job) {
  if (Array.isArray(job)) job.forEach(patchJob)
  else {
    job.createTimeOriginal = job.createTime
    job.dueDateOriginal = job.dueDate
    if (job.createTime) job.createTime = moment(job.createTime).format('LL HH:mm')
    if (job.dueDate) job.dueDate = moment(job.dueDate).format('LL HH:mm')
  }
  return job
}

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
    return axios.get(appConfig.servicesBasePath + '/task', { params: { filter: filterToUrlParams(filters) } }).then(patchTask)
  },
  findTasksPost: function(filters) {
    return axios.post(appConfig.servicesBasePath + '/task', filters).then(patchTask)
  },
  findTasksByProcessInstance: function(processInstanceId) {
    return axios.get(appConfig.servicesBasePath + "/task/by-process-instance/" + processInstanceId).then(patchTask)
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
    return axios.post(appConfig.servicesBasePath + "/task/by-filter/" + filterId + queryPagination, filters).then(patchTask)
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
  }
}

var FilterService = {
  findFilters: function() { return axios.get(appConfig.servicesBasePath + "/filter") },
  createFilter: function(filter) { return axios.post(appConfig.servicesBasePath + "/filter", filter) },
  updateFilter: function(filter) { return axios.put(appConfig.servicesBasePath + "/filter", filter) },
  deleteFilter: function(filterId) { return axios.delete(appConfig.servicesBasePath + "/filter/" + filterId) }
}

var ProcessService = {
  findProcesses: function() { return axios.get(appConfig.servicesBasePath + "/process").then(patchProcess) },
  findProcessesWithInfo: function() { return axios.get(appConfig.servicesBasePath + "/process/extra-info").then(patchProcess) },
  findProcessesWithFilters: function(filters) { return axios.post(appConfig.servicesBasePath + "/process", filters).then(patchProcess) },
  findProcessByDefinitionKey: function(key) { return axios.get(appConfig.servicesBasePath + "/process/" + key).then(patchProcess) },
  findProcessVersionsByDefinitionKey: function(key, lazyLoad = false) {
    return axios.get(appConfig.servicesBasePath + "/process/process-definition/versions/" + key + '?lazyLoad=' + lazyLoad).then(patchProcess)
  },
  findProcessById: function(id, extraInfo = false) {
    return axios.get(appConfig.servicesBasePath + "/process/process-definition-id/" + id + '?extraInfo=' + extraInfo).then(patchProcess)
  },
  findProcessesInstances: function(key) {
    return axios.get(appConfig.servicesBasePath + "/process/instances/by-process-key/" + key).then(patchProcess)
  },
  findProcessInstance: function(processInstanceId) {
    return axios.get(appConfig.servicesBasePath + "/process/process-instance/" + processInstanceId).then(patchProcess)
  },
  findCurrentProcessesInstances: function(filter) {
    return axios.post(appConfig.servicesBasePath + "/process/instances", filter).then(patchProcess)
  },
  findActivityInstance: function(processInstanceId) {
    return axios.get(appConfig.servicesBasePath + "/process/activity/by-process-instance/" + processInstanceId)
  },
  findCalledProcessDefinitions: function(processDefinitionId) {
    return axios.get(appConfig.servicesBasePath + "/process/called-process-definitions/" + processDefinitionId)
  },
  fetchDiagram: function(processId) { return axios.get(appConfig.servicesBasePath + "/process/" + processId + "/diagram") },
  startProcess: function(key, locale) {
    return axios.post(appConfig.servicesBasePath + "/process/" + key + "/start", {
      variables: { _locale: { value: locale, type: String } /*initiator: { value: userId, type: String }*/
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
  deleteInstance: function(processInstanceId) {
    return axios.delete(appConfig.servicesBasePath + "/process/instance/" + processInstanceId + "/delete")
  },
  findDeployments: function() {
    return axios.get(appConfig.servicesBasePath + "/process/deployments").then(patchDeployment)
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
    }).then(patchTask)
  },
  findTasksByProcessInstanceHistory: function(processInstanceId) {
    return axios.get(appConfig.servicesBasePath + "/task-history/by-process-instance/" + processInstanceId).then(patchTask)
  },
  fetchActivityVariablesHistory: function(activityInstanceId) {
    return axios.get(appConfig.servicesBasePath + "/task-history/" + activityInstanceId + "/variables")
  },
  findProcessesInstancesHistory: function(key, firstResult, maxResults, active) {
    return axios.get(appConfig.servicesBasePath + "/process-history/instance/by-process-key/" + key, {
      params: {
        active: active,
        firstResult: firstResult,
        maxResults: maxResults
      }
    }).then(patchProcess)
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
    }).then(patchProcess)
  },
  findActivitiesInstancesHistory: function(processInstanceId) {
    return axios.get(appConfig.servicesBasePath + "/process-history/activity/by-process-instance/" + processInstanceId)
  },
  findActivitiesProcessDefinitionHistory: function(processDefinitionId) {
    return axios.get(appConfig.servicesBasePath + "/process-history/activity/by-process-definition/" + processDefinitionId)
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
    return axios.get(appConfig.servicesBasePath + "/process-history/instance/" + id).then(patchProcess)
  },
  findTasksByTaskIdHistory: function(taskId) {
    return axios.get(appConfig.servicesBasePath + "/task-history/by-task-id/" + taskId).then(patchProcess)
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
  }
}

var IncidentService = {
  fetchIncidentStacktraceByJobId: function(id) {
    return axios.get(appConfig.servicesBasePath + "/incident/" + id + "/stacktrace")
  },
  retryJobById: function(id) {
    return axios.put(appConfig.servicesBasePath + "/incident/job/" + id + "/retries", {
        retries: 1
    })
  },
  findIncidents: function(processDefinitionId) {
    return axios.get(appConfig.servicesBasePath + "/incident?processDefinitionId=" + processDefinitionId)
  }
}

var AuthService = {
  fetchAuths: function() {return axios.get(appConfig.servicesBasePath + "/auth/authorizations") },
  createAnonUserToken: function() { return axios.get(appConfig.servicesBasePath + "/auth/anon-user") },
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
  submitStartFormVariables: function(processDefinitionKey, formResult, locale) {
    formResult.push({ name: '_locale', type: 'String', value: locale })
    return axios.post(appConfig.servicesBasePath + '/process/' + processDefinitionKey + '/submit-startform-variables', formResult)
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

var JobService = {
  getJobs(params) {
    return axios.post(appConfig.servicesBasePath + '/job', params).then(patchJob)
  },
  setSuspended(id, data) {
    return axios.put(appConfig.servicesBasePath + '/job/' + id + '/suspended', data)
  }
}

var BatchService = {
  getHistoricBatches(params) {
    return axios.get('/history/batch', { params })
  },

  getHistoricBatchCount(params) {
    return axios.get('/history/batch/count', { params })
  },

  getHistoricBatchById(id) {
    return axios.get(`/history/batch/${id}`)
  },

  deleteHistoricBatch(id) {
    return axios.delete(`/history/batch/${id}`)
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

export { TaskService, FilterService, ProcessService, AdminService, JobService, JobDefinitionService,
  HistoryService, IncidentService, AuthService, InfoService, FormsService, TemplateService, BatchService }
