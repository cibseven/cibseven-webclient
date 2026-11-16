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
import CalledProcessDefinitionsTable from '@/components/process/tables/CalledProcessDefinitionsTable.vue'

const load = CalledProcessDefinitionsTable.methods.loadCalledProcessDefinitionsData

function tableContext(loadCalledProcessDefinitions) {
  return {
    ...CalledProcessDefinitionsTable.data(),
    process: { id: 'pd-1' },
    $root: { config: {} },
    selectedActivityId: '',
    loadCalledProcessDefinitions,
    resetCalledProcessDefinitions: vi.fn(),
    filterByActivity: vi.fn()
  }
}

describe('CalledProcessDefinitionsTable - loadCalledProcessDefinitionsData', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('should load the called definitions of the displayed process', async () => {
    const vm = tableContext(vi.fn(() => Promise.resolve()))

    await load.call(vm)

    expect(vm.loadCalledProcessDefinitions).toHaveBeenCalledWith({ processId: 'pd-1', chunkSize: 50 })
    expect(vm.loading).toBe(false)
    expect(vm.loadFailed).toBe(false)
  })

  // CIB7-2717: e.g. the static definitions could not be loaded; an empty table would claim there are none
  it('should report a failed load instead of showing an empty table', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    const vm = tableContext(vi.fn(() => Promise.reject(new Error('could not be loaded'))))

    await load.call(vm)

    expect(vm.loadFailed).toBe(true)
    expect(vm.loading).toBe(false)
  })

  it('should clear an earlier failure when it loads again', async () => {
    const vm = tableContext(vi.fn(() => Promise.resolve()))
    vm.loadFailed = true

    await load.call(vm)

    expect(vm.loadFailed).toBe(false)
  })

  // Switching definitions: the older load ending must not end the newer one's spinner or flag an error
  it('should leave the flags to the latest load', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    let failOlder
    const loads = [
      new Promise((resolve, reject) => { failOlder = reject }),
      new Promise(() => {})
    ]
    const vm = tableContext(vi.fn(() => loads.shift()))

    const older = load.call(vm)
    load.call(vm)
    failOlder(new Error('outdated'))
    await older

    expect(vm.loading).toBe(true)
    expect(vm.loadFailed).toBe(false)
  })

  // The diagram of the new definition may not be ready yet, so no load would reset it
  it('should drop the previous definition\'s error when the definition changes', () => {
    const vm = { ...tableContext(vi.fn()), isDiagramReady: false, loadFailed: true }

    CalledProcessDefinitionsTable.watch['process.id'].handler.call(vm, 'pd-2')

    expect(vm.loadFailed).toBe(false)
    expect(vm.loadCalledProcessDefinitions).not.toHaveBeenCalled()
  })

  // Otherwise the previous definition's rows stay, and a load still running for them would fill or block this one.
  // The spinner shows while the new diagram loads, not "No results"
  it('should clear the previous rows, outdate their load and show the spinner when the new diagram is not ready', () => {
    const vm = { ...tableContext(vi.fn()), isDiagramReady: false, loading: true, initialLoading: false, loadRequest: 3 }

    CalledProcessDefinitionsTable.watch['process.id'].handler.call(vm, 'pd-2')

    expect(vm.resetCalledProcessDefinitions).toHaveBeenCalled()
    expect(vm.loading).toBe(false)
    expect(vm.initialLoading).toBe(true)
    expect(vm.loadRequest).toBe(4)
  })

  it('should not show the spinner without a process to load', () => {
    const vm = { ...tableContext(vi.fn()), isDiagramReady: false, initialLoading: true }

    CalledProcessDefinitionsTable.watch['process.id'].handler.call(vm, undefined, 'pd-1')

    expect(vm.initialLoading).toBe(false)
  })

  // Switching versions: the previous diagram still counts as ready, so loading now would use its statistics and names
  it('should wait for the new diagram after a definition switch even while the old one counts as ready', () => {
    const vm = { ...tableContext(vi.fn()), isDiagramReady: true }
    vm.loadCalledProcessDefinitionsData = vi.fn()

    CalledProcessDefinitionsTable.watch['process.id'].handler.call(vm, 'pd-2', 'pd-1')

    expect(vm.loadCalledProcessDefinitionsData).not.toHaveBeenCalled()
    expect(vm.resetCalledProcessDefinitions).toHaveBeenCalled()
  })

  it('should load right away when the diagram is ready', () => {
    const vm = { ...tableContext(vi.fn(() => Promise.resolve())), isDiagramReady: true }
    vm.loadCalledProcessDefinitionsData = vi.fn()

    CalledProcessDefinitionsTable.watch['process.id'].handler.call(vm, 'pd-2')

    expect(vm.loadCalledProcessDefinitionsData).toHaveBeenCalled()
    expect(vm.resetCalledProcessDefinitions).not.toHaveBeenCalled()
  })

  // e.g. a call activity link selected it while the tab was opening: the loaded rows must be narrowed too
  it('should narrow the loaded rows to an activity selected before they arrived', async () => {
    const vm = { ...tableContext(vi.fn(() => Promise.resolve())), selectedActivityId: 'call_1' }

    await load.call(vm)

    expect(vm.filterByActivity).toHaveBeenCalledWith('call_1')
  })

  it('should not narrow when no activity is selected', async () => {
    const vm = tableContext(vi.fn(() => Promise.resolve()))

    await load.call(vm)

    expect(vm.filterByActivity).not.toHaveBeenCalled()
  })

  it('should not load without a process', async () => {
    const vm = tableContext(vi.fn())
    vm.process = null

    await load.call(vm)

    expect(vm.loadCalledProcessDefinitions).not.toHaveBeenCalled()
  })
})
