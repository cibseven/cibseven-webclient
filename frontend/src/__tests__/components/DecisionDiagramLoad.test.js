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
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import DecisionInstance from '@/components/decision/DecisionInstance.vue'
import DecisionDefinitionVersion from '@/components/decision/DecisionDefinitionVersion.vue'

// Both views load the diagram behind a short timeout. Leaving the view before it fires unmounts
// the viewer, and the delayed call used to throw on the missing ref.
describe.each([
  ['DecisionInstance', DecisionInstance, { instance: { decisionDefinitionId: 'd1' } }],
  ['DecisionDefinitionVersion', DecisionDefinitionVersion, { decision: { id: 'd1' } }]
])('%s loadDiagram', (_, component, props) => {
  const context = diagram => ({
    ...props,
    $refs: { diagram },
    getXmlById: vi.fn(() => Promise.resolve({ dmnXml: '<xml/>' })),
    restoreViewboxIfSaved: vi.fn()
  })

  beforeEach(() => { vi.useFakeTimers() })
  afterEach(() => { vi.useRealTimers() })

  it('shows the diagram once the timeout has passed', async () => {
    const diagram = { showDiagram: vi.fn(() => Promise.resolve()) }
    const vm = context(diagram)

    component.methods.loadDiagram.call(vm)
    await vi.runAllTimersAsync()

    expect(diagram.showDiagram).toHaveBeenCalledWith('<xml/>')
    expect(vm.restoreViewboxIfSaved).toHaveBeenCalled()
  })

  it('does nothing when the viewer is gone before the timeout fires', async () => {
    const vm = context(null)

    component.methods.loadDiagram.call(vm)

    await expect(vi.runAllTimersAsync()).resolves.not.toThrow()
    expect(vm.restoreViewboxIfSaved).not.toHaveBeenCalled()
  })
})
