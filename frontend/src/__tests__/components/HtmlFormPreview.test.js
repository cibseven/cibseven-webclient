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
import HtmlFormPreview from '@/components/deployment/HtmlFormPreview.vue'

const HTML = '<form><input name="amount"><script>alert(1)</script></form>'

function mountPreview() {
  return mount(HtmlFormPreview, { global: { mocks: { $t: key => key } } })
}

describe('HtmlFormPreview', () => {
  it('renders the deployed HTML in the frame', async () => {
    const wrapper = mountPreview()

    wrapper.vm.showForm(HTML)
    await wrapper.vm.$nextTick()

    expect(wrapper.find('iframe').attributes('srcdoc')).toBe(HTML)
  })

  // The deployed HTML must neither run scripts nor share the webclient's origin
  it('isolates the frame completely', async () => {
    const wrapper = mountPreview()

    wrapper.vm.showForm(HTML)
    await wrapper.vm.$nextTick()

    expect(wrapper.find('iframe').attributes('sandbox')).toBe('')
  })

  // A sandboxed frame created empty stays blank when its srcdoc is set later
  it('creates no frame before there is a form', () => {
    expect(mountPreview().find('iframe').exists()).toBe(false)
  })

  it('shows each form in a frame of its own', async () => {
    const wrapper = mountPreview()
    wrapper.vm.showForm('<p>first</p>')
    await wrapper.vm.$nextTick()
    const first = wrapper.find('iframe').element

    wrapper.vm.showForm(HTML)
    await wrapper.vm.$nextTick()

    expect(wrapper.find('iframe').element).not.toBe(first)
    expect(wrapper.find('iframe').attributes('srcdoc')).toBe(HTML)
  })

  it('shows the source below the frame as text, not as markup', async () => {
    const wrapper = mountPreview()

    wrapper.vm.showForm(HTML)
    await wrapper.vm.$nextTick()

    expect(wrapper.find('pre').text()).toBe(HTML)
    expect(wrapper.find('pre input').exists()).toBe(false)
  })

  it('says so when there is nothing to show', async () => {
    const wrapper = mountPreview()

    wrapper.vm.showForm('   ')
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('deployment.formPreviewError')
  })

  it('treats a missing value as nothing to show', async () => {
    const wrapper = mountPreview()

    wrapper.vm.showForm(null)
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.error).toBe(true)
  })

  it('shows a value that is no text as text', async () => {
    const wrapper = mountPreview()

    wrapper.vm.showForm(42)
    await wrapper.vm.$nextTick()

    expect(wrapper.find('iframe').attributes('srcdoc')).toBe('42')
  })

  it('clears an earlier error when the next form can be shown', async () => {
    const wrapper = mountPreview()
    wrapper.vm.showForm('')

    wrapper.vm.showForm(HTML)
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.error).toBe(false)
    expect(wrapper.text()).not.toContain('deployment.formPreviewError')
  })
})
