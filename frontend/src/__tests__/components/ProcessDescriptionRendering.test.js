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
import { describe, it, expect } from 'vitest'
import ProcessCard from '@/components/start-process/ProcessCard.vue'
import ProcessTable from '@/components/start-process/ProcessTable.vue'
import { mountWithDefaults } from '../support/mountWithDefaults.js'

// CIB7-2008: a process description comes from the BPMN documentation element,
// so it is business data and must never be interpreted as markup. $te is
// stubbed to false here so getDescription falls back to process.description.
const XSS_PAYLOADS = [
  ['a script tag', '<script>alert(1)</script>'],
  ['an img/onerror payload', '<img src="x" onerror="alert(1)">'],
  ['an attribute break-out', '"><svg onload="alert(1)">'],
]

const process = description => ({
  id: 'p1',
  key: 'myProcess',
  name: 'My Process',
  description,
  suspended: 'false'
})

function mountCard(description) {
  return mountWithDefaults(ProcessCard, {
    props: { process: process(description), view: 'view-comfy', filter: '' },
    global: { mocks: { $te: () => false } }
  })
}

function mountTable(description) {
  return mountWithDefaults(ProcessTable, {
    props: { processes: [process(description)], processesFilter: '' },
    global: { mocks: { $te: () => false }, stubs: { FlowTable: false } }
  })
}

describe('ProcessCard description rendering', () => {
  it.each(XSS_PAYLOADS)('renders %s as text, not markup', (_name, description) => {
    const wrapper = mountCard(description)

    expect(wrapper.element.querySelector('script, img[onerror], svg')).toBeNull()
    expect(wrapper.find('.inline-description').text()).toBe(description)
  })

  it('still shows a plain description', () => {
    const wrapper = mountCard('A plain description')

    expect(wrapper.find('.inline-description').text()).toBe('A plain description')
  })
})

describe('ProcessTable description rendering', () => {
  it.each(XSS_PAYLOADS)('renders %s as text, not markup', (_name, description) => {
    const wrapper = mountTable(description)

    expect(wrapper.element.querySelector('script, img[onerror], svg')).toBeNull()
    expect(wrapper.text()).toContain(description)
  })

  it('still shows a plain description', () => {
    const wrapper = mountTable('A plain description')

    expect(wrapper.text()).toContain('A plain description')
  })
})
