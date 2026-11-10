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
import { describe, it, expect, afterEach } from 'vitest'
import BuilderStartView from '@/components/start/BuilderStartView.vue'
import { registerNavGroupsExtender } from '@/navigation/navGroups.js'

const unregisterExtenders = []
afterEach(() => unregisterExtenders.splice(0).forEach(unregister => unregister()))

function evaluateBuilderStartView(overrides = {}) {
  const vm = {
    permissionsModeler: false,
    $t: (key) => key,
    $options: { components: {} },
    ...overrides
  }
  Object.defineProperty(vm, 'items', {
    get: () => BuilderStartView.computed.items.call(vm)
  })
  return vm
}

describe('BuilderStartView.vue', () => {
  it('shows no items without the modeler permission', () => {
    const vm = evaluateBuilderStartView()
    expect(vm.items).toEqual([])
  })

  it('shows Modeler when the modeler permission is granted', () => {
    const vm = evaluateBuilderStartView({ permissionsModeler: true })
    expect(vm.items.map(i => i.title)).toEqual(['start.modeler.title'])
    expect(vm.items[0].to).toBe('/seven/auth/modeler')
  })

  it('shows what a registered extender adds to the builder group', () => {
    unregisterExtenders.push(registerNavGroupsExtender(groups => groups.map(group => group.id === 'builder'
      ? { ...group, items: [...group.items, { to: { name: 'extra' }, title: 'extra.title', tileImage: 'extra.svg' }] }
      : group)))
    const vm = evaluateBuilderStartView({ permissionsModeler: true })
    expect(vm.items.map(i => i.title)).toEqual(['start.modeler.title', 'extra.title'])
  })
})
