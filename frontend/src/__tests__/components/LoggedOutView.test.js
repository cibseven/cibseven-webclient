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
import { mount } from '@vue/test-utils'
import LoggedOutView from '@/components/login/LoggedOutView.vue'
import { LOGGED_OUT_KEY } from '@/constants.js'

describe('LoggedOutView', () => {
  const push = vi.fn()
  const mountView = (config = {}) => mount(LoggedOutView, {
    global: { mocks: { $t: key => key, $router: { push }, config } }
  })

  beforeEach(() => {
    push.mockReset()
    sessionStorage.clear()
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
})
