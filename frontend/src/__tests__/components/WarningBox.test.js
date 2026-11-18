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
import { mount } from '@vue/test-utils'
import WarningBox from '@/components/common-components/WarningBox.vue'

function createWrapper(props = {}, slots = {}) {
  return mount(WarningBox, {
    props,
    slots
  })
}

describe('WarningBox.vue', () => {
  it('renders the warning icon and alert role', () => {
    const wrapper = createWrapper()
    expect(wrapper.attributes('role')).toBe('alert')
    expect(wrapper.find('.mdi-alert-outline').exists()).toBe(true)
  })

  it('defaults message to an empty string and renders a single empty paragraph', () => {
    const wrapper = createWrapper()
    const paragraphs = wrapper.findAll('p')
    expect(paragraphs).toHaveLength(1)
    expect(paragraphs[0].text()).toBe('')
  })

  it('renders one paragraph per line of the message', () => {
    const wrapper = createWrapper({ message: 'first line\nsecond line\nthird line' })
    const paragraphs = wrapper.findAll('p')
    expect(paragraphs).toHaveLength(3)
    expect(paragraphs[0].text()).toBe('first line')
    expect(paragraphs[1].text()).toBe('second line')
    expect(paragraphs[2].text()).toBe('third line')
  })

  it('wraps quoted words in <strong> tags', () => {
    const wrapper = createWrapper({ message: 'the task "reviewApproval" failed' })
    const span = wrapper.find('p span')
    expect(span.find('strong').text()).toBe('reviewApproval')
    expect(span.text()).toBe('the task "reviewApproval" failed')
  })

  it('wraps multiple quoted words on the same line', () => {
    const wrapper = createWrapper({ message: '"foo" and "bar" are missing' })
    const strongs = wrapper.findAll('p span strong')
    expect(strongs).toHaveLength(2)
    expect(strongs[0].text()).toBe('foo')
    expect(strongs[1].text()).toBe('bar')
  })

  it('leaves lines without quotes unmodified', () => {
    const wrapper = createWrapper({ message: 'no quotes here' })
    expect(wrapper.find('p span').html()).toContain('no quotes here')
    expect(wrapper.find('strong').exists()).toBe(false)
  })

  it('renders slot content after the message', () => {
    const wrapper = createWrapper({ message: 'line one' }, { default: '<button class="extra-action">Retry</button>' })
    expect(wrapper.find('.extra-action').exists()).toBe(true)
    expect(wrapper.find('.extra-action').text()).toBe('Retry')
  })

  it('keeps the quotes around the bolded word', () => {
    const wrapper = createWrapper({ message: 'say "hello" now' })
    expect(wrapper.find('p span').text()).toBe('say "hello" now')
    expect(wrapper.find('p span strong').text()).toBe('hello')
  })

  // CIB7-2008: a warning message may carry backend detail, which must stay text.
  it.each([
    ['a script tag', '<script>alert(1)</script>'],
    ['an img/onerror payload', '<img src="x" onerror="alert(1)">'],
    ['an attribute break-out', '"><svg onload="alert(1)">'],
  ])('renders %s in the message as text, not markup', (_name, message) => {
    const wrapper = createWrapper({ message })
    expect(wrapper.element.querySelector('script, img, svg')).toBeNull()
    expect(wrapper.find('p span').text()).toBe(message)
  })

  it('still bolds quoted words when the message also contains markup', () => {
    const wrapper = createWrapper({ message: 'the task "<b>review</b>" failed' })
    expect(wrapper.find('p span strong').text()).toBe('<b>review</b>')
    expect(wrapper.element.querySelector('b')).toBeNull()
  })

  it('keeps an ampersand inside a quoted word intact', () => {
    const wrapper = createWrapper({ message: 'the task "a & b" failed' })
    expect(wrapper.find('p span strong').text()).toBe('a & b')
  })
})
