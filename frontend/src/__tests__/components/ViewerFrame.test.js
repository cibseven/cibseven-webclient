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
import { mount } from '@vue/test-utils'
import ViewerFrame from '@/components/common-components/ViewerFrame.vue'

function createWrapper(resizerMixin) {
  return mount(ViewerFrame, {
    props: {
      resizerMixin: {
        bpmnViewerHeight: 400,
        toggleTransition: '',
        handleMouseDown: vi.fn(),
        toggleContent: vi.fn(),
        ...resizerMixin
      }
    },
    slots: { default: '<div class="viewer-content">diagram</div>' },
    global: {
      mocks: { $t: key => key },
      stubs: { 'b-button': { template: '<button v-bind="$attrs"><slot /></button>' } }
    }
  })
}

describe('ViewerFrame.vue', () => {
  // Chevron down: the tabs are visible, so clicking hides them
  it('offers to collapse while the tabs are shown', () => {
    const wrapper = createWrapper({ toggleIcon: 'mdi-chevron-down' })
    expect(wrapper.find('button').attributes('title')).toBe('task.collapse')
  })

  // Chevron up: the viewer covers the tabs, so clicking brings them back
  it('offers to expand while the tabs are hidden', () => {
    const wrapper = createWrapper({ toggleIcon: 'mdi-chevron-up' })
    expect(wrapper.find('button').attributes('title')).toBe('task.expand')
  })

  it('toggles the content when the button is clicked', async () => {
    const toggleContent = vi.fn()
    const wrapper = createWrapper({ toggleIcon: 'mdi-chevron-down', toggleContent })
    await wrapper.find('button').trigger('click')
    expect(toggleContent).toHaveBeenCalledOnce()
  })

  it('renders the slot and applies the viewer height', () => {
    const wrapper = createWrapper({ toggleIcon: 'mdi-chevron-down', bpmnViewerHeight: 250 })
    expect(wrapper.find('.viewer-content').exists()).toBe(true)
    expect(wrapper.attributes('style')).toContain('height: 250px')
  })
})
