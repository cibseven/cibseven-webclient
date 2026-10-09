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
import VariablesTable from '@/components/process/tables/VariablesTable.vue'
import variableUtils from '@/components/process/mixins/variableUtils.js'
import { permissionsMixin } from '@/permissions.js'

describe('VariablesTable', () => {
  describe('hasDeepLinks', () => {
    it('returns true only when a button-type processInstance deep link is configured', () => {
      const tabOnly = { $root: { config: { deepLinks: { processInstance: [{ id: 'tabLink', url: 'https://external.example', type: 'tab' }] } } } }
      const buttonLink = { $root: { config: { deepLinks: { processInstance: [{ id: 'buttonLink', url: 'https://external.example', type: 'button' }] } } } }
      expect(VariablesTable.computed.hasDeepLinks.call(tabOnly)).toBe(false)
      expect(VariablesTable.computed.hasDeepLinks.call(buttonLink)).toBe(true)
    })
  })

  describe('matchedDeepLinkParams', () => {
    it('builds process instance and process definition context (with distinct tenant ids) and language params', () => {
      const context = {
        selectedInstance: { id: 'pi-1', tenantId: 'tenant-instance', businessKey: 'bk-1' },
        process: { id: 'def-1', key: 'myProcess', tenantId: 'tenant-def', version: '3', versionTag: 'v3' },
        currentLanguage: () => 'en'
      }
      expect(VariablesTable.computed.matchedDeepLinkParams.call(context)).toEqual({
        processInstanceId: 'pi-1',
        processInstanceTenantId: 'tenant-instance',
        businessKey: 'bk-1',
        processDefinitionId: 'def-1',
        processDefinitionKey: 'myProcess',
        processDefinitionVersion: '3',
        processDefinitionVersionTag: 'v3',
        processDefinitionTenantId: 'tenant-def',
        lang: 'en'
      })
    })

    it('fills fields with undefined when selectedInstance/process are missing', () => {
      const context = { selectedInstance: null, process: null, currentLanguage: () => 'en' }
      expect(VariablesTable.computed.matchedDeepLinkParams.call(context)).toEqual({
        processInstanceId: undefined,
        processInstanceTenantId: undefined,
        businessKey: undefined,
        processDefinitionId: undefined,
        processDefinitionKey: undefined,
        processDefinitionVersion: undefined,
        processDefinitionVersionTag: undefined,
        processDefinitionTenantId: undefined,
        lang: 'en'
      })
    })
  })

  describe('uploadFileClicked', () => {
    const context = (uploadResult) => ({
      uploadError: 'stale',
      uploadFile: vi.fn(() => Promise.resolve(uploadResult)),
      $refs: { uploadFile: { hide: vi.fn() }, success: { show: vi.fn() } },
    })

    it('closes the dialog and shows the success alert when the upload succeeds', async () => {
      const vm = context(true)
      await VariablesTable.methods.uploadFileClicked.call(vm)
      expect(vm.uploadError).toBeNull()
      expect(vm.$refs.uploadFile.hide).toHaveBeenCalled()
      expect(vm.$refs.success.show).toHaveBeenCalled()
    })

    it('keeps the dialog open and exposes the error message when the upload fails', async () => {
      const vm = context('Request failed with status code 500')
      await VariablesTable.methods.uploadFileClicked.call(vm)
      expect(vm.uploadError).toBe('Request failed with status code 500')
      expect(vm.$refs.uploadFile.hide).not.toHaveBeenCalled()
      expect(vm.$refs.success.show).not.toHaveBeenCalled()
    })
  })

  describe('variable action buttons', () => {
    const FILE_OBJECT = 'org.example.FileValueDataSource'
    const live = (extra) => ({ name: 'v', isLive: true, ...extra })
    const historic = (extra) => ({ name: 'v', isLive: false, ...extra })
    const string = { type: 'String', value: 'x' }
    const file = { type: 'File' }
    const bytes = { type: 'Bytes' }
    const fileDataSource = { type: 'Object', valueInfo: { objectTypeName: FILE_OBJECT } }

    const context = (overrides = {}) => {
      const vm = {
        ...variableUtils,
        getFileObjects: () => [FILE_OBJECT],
        processByPermissions: vi.fn(() => true),
        canDeleteHistoryProcessInstance: vi.fn(() => true),
        canUpdateVariables: true,
        processDefinitionKey: 'proc-key',
        selectedInstance: { id: 'pi-1' },
        ...overrides,
      }
      for (const name of ['hasEditVariableButton', 'hasViewVariableButton', 'hasDownloadVariableButton', 'hasUploadVariableButton', 'hasDeleteVariableButton']) {
        vm[name] = VariablesTable.methods[name].bind(vm)
      }
      return vm
    }
    const buttons = (variable, overrides) => {
      const vm = context(overrides)
      return {
        edit: vm.hasEditVariableButton(variable),
        view: vm.hasViewVariableButton(variable),
        download: vm.hasDownloadVariableButton(variable),
        upload: vm.hasUploadVariableButton(variable),
      }
    }

    it.each([
      ['live string', live(string), { edit: true, view: false, download: false, upload: false }],
      ['historic string', historic(string), { edit: false, view: true, download: false, upload: false }],
      ['live file', live(file), { edit: false, view: false, download: true, upload: true }],
      ['historic file', historic(file), { edit: false, view: false, download: true, upload: false }],
      ['live bytes', live(bytes), { edit: false, view: false, download: true, upload: true }],
      ['historic bytes', historic(bytes), { edit: false, view: false, download: true, upload: false }],
      // isFile() covers file-value-datasource objects, so they are uploadable as well
      ['live file-value-datasource object', live(fileDataSource), { edit: false, view: false, download: true, upload: true }],
      ['historic file-value-datasource object', historic(fileDataSource), { edit: false, view: false, download: true, upload: false }],
    ])('%s shows %o buttons', (_, variable, expected) => {
      expect(buttons(variable)).toEqual(expected)
    })

    // read-only user: no right to change the variables of the instance (CIB7-1986)
    it.each([
      ['live string', live(string), { edit: false, view: true, download: false, upload: false }],
      ['historic string', historic(string), { edit: false, view: true, download: false, upload: false }],
      ['live file', live(file), { edit: false, view: false, download: true, upload: false }],
      ['live bytes', live(bytes), { edit: false, view: false, download: true, upload: false }],
      ['live file-value-datasource object', live(fileDataSource), { edit: false, view: false, download: true, upload: false }],
    ])('without update permission, %s shows %o buttons', (_, variable, expected) => {
      expect(buttons(variable, { canUpdateVariables: false })).toEqual(expected)
    })

    it('never offers edit and view at the same time', () => {
      for (const canUpdateVariables of [true, false]) {
        for (const v of [string, file, bytes, fileDataSource]) {
          for (const state of [live(v), historic(v)]) {
            const { edit, view } = buttons(state, { canUpdateVariables })
            expect(edit && view).toBe(false)
          }
        }
      }
    })

    describe('hasDeleteVariableButton', () => {
      it('needs DELETE on the process instance and the permission to change its variables for a live variable', () => {
        const vm = context()
        expect(vm.hasDeleteVariableButton(live(string))).toBe(true)
        expect(vm.processByPermissions).toHaveBeenCalledWith({ processInstance: ['DELETE'] }, { key: 'pi-1' })
        expect(vm.canDeleteHistoryProcessInstance).not.toHaveBeenCalled()
      })

      it('needs DELETE_HISTORY on the process definition for a historic variable', () => {
        const vm = context()
        expect(vm.hasDeleteVariableButton(historic(string))).toBe(true)
        expect(vm.canDeleteHistoryProcessInstance).toHaveBeenCalledWith({ key: 'proc-key' })
        expect(vm.processByPermissions).not.toHaveBeenCalled()
      })

      it('hides the runtime delete without DELETE on the process instance', () => {
        const vm = context({ processByPermissions: vi.fn(() => false) })
        expect(vm.hasDeleteVariableButton(live(string))).toBe(false)
        // the historic deletion does not depend on it
        expect(vm.hasDeleteVariableButton(historic(file))).toBe(true)
      })

      it('hides the runtime delete without the engine permission to change variables', () => {
        const vm = context({ canUpdateVariables: false })
        expect(vm.hasDeleteVariableButton(live(string))).toBe(false)
        // the historic deletion does not depend on it
        expect(vm.hasDeleteVariableButton(historic(string))).toBe(true)
      })

      it('hides the historic delete without DELETE_HISTORY on the process definition', () => {
        const vm = context({ canDeleteHistoryProcessInstance: vi.fn(() => false) })
        expect(vm.hasDeleteVariableButton(historic(string))).toBe(false)
        // the runtime deletion does not depend on it
        expect(vm.hasDeleteVariableButton(live(string))).toBe(true)
      })

      it('matches a DELETE grant against the id of the selected process instance', () => {
        const grant = (resourceId) => ({ userId: 'demo', groupId: null, resourceId, permissions: ['DELETE'], type: 1 })
        const withGrantOn = (resourceId) => {
          const vm = context({ $root: { config: { authorizationEnabled: true }, user: { permissions: { processInstance: [grant(resourceId)] } } } })
          for (const [name, fn] of Object.entries(permissionsMixin.methods)) {
            if (name !== 'canDeleteHistoryProcessInstance') vm[name] = fn.bind(vm)
          }
          return vm
        }
        expect(withGrantOn('pi-1').hasDeleteVariableButton(live(string))).toBe(true)
        expect(withGrantOn('*').hasDeleteVariableButton(live(string))).toBe(true)
        expect(withGrantOn('pi-2').hasDeleteVariableButton(live(string))).toBe(false)
      })

      it('is offered for downloadable variables too', () => {
        expect(context().hasDeleteVariableButton(live(file))).toBe(true)
      })
    })
  })

  describe('hasAddVariableButton', () => {
    const { hasAddVariableButton } = VariablesTable.computed

    it('requires an active process instance and the permission to change its variables', () => {
      expect(hasAddVariableButton.call({ isActiveInstance: true, canUpdateVariables: true })).toBe(true)
      expect(hasAddVariableButton.call({ isActiveInstance: false, canUpdateVariables: true })).toBe(false)
      expect(hasAddVariableButton.call({ isActiveInstance: true, canUpdateVariables: false })).toBe(false)
    })
  })

  describe('processDefinitionKey', () => {
    const { processDefinitionKey } = VariablesTable.computed

    it('prefers the key of the process definition', () => {
      expect(processDefinitionKey.call({ process: { key: 'def' }, selectedInstance: { processDefinitionKey: 'inst' } })).toBe('def')
    })

    it('falls back to the key stored on the (historic) process instance', () => {
      expect(processDefinitionKey.call({ process: undefined, selectedInstance: { processDefinitionKey: 'inst' } })).toBe('inst')
      expect(processDefinitionKey.call({ process: undefined, selectedInstance: null })).toBeUndefined()
    })
  })

  describe('canUpdateVariables', () => {
    it('checks the selected process instance and its process definition', () => {
      const vm = {
        selectedInstance: { id: 'pi-1' },
        processDefinitionKey: 'def',
        canUpdateProcessInstanceVariables: vi.fn(() => true),
      }
      expect(VariablesTable.computed.canUpdateVariables.call(vm)).toBe(true)
      expect(vm.canUpdateProcessInstanceVariables).toHaveBeenCalledWith('pi-1', 'def')
    })
  })

  describe('modifyVariable', () => {
    const context = () => ({ $refs: { editVariableModal: { show: vi.fn() } } })

    it('opens the modal in edit mode for a live variable', async () => {
      const vm = context()
      await VariablesTable.methods.modifyVariable.call(vm, { id: 'a', name: 'n', isLive: true }, false)
      expect(vm.$refs.editVariableModal.show).toHaveBeenCalledWith('a', 'n', false, false)
    })

    it('opens the modal read-only for a historic variable', async () => {
      const vm = context()
      await VariablesTable.methods.modifyVariable.call(vm, { id: 'a', name: 'n', isLive: false }, true)
      expect(vm.$refs.editVariableModal.show).toHaveBeenCalledWith('a', 'n', true, true)
    })

    it('can open a live variable read-only', async () => {
      const vm = context()
      await VariablesTable.methods.modifyVariable.call(vm, { id: 'a', name: 'n', isLive: true }, true)
      expect(vm.$refs.editVariableModal.show).toHaveBeenCalledWith('a', 'n', false, true)
    })
  })
})
