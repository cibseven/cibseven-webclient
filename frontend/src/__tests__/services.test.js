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
import { describe, it, expect, vi, beforeAll, beforeEach, afterEach } from 'vitest'
import { createAxiosMock, resetAxiosMock } from './support/axiosMock.js'
import { ENGINE_STORAGE_KEY } from '@/constants.js'

const axios = createAxiosMock()

vi.mock('@/globals.js', async (importOriginal) => {
  const actual = await importOriginal()
  return { ...actual, axios }
})

const {
  TaskService, FilterService, ProcessService, VariableInstanceService, HistoricVariableInstanceService,
  AdminService, JobService, JobDefinitionService, SystemService, HistoryService, IncidentService,
  AuthService, InfoService, FormsService, TemplateService, DecisionService, AnalyticsService,
  BatchService, TenantService, ExternalTaskService, DeploymentService, EngineService, SetupService,
  getServicesBasePath, setServicesBasePath, createDocumentEndpointUrl
} = await import('@/services.js')

const BASE = '/services/v1'

beforeAll(() => {
  setServicesBasePath(BASE)
})

beforeEach(() => {
  resetAxiosMock(axios)
  localStorage.clear()
})

/**
 * Run a table of endpoint contracts.
 *
 * Each row is `{ name, call, verb, args }`: `call` invokes the service method, and `args`
 * is the exact argument list the underlying axios verb must receive. Pinning the URL,
 * params and body this way is what catches an endpoint being renamed or a query parameter
 * being dropped — the kind of change that otherwise only shows up against a live backend.
 */
function itMatchesEndpoints(table) {
  it.each(table)('$name', ({ call, verb, args }) => {
    call()
    expect(axios[verb]).toHaveBeenCalledTimes(1)
    expect(axios[verb]).toHaveBeenCalledWith(...args)
  })
}

describe('base path helpers', () => {
  it('should report the configured base path', () => {
    expect(getServicesBasePath()).toBe(BASE)
  })

  it('should let the base path be reconfigured', () => {
    setServicesBasePath('/other')
    expect(getServicesBasePath()).toBe('/other')
    setServicesBasePath(BASE)
  })

  it('should prefix requests with the current base path', () => {
    setServicesBasePath('/custom/api')
    TaskService.findTaskById('t1')
    expect(axios.get).toHaveBeenCalledWith('/custom/api/task/t1')
    setServicesBasePath(BASE)
  })
})

describe('TaskService', () => {
  itMatchesEndpoints([
    { name: 'findIdentityLinks', verb: 'get', call: () => TaskService.findIdentityLinks('t1'), args: [`${BASE}/task/t1/identity-links`] },
    { name: 'addIdentityLink', verb: 'post', call: () => TaskService.addIdentityLink('t1', { userId: 'demo' }), args: [`${BASE}/task/t1/identity-links`, { userId: 'demo' }] },
    { name: 'removeIdentityLink', verb: 'post', call: () => TaskService.removeIdentityLink('t1', { userId: 'demo' }), args: [`${BASE}/task/t1/identity-links/delete`, { userId: 'demo' }] },
    { name: 'findTasksPost', verb: 'post', call: () => TaskService.findTasksPost({ assignee: 'demo' }), args: [`${BASE}/task`, { assignee: 'demo' }] },
    { name: 'findTasksByProcessInstance', verb: 'get', call: () => TaskService.findTasksByProcessInstance('pi1'), args: [`${BASE}/task/by-process-instance/pi1`] },
    { name: 'findTaskById', verb: 'get', call: () => TaskService.findTaskById('t1'), args: [`${BASE}/task/t1`] },
    { name: 'findTasksByFilter', verb: 'post', call: () => TaskService.findTasksByFilter('f1', { a: 1 }, { firstResult: 0, maxResults: 50 }), args: [`${BASE}/task/by-filter/f1?firstResult=0&maxResults=50`, { a: 1 }] },
    { name: 'submit', verb: 'post', call: () => TaskService.submit('t1'), args: [`${BASE}/task/submit/t1`] },
    { name: 'submitWithVariables', verb: 'post', call: () => TaskService.submitWithVariables('t1', { amount: 1 }), args: [`${BASE}/task/t1/submit-form`, { amount: 1 }] },
    { name: 'formReference', verb: 'get', call: () => TaskService.formReference('t1'), args: [`${BASE}/task/t1/form-reference`] },
    { name: 'getDeployedForm', verb: 'get', call: () => TaskService.getDeployedForm('t1'), args: [`${BASE}/task/t1/deployed-form`] },
    { name: 'form', verb: 'get', call: () => TaskService.form('t1'), args: [`${BASE}/task/t1/form`] },
    { name: 'setAssignee', verb: 'post', call: () => TaskService.setAssignee('t1', 'demo'), args: [`${BASE}/task/t1/assignee/demo`] },
    { name: 'update', verb: 'put', call: () => TaskService.update({ id: 't1' }), args: [`${BASE}/task/update`, { id: 't1' }] },
    { name: 'fetchActivityVariables', verb: 'get', call: () => TaskService.fetchActivityVariables('a1'), args: [`${BASE}/task/a1/variables`] },
    { name: 'findTasksCountByFilter', verb: 'post', call: () => TaskService.findTasksCountByFilter('f1', { a: 1 }), args: [`${BASE}/task/by-filter/f1/count`, { a: 1 }] },
    { name: 'downloadFile requests a blob', verb: 'get', call: () => TaskService.downloadFile('pi1', 'invoice.pdf'), args: [`${BASE}/task/pi1/variable/download/invoice.pdf`, { responseType: 'blob' }] },
    { name: 'findHistoryTasksCount', verb: 'post', call: () => TaskService.findHistoryTasksCount({ finished: true }), args: [`${BASE}/task-history/count`, { finished: true }] },
    { name: 'getTaskCountByCandidateGroup', verb: 'get', call: () => TaskService.getTaskCountByCandidateGroup(), args: [`${BASE}/task/report/candidate-group-count`] }
  ])
})

describe('TaskService special cases', () => {
  // findTasks serialises its filter array into a query-string fragment via the private
  // filterToUrlParams helper, which is only reachable through this method.
  it('findTasks should flatten a filter array into &key=value pairs', () => {
    TaskService.findTasks([{ key: 'assignee', value: 'demo' }, { key: 'priority', value: 50 }])

    expect(axios.get).toHaveBeenCalledWith(`${BASE}/task`, {
      params: { filter: '&assignee=demo&priority=50' }
    })
  })

  it.each([[undefined], [null], ['not-an-array'], [{}]])('findTasks should send an empty filter for %j', (filters) => {
    TaskService.findTasks(filters)

    expect(axios.get).toHaveBeenCalledWith(`${BASE}/task`, { params: { filter: '' } })
  })

  it('findTasksByProcessInstanceAsignee should send both params when given', () => {
    TaskService.findTasksByProcessInstanceAsignee('pi1', '2026-01-01')

    expect(axios.get).toHaveBeenCalledWith(`${BASE}/task/by-process-instance-asignee`, {
      params: { processInstanceId: 'pi1', createdAfter: '2026-01-01' }
    })
  })

  it('findTasksByProcessInstanceAsignee should omit absent params rather than sending undefined', () => {
    TaskService.findTasksByProcessInstanceAsignee(undefined, undefined)

    expect(axios.get).toHaveBeenCalledWith(`${BASE}/task/by-process-instance-asignee`, { params: {} })
  })

  it('findTasksByProcessInstanceAsignee should send only the process instance when no date is given', () => {
    TaskService.findTasksByProcessInstanceAsignee('pi1')

    expect(axios.get).toHaveBeenCalledWith(`${BASE}/task/by-process-instance-asignee`, {
      params: { processInstanceId: 'pi1' }
    })
  })

  // checkActiveTask runs before the interceptors are meaningful, so it deliberately uses
  // a bare instance and passes the token explicitly.
  it('checkActiveTask should use a fresh axios instance and an explicit authorization header', () => {
    TaskService.checkActiveTask('t1', 'Bearer abc')

    expect(axios.create).toHaveBeenCalled()
    expect(axios.get).toHaveBeenCalledWith(`${BASE}/task/t1`, { headers: { authorization: 'Bearer abc' } })
  })
})

describe('FilterService', () => {
  itMatchesEndpoints([
    { name: 'findFilters', verb: 'get', call: () => FilterService.findFilters(), args: [`${BASE}/filter`] },
    { name: 'createFilter', verb: 'post', call: () => FilterService.createFilter({ name: 'f' }), args: [`${BASE}/filter`, { name: 'f' }] },
    { name: 'updateFilter', verb: 'put', call: () => FilterService.updateFilter({ id: 'f1' }), args: [`${BASE}/filter`, { id: 'f1' }] },
    { name: 'deleteFilter', verb: 'delete', call: () => FilterService.deleteFilter('f1'), args: [`${BASE}/filter/f1`] }
  ])
})

describe('ProcessService', () => {
  itMatchesEndpoints([
    { name: 'findProcesses', verb: 'get', call: () => ProcessService.findProcesses(), args: [`${BASE}/process`] },
    { name: 'findProcessesWithInfo', verb: 'get', call: () => ProcessService.findProcessesWithInfo(), args: [`${BASE}/process/extra-info`] },
    { name: 'findProcessesWithFilters', verb: 'post', call: () => ProcessService.findProcessesWithFilters({ key: 'invoice' }), args: [`${BASE}/process`, { key: 'invoice' }] },
    { name: 'findProcessesInstances', verb: 'get', call: () => ProcessService.findProcessesInstances('invoice'), args: [`${BASE}/process/instances/by-process-key/invoice`] },
    { name: 'findProcessInstance', verb: 'get', call: () => ProcessService.findProcessInstance('pi1'), args: [`${BASE}/process/process-instance/pi1`] },
    { name: 'findActivityInstance', verb: 'get', call: () => ProcessService.findActivityInstance('pi1'), args: [`${BASE}/process/activity/by-process-instance/pi1`] },
    { name: 'findCalledProcessDefinitions', verb: 'get', call: () => ProcessService.findCalledProcessDefinitions('pd1'), args: [`${BASE}/process/called-process-definitions/pd1`] },
    { name: 'fetchDiagram', verb: 'get', call: () => ProcessService.fetchDiagram('pd1'), args: [`${BASE}/process/pd1/diagram`] },
    { name: 'startForm', verb: 'get', call: () => ProcessService.startForm('pd1'), args: [`${BASE}/process-definition/pd1/startForm`] },
    { name: 'getDeployedStartForm', verb: 'get', call: () => ProcessService.getDeployedStartForm('pd1'), args: [`${BASE}/process/pd1/deployed-start-form`] },
    { name: 'stopInstance', verb: 'delete', call: () => ProcessService.stopInstance('pi1'), args: [`${BASE}/process/instance/pi1/delete`] },
    { name: 'findDeployment', verb: 'get', call: () => ProcessService.findDeployment('d1'), args: [`${BASE}/process/deployments/d1`] },
    { name: 'findDeploymentResources', verb: 'get', call: () => ProcessService.findDeploymentResources('d1'), args: [`${BASE}/process/deployments/d1/resources`] },
    { name: 'deleteDeployment', verb: 'delete', call: () => ProcessService.deleteDeployment('d1', true), args: [`${BASE}/process/deployments/d1`, { params: { cascade: true } }] },
    { name: 'fetchVariableDataByExecutionId requests a blob', verb: 'get', call: () => ProcessService.fetchVariableDataByExecutionId('e1', 'doc'), args: [`${BASE}/process/execution/e1/localVariables/doc/data`, { responseType: 'blob' }] },
    { name: 'findProcessStatistics', verb: 'get', call: () => ProcessService.findProcessStatistics('pd1'), args: [`${BASE}/process/process-definition/pd1/statistics`] },
    { name: 'fetchProcessInstanceVariables', verb: 'get', call: () => ProcessService.fetchProcessInstanceVariables('pi1', { deserializeValues: false }), args: [`${BASE}/process/variable-instance/process-instance/pi1/variables`, { params: { deserializeValues: false } }] },
    { name: 'modifyVariableByExecutionId', verb: 'post', call: () => ProcessService.modifyVariableByExecutionId('e1', { a: 1 }), args: [`${BASE}/process/execution/e1/localVariables`, { a: 1 }] },
    { name: 'modifyVariableDataByExecutionId', verb: 'post', call: () => ProcessService.modifyVariableDataByExecutionId('e1', 'doc', { a: 1 }), args: [`${BASE}/process/execution/e1/localVariables/doc/data`, { a: 1 }] },
    { name: 'fetchChatComments', verb: 'get', call: () => ProcessService.fetchChatComments('pi1', 'invoice', true), args: [`${BASE}/process/process-instance/pi1/chat-comments`, { params: { deserialize: true, processDefinitionKey: 'invoice' } }] },
    { name: 'fetchStatusDataset', verb: 'get', call: () => ProcessService.fetchStatusDataset('pi1', 'invoice', false), args: [`${BASE}/process/process-instance/pi1/status-dataset`, { params: { deserialize: false, processDefinitionKey: 'invoice' } }] },
    { name: 'submitVariables', verb: 'post', call: () => ProcessService.submitVariables('pi1', { a: 1 }), args: [`${BASE}/process/process-instance/pi1/submit-variables`, { a: 1 }] },
    { name: 'updateHistoryTimeToLive', verb: 'put', call: () => ProcessService.updateHistoryTimeToLive('pd1', { historyTimeToLive: 5 }), args: [`${BASE}/process/pd1/history-time-to-live`, { historyTimeToLive: 5 }] },
    { name: 'deleteProcessDefinition', verb: 'delete', call: () => ProcessService.deleteProcessDefinition('pd1', true), args: [`${BASE}/process/pd1/delete`, { params: { cascade: true } }] },
    { name: 'putLocalExecutionVariable', verb: 'put', call: () => ProcessService.putLocalExecutionVariable('e1', 'doc', { value: 1 }), args: [`${BASE}/process/execution/e1/localVariables/doc`, { value: 1 }] },
    { name: 'deleteVariableByExecutionId', verb: 'delete', call: () => ProcessService.deleteVariableByExecutionId('e1', 'doc'), args: [`${BASE}/process/execution/e1/localVariables/doc`] },
    { name: 'suspendInstance', verb: 'put', call: () => ProcessService.suspendInstance('pi1', true), args: [`${BASE}/process/instance/pi1/suspend`, null, { params: { suspend: true } }] },
    { name: 'suspendProcess', verb: 'put', call: () => ProcessService.suspendProcess('pd1', true, false, '2026-01-01'), args: [`${BASE}/process/pd1/suspend`, null, { params: { suspend: true, includeProcessInstances: false, executionDate: '2026-01-01' } }] },
    { name: 'findDeploymentsCount', verb: 'get', call: () => ProcessService.findDeploymentsCount('inv'), args: [`${BASE}/process/deployments/count`, { params: { nameLike: 'inv' } }] },
    { name: 'findDeploymentsCount defaults nameLike to empty', verb: 'get', call: () => ProcessService.findDeploymentsCount(), args: [`${BASE}/process/deployments/count`, { params: { nameLike: '' } }] },
    { name: 'findDeployments applies paging and sorting defaults', verb: 'get', call: () => ProcessService.findDeployments(), args: [`${BASE}/process/deployments`, { params: { nameLike: '', firstResult: 0, maxResults: 50, sortBy: 'name', sortOrder: 'asc' } }] },
    { name: 'findDeployments honours explicit paging and sorting', verb: 'get', call: () => ProcessService.findDeployments('inv', 10, 20, 'deploymentTime', 'desc'), args: [`${BASE}/process/deployments`, { params: { nameLike: 'inv', firstResult: 10, maxResults: 20, sortBy: 'deploymentTime', sortOrder: 'desc' } }] }
  ])
})

describe('ProcessService special cases', () => {
  describe('findProcessByDefinitionKey', () => {
    it('should omit the tenant when there is none', () => {
      ProcessService.findProcessByDefinitionKey('invoice')
      expect(axios.get).toHaveBeenCalledWith(`${BASE}/process/invoice`)
    })

    it('should append the tenant when given', () => {
      ProcessService.findProcessByDefinitionKey('invoice', 't1')
      expect(axios.get).toHaveBeenCalledWith(`${BASE}/process/invoice?tenantId=t1`)
    })
  })

  describe('findProcessVersionsByDefinitionKey', () => {
    it('should default lazyLoad to false', () => {
      ProcessService.findProcessVersionsByDefinitionKey('invoice')
      expect(axios.get).toHaveBeenCalledWith(`${BASE}/process/process-definition/versions/invoice?lazyLoad=false`)
    })

    // The tenant is appended with & because lazyLoad already opened the query string.
    it('should append the tenant after the lazyLoad parameter', () => {
      ProcessService.findProcessVersionsByDefinitionKey('invoice', 't1', true)
      expect(axios.get).toHaveBeenCalledWith(`${BASE}/process/process-definition/versions/invoice?lazyLoad=true&tenantId=t1`)
    })
  })

  describe('findProcessById', () => {
    it('should default extraInfo to false', () => {
      ProcessService.findProcessById('pd1')
      expect(axios.get).toHaveBeenCalledWith(`${BASE}/process/process-definition-id/pd1?extraInfo=false`)
    })

    it('should pass extraInfo through when requested', () => {
      ProcessService.findProcessById('pd1', true)
      expect(axios.get).toHaveBeenCalledWith(`${BASE}/process/process-definition-id/pd1?extraInfo=true`)
    })
  })

  describe('findCurrentProcessesInstances', () => {
    it('should send paging params when given', () => {
      ProcessService.findCurrentProcessesInstances({ processDefinitionId: 'pd1' }, 0, 50)

      expect(axios.post).toHaveBeenCalledWith(
        `${BASE}/process/instances`,
        { processDefinitionId: 'pd1' },
        { params: { firstResult: 0, maxResults: 50 } }
      )
    })

    // Omitting rather than sending undefined keeps the query string clean; note that 0 is
    // a meaningful firstResult and must survive the null check.
    it('should omit paging params that are not supplied', () => {
      ProcessService.findCurrentProcessesInstances({ processDefinitionId: 'pd1' })

      expect(axios.post).toHaveBeenCalledWith(
        `${BASE}/process/instances`,
        { processDefinitionId: 'pd1' },
        { params: {} }
      )
    })
  })

  describe('startProcess', () => {
    // The locale is always injected as a process variable so that the engine can render
    // notifications and forms in the user's language.
    it('should inject the locale as a process variable', () => {
      ProcessService.startProcess('invoice', null, 'de')

      expect(axios.post).toHaveBeenCalledWith(`${BASE}/process/invoice/start`, {
        variables: { _locale: { value: 'de', type: String } }
      })
    })

    it('should merge caller variables alongside the locale', () => {
      ProcessService.startProcess('invoice', null, 'en', null, { amount: { value: 10, type: 'Integer' } })

      expect(axios.post).toHaveBeenCalledWith(`${BASE}/process/invoice/start`, {
        variables: {
          _locale: { value: 'en', type: String },
          amount: { value: 10, type: 'Integer' }
        }
      })
    })

    it('should append the tenant to the URL when given', () => {
      ProcessService.startProcess('invoice', 't1', 'en')

      expect(axios.post).toHaveBeenCalledWith(`${BASE}/process/invoice/start?tenantId=t1`, expect.any(Object))
    })

    it('should include the business key only when one is supplied', () => {
      ProcessService.startProcess('invoice', null, 'en', 'ORDER-1')

      expect(axios.post).toHaveBeenCalledWith(`${BASE}/process/invoice/start`, {
        businessKey: 'ORDER-1',
        variables: { _locale: { value: 'en', type: String } }
      })
    })

    it('should omit the business key when it is falsy', () => {
      ProcessService.startProcess('invoice', null, 'en', '')

      expect(axios.post.mock.calls[0][1]).not.toHaveProperty('businessKey')
    })
  })

  describe('uploadProcessInstanceVariableFileData', () => {
    it('should post multipart form data with the file and value type', () => {
      const file = new File(['x'], 'invoice.pdf', { type: 'application/pdf' })

      ProcessService.uploadProcessInstanceVariableFileData('pi1', 'invoice', file)

      const [url, body, config] = axios.post.mock.calls[0]
      expect(url).toBe(`${BASE}/process/process-instance/pi1/variables/invoice/data`)
      expect(body).toBeInstanceOf(FormData)
      expect(body.get('data')).toBe(file)
      expect(body.get('valueType')).toBe('File')
      expect(config).toEqual({ headers: { 'Content-Type': 'multipart/form-data' } })
    })

    it('should honour an explicit value type', () => {
      ProcessService.uploadProcessInstanceVariableFileData('pi1', 'invoice', new Blob(['x']), 'Bytes')

      expect(axios.post.mock.calls[0][1].get('valueType')).toBe('Bytes')
    })
  })
})

describe('AdminService', () => {
  itMatchesEndpoints([
    { name: 'findUsers passes the filter as query params', verb: 'get', call: () => AdminService.findUsers({ idIn: 'a,b' }), args: [`${BASE}/admin/user`, { params: { idIn: 'a,b' } }] },
    { name: 'createUser', verb: 'post', call: () => AdminService.createUser({ id: 'demo' }), args: [`${BASE}/admin/user/create`, { id: 'demo' }] },
    { name: 'updateUserProfile', verb: 'put', call: () => AdminService.updateUserProfile('demo', { firstName: 'D' }), args: [`${BASE}/admin/user/demo/profile`, { firstName: 'D' }] },
    { name: 'updateUserCredentials wraps both passwords in the body', verb: 'put', call: () => AdminService.updateUserCredentials('demo', 'new', 'old'), args: [`${BASE}/admin/user/demo/credentials`, { password: 'new', authenticatedUserPassword: 'old' }] },
    { name: 'deleteUser', verb: 'delete', call: () => AdminService.deleteUser('demo'), args: [`${BASE}/admin/user/demo`] },
    { name: 'findGroups passes the filter as query params', verb: 'get', call: () => AdminService.findGroups({ nameLike: '*sales*' }), args: [`${BASE}/admin/group`, { params: { nameLike: '*sales*' } }] },
    { name: 'createGroup', verb: 'post', call: () => AdminService.createGroup({ id: 'sales' }), args: [`${BASE}/admin/group/create`, { id: 'sales' }] },
    { name: 'updateGroup', verb: 'put', call: () => AdminService.updateGroup('sales', { name: 'Sales' }), args: [`${BASE}/admin/group/sales`, { name: 'Sales' }] },
    { name: 'deleteGroup', verb: 'delete', call: () => AdminService.deleteGroup('sales'), args: [`${BASE}/admin/group/sales`] },
    { name: 'addMember', verb: 'put', call: () => AdminService.addMember('sales', 'demo'), args: [`${BASE}/admin/group/sales/members/demo`] },
    { name: 'deleteMember', verb: 'delete', call: () => AdminService.deleteMember('sales', 'demo'), args: [`${BASE}/admin/group/sales/members/demo`] },
    { name: 'findAuthorizations passes the filter as query params', verb: 'get', call: () => AdminService.findAuthorizations({ resourceType: 0 }), args: [`${BASE}/admin/authorization`, { params: { resourceType: 0 } }] },
    { name: 'createAuthorization', verb: 'post', call: () => AdminService.createAuthorization({ type: 1 }), args: [`${BASE}/admin/authorization/create`, { type: 1 }] },
    { name: 'updateAuthorization', verb: 'put', call: () => AdminService.updateAuthorization('a1', { type: 1 }), args: [`${BASE}/admin/authorization/a1`, { type: 1 }] },
    { name: 'deleteAuthorization', verb: 'delete', call: () => AdminService.deleteAuthorization('a1'), args: [`${BASE}/admin/authorization/a1`] }
  ])
})

describe('VariableInstanceService', () => {
  // deserializeValue is tri-state: absent means "let the backend decide", so the query
  // parameter must only appear when the caller passed an explicit boolean.
  it('should omit deserializeValue when it is not supplied', () => {
    VariableInstanceService.getVariableInstance('v1')
    expect(axios.get).toHaveBeenCalledWith(`${BASE}/variable-instance/v1`)
  })

  it('should omit deserializeValue when it is null', () => {
    VariableInstanceService.getVariableInstance('v1', null)
    expect(axios.get).toHaveBeenCalledWith(`${BASE}/variable-instance/v1`)
  })

  it.each([[true], [false]])('should append deserializeValue=%s when given', (value) => {
    VariableInstanceService.getVariableInstance('v1', value)
    expect(axios.get).toHaveBeenCalledWith(`${BASE}/variable-instance/v1?deserializeValue=${value}`)
  })

  // A File rather than a bare Blob, because FormData wraps a Blob into a new File and the
  // original reference would no longer be identity-comparable.
  it('uploadFile should delegate to the execution-local variable data endpoint', async () => {
    const file = new File(['x'], 'invoice.pdf')

    await VariableInstanceService.uploadFile('e1', { name: 'invoice', type: 'File', file })

    const [url, body] = axios.post.mock.calls[0]
    expect(url).toBe(`${BASE}/process/execution/e1/localVariables/invoice/data`)
    expect(body).toBeInstanceOf(FormData)
    expect(body.get('data')).toBe(file)
    expect(body.get('valueType')).toBe('File')
  })
})

describe('HistoricVariableInstanceService', () => {
  it('should omit deserializeValue when it is not supplied', () => {
    HistoricVariableInstanceService.getHistoricVariableInstance('hv1')
    expect(axios.get).toHaveBeenCalledWith(`${BASE}/history/variable-instance/hv1`)
  })

  it.each([[true], [false]])('should append deserializeValue=%s when given', (value) => {
    HistoricVariableInstanceService.getHistoricVariableInstance('hv1', value)
    expect(axios.get).toHaveBeenCalledWith(`${BASE}/history/variable-instance/hv1?deserializeValue=${value}`)
  })
})

describe('JobDefinitionService', () => {
  itMatchesEndpoints([
    { name: 'findJobDefinitions', verb: 'post', call: () => JobDefinitionService.findJobDefinitions({ processDefinitionId: 'pd1' }), args: [`${BASE}/job-definition`, { processDefinitionId: 'pd1' }] },
    { name: 'suspendJobDefinition', verb: 'put', call: () => JobDefinitionService.suspendJobDefinition('jd1', { suspended: true }), args: [`${BASE}/job-definition/jd1/suspend`, { suspended: true }] },
    { name: 'overrideJobDefinitionPriority', verb: 'put', call: () => JobDefinitionService.overrideJobDefinitionPriority('jd1', { priority: 10 }), args: [`${BASE}/job-definition/jd1/job-priority`, { priority: 10 }] },
    { name: 'findJobDefinition', verb: 'get', call: () => JobDefinitionService.findJobDefinition('jd1'), args: [`${BASE}/job-definition/jd1`] },
    { name: 'retryJobDefinitionById', verb: 'put', call: () => JobDefinitionService.retryJobDefinitionById('jd1', { retries: 3 }), args: [`${BASE}/job-definition/jd1/retries`, { retries: 3 }] }
  ])
})

describe('IncidentService', () => {
  itMatchesEndpoints([
    { name: 'fetchIncidentStacktraceByJobId', verb: 'get', call: () => IncidentService.fetchIncidentStacktraceByJobId('j1'), args: [`${BASE}/incident/j1/stacktrace`] },
    { name: 'fetchIncidentStacktraceByExternalTaskId', verb: 'get', call: () => IncidentService.fetchIncidentStacktraceByExternalTaskId('et1'), args: [`${BASE}/incident/external-task/et1/errorDetails`] },
    { name: 'fetchHistoricIncidentStacktraceByExternalTaskId', verb: 'get', call: () => IncidentService.fetchHistoricIncidentStacktraceByExternalTaskId('et1'), args: [`${BASE}/incident/history/external-task/et1/errorDetails`] },
    { name: 'fetchHistoricIncidents', verb: 'get', call: () => IncidentService.fetchHistoricIncidents({ processInstanceId: 'pi1' }), args: [`${BASE}/incident/history`, { params: { processInstanceId: 'pi1' } }] },
    { name: 'fetchHistoricIncidentsCount', verb: 'get', call: () => IncidentService.fetchHistoricIncidentsCount({ processInstanceId: 'pi1' }), args: [`${BASE}/incident/history/count`, { params: { processInstanceId: 'pi1' } }] },
    { name: 'fetchHistoricStacktraceByJobId', verb: 'get', call: () => IncidentService.fetchHistoricStacktraceByJobId('j1'), args: [`${BASE}/incident/history/j1/stacktrace`] },
    { name: 'retryJobById', verb: 'put', call: () => IncidentService.retryJobById('j1', { retries: 1 }), args: [`${BASE}/incident/job/j1/retries`, { retries: 1 }] },
    { name: 'retryExternalTaskById', verb: 'put', call: () => IncidentService.retryExternalTaskById('et1', { retries: 1 }), args: [`${BASE}/incident/external-task/et1/retries`, { retries: 1 }] },
    { name: 'findIncidents', verb: 'get', call: () => IncidentService.findIncidents({ processInstanceId: 'pi1' }), args: [`${BASE}/incident`, { params: { processInstanceId: 'pi1' } }] },
    { name: 'findIncidentsCount', verb: 'get', call: () => IncidentService.findIncidentsCount({ processInstanceId: 'pi1' }), args: [`${BASE}/incident/count`, { params: { processInstanceId: 'pi1' } }] },
    { name: 'setIncidentAnnotation', verb: 'put', call: () => IncidentService.setIncidentAnnotation('i1', { annotation: 'note' }), args: [`${BASE}/incident/i1/annotation`, { annotation: 'note' }] }
  ])
})

describe('JobService', () => {
  itMatchesEndpoints([
    { name: 'getJobs', verb: 'post', call: () => JobService.getJobs({ processInstanceId: 'pi1' }), args: [`${BASE}/job`, { processInstanceId: 'pi1' }] },
    { name: 'setSuspended', verb: 'put', call: () => JobService.setSuspended('j1', { suspended: true }), args: [`${BASE}/job/j1/suspended`, { suspended: true }] },
    { name: 'deleteJob', verb: 'delete', call: () => JobService.deleteJob('j1'), args: [`${BASE}/job/j1`] },
    { name: 'getHistoryJobLog', verb: 'get', call: () => JobService.getHistoryJobLog({ jobId: 'j1' }), args: [`${BASE}/job/history/job-log`, { params: { jobId: 'j1' } }] },
    { name: 'getHistoryJobLogStacktrace', verb: 'get', call: () => JobService.getHistoryJobLogStacktrace('l1'), args: [`${BASE}/job/history/job-log/l1/stacktrace`] },
    { name: 'changeDueDate', verb: 'put', call: () => JobService.changeDueDate('j1', { duedate: '2026-01-01' }), args: [`${BASE}/job/j1/duedate`, { duedate: '2026-01-01' }] },
    { name: 'recalculateDueDate sends params with an empty body', verb: 'post', call: () => JobService.recalculateDueDate('j1', { creationDateBased: true }), args: [`${BASE}/job/j1/duedate/recalculate`, null, { params: { creationDateBased: true } }] }
  ])
})

describe('BatchService', () => {
  itMatchesEndpoints([
    { name: 'getBatches', verb: 'get', call: () => BatchService.getBatches({ maxResults: 10 }), args: [`${BASE}/batch`, { params: { maxResults: 10 } }] },
    { name: 'getBatchStatistics', verb: 'get', call: () => BatchService.getBatchStatistics({ maxResults: 10 }), args: [`${BASE}/batch/statistics`, { params: { maxResults: 10 } }] },
    { name: 'setBatchSuspensionState', verb: 'put', call: () => BatchService.setBatchSuspensionState('b1', { suspended: true }), args: [`${BASE}/batch/b1/suspended`, { suspended: true }] },
    { name: 'deleteBatch', verb: 'delete', call: () => BatchService.deleteBatch('b1', { cascade: true }), args: [`${BASE}/batch/b1`, { cascade: true }] },
    { name: 'getHistoricBatches', verb: 'get', call: () => BatchService.getHistoricBatches({ maxResults: 10 }), args: [`${BASE}/history/batch`, { params: { maxResults: 10 } }] },
    { name: 'getHistoricBatchCount', verb: 'get', call: () => BatchService.getHistoricBatchCount({ type: 'x' }), args: [`${BASE}/history/batch/count`, { params: { type: 'x' } }] },
    { name: 'getHistoricBatchById', verb: 'get', call: () => BatchService.getHistoricBatchById('b1'), args: [`${BASE}/history/batch/b1`] },
    { name: 'deleteHistoricBatch', verb: 'delete', call: () => BatchService.deleteHistoricBatch('b1'), args: [`${BASE}/history/batch/b1`] },
    { name: 'setRemovalTime', verb: 'post', call: () => BatchService.setRemovalTime({ calculatedRemovalTime: true }), args: [`${BASE}/history/batch/set-removal-time`, { calculatedRemovalTime: true }] },
    { name: 'getCleanableBatchReport', verb: 'get', call: () => BatchService.getCleanableBatchReport({ maxResults: 10 }), args: [`${BASE}/history/batch/cleanable-batch-report`, { params: { maxResults: 10 } }] },
    { name: 'getCleanableBatchReportCount', verb: 'get', call: () => BatchService.getCleanableBatchReportCount(), args: [`${BASE}/history/batch/cleanable-batch-report/count`] }
  ])
})

describe('TenantService', () => {
  itMatchesEndpoints([
    { name: 'getTenants', verb: 'get', call: () => TenantService.getTenants({ userMember: 'demo' }), args: [`${BASE}/tenant`, { params: { userMember: 'demo' } }] },
    { name: 'getTenantById', verb: 'get', call: () => TenantService.getTenantById('t1'), args: [`${BASE}/tenant/t1`] },
    { name: 'createTenant', verb: 'post', call: () => TenantService.createTenant({ id: 't1' }), args: [`${BASE}/tenant`, { id: 't1' }] },
    { name: 'updateTenant addresses the tenant by its own id', verb: 'put', call: () => TenantService.updateTenant({ id: 't1', name: 'Acme' }), args: [`${BASE}/tenant/t1`, { id: 't1', name: 'Acme' }] },
    { name: 'deleteTenant', verb: 'delete', call: () => TenantService.deleteTenant('t1'), args: [`${BASE}/tenant/t1`] },
    { name: 'removeUserFromTenant', verb: 'delete', call: () => TenantService.removeUserFromTenant('t1', 'demo'), args: [`${BASE}/tenant/t1/users/demo`] },
    { name: 'addUserToTenant', verb: 'post', call: () => TenantService.addUserToTenant('t1', 'demo'), args: [`${BASE}/tenant/t1/users/demo`] },
    { name: 'removeGroupFromTenant', verb: 'delete', call: () => TenantService.removeGroupFromTenant('t1', 'sales'), args: [`${BASE}/tenant/t1/groups/sales`] },
    { name: 'addGroupToTenant', verb: 'post', call: () => TenantService.addGroupToTenant('t1', 'sales'), args: [`${BASE}/tenant/t1/groups/sales`] }
  ])
})

describe('small services', () => {
  itMatchesEndpoints([
    { name: 'AnalyticsService.getAnalytics', verb: 'get', call: () => AnalyticsService.getAnalytics(), args: [`${BASE}/analytics`] },
    { name: 'SystemService.getTelemetryData', verb: 'get', call: () => SystemService.getTelemetryData(), args: [`${BASE}/system/telemetry/data`] },
    { name: 'SystemService.getMetricsData', verb: 'get', call: () => SystemService.getMetricsData({ name: 'x' }), args: [`${BASE}/system/metrics/data`, { params: { name: 'x' } }] },
    { name: 'ExternalTaskService.fetchExternalTasks', verb: 'get', call: () => ExternalTaskService.fetchExternalTasks({ processInstanceId: 'pi1' }), args: [`${BASE}/external-tasks`, { params: { processInstanceId: 'pi1' } }] },
    { name: 'EngineService.getEngines', verb: 'get', call: () => EngineService.getEngines(), args: [`${BASE}/engine`] },
    { name: 'DeploymentService.redeployDeployment', verb: 'post', call: () => DeploymentService.redeployDeployment('d1', { resourceIds: ['r1'] }), args: [`${BASE}/deployment/d1/redeploy`, { resourceIds: ['r1'] }] },
    { name: 'TemplateService.getTemplate sends the token as an Authorization header', verb: 'get', call: () => TemplateService.getTemplate('el1', 't1', 'en', 'Bearer abc'), args: [`${BASE}/template/el1/t1?locale=en`, { headers: { Authorization: 'Bearer abc' } }] },
    { name: 'AuthService.fetchAuths', verb: 'get', call: () => AuthService.fetchAuths(), args: [`${BASE}/auth/authorizations`] },
    { name: 'AuthService.passwordRecover', verb: 'post', call: () => AuthService.passwordRecover({ email: 'a@b.c' }), args: [`${BASE}/auth/password-recover`, { email: 'a@b.c' }] },
    { name: 'AuthService.passwordRecoverCheck sends the recovery token as the header', verb: 'get', call: () => AuthService.passwordRecoverCheck('tok'), args: [`${BASE}/auth/password-recover-check`, { headers: { authorization: 'tok' } }] },
    { name: 'AuthService.passwordRecoverUpdatePassword', verb: 'put', call: () => AuthService.passwordRecoverUpdatePassword('demo', 'new', 'old', 'tok'), args: [`${BASE}/auth/password-recovery-update-password/demo`, { password: 'new', authenticatedUserPassword: 'old' }, { headers: { authorization: 'tok' } }] },
    { name: 'InfoService.getVersion is not base-path prefixed', verb: 'get', call: () => InfoService.getVersion(), args: ['info'] }
  ])
})

describe('DecisionService', () => {
  itMatchesEndpoints([
    { name: 'getDecisionList', verb: 'get', call: () => DecisionService.getDecisionList({ latestVersion: true }), args: [`${BASE}/decision`, { params: { latestVersion: true } }] },
    { name: 'getDecisionVersionsByKey defaults lazyLoad to false', verb: 'get', call: () => DecisionService.getDecisionVersionsByKey('invoice'), args: [`${BASE}/decision/key/invoice/versions?lazyLoad=false`] },
    { name: 'getDecisionVersionsByKey passes lazyLoad through', verb: 'get', call: () => DecisionService.getDecisionVersionsByKey('invoice', true), args: [`${BASE}/decision/key/invoice/versions?lazyLoad=true`] },
    { name: 'getDecisionDefinitionById defaults extraInfo to false', verb: 'get', call: () => DecisionService.getDecisionDefinitionById('dd1'), args: [`${BASE}/decision/id/dd1?extraInfo=false`] },
    { name: 'getDecisionDefinitionById passes extraInfo through', verb: 'get', call: () => DecisionService.getDecisionDefinitionById('dd1', true), args: [`${BASE}/decision/id/dd1?extraInfo=true`] },
    { name: 'getDecisionByKey', verb: 'get', call: () => DecisionService.getDecisionByKey('invoice'), args: [`${BASE}/decision/key/invoice`] },
    { name: 'getDecisionByKeyAndTenant', verb: 'get', call: () => DecisionService.getDecisionByKeyAndTenant('invoice', 't1'), args: [`${BASE}/decision/key/invoice/tenant/t1`] },
    { name: 'getDiagramByKey', verb: 'get', call: () => DecisionService.getDiagramByKey('invoice'), args: [`${BASE}/decision/key/invoice/diagram`] },
    { name: 'getDiagramById', verb: 'get', call: () => DecisionService.getDiagramById('dd1'), args: [`${BASE}/decision/id/dd1/diagram`] },
    { name: 'getDiagramByKeyAndTenant', verb: 'get', call: () => DecisionService.getDiagramByKeyAndTenant('invoice', 't1'), args: [`${BASE}/decision/key/invoice/tenant/t1/diagram`] },
    { name: 'getXmlByKey', verb: 'get', call: () => DecisionService.getXmlByKey('invoice'), args: [`${BASE}/decision/key/invoice/xml`] },
    { name: 'getXmlByKeyAndTenant', verb: 'get', call: () => DecisionService.getXmlByKeyAndTenant('invoice', 't1'), args: [`${BASE}/decision/key/invoice/tenant/t1/xml`] },
    { name: 'getXmlById', verb: 'get', call: () => DecisionService.getXmlById('dd1'), args: [`${BASE}/decision/id/dd1/xml`] },
    { name: 'evaluateByKey', verb: 'post', call: () => DecisionService.evaluateByKey('invoice', { a: 1 }), args: [`${BASE}/decision/key/invoice/evaluate`, { a: 1 }] },
    { name: 'evaluateByKeyAndTenant', verb: 'post', call: () => DecisionService.evaluateByKeyAndTenant('invoice', 't1', { a: 1 }), args: [`${BASE}/decision/key/invoice/tenant/t1/evaluate`, { a: 1 }] },
    { name: 'evaluateById', verb: 'post', call: () => DecisionService.evaluateById('dd1', { a: 1 }), args: [`${BASE}/decision/id/dd1/evaluate`, { a: 1 }] },
    { name: 'updateHistoryTTLByKey', verb: 'put', call: () => DecisionService.updateHistoryTTLByKey('invoice', { historyTimeToLive: 5 }), args: [`${BASE}/decision/key/invoice/history-ttl`, { historyTimeToLive: 5 }] },
    { name: 'updateHistoryTTLByKeyAndTenant', verb: 'put', call: () => DecisionService.updateHistoryTTLByKeyAndTenant('invoice', 't1', { historyTimeToLive: 5 }), args: [`${BASE}/decision/key/invoice/tenant/t1/history-ttl`, { historyTimeToLive: 5 }] },
    { name: 'updateHistoryTTLById', verb: 'put', call: () => DecisionService.updateHistoryTTLById('dd1', { historyTimeToLive: 5 }), args: [`${BASE}/decision/id/dd1/history-ttl`, { historyTimeToLive: 5 }] },
    { name: 'getHistoricDecisionInstances', verb: 'get', call: () => DecisionService.getHistoricDecisionInstances({ maxResults: 10 }), args: [`${BASE}/decision/history/instances`, { params: { maxResults: 10 } }] },
    { name: 'getHistoricDecisionInstanceCount', verb: 'get', call: () => DecisionService.getHistoricDecisionInstanceCount({ key: 'invoice' }), args: [`${BASE}/decision/history/instances/count`, { params: { key: 'invoice' } }] },
    { name: 'getHistoricDecisionInstanceById', verb: 'get', call: () => DecisionService.getHistoricDecisionInstanceById('di1', { includeInputs: true }), args: [`${BASE}/decision/history/instances/di1`, { params: { includeInputs: true } }] },
    { name: 'deleteHistoricDecisionInstances', verb: 'post', call: () => DecisionService.deleteHistoricDecisionInstances({ ids: ['di1'] }), args: [`${BASE}/decision/history/instances/delete`, { ids: ['di1'] }] },
    { name: 'setHistoricDecisionInstanceRemovalTime', verb: 'post', call: () => DecisionService.setHistoricDecisionInstanceRemovalTime({ calculatedRemovalTime: true }), args: [`${BASE}/decision/history/instances/set-removal-time`, { calculatedRemovalTime: true }] }
  ])
})

const SUBMIT_TASK = {
  id: 't1',
  name: 'Approve',
  processInstanceId: 'pi1',
  processDefinitionId: 'pd1',
  assignee: 'demo'
}

describe('FormsService', () => {
  itMatchesEndpoints([
    {
      name: 'submitVariables carries the task metadata as query params',
      verb: 'post',
      call: () => FormsService.submitVariables(SUBMIT_TASK, [{ name: 'a' }], true),
      args: [`${BASE}/task/t1/submit-variables`, [{ name: 'a' }], {
        params: {
          name: 'Approve',
          processInstanceId: 'pi1',
          processDefinitionId: 'pd1',
          assignee: 'demo',
          close: true
        }
      }]
    },
    { name: 'downloadFiles requests a blob', verb: 'post', call: () => FormsService.downloadFiles('pi1', ['doc1']), args: [`${BASE}/task/pi1/download`, ['doc1'], { responseType: 'blob' }] },
    { name: 'downloadFile requests a blob', verb: 'get', call: () => FormsService.downloadFile('pi1', 'invoice.pdf'), args: [`${BASE}/task/pi1/variable/download/invoice.pdf`, { responseType: 'blob' }] },
    { name: 'fetchVariable', verb: 'get', call: () => FormsService.fetchVariable('t1', 'invoice', true), args: [`${BASE}/task/t1/variable/invoice`, { params: { deserialize: true } }] },
    { name: 'sendMessage', verb: 'post', call: () => FormsService.sendMessage({ messageName: 'm' }), args: [`${BASE}/process/message`, { messageName: 'm' }] },
    { name: 'fetchVariables posts an empty body with the deserialize flag', verb: 'post', call: () => FormsService.fetchVariables('t1', false), args: [`${BASE}/task/t1`, null, { params: { deserialize: false } }] },
    { name: 'deleteVariable', verb: 'delete', call: () => FormsService.deleteVariable('t1', 'invoice'), args: [`${BASE}/task/t1/variable/invoice`] },
    { name: 'handleBpmnError', verb: 'post', call: () => FormsService.handleBpmnError('t1', { errorCode: 'E1' }), args: [`${BASE}/task/t1/bpmnError`, { errorCode: 'E1' }] }
  ])
})

describe('FormsService special cases', () => {
  // The locale is appended to the caller's array in place, so the engine receives it as
  // just another form variable.
  it('submitStartFormVariables should append the locale to the submitted variables', () => {
    const formResult = [{ name: 'amount', type: 'Integer', value: 10 }]

    FormsService.submitStartFormVariables('pd1', formResult, 'de')

    expect(axios.post).toHaveBeenCalledWith(`${BASE}/process/pd1/submit-startform-variables`, [
      { name: 'amount', type: 'Integer', value: 10 },
      { name: '_locale', type: 'String', value: 'de' }
    ])
    expect(formResult).toHaveLength(2)
  })

  it('uploadVariableFileData should post multipart form data', () => {
    const file = new File(['x'], 'invoice.pdf')

    FormsService.uploadVariableFileData('t1', 'invoice', file)

    const [url, body, config] = axios.post.mock.calls[0]
    expect(url).toBe(`${BASE}/task/t1/variables/invoice/data`)
    expect(body.get('data')).toBe(file)
    expect(body.get('valueType')).toBe('File')
    expect(config).toEqual({ headers: { 'Content-Type': 'multipart/form-data' } })
  })

  it('uploadVariableFileData should honour an explicit value type', () => {
    FormsService.uploadVariableFileData('t1', 'invoice', new File(['x'], 'f'), 'Bytes')

    expect(axios.post.mock.calls[0][1].get('valueType')).toBe('Bytes')
  })
})

describe('HistoryService', () => {
  itMatchesEndpoints([
    { name: 'findTasksByDefinitionKeyHistory', verb: 'get', call: () => HistoryService.findTasksByDefinitionKeyHistory('task_1', 'pi1'), args: [`${BASE}/task-history/by-process-key`, { params: { taskDefinitionKey: 'task_1', processInstanceId: 'pi1' } }] },
    { name: 'findTasksByProcessInstanceHistory', verb: 'get', call: () => HistoryService.findTasksByProcessInstanceHistory('pi1'), args: [`${BASE}/task-history/by-process-instance/pi1`] },
    { name: 'fetchActivityVariablesHistory', verb: 'get', call: () => HistoryService.fetchActivityVariablesHistory('a1'), args: [`${BASE}/task-history/a1/variables`] },
    { name: 'findActivitiesInstancesHistory', verb: 'get', call: () => HistoryService.findActivitiesInstancesHistory('pi1'), args: [`${BASE}/process-history/activity/by-process-instance/pi1`] },
    { name: 'findActivitiesProcessDefinitionHistory', verb: 'get', call: () => HistoryService.findActivitiesProcessDefinitionHistory('pd1', { unfinished: true }), args: [`${BASE}/process-history/activity/by-process-definition/pd1`, { params: { unfinished: true } }] },
    { name: 'findActivitiesInstancesHistoryWithFilter', verb: 'get', call: () => HistoryService.findActivitiesInstancesHistoryWithFilter({ processInstanceId: 'pi1' }), args: [`${BASE}/process-history/activity`, { params: { processInstanceId: 'pi1' } }] },
    { name: 'fetchProcessInstanceVariablesHistory', verb: 'get', call: () => HistoryService.fetchProcessInstanceVariablesHistory('pi1', { deserializeValues: false }), args: [`${BASE}/process-history/instance/by-process-instance/pi1/variables`, { params: { deserializeValues: false } }] },
    { name: 'fetchHistoryVariableDataById requests a blob', verb: 'get', call: () => HistoryService.fetchHistoryVariableDataById('v1'), args: [`${BASE}/process-history/variable/v1/data`, { responseType: 'blob' }] },
    { name: 'findProcessInstance', verb: 'get', call: () => HistoryService.findProcessInstance('pi1'), args: [`${BASE}/process-history/instance/pi1`] },
    { name: 'findTasksByTaskIdHistory', verb: 'get', call: () => HistoryService.findTasksByTaskIdHistory('t1'), args: [`${BASE}/task-history/by-task-id/t1`] },
    { name: 'deleteProcessInstanceFromHistory', verb: 'delete', call: () => HistoryService.deleteProcessInstanceFromHistory('pi1'), args: [`${BASE}/process-history/instance/pi1`] },
    { name: 'deleteVariableHistoryInstance', verb: 'delete', call: () => HistoryService.deleteVariableHistoryInstance('pi1'), args: [`${BASE}/process-history/instance/pi1/variables`] },
    { name: 'findHistoryActivityStatistics', verb: 'get', call: () => HistoryService.findHistoryActivityStatistics('pd1', { incidents: true }), args: [`${BASE}/process-history/process-definition/pd1/statistics`, { params: { incidents: true } }] },
    { name: 'findProcessesInstancesHistory sends paging params', verb: 'post', call: () => HistoryService.findProcessesInstancesHistory({ a: 1 }, 0, 50), args: [`${BASE}/process-history/instance`, { a: 1 }, { params: { firstResult: 0, maxResults: 50 } }] },
    { name: 'findProcessesInstancesHistory omits absent paging params', verb: 'post', call: () => HistoryService.findProcessesInstancesHistory({ a: 1 }), args: [`${BASE}/process-history/instance`, { a: 1 }, { params: {} }] }
  ])
})

describe('HistoryService.findProcessesInstancesHistoryById', () => {
  const body = () => axios.post.mock.calls[0][1]
  const config = () => axios.post.mock.calls[0][2]

  it('should post to the instance history endpoint with the definition id in the body', () => {
    HistoryService.findProcessesInstancesHistoryById('pd1')

    expect(axios.post).toHaveBeenCalledWith(`${BASE}/process-history/instance`, expect.any(Object), expect.any(Object))
    expect(body().processDefinitionId).toBe('pd1')
  })

  it('should default to sorting by start time descending', () => {
    HistoryService.findProcessesInstancesHistoryById('pd1')

    expect(body().sorting).toEqual([{ sortBy: 'startTime', sortOrder: 'desc' }])
  })

  it('should map caller sorting criteria onto the engine field names', () => {
    HistoryService.findProcessesInstancesHistoryById('pd1', null, null, {}, null, [
      { field: 'businessKey', order: 'asc' },
      { field: 'startTime', order: 'desc' }
    ])

    expect(body().sorting).toEqual([
      { sortBy: 'businessKey', sortOrder: 'asc' },
      { sortBy: 'startTime', sortOrder: 'desc' }
    ])
  })

  it('should merge the caller filter into the request body', () => {
    HistoryService.findProcessesInstancesHistoryById('pd1', null, null, { businessKey: 'ORDER-1' })

    expect(body()).toMatchObject({ businessKey: 'ORDER-1', processDefinitionId: 'pd1' })
  })

  it('should tolerate a null filter', () => {
    HistoryService.findProcessesInstancesHistoryById('pd1', null, null, null)

    expect(body().processDefinitionId).toBe('pd1')
  })

  it('should request incidents only when asked', () => {
    HistoryService.findProcessesInstancesHistoryById('pd1', null, null, {}, null, [], true)
    expect(body().fetchIncidents).toBe(true)

    resetAxiosMock(axios)
    HistoryService.findProcessesInstancesHistoryById('pd1')
    expect(body()).not.toHaveProperty('fetchIncidents')
  })

  // The free-text box searches business key *or* exact instance id, which the engine can
  // only express as an orQueries block.
  it('should turn the editField text into an orQueries block', () => {
    HistoryService.findProcessesInstancesHistoryById('pd1', null, null, { editField: '  ORDER-1  ' })

    expect(body().orQueries).toEqual([{
      processInstanceBusinessKeyLike: '%ORDER-1%',
      processInstanceId: 'ORDER-1'
    }])
  })

  it.each([[''], ['   '], [undefined]])('should not add orQueries for editField %j', (editField) => {
    HistoryService.findProcessesInstancesHistoryById('pd1', null, null, { editField })

    expect(body()).not.toHaveProperty('orQueries')
  })

  it('should translate active=true into unfinished', () => {
    HistoryService.findProcessesInstancesHistoryById('pd1', null, null, {}, true)

    expect(body().unfinished).toBe(true)
    expect(body()).not.toHaveProperty('finished')
  })

  it('should translate active=false into finished', () => {
    HistoryService.findProcessesInstancesHistoryById('pd1', null, null, {}, false)

    expect(body().finished).toBe(true)
    expect(body()).not.toHaveProperty('unfinished')
  })

  it.each([[undefined], [null]])('should add neither flag when active is %j', (active) => {
    HistoryService.findProcessesInstancesHistoryById('pd1', null, null, {}, active)

    expect(body()).not.toHaveProperty('unfinished')
    expect(body()).not.toHaveProperty('finished')
  })

  // activeOrExecutedActivityIdIn is a UI-level concept: the engine has no such filter, so
  // it is expanded into an OR over its two real equivalents and then removed.
  it('should expand activeOrExecutedActivityIdIn into an orQueries entry', () => {
    HistoryService.findProcessesInstancesHistoryById('pd1', null, null, { activeOrExecutedActivityIdIn: ['task_1'] })

    expect(body()).not.toHaveProperty('activeOrExecutedActivityIdIn')
    expect(body().orQueries).toEqual([{
      activityIdIn: ['task_1'],
      executedActivityIdIn: ['task_1']
    }])
  })

  it('should append the activity expansion to an existing text orQueries block', () => {
    HistoryService.findProcessesInstancesHistoryById('pd1', null, null, {
      editField: 'ORDER-1',
      activeOrExecutedActivityIdIn: ['task_1']
    })

    expect(body().orQueries).toEqual([
      { processInstanceBusinessKeyLike: '%ORDER-1%', processInstanceId: 'ORDER-1' },
      { activityIdIn: ['task_1'], executedActivityIdIn: ['task_1'] }
    ])
  })

  it('should send paging params when supplied', () => {
    HistoryService.findProcessesInstancesHistoryById('pd1', 0, 50)

    expect(config()).toEqual({ params: { firstResult: 0, maxResults: 50 } })
  })

  it('should omit paging params that are not supplied', () => {
    HistoryService.findProcessesInstancesHistoryById('pd1')

    expect(config()).toEqual({ params: {} })
  })
})

describe('HistoryService.findHistoryActivityStatisticsForInstances', () => {
  const body = () => axios.post.mock.calls[0][1]

  it('should post the filter to the definition statistics endpoint', () => {
    HistoryService.findHistoryActivityStatisticsForInstances('pd1', { includeIncidents: true })

    expect(axios.post).toHaveBeenCalledWith(
      `${BASE}/process-history/process-definition/pd1/statistics`,
      { includeIncidents: true }
    )
  })

  it('should tolerate a null filter', () => {
    HistoryService.findHistoryActivityStatisticsForInstances('pd1', null)

    expect(body()).toEqual({})
  })

  it('should turn the editField text into an orQueries block', () => {
    HistoryService.findHistoryActivityStatisticsForInstances('pd1', { editField: ' ORDER-1 ' })

    expect(body().orQueries).toEqual([{
      processInstanceBusinessKeyLike: '%ORDER-1%',
      processInstanceId: 'ORDER-1'
    }])
  })

  it('should translate active=true into unfinished and active=false into finished', () => {
    HistoryService.findHistoryActivityStatisticsForInstances('pd1', {}, true)
    expect(body().unfinished).toBe(true)

    resetAxiosMock(axios)
    HistoryService.findHistoryActivityStatisticsForInstances('pd1', {}, false)
    expect(body().finished).toBe(true)
  })

  it('should expand activeOrExecutedActivityIdIn into an orQueries entry', () => {
    HistoryService.findHistoryActivityStatisticsForInstances('pd1', { activeOrExecutedActivityIdIn: ['task_1'] })

    expect(body()).not.toHaveProperty('activeOrExecutedActivityIdIn')
    expect(body().orQueries).toEqual([{ activityIdIn: ['task_1'], executedActivityIdIn: ['task_1'] }])
  })

  it('should append the activity expansion to an existing text orQueries block', () => {
    HistoryService.findHistoryActivityStatisticsForInstances('pd1', {
      editField: 'ORDER-1',
      activeOrExecutedActivityIdIn: ['task_1']
    })

    expect(body().orQueries).toHaveLength(2)
  })
})

describe('DeploymentService.createDeployment', () => {
  const body = () => axios.post.mock.calls[0][1]

  it('should post multipart form data to the create endpoint', () => {
    DeploymentService.createDeployment({ 'deployment-name': 'invoice' }, new File(['x'], 'invoice.bpmn'))

    expect(axios.post).toHaveBeenCalledWith(
      `${BASE}/deployment/create`,
      expect.any(FormData),
      { headers: { 'Content-Type': 'multipart/form-data' } }
    )
    expect(body().get('deployment-name')).toBe('invoice')
  })

  it('should skip falsy data entries', () => {
    DeploymentService.createDeployment({ a: 'kept', b: '', c: null, d: false }, null)

    expect(body().get('a')).toBe('kept')
    expect(body().has('b')).toBe(false)
    expect(body().has('c')).toBe(false)
    expect(body().has('d')).toBe(false)
  })

  it('should accept an array of files under a single field name', () => {
    const files = [new File(['x'], 'a.bpmn'), new File(['y'], 'b.dmn')]

    DeploymentService.createDeployment({}, files)

    expect(body().getAll('files')).toEqual(files)
  })

  it('should ignore array entries that are not files or blobs', () => {
    DeploymentService.createDeployment({}, ['not-a-file', new File(['x'], 'a.bpmn')])

    expect(body().getAll('files')).toHaveLength(1)
  })

  it('should accept a single Blob', () => {
    DeploymentService.createDeployment({}, new Blob(['x']))

    expect(body().getAll('files')).toHaveLength(1)
  })

  it('should tolerate no data and no files', () => {
    DeploymentService.createDeployment()

    expect(body().has('files')).toBe(false)
  })
})

// These endpoints are reachable before login and are engine-scoped, so they select the
// engine with a header and deliberately bypass the app's axios interceptors.
describe('engine-scoped, interceptor-free endpoints', () => {
  const withEngine = (name) => localStorage.setItem(ENGINE_STORAGE_KEY, name)

  it('InfoService.getProperties should send the engine header when one is selected', () => {
    withEngine('second-engine')

    InfoService.getProperties()

    expect(axios.get).toHaveBeenCalledWith('info/properties', { headers: { 'X-Process-Engine': 'second-engine' } })
  })

  it('InfoService.getProperties should send no headers when no engine is selected', () => {
    InfoService.getProperties()

    expect(axios.get).toHaveBeenCalledWith('info/properties', { headers: {} })
  })

  it('SetupService.getStatus should use a fresh instance and the engine header', () => {
    withEngine('second-engine')

    SetupService.getStatus()

    expect(axios.create).toHaveBeenCalled()
    expect(axios.get).toHaveBeenCalledWith(`${BASE}/setup/status`, { headers: { 'X-Process-Engine': 'second-engine' } })
  })

  it('SetupService.getStatus should send no headers when no engine is selected', () => {
    SetupService.getStatus()

    expect(axios.get).toHaveBeenCalledWith(`${BASE}/setup/status`, { headers: {} })
  })

  it('SetupService.createInitialUser should post the user with the engine header', () => {
    withEngine('second-engine')

    SetupService.createInitialUser({ id: 'admin' })

    expect(axios.create).toHaveBeenCalled()
    expect(axios.post).toHaveBeenCalledWith(`${BASE}/setup/user`, { id: 'admin' }, { headers: { 'X-Process-Engine': 'second-engine' } })
  })

  it('SetupService.createInitialUser should send no headers when no engine is selected', () => {
    SetupService.createInitialUser({ id: 'admin' })

    expect(axios.post).toHaveBeenCalledWith(`${BASE}/setup/user`, { id: 'admin' }, { headers: {} })
  })

  it('SetupService.validatePasswordPolicy should wrap password and profile in the body', () => {
    withEngine('second-engine')

    SetupService.validatePasswordPolicy('secret', { id: 'admin' })

    expect(axios.post).toHaveBeenCalledWith(
      `${BASE}/setup/validate-password`,
      { password: 'secret', profile: { id: 'admin' } },
      { headers: { 'X-Process-Engine': 'second-engine' } }
    )
  })

  it('SetupService.validatePasswordPolicy should send no headers when no engine is selected', () => {
    SetupService.validatePasswordPolicy('secret', { id: 'admin' })

    expect(axios.post).toHaveBeenCalledWith(
      `${BASE}/setup/validate-password`,
      { password: 'secret', profile: { id: 'admin' } },
      { headers: {} }
    )
  })
})

// The document endpoint is handed to an <img>/<iframe> rather than fetched through axios,
// so it has to be an absolute URL that survives the deployment context path.
describe('createDocumentEndpointUrl', () => {
  let originalHref
  let originalHash

  beforeEach(() => {
    originalHref = window.location.href
    originalHash = window.location.hash
  })

  afterEach(() => {
    if (window.location.href !== originalHref) {
      Object.defineProperty(window, 'location', {
        value: { ...window.location, href: originalHref, hash: originalHash },
        writable: true,
        configurable: true
      })
    }
  })

  const stubLocation = (href, hash) => {
    Object.defineProperty(window, 'location', {
      value: { href, hash },
      writable: true,
      configurable: true
    })
  }

  it('should build an absolute URL from the current location with the hash stripped', () => {
    stubLocation('http://localhost:8080/webapp/#/seven/auth/process', '#/seven/auth/process')

    const url = createDocumentEndpointUrl('pi1', 'invoice', 'tok', 'application/pdf', 42)

    expect(url).toBe(
      'http://localhost:8080/webapp/' + BASE +
      '/process/process-instance/pi1/variables/invoice/data?token=tok&contentType=application%2Fpdf&cacheBust=42'
    )
  })

  it('should preserve a deployment context path', () => {
    stubLocation('https://example.com/cibseven/ui/#/x', '#/x')

    const url = createDocumentEndpointUrl('pi1', 'invoice', 'tok', 'text/plain', 1)

    expect(url).toContain('https://example.com/cibseven/ui/')
  })

  it('should url-encode the content type', () => {
    stubLocation('http://localhost/', '')

    const url = createDocumentEndpointUrl('pi1', 'invoice', 'tok', 'image/svg+xml', 1)

    expect(url).toContain('contentType=image%2Fsvg%2Bxml')
  })
})
