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
import { createAxiosMock, resetAxiosMock } from './support/axiosMock.js'
import { ENGINE_STORAGE_KEY } from '@/constants.js'

const axios = createAxiosMock()

vi.mock('@/globals.js', async (importOriginal) => {
  const actual = await importOriginal()
  return { ...actual, axios }
})

const { setServicesBasePath } = await import('@/services.js')
const authService = (await import('@/components/login/authService.js')).default

const BASE = '/services/v1'

beforeEach(() => {
  resetAxiosMock(axios)
  localStorage.clear()
  sessionStorage.clear()
  axios.defaults.headers.common = {}
  setServicesBasePath(BASE)
})

describe('authService', () => {
  describe('login', () => {
    const loginResponse = { data: { authToken: 'tok-123', id: 'demo' } }

    // Login runs before interceptors are useful and must be able to select the engine, so
    // it posts through a bare instance with the engine header.
    it('should post the credentials through a fresh axios instance', async () => {
      axios.post.mockResolvedValue(loginResponse)

      await authService.login({ username: 'demo', password: 'x' })

      expect(axios.create).toHaveBeenCalled()
      expect(axios.post).toHaveBeenCalledWith(
        `${BASE}/auth/login`,
        { username: 'demo', password: 'x' },
        { params: { source: 'WEBSITE' }, headers: {} }
      )
    })

    it('should send the engine header when an engine is selected', async () => {
      localStorage.setItem(ENGINE_STORAGE_KEY, 'second-engine')
      axios.post.mockResolvedValue(loginResponse)

      await authService.login({ username: 'demo' })

      expect(axios.post).toHaveBeenCalledWith(
        `${BASE}/auth/login`,
        { username: 'demo' },
        { params: { source: 'WEBSITE' }, headers: { 'X-Process-Engine': 'second-engine' } }
      )
    })

    it('should return the user payload', async () => {
      axios.post.mockResolvedValue(loginResponse)

      await expect(authService.login({})).resolves.toEqual({ authToken: 'tok-123', id: 'demo' })
    })

    it('should install the token as the default authorization header', async () => {
      axios.post.mockResolvedValue(loginResponse)

      await authService.login({})

      expect(axios.defaults.headers.common.authorization).toBe('tok-123')
    })

    // "Remember me" is the difference between surviving a browser restart and not.
    it('should persist the token in localStorage when remembering', async () => {
      axios.post.mockResolvedValue(loginResponse)

      await authService.login({}, true)

      expect(localStorage.getItem('token')).toBe('tok-123')
      expect(sessionStorage.getItem('token')).toBeNull()
    })

    it.each([[false], [undefined]])('should persist the token in sessionStorage when remember is %j', async (remember) => {
      axios.post.mockResolvedValue(loginResponse)

      await authService.login({}, remember)

      expect(sessionStorage.getItem('token')).toBe('tok-123')
      expect(localStorage.getItem('token')).toBeNull()
    })

    it('should propagate a failed login', async () => {
      axios.post.mockRejectedValue(new Error('401'))

      await expect(authService.login({})).rejects.toThrow('401')
      expect(axios.defaults.headers.common.authorization).toBeUndefined()
    })
  })

  describe('update', () => {
    // Note the response here is the user object itself, not an axios envelope.
    it('should patch the user and install the new token', async () => {
      axios.patch.mockResolvedValue({ authToken: 'tok-new', id: 'demo' })

      const result = await authService.update({ firstName: 'Demo' })

      expect(axios.patch).toHaveBeenCalledWith(
        `${BASE}/auth/user`,
        { firstName: 'Demo' },
        { params: { source: 'WEBSITE' } }
      )
      expect(axios.defaults.headers.common.authorization).toBe('tok-new')
      expect(result).toEqual({ authToken: 'tok-new', id: 'demo' })
    })

    // The refreshed token has to land in whichever storage held the old one, or the user
    // is silently logged out on the next reload.
    it('should refresh the token in sessionStorage when the session holds it', async () => {
      sessionStorage.setItem('token', 'tok-old')
      axios.patch.mockResolvedValue({ authToken: 'tok-new' })

      await authService.update({})

      expect(sessionStorage.getItem('token')).toBe('tok-new')
      expect(localStorage.getItem('token')).toBeNull()
    })

    it('should refresh the token in localStorage when only localStorage holds it', async () => {
      localStorage.setItem('token', 'tok-old')
      axios.patch.mockResolvedValue({ authToken: 'tok-new' })

      await authService.update({})

      expect(localStorage.getItem('token')).toBe('tok-new')
      expect(sessionStorage.getItem('token')).toBeNull()
    })

    it('should default to sessionStorage when neither storage holds a token', async () => {
      axios.patch.mockResolvedValue({ authToken: 'tok-new' })

      await authService.update({})

      expect(sessionStorage.getItem('token')).toBe('tok-new')
      expect(localStorage.getItem('token')).toBeNull()
    })

    it('should prefer sessionStorage when both storages hold a token', async () => {
      sessionStorage.setItem('token', 'tok-session')
      localStorage.setItem('token', 'tok-local')
      axios.patch.mockResolvedValue({ authToken: 'tok-new' })

      await authService.update({})

      expect(sessionStorage.getItem('token')).toBe('tok-new')
      expect(localStorage.getItem('token')).toBe('tok-local')
    })
  })

  describe('delete', () => {
    it('should delete the user with the source marker in the query string', () => {
      authService.delete('demo')

      expect(axios.delete).toHaveBeenCalledWith(`${BASE}/auth/user/demo?source=WEBSITE`)
    })
  })

  describe('requestPasswordReset', () => {
    // Also runs unauthenticated, hence the bare instance.
    it('should post through a fresh instance with the source added to the params', () => {
      const params = { email: 'a@b.c' }

      authService.requestPasswordReset(params)

      expect(axios.create).toHaveBeenCalled()
      expect(axios.post).toHaveBeenCalledWith(`${BASE}/auth/reset`, null, {
        params: { email: 'a@b.c', source: 'WEBSITE' }
      })
      expect(params.source).toBe('WEBSITE')
    })
  })

  describe('poll4otp', () => {
    it('should poll the one-time-password endpoint for the user', () => {
      authService.poll4otp('demo')

      expect(axios.get).toHaveBeenCalledWith(`${BASE}/auth/otp/demo?source=WEBSITE`)
    })
  })
})
