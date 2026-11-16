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
import DmnJS from 'dmn-js'
import DmnViewer from '@/components/decision/DmnViewer.vue'

/**
 * showDiagram against the real dmn-js viewer rather than a stand-in, so a call dmn-js does not have
 * cannot pass unnoticed. Without diagram information (DMNDI), because only the DRD view needs SVG
 * measuring that jsdom lacks; dmn-js then opens the first decision.
 */
const table = (id, ruleId, output) => `
  <decision id="${id}" name="${id}">
    <decisionTable id="${id}_table">
      <input id="${id}_in" label="season"><inputExpression id="${id}_expr" typeRef="string"><text>season</text></inputExpression></input>
      <output id="${id}_out" name="${id}" typeRef="string" />
      <rule id="${ruleId}"><inputEntry id="${ruleId}_in"><text></text></inputEntry><outputEntry id="${ruleId}_out"><text>"${output}"</text></outputEntry></rule>
    </decisionTable>
  </decision>`
const DRD = `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="https://www.omg.org/spec/DMN/20191111/MODEL/" id="dinner" name="Dinner" namespace="http://camunda.org/schema/1.0/dmn">
  ${table('dish', 'dish_rule', 'Stew')}
  ${table('beverages', 'beverages_rule', 'Water')}
</definitions>`

describe('DmnViewer.showDiagram with the real dmn-js', () => {
  let container
  afterEach(() => container?.remove())

  async function show(decisionId) {
    container = document.createElement('div')
    document.body.appendChild(container)
    const ctx = { setDiagramReady: vi.fn(), viewer: new DmnJS({ container }) }
    vi.useFakeTimers({ toFake: ['setTimeout'] })
    const shown = DmnViewer.methods.showDiagram.call(ctx, DRD, decisionId)
    await vi.runAllTimersAsync()
    await shown
    vi.useRealTimers()
    return ctx
  }

  it('opens the decision it is asked for', async () => {
    const ctx = await show('beverages')

    expect(ctx.viewer.getActiveView().element.id).toBe('beverages')
    expect(container.querySelector('td.rule-index[data-row-id="beverages_rule"]')).not.toBeNull()
    expect(ctx.setDiagramReady).toHaveBeenLastCalledWith(true)
  })

  it('keeps the view dmn-js chose without a decision, and a decision table is ready too', async () => {
    const ctx = await show()

    expect(ctx.viewer.getActiveView().element.id).toBe('dish')
    expect(ctx.setDiagramReady).toHaveBeenLastCalledWith(true)
  })
})
