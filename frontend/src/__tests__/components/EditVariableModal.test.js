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
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import EditVariableModal from '@/components/process/modals/EditVariableModal.vue'
import { VariableInstanceService, HistoricVariableInstanceService } from '@/services.js'

vi.mock('@/services.js', () => ({
  ProcessService: {
    modifyVariableByExecutionId: vi.fn(() => Promise.resolve()),
    modifyVariableDataByExecutionId: vi.fn(() => Promise.resolve())
  },
  VariableInstanceService: {
    getVariableInstance: vi.fn(() => Promise.resolve({ name: 'var', type: 'String', value: 'runtime', executionId: 'ex1' })),
    uploadFile: vi.fn(() => Promise.resolve())
  },
  HistoricVariableInstanceService: {
    getHistoricVariableInstance: vi.fn(() => Promise.resolve({ name: 'var', type: 'String', value: 'historic' }))
  }
}))

const AddVariableModalUIStub = {
  template: '<div></div>',
  props: ['disabled'],
  methods: {
    show: vi.fn(),
    hide: vi.fn()
  }
}

function createWrapper(props = {}) {
  const wrapper = mount(EditVariableModal, {
    props,
    global: {
      stubs: { AddVariableModalUI: AddVariableModalUIStub },
      mocks: { $t: (msg) => msg }
    }
  })
  wrapper.vm.$root.config = { camundaHistoryLevel: 'full' }
  return wrapper
}

describe('EditVariableModal.vue', () => {

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('fetches the runtime variable by default (historic prop false)', async () => {
    const wrapper = createWrapper()
    await wrapper.vm.show('v1', 'var')
    await flushPromises()
    expect(VariableInstanceService.getVariableInstance).toHaveBeenCalledWith('v1', false)
    expect(HistoricVariableInstanceService.getHistoricVariableInstance).not.toHaveBeenCalled()
    expect(wrapper.vm.effectiveHistoric).toBe(false)
    expect(wrapper.vm.executionId).toBe('ex1')
  })

  it('fetches the historic variable when the historic prop is set', async () => {
    const wrapper = createWrapper({ historic: true })
    await wrapper.vm.show('v1', 'var')
    await flushPromises()
    expect(HistoricVariableInstanceService.getHistoricVariableInstance).toHaveBeenCalledWith('v1', false)
    expect(VariableInstanceService.getVariableInstance).not.toHaveBeenCalled()
    expect(wrapper.vm.effectiveHistoric).toBe(true)
  })

  it('per-call override show(id, name, true) forces historic fetch and read-only mode despite historic prop false', async () => {
    const wrapper = createWrapper({ historic: false })
    await wrapper.vm.show('v1', 'var', true)
    await flushPromises()
    expect(HistoricVariableInstanceService.getHistoricVariableInstance).toHaveBeenCalledWith('v1', false)
    expect(VariableInstanceService.getVariableInstance).not.toHaveBeenCalled()
    expect(wrapper.vm.effectiveHistoric).toBe(true)
  })

  it('per-call override show(id, name, false) forces runtime fetch despite historic prop true', async () => {
    const wrapper = createWrapper({ historic: true })
    await wrapper.vm.show('v1', 'var', false)
    await flushPromises()
    expect(VariableInstanceService.getVariableInstance).toHaveBeenCalledWith('v1', false)
    expect(HistoricVariableInstanceService.getHistoricVariableInstance).not.toHaveBeenCalled()
    expect(wrapper.vm.effectiveHistoric).toBe(false)
  })
})
