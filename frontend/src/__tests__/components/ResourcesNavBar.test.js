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
import { describe, it, expect, vi, afterEach } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import ResourcesNavBar from '@/components/deployment/ResourcesNavBar.vue'
import { ProcessService } from '@/services.js'
import { mountWithDefaults } from '../support/mountWithDefaults.js'

vi.mock('@/services.js', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    ProcessService: {
      findDeployment: vi.fn(() => Promise.resolve({
        id: 'd1', name: 'Dep1', tenantId: 't1', source: 'src', deploymentTime: '2026-01-01T00:00:00Z'
      })),
      findProcessesWithFilters: vi.fn(() => Promise.resolve([{ id: 'pd1' }])),
      fetchDiagram: vi.fn()
    }
  }
})

const m = ResourcesNavBar.methods

/**
 * A `this` for ResourcesNavBar's own methods, without mounting. `openModeler` is the only
 * method under test here; the diagram viewer/download paths are exercised by mounting below.
 */
function context(overrides = {}) {
  const vm = {
    deployment: { id: 'd1' },
    getDecisionList: vi.fn(() => Promise.resolve([{ id: 'dd1' }])),
    $router: { push: vi.fn() },
    ...overrides
  }
  for (const [name, method] of Object.entries(m)) {
    if (!(name in vm)) vm[name] = method.bind(vm)
  }
  return vm
}

describe('ResourcesNavBar.vue', () => {
  describe('openModeler (method)', () => {
    afterEach(() => vi.clearAllMocks())

    it('opens a bpmn resource by looking up its process definition id', async () => {
      const vm = context()
      await vm.openModeler({ name: 'invoice.bpmn' })
      expect(ProcessService.findProcessesWithFilters).toHaveBeenCalledWith('deploymentId=d1&resourceName=invoice.bpmn')
      expect(vm.$router.push).toHaveBeenCalledWith({ name: 'modeler', query: { processId: 'pd1', type: 'bpmn' } })
    })

    it('opens a dmn resource by looking up its decision definition id', async () => {
      const vm = context()
      await vm.openModeler({ name: 'rules.dmn' })
      expect(vm.getDecisionList).toHaveBeenCalledWith({ deploymentId: 'd1', resourceName: 'rules.dmn' })
      expect(vm.$router.push).toHaveBeenCalledWith({ name: 'modeler', query: { processId: 'dd1', type: 'dmn' } })
    })

    it('does not navigate when no matching definition is found', async () => {
      ProcessService.findProcessesWithFilters.mockResolvedValueOnce([])
      const vm = context()
      await vm.openModeler({ name: 'invoice.bpmn' })
      expect(vm.$router.push).not.toHaveBeenCalled()
    })
  })

  describe('open-in-modeler action (mounted, permission-gated)', () => {
    const resources = [{ id: 'r1', name: 'invoice.bpmn' }, { id: 'r2', name: 'rules.dmn' }]

    function createWrapper(configOverrides = {}) {
      return mountWithDefaults(ResourcesNavBar, {
        props: { resources, deploymentId: 'd1' },
        global: {
          stubs: { BpmnViewer: true, DmnViewer: true, TaskPopper: true },
          mocks: {
            $router: { push: vi.fn() },
            $store: { dispatch: vi.fn(() => Promise.resolve([{ id: 'dd1' }])) },
            // ResourcesNavBar reads `this.$root.*`; mounted standalone (no parent), $root
            // resolves to this very instance, so mocking these directly is equivalent.
            user: { id: 'u1' },
            config: { authorizationEnabled: false, modelerEnabled: true, permissions: {}, ...configOverrides }
          }
        }
      })
    }

    it('is shown for each bpmn/dmn resource when the user has modeler permission', async () => {
      const wrapper = createWrapper()
      await flushPromises()

      const buttons = wrapper.findAll('[title="process.openModeler"]')
      expect(buttons).toHaveLength(2)

      await buttons[0].trigger('click')
      await flushPromises()
      expect(wrapper.vm.$router.push).toHaveBeenCalledWith({ name: 'modeler', query: { processId: 'pd1', type: 'bpmn' } })

      await buttons[1].trigger('click')
      await flushPromises()
      expect(wrapper.vm.$router.push).toHaveBeenCalledWith({ name: 'modeler', query: { processId: 'dd1', type: 'dmn' } })
    })

    it('is hidden when the user lacks modeler permission', async () => {
      const wrapper = createWrapper({ authorizationEnabled: true, permissions: { modeler: undefined } })
      await flushPromises()

      expect(wrapper.findAll('[title="process.openModeler"]')).toHaveLength(0)
    })
  })
})
