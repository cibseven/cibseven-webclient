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

function createWrapper(props = {}, stubs = {}, mocks = {}) {
  const store = createStore({
    actions: { clearActivitySelection: vi.fn(), getProcessById: vi.fn(), loadStatistics: vi.fn() },
    modules: {
      instances: { namespaced: true, getters: { instances: () => [] } },
      calledProcessDefinitions: { namespaced: true, actions: { clearStaticCalledProcessDefinitions: vi.fn() } }
    }
  })
  return mountWithDefaults(ProcessDefinitionView, {
    shallow: true,
    // a test's own `$t` only takes effect without the i18n plugin
    i18nPlugin: !mocks.$t,
    props: { processKey: 'invoice', versionIndex: '2', ...props },
    global: {
      plugins: [store],
      stubs,
      // the component is the root here, so `$root.config` falls through to this global property
      mocks: { config: { camundaHistoryLevel: 'full', lazyLoadHistory: false }, $route: { query: {} }, $router: { replace: vi.fn() }, ...mocks }
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

  /**
   * Scenario: the user opens an instance link directly (a deep link, or F5); the view mounts with it.
   * Link: #/seven/auth/process/invoice/2/pi2
   * Expected: one request each for the instance, the versions and the definition; v2 and pi2 are shown.
   * Happy path: the view loads the route it is mounted with, once.
   */
  it('loads the route it is mounted with', async () => {
    const wrapper = createWrapper({ instanceId: 'pi2' })
    await flushPromises()

    expect(HistoryService.findProcessInstance).toHaveBeenCalledOnce()
    expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenCalledOnce()
    expect(ProcessService.findProcessById).toHaveBeenCalledOnce()
    expect(wrapper.vm.selectedInstance.id).toBe('pi2')
    expect(wrapper.vm.process.version).toBe('2')
  })

  /**
   * Scenario: one navigation changes the version and the instance together, e.g. an instance of another version.
   * Link: from #/seven/auth/process/invoice/2 to #/seven/auth/process/invoice/1/pi1
   * Expected: one instance lookup and one activity tree request; the versions are not reloaded.
   * Guards against: CIB7-1895, separate watchers per prop and beforeUpdate each starting their own loading.
   */
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

  /**
   * Scenario: the user opens another process, whose versions are slow.
   * Link: from #/seven/auth/process/invoice/2 to #/seven/auth/process/order/2
   * Expected: the middle pane shows the loading box until order can be shown, then ProcessInstancesView.
   * Guards against: P8, a blank middle pane while the route loads.
   */
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

  /**
   * Scenario: an instance link whose instance cannot be loaded (404), for a key without any versions.
   * Link: #/seven/auth/process/invoice/2/pi2
   * Expected: the middle pane shows only "instance not found"; the sidebar still says the definition was not found.
   * Guards against: P16, the middle pane naming the definition instead of the instance the link points to.
   */
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

  /**
   * Scenario: the instance runs on invoice v5, which is not among the versions and cannot be read (403).
   * Link: #/seven/auth/process/invoice/2/pi5
   * Expected: no ProcessInstanceView; the middle pane shows only that definition invoice:5:id of instance pi5 could
   *   not be loaded.
   * Guards against: P18, the instance drawn on v2's diagram; P26 and P35, the warning naming the URL's key, calling the
   *   definition id a key, or naming the instance's tenant as the definition's.
   */
  it('tells that the definition of the instance could not be loaded instead of showing another one', async () => {
    // pi5 runs on version 5, which is not among the versions and cannot be read
    ProcessService.findProcessById.mockRejectedValueOnce(new Error('403 forbidden'))
    const wrapper = createWrapper({ instanceId: 'pi5' }, {
      SidebarsFlow: { template: '<div><div class="left"><slot name="left"></slot></div><div class="main"><slot></slot></div></div>' }
    }, {
      // echoes the parameters too, to see which definition the message names
      $t: (key, params) => params ? `${key} ${JSON.stringify(params)}` : key
    })
    await flushPromises()

    expect(wrapper.findComponent({ name: 'ProcessInstanceView' }).exists()).toBe(false)
    const warnings = wrapper.find('.main').findAllComponents({ name: 'WarningBox' })
    expect(warnings).toHaveLength(1)
    // the definition that is missing and the instance it belongs to, not the key and tenant of the URL
    expect(warnings[0].props('message')).toBe('process.instanceDefinitionNotFound {"instanceId":"pi5","definitionId":"invoice:5:id"}')
  })

  /**
   * Scenario: an instance link whose lookup answers without an error but finds nothing; the key's versions exist.
   * Link: #/seven/auth/process/invoice/2/pi9
   * Expected: the middle pane shows only "instance not found".
   * Guards against: a blank middle pane, as neither the instance view nor a warning had anything to show.
   */
  it('shows the instance-not-found warning when the lookup finds nothing', async () => {
    HistoryService.findProcessInstance.mockResolvedValueOnce(null)
    const wrapper = createWrapper({ instanceId: 'pi9' }, {
      SidebarsFlow: { template: '<div><div class="left"><slot name="left"></slot></div><div class="main"><slot></slot></div></div>' }
    })
    await flushPromises()

    const warnings = wrapper.find('.main').findAllComponents({ name: 'WarningBox' })
    expect(warnings).toHaveLength(1)
    expect(warnings[0].props('message')).toBe('process.instanceNotFound')
  })

  /**
   * Scenario: the view re-renders without a route change, e.g. when the instances filter changes.
   * Link: #/seven/auth/process/invoice/2/pi2, unchanged
   * Expected: no request.
   * Guards against: CIB7-1895, beforeUpdate reloading the instance and the definition on every re-render.
   */
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
