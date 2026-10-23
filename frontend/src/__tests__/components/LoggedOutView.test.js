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
import { mount, flushPromises } from '@vue/test-utils'
import LoggedOutView from '@/components/login/LoggedOutView.vue'
import { AuthService } from '@/services.js'
import { axios } from '@/globals.js'
import { LOGGED_OUT_KEY } from '@/constants.js'

vi.mock('@/services.js', () => ({
  AuthService: { logout: vi.fn() },
  // AppFooter loads the version when it is created
  InfoService: { getVersion: vi.fn(() => Promise.resolve('1.0.0')) }
}))

describe('LoggedOutView', () => {
  const push = vi.fn()
  const originalLocation = window.location
  let wrappers = []
  const mountView = (mocks = {}) => {
    const wrapper = mount(LoggedOutView, {
      global: { mocks: { $t: key => key, $router: { push }, config: {}, ...mocks } }
    })
    wrappers.push(wrapper)
    return wrapper
  }
  // With the session still loaded: reached from inside the application, which is the logout itself
  const mountSignedIn = (mocks = {}) => mountView({ user: { id: 'demo' }, ...mocks })

  beforeEach(() => {
    push.mockReset()
    AuthService.logout.mockReset()
    sessionStorage.clear()
    localStorage.setItem('accessToken', 'a')
    axios.defaults.headers.common.authorization = 'Bearer old'
    delete window.location
    window.location = { hash: '#/seven/logged-out', href: 'http://localhost/', reload: vi.fn() }
  })

  // Unmount what a test mounted
  afterEach(() => {
    wrappers.forEach(wrapper => { if (wrapper.exists()) wrapper.unmount() })
    wrappers = []
    window.location = originalLocation
    localStorage.clear()
    delete axios.defaults.headers.common.authorization
  })

  describe('without a session (the identity provider sent the browser back, a reload, going back)', () => {
    // Same frame as the login page, so the user knows where they are
    it('should tell the user they are logged out under the product name and slogan', () => {
      const wrapper = mountView({ config: { productNamePageTitle: 'My Flow' } })

      expect(wrapper.find('h1').text()).toBe('My Flow')
      expect(wrapper.find('h2').text()).toBe('login.productSlogan')
      expect(wrapper.text()).toContain('login.loggedOutText')
    })

    it('should show the login page image like the login page does', () => {
      expect(mountView({ loginImgPath: 'login.png' }).find('img').attributes('src')).toBe('login.png')
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

    it('should not ask the backend or touch anything, nobody is signed in', async () => {
      mountView()
      await flushPromises()

      expect(AuthService.logout).not.toHaveBeenCalled()
      expect(axios.defaults.headers.common.authorization).toBe('Bearer old')
      expect(window.location.reload).not.toHaveBeenCalled()
    })
  })

  describe('with a session (the logout)', () => {
    it('should show the spinner, not the message, while the backend is asked', () => {
      AuthService.logout.mockReturnValue(new Promise(() => {}))
      const wrapper = mountSignedIn()

      expect(wrapper.find('[role=status]').text()).toContain('login.loggingOut')
      expect(wrapper.find('button').exists()).toBe(false)
      expect(wrapper.text()).not.toContain('login.loggedOutText')
    })

    it('should ask the backend to return to the app, with the token still set', async () => {
      let headerWhenAsked
      AuthService.logout.mockImplementation(async () => {
        headerWhenAsked = axios.defaults.headers.common.authorization
        return {}
      })

      mountSignedIn()
      await flushPromises()

      expect(AuthService.logout).toHaveBeenCalledWith('http://localhost/')
      expect(headerWhenAsked).toBe('Bearer old')
    })

    it('should clear the user, the header and the stored tokens afterwards', async () => {
      AuthService.logout.mockResolvedValue({})
      const wrapper = mountSignedIn()

      await flushPromises()

      expect(wrapper.vm.$root.user).toBeNull()
      expect(axios.defaults.headers.common.authorization).toBeUndefined()
      expect(localStorage.getItem('accessToken')).toBeNull()
    })

    it('should go to the logout redirect URL the backend returns, instead of reloading', async () => {
      AuthService.logout.mockResolvedValue({ logoutRedirectUrl: 'https://idp/logout?client_id=c' })
      const wrapper = mountSignedIn()

      await flushPromises()

      // the marker makes the app show this page again when the identity provider sends the browser back
      expect(sessionStorage.getItem(LOGGED_OUT_KEY)).toBe('1')
      expect(window.location.href).toBe('https://idp/logout?client_id=c')
      expect(window.location.reload).not.toHaveBeenCalled()
      // the spinner stays until the browser has left
      expect(wrapper.find('[role=status]').exists()).toBe(true)
    })

    it('should navigate to the start page before reloading when the backend names no redirect URL', async () => {
      const reloadOrder = []
      window.location.reload = vi.fn(() => reloadOrder.push(window.location.hash))
      AuthService.logout.mockResolvedValue({})

      mountSignedIn()
      await flushPromises()

      // reload must fire AFTER the hash moves to the start page, else login returns
      // to the (now-stale) previous page
      expect(reloadOrder).toEqual(['#/'])
      expect(window.location.hash).toBe('#/')
      expect(sessionStorage.getItem(LOGGED_OUT_KEY)).toBeNull()
    })

    it('should still log out locally when the backend call fails', async () => {
      const error = vi.spyOn(console, 'error').mockImplementation(() => {})
      AuthService.logout.mockRejectedValue(new Error('timeout'))
      const wrapper = mountSignedIn()

      await flushPromises()

      expect(wrapper.vm.$root.user).toBeNull()
      expect(localStorage.getItem('accessToken')).toBeNull()
      expect(window.location.reload).toHaveBeenCalled()
      error.mockRestore()
    })
  })
})
