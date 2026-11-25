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
import { flushPromises } from '@vue/test-utils'
import { createStore } from 'vuex'
import ProcessDefinitionView from '@/components/process/ProcessDefinitionView.vue'
import { ProcessService, HistoryService } from '@/services.js'
import { mountWithDefaults } from '../support/mountWithDefaults.js'
import { BWaitingBox } from '@cib/common-frontend'

vi.mock('@/services.js', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    ProcessService: {
      findProcessById: vi.fn(async (id) => ({ id, key: 'invoice', version: id.split(':')[1] })),
      findProcessVersionsByDefinitionKey: vi.fn(async (key) => ['2', '1'].map(v => ({ id: `${key}:${v}:id`, key, version: v }))),
      findActivityInstance: vi.fn(async () => ({}))
    },
    HistoryService: {
      // the instance 'piN' belongs to version N
      findProcessInstance: vi.fn(async (id) => ({ id, processDefinitionId: `invoice:${id.at(-1)}:id`, state: 'ACTIVE' })),
      findActivitiesInstancesHistory: vi.fn(async () => [])
    }
  }
})

function createWrapper(props = {}, stubs = {}) {
  const store = createStore({
    actions: { clearActivitySelection: vi.fn(), getProcessById: vi.fn(), loadStatistics: vi.fn() },
    modules: {
      instances: { namespaced: true, getters: { instances: () => [] } },
      calledProcessDefinitions: { namespaced: true, actions: { clearStaticCalledProcessDefinitions: vi.fn() } }
    }
  })
  return mountWithDefaults(ProcessDefinitionView, {
    shallow: true,
    props: { processKey: 'invoice', versionIndex: '2', ...props },
    global: {
      plugins: [store],
      stubs,
      // the component is the root here, so `$root.config` falls through to this global property
      mocks: { config: { camundaHistoryLevel: 'full', lazyLoadHistory: false }, $route: { query: {} }, $router: { replace: vi.fn() } }
    }
  })
}

beforeEach(() => {
  vi.clearAllMocks()
  localStorage.clear()
})

describe('ProcessDefinitionView - mounted', () => {
  it('updates leftOpen and persists it under the current sidebar scope', () => {
    const saveLeftOpen = vi.fn()
    const ctx = { saveLeftOpen, sidebarScope: 'process-instance' }
    ProcessDefinitionView.methods.onLeftOpenChanged.call(ctx, false)
    expect(ctx.leftOpen).toBe(false)
    expect(saveLeftOpen).toHaveBeenCalledWith('process-instance', false)
  })

  it('loads the route it is mounted with', async () => {
    const wrapper = createWrapper({ instanceId: 'pi2' })
    await flushPromises()

    expect(HistoryService.findProcessInstance).toHaveBeenCalledOnce()
    expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenCalledOnce()
    expect(ProcessService.findProcessById).toHaveBeenCalledOnce()
    expect(wrapper.vm.selectedInstance.id).toBe('pi2')
    expect(wrapper.vm.process.version).toBe('2')
  })

  // CIB7-1895: separate watchers per prop and beforeUpdate each started their own loading
  it('handles a navigation that changes several route props once', async () => {
    const wrapper = createWrapper()
    await flushPromises()
    vi.clearAllMocks()

    await wrapper.setProps({ versionIndex: '1', instanceId: 'pi1' })
    await flushPromises()

    expect(HistoryService.findProcessInstance).toHaveBeenCalledOnce()
    expect(ProcessService.findActivityInstance).toHaveBeenCalledOnce()
    expect(ProcessService.findProcessVersionsByDefinitionKey).not.toHaveBeenCalled()
  })

  // the middle pane stayed blank while the definition of a newly opened key was loading
  it('shows the loading box until the definition can be shown', async () => {
    // the shallow SidebarsFlow stub would drop what it holds in its default slot
    const wrapper = createWrapper({}, { SidebarsFlow: { template: '<div><slot name="left"></slot><slot></slot></div>' } })
    await flushPromises()
    expect(wrapper.findComponent(BWaitingBox).exists()).toBe(false)
    expect(wrapper.findComponent({ name: 'ProcessInstancesView' }).exists()).toBe(true)

    let resolveVersions
    ProcessService.findProcessVersionsByDefinitionKey.mockReturnValueOnce(new Promise(r => { resolveVersions = r }))
    await wrapper.setProps({ processKey: 'order' })
    expect(wrapper.vm.loading).toBe(true)
    expect(wrapper.findComponent(BWaitingBox).exists()).toBe(true)

    resolveVersions(['2', '1'].map(v => ({ id: `order:${v}:id`, key: 'order', version: v })))
    await flushPromises()
    expect(wrapper.findComponent(BWaitingBox).exists()).toBe(false)
    expect(wrapper.findComponent({ name: 'ProcessInstancesView' }).exists()).toBe(true)
  })

  // with both the instance and the definitions missing, the middle pane names the instance the link points to
  it('shows only the instance-not-found warning in the middle pane when the definitions are missing too', async () => {
    HistoryService.findProcessInstance.mockRejectedValueOnce(new Error('404 not found'))
    ProcessService.findProcessVersionsByDefinitionKey.mockResolvedValueOnce([])
    const wrapper = createWrapper({ instanceId: 'pi2' }, {
      SidebarsFlow: { template: '<div><div class="left"><slot name="left"></slot></div><div class="main"><slot></slot></div></div>' }
    })
    await flushPromises()

    const warnings = wrapper.find('.main').findAllComponents({ name: 'WarningBox' })
    expect(warnings).toHaveLength(1)
    expect(warnings[0].props('message')).toBe('process.instanceNotFound')
    // the sidebar still tells that the definition is missing
    expect(wrapper.find('.left').findAllComponents({ name: 'WarningBox' }).map(w => w.props('message')))
      .toContain('process.definitionNotFound')
  })

  // the instance used to be drawn on the URL's version when its own definition could not be read
  it('tells that the definition of the instance could not be loaded instead of showing another one', async () => {
    // pi5 runs on version 5, which is not among the versions and cannot be read
    ProcessService.findProcessById.mockRejectedValueOnce(new Error('403 forbidden'))
    const wrapper = createWrapper({ instanceId: 'pi5' }, {
      SidebarsFlow: { template: '<div><div class="left"><slot name="left"></slot></div><div class="main"><slot></slot></div></div>' }
    })
    await flushPromises()

    expect(wrapper.findComponent({ name: 'ProcessInstanceView' }).exists()).toBe(false)
    const warnings = wrapper.find('.main').findAllComponents({ name: 'WarningBox' })
    expect(warnings).toHaveLength(1)
    expect(warnings[0].props('message')).toBe('process.definitionNotFound')
  })

  it('does not load anything again on a re-render', async () => {
    const wrapper = createWrapper({ instanceId: 'pi2' })
    await flushPromises()
    vi.clearAllMocks()

    wrapper.vm.filter = { finished: true }
    await flushPromises()

    expect(HistoryService.findProcessInstance).not.toHaveBeenCalled()
    expect(ProcessService.findProcessById).not.toHaveBeenCalled()
  })
})
