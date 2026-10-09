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
import InstancesTable from '@/components/process/tables/InstancesTable.vue'

describe('InstancesTable', () => {
  describe('hasStopInstanceButton', () => {
    const context = (allowed = true) => ({ canDeleteRuntimeProcessInstance: vi.fn(() => allowed) })
    const instance = (state) => ({ id: 'pi-1', processDefinitionKey: 'order-process', state })

    it.each(['ACTIVE', 'SUSPENDED'])('is shown for a %s instance the user may delete', (state) => {
      const vm = context()
      expect(InstancesTable.methods.hasStopInstanceButton.call(vm, instance(state))).toBe(true)
      expect(vm.canDeleteRuntimeProcessInstance).toHaveBeenCalledWith('pi-1', 'order-process')
    })

    it('is hidden without the permission to delete the instance', () => {
      expect(InstancesTable.methods.hasStopInstanceButton.call(context(false), instance('ACTIVE'))).toBe(false)
    })

    it.each(['COMPLETED', 'EXTERNALLY_TERMINATED', 'INTERNALLY_TERMINATED'])('is hidden for a %s instance', (state) => {
      const vm = context()
      expect(InstancesTable.methods.hasStopInstanceButton.call(vm, instance(state))).toBe(false)
      expect(vm.canDeleteRuntimeProcessInstance).not.toHaveBeenCalled()
    })
  })
})
