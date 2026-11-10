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
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

const forms = vi.hoisted(() => [])

vi.mock('@bpmn-io/form-js', () => ({
  Form: vi.fn(function (options) {
    this.options = options
    this.importSchema = vi.fn().mockResolvedValue({})
    this.destroy = vi.fn()
    forms.push(this)
  })
}))

import { Form } from '@bpmn-io/form-js'
import FormPreview from '@/components/deployment/FormPreview.vue'

const SCHEMA = { type: 'default', components: [{ type: 'textfield', key: 'amount' }] }

function mountPreview() {
  return mount(FormPreview, { global: { mocks: { $t: key => key } } })
}

describe('FormPreview', () => {
  beforeEach(() => {
    forms.length = 0
    vi.clearAllMocks()
  })

  it('renders a schema that arrives as the text of the deployed file', async () => {
    const wrapper = mountPreview()

    await wrapper.vm.showForm(JSON.stringify(SCHEMA))

    expect(forms).toHaveLength(1)
    expect(forms[0].importSchema).toHaveBeenCalledWith(SCHEMA)
    expect(wrapper.vm.error).toBe(false)
  })

  it('renders a schema that is already parsed', async () => {
    const wrapper = mountPreview()

    await wrapper.vm.showForm(SCHEMA)

    expect(forms[0].importSchema).toHaveBeenCalledWith(SCHEMA)
  })

  // A preview is for reading: a user must not fill in or change the form
  it('renders the form read-only, into its own container', async () => {
    const wrapper = mountPreview()

    await wrapper.vm.showForm(SCHEMA)

    expect(Form).toHaveBeenCalledWith(expect.objectContaining({ properties: { readOnly: true } }))
    expect(forms[0].options.container).toBe(wrapper.vm.$refs.container)
  })

  it('shows the schema below the form', async () => {
    const wrapper = mountPreview()

    await wrapper.vm.showForm(SCHEMA)
    await flushPromises()

    expect(wrapper.find('pre').text()).toBe(JSON.stringify(SCHEMA, null, 2))
  })

  it('says so when the file is no JSON', async () => {
    const wrapper = mountPreview()
    vi.spyOn(console, 'error').mockImplementation(() => {})

    await wrapper.vm.showForm('{ not json')
    await flushPromises()

    expect(wrapper.text()).toContain('deployment.formPreviewError')
    expect(Form).not.toHaveBeenCalled()
  })

  it('says so when the JSON is no schema', async () => {
    const wrapper = mountPreview()
    vi.spyOn(console, 'error').mockImplementation(() => {})

    await wrapper.vm.showForm('42')

    expect(wrapper.vm.error).toBe(true)
    expect(Form).not.toHaveBeenCalled()
  })

  it('says so when form-js cannot import the schema', async () => {
    Form.mockImplementationOnce(function (options) {
      this.options = options
      this.importSchema = vi.fn().mockRejectedValue(new Error('unsupported component'))
      this.destroy = vi.fn()
    })
    const wrapper = mountPreview()
    vi.spyOn(console, 'error').mockImplementation(() => {})

    await wrapper.vm.showForm(SCHEMA)

    expect(wrapper.vm.error).toBe(true)
  })

  it('removes the previous form before showing the next one', async () => {
    const wrapper = mountPreview()
    await wrapper.vm.showForm(SCHEMA)

    await wrapper.vm.showForm(SCHEMA)

    expect(forms[0].destroy).toHaveBeenCalled()
    expect(forms).toHaveLength(2)
  })

  it('removes its form when it goes away', async () => {
    const wrapper = mountPreview()
    await wrapper.vm.showForm(SCHEMA)

    wrapper.unmount()

    expect(forms[0].destroy).toHaveBeenCalled()
  })

  it('goes on when a form that is already gone cannot be removed', async () => {
    const wrapper = mountPreview()
    await wrapper.vm.showForm(SCHEMA)
    forms[0].destroy.mockImplementation(() => { throw new Error('already destroyed') })
    vi.spyOn(console, 'warn').mockImplementation(() => {})

    await wrapper.vm.showForm(SCHEMA)

    expect(forms).toHaveLength(2)
    expect(wrapper.vm.error).toBe(false)
  })
})
