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
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import LoggedOutView from '@/components/login/LoggedOutView.vue'
import { LOGGED_OUT_KEY } from '@/constants.js'
import { logoutState } from '@/utils/logoutState.js'

describe('LoggedOutView', () => {
  const push = vi.fn()
  let wrappers = []
  const mountView = (config = {}) => {
    const wrapper = mount(LoggedOutView, {
      global: { mocks: { $t: key => key, $router: { push }, config } }
    })
    wrappers.push(wrapper)
    return wrapper
  }

  beforeEach(() => {
    push.mockReset()
    sessionStorage.clear()
    logoutState.busy = false
  })

  // The page listens on the window: a wrapper left mounted would answer the events of later tests
  afterEach(() => {
    wrappers.forEach(wrapper => { if (wrapper.exists()) wrapper.unmount() })
    wrappers = []
  })

  // Same frame as the login page, so the user knows where they are
  it('should tell the user they are logged out under the product name and slogan', () => {
    const wrapper = mountView({ productNamePageTitle: 'My Flow' })

    expect(wrapper.find('h1').text()).toBe('My Flow')
    expect(wrapper.find('h2').text()).toBe('login.productSlogan')
    expect(wrapper.text()).toContain('login.loggedOutText')
  })

  it('should show the login page image like the login page does', () => {
    const wrapper = mount(LoggedOutView, {
      global: { mocks: { $t: key => key, $router: { push }, config: {}, loginImgPath: 'login.png' } }
    })
    wrappers.push(wrapper)

    expect(wrapper.find('img').attributes('src')).toBe('login.png')
  })

  it('should fall back to the translated product name', () => {
    expect(mountView().find('h1').text()).toBe('login.productName')
  })

  it('should clear the logged-out marker and go to the login on the button', async () => {
    sessionStorage.setItem(LOGGED_OUT_KEY, '1')
    const wrapper = mountView()

    await wrapper.find('button').trigger('click')

    expect(sessionStorage.getItem(LOGGED_OUT_KEY)).toBeNull()
    expect(push).toHaveBeenCalledWith({ name: 'login' })
  })

  describe('while the logout is under way', () => {
    it('should show a spinner and no button', () => {
      logoutState.busy = true
      const wrapper = mountView()

      expect(wrapper.find('[role=status]').text()).toContain('login.loggingOut')
      expect(wrapper.find('button').exists()).toBe(false)
      expect(wrapper.text()).not.toContain('login.loggedOutText')
    })

    it('should switch to the message when the logout is over', async () => {
      logoutState.busy = true
      const wrapper = mountView()

      logoutState.busy = false
      await nextTick()

      expect(wrapper.find('[role=status]').exists()).toBe(false)
      expect(wrapper.find('button').exists()).toBe(true)
    })

    // The browser froze the page while it left for the identity provider
    it('should be over when the page is restored from the back/forward cache', async () => {
      logoutState.busy = true
      const wrapper = mountView()
      const restored = new Event('pageshow')
      restored.persisted = true

      window.dispatchEvent(restored)
      await nextTick()

      expect(logoutState.busy).toBe(false)
      expect(wrapper.find('button').exists()).toBe(true)
    })

    it('should stay busy when the page was not restored from the cache', () => {
      logoutState.busy = true
      mountView()
      const shown = new Event('pageshow')
      shown.persisted = false

      window.dispatchEvent(shown)

      expect(logoutState.busy).toBe(true)
    })

    it('should stop listening once it is gone', () => {
      logoutState.busy = true
      mountView().unmount()
      const restored = new Event('pageshow')
      restored.persisted = true

      window.dispatchEvent(restored)

      expect(logoutState.busy).toBe(true)
    })
  })
})
