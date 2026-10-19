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
import { describe, it, expect, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import TaskVariables from '@/components/render-template/TaskVariables.vue'
import { ProcessService } from '@/services.js'

vi.mock('@/services.js', () => ({
  FormsService: { fetchVariables: vi.fn(() => Promise.resolve({})) },
  ProcessService: {
    findProcessInstance: vi.fn(() => Promise.resolve({})),
    fetchVariableDataByExecutionId: vi.fn(() => Promise.resolve(new Blob(['x'])))
  }
}))

describe('TaskVariables downloadFile', () => {
  function context() {
    return {
      task: { executionId: 'ex1' },
      $refs: { importPopper: { triggerDownload: vi.fn() } },
      getFileVariableName: TaskVariables.methods.getFileVariableName
    }
  }

  it('downloads File variables using valueInfo.filename', async () => {
    const vm = context()
    TaskVariables.methods.downloadFile.call(vm, { name: 'doc', type: 'File', valueInfo: { filename: 'doc.txt' } })
    await flushPromises()
    expect(ProcessService.fetchVariableDataByExecutionId).toHaveBeenCalledWith('ex1', 'doc')
    expect(vm.$refs.importPopper.triggerDownload).toHaveBeenCalledWith(expect.any(Blob), 'doc.txt')
  })

  // 'Bytes' variables have no valueInfo.filename (CIB7-2132), so the download must fall
  // back to the variable name (plus a generic extension) instead of passing 'undefined'
  // as the file name
  it('falls back to the variable name for Bytes variables, which have no valueInfo.filename', async () => {
    const vm = context()
    TaskVariables.methods.downloadFile.call(vm, { name: 'atisData', type: 'Bytes', valueInfo: {} })
    await flushPromises()
    expect(ProcessService.fetchVariableDataByExecutionId).toHaveBeenCalledWith('ex1', 'atisData')
    expect(vm.$refs.importPopper.triggerDownload).toHaveBeenCalledWith(expect.any(Blob), 'atisData.dat')
  })
})

describe('TaskVariables displayValueTooltip', () => {
  function context() {
    return {
      $t: key => key,
      isDownloadable: TaskVariables.methods.isDownloadable,
      displayVariableValue: TaskVariables.methods.displayVariableValue,
    }
  }

  // isDownloadable() (not the File-only isFile()) must gate the "download" label, otherwise
  // a Bytes variable shows a download icon/click action without a matching tooltip (CIB7-2132)
  it('prefixes the download label for downloadable existing variables, including Bytes', () => {
    const vm = context()
    const variable = { name: 'atisData', type: 'Bytes', existing: true, valueInfo: {} }
    expect(TaskVariables.methods.displayValueTooltip.call(vm, variable)).toBe('process-instance.download: atisData.dat')
  })

  it('shows the plain value for non-downloadable variables', () => {
    const vm = context()
    const variable = { type: 'String', value: 'hello', existing: true }
    expect(TaskVariables.methods.displayValueTooltip.call(vm, variable)).toBe('hello')
  })

  it('shows the plain value for a downloadable variable that no longer exists', () => {
    const vm = context()
    const variable = { type: 'File', existing: false, valueInfo: { filename: 'doc.txt' } }
    expect(TaskVariables.methods.displayValueTooltip.call(vm, variable)).toBe('doc.txt')
  })
})
