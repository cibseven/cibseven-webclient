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
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { redirectToProcessDefinition, redirectToProcessInstance, redirectToTask } from '@/utils/redirects.js'
import { HistoryService, ProcessService } from '@/services.js'

vi.mock('@/services.js', () => ({
  HistoryService: { findProcessInstance: vi.fn() },
  ProcessService: { findProcessInstance: vi.fn(), findProcessById: vi.fn() }
}))

const route = (overrides = {}) => ({ params: {}, query: {}, fullPath: '/from', ...overrides })
const routerWith = (camundaHistoryLevel) => ({ root: { config: { camundaHistoryLevel } } })

beforeEach(() => {
  vi.clearAllMocks()
})

describe('redirectToProcessDefinition', () => {
  it('should resolve a definition id to its key and version', async () => {
    ProcessService.findProcessById.mockResolvedValue({ key: 'invoice', version: 3, tenantId: null })

    const target = await redirectToProcessDefinition(route({ params: { definitionId: 'pd1' } }), route())

    expect(ProcessService.findProcessById).toHaveBeenCalledWith('pd1', false)
    expect(target).toEqual({
      name: 'process',
      params: { processKey: 'invoice', versionIndex: 3 },
      query: { tab: 'instances' }
    })
  })

  it('should default the tab to instances and keep other query params', async () => {
    ProcessService.findProcessById.mockResolvedValue({ key: 'invoice', version: 3 })

    const target = await redirectToProcessDefinition(
      route({ params: { definitionId: 'pd1' }, query: { foo: 'bar' } }), route()
    )

    expect(target.query).toEqual({ foo: 'bar', tab: 'instances' })
  })

  it('should keep an explicitly requested tab', async () => {
    ProcessService.findProcessById.mockResolvedValue({ key: 'invoice', version: 3 })

    const target = await redirectToProcessDefinition(
      route({ params: { definitionId: 'pd1' }, query: { tab: 'incidents' } }), route()
    )

    expect(target.query.tab).toBe('incidents')
  })

  it('should carry the tenant into the query when the definition has one', async () => {
    ProcessService.findProcessById.mockResolvedValue({ key: 'invoice', version: 3, tenantId: 't1' })

    const target = await redirectToProcessDefinition(route({ params: { definitionId: 'pd1' } }), route())

    expect(target.query.tenantId).toBe('t1')
  })

  // An unknown or inaccessible definition must land on the not-found page, carrying the id
  // and where the user came from so the page can offer a way back.
  it('should fall back to the not-found route when the lookup fails', async () => {
    ProcessService.findProcessById.mockRejectedValue(new Error('404'))

    const target = await redirectToProcessDefinition(
      route({ params: { definitionId: 'missing' } }), route({ fullPath: '/seven/auth/start' })
    )

    expect(target).toEqual({
      name: 'not-found-definitionId',
      query: { definitionId: 'missing', refPath: '/seven/auth/start' }
    })
  })
})

describe('redirectToProcessInstance', () => {
  it('should resolve the instance from history when history is enabled', async () => {
    HistoryService.findProcessInstance.mockResolvedValue({
      processDefinitionKey: 'invoice',
      processDefinitionVersion: 2,
      tenantId: null
    })

    const target = await redirectToProcessInstance(
      routerWith('full'), route({ params: { instanceId: 'pi1' } }), route()
    )

    expect(HistoryService.findProcessInstance).toHaveBeenCalledWith('pi1')
    expect(ProcessService.findProcessInstance).not.toHaveBeenCalled()
    expect(target).toEqual({
      name: 'process',
      params: { processKey: 'invoice', versionIndex: 2, instanceId: 'pi1' },
      query: { tab: 'variables' }
    })
  })

  it('should default the history level to full when the config omits it', async () => {
    HistoryService.findProcessInstance.mockResolvedValue({ processDefinitionKey: 'invoice', processDefinitionVersion: 1 })

    await redirectToProcessInstance(routerWith(undefined), route({ params: { instanceId: 'pi1' } }), route())

    expect(HistoryService.findProcessInstance).toHaveBeenCalled()
  })

  // With history switched off the instance has to be looked up in the runtime API, which
  // needs a second call to turn the definition id into a key and version.
  it('should resolve the instance from the runtime API when history is off', async () => {
    ProcessService.findProcessInstance.mockResolvedValue({ definitionId: 'pd1', tenantId: 't1' })
    ProcessService.findProcessById.mockResolvedValue({ key: 'invoice', version: 4 })

    const target = await redirectToProcessInstance(
      routerWith('none'), route({ params: { instanceId: 'pi1' } }), route()
    )

    expect(ProcessService.findProcessInstance).toHaveBeenCalledWith('pi1')
    expect(ProcessService.findProcessById).toHaveBeenCalledWith('pd1', false)
    expect(HistoryService.findProcessInstance).not.toHaveBeenCalled()
    expect(target.params).toEqual({ processKey: 'invoice', versionIndex: 4, instanceId: 'pi1' })
    expect(target.query.tenantId).toBe('t1')
  })

  it('should default the tab to variables and keep other query params', async () => {
    HistoryService.findProcessInstance.mockResolvedValue({ processDefinitionKey: 'invoice', processDefinitionVersion: 1 })

    const target = await redirectToProcessInstance(
      routerWith('full'), route({ params: { instanceId: 'pi1' }, query: { foo: 'bar' } }), route()
    )

    expect(target.query).toEqual({ foo: 'bar', tab: 'variables' })
  })

  it('should keep an explicitly requested tab', async () => {
    HistoryService.findProcessInstance.mockResolvedValue({ processDefinitionKey: 'invoice', processDefinitionVersion: 1 })

    const target = await redirectToProcessInstance(
      routerWith('full'), route({ params: { instanceId: 'pi1' }, query: { tab: 'incidents' } }), route()
    )

    expect(target.query.tab).toBe('incidents')
  })

  it('should fall back to the not-found route when the history lookup fails', async () => {
    HistoryService.findProcessInstance.mockRejectedValue(new Error('404'))

    const target = await redirectToProcessInstance(
      routerWith('full'), route({ params: { instanceId: 'missing' } }), route({ fullPath: '/seven/auth/start' })
    )

    expect(target).toEqual({
      name: 'not-found-instanceId',
      query: { instanceId: 'missing', refPath: '/seven/auth/start' }
    })
  })

  it('should fall back to the not-found route when the runtime definition lookup fails', async () => {
    ProcessService.findProcessInstance.mockResolvedValue({ definitionId: 'pd1' })
    ProcessService.findProcessById.mockRejectedValue(new Error('404'))

    const target = await redirectToProcessInstance(
      routerWith('none'), route({ params: { instanceId: 'pi1' } }), route()
    )

    expect(target.name).toBe('not-found-instanceId')
  })
})

describe('redirectToTask', () => {
  // The filter a task belongs to is not known at redirect time, so '-' stands in for
  // "pick one" and the task list resolves it.
  it('should route to the task list with a placeholder filter', async () => {
    const target = await redirectToTask(route({ params: { taskId: 't1' } }))

    expect(target).toEqual({
      name: 'tasklist',
      params: { filterId: '-', taskId: 't1' },
      query: {}
    })
  })

  it('should preserve the incoming query', async () => {
    const target = await redirectToTask(route({ params: { taskId: 't1' }, query: { foo: 'bar' } }))

    expect(target.query).toEqual({ foo: 'bar' })
  })
})
