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
import registerComponents from '@/register.js'
import postMessageMixin from '@/components/forms/postMessage.js'
import { GlobalEvents } from 'vue-global-events'
import { registerComponents as registerCommonComponents } from '@cib/common-frontend'

vi.mock('@cib/common-frontend', () => ({ registerComponents: vi.fn() }))

describe('registerComponents', () => {
  // Library consumers rely on this one call installing both the shared CIB components and
  // the GlobalEvents helper the shortcut handling needs.
  it('should install the shared components and GlobalEvents on the app', () => {
    const app = { component: vi.fn() }

    registerComponents(app)

    expect(registerCommonComponents).toHaveBeenCalledWith(app)
    expect(app.component).toHaveBeenCalledWith('GlobalEvents', GlobalEvents)
  })
})

describe('postMessageMixin', () => {
  it('should post the payload to the parent frame', () => {
    const postMessage = vi.fn()
    const originalParent = window.parent
    Object.defineProperty(window, 'parent', { value: { postMessage }, configurable: true })

    postMessageMixin.methods.sendMessageToParent({ event: 'done' })

    expect(postMessage).toHaveBeenCalledWith({ event: 'done' }, '*')
    Object.defineProperty(window, 'parent', { value: originalParent, configurable: true })
  })
})
