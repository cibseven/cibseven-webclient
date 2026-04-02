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
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import {
  getEngineTokens,
  getTokenForEngine,
  storeTokenForEngine,
  removeTokenForEngine,
  clearAllEngineTokens,
  hasTokenForEngine,
  restoreTokenForEngine
} from '@/utils/engineTokens.js'
import { ENGINE_TOKENS_STORAGE_KEY } from '@/constants.js'

describe('engineTokens utility', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
  })

  afterEach(() => {
    localStorage.clear()
    sessionStorage.clear()
  })

  describe('getEngineTokens', () => {
    it('should return empty object when no tokens stored', () => {
      expect(getEngineTokens()).toEqual({})
    })

    it('should return stored tokens', () => {
      const tokens = { engine1: 'token1', engine2: 'token2' }
      localStorage.setItem(ENGINE_TOKENS_STORAGE_KEY, JSON.stringify(tokens))
      
      expect(getEngineTokens()).toEqual(tokens)
    })

    it('should return empty object on parse error', () => {
      localStorage.setItem(ENGINE_TOKENS_STORAGE_KEY, 'invalid json')
      const consoleSpy = vi.spyOn(console, 'warn').mockImplementation(() => {})
      
      expect(getEngineTokens()).toEqual({})
      
      consoleSpy.mockRestore()
    })
  })

  describe('getTokenForEngine', () => {
    it('should return null when no token for engine', () => {
      expect(getTokenForEngine('engine1')).toBeNull()
    })

    it('should return token for engine', () => {
      const tokens = { engine1: 'token1' }
      localStorage.setItem(ENGINE_TOKENS_STORAGE_KEY, JSON.stringify(tokens))
      
      expect(getTokenForEngine('engine1')).toBe('token1')
    })
  })

  describe('storeTokenForEngine', () => {
    it('should not store if no engineId', () => {
      storeTokenForEngine(null, 'token')
      expect(localStorage.getItem(ENGINE_TOKENS_STORAGE_KEY)).toBeNull()
    })

    it('should store token from parameter', () => {
      storeTokenForEngine('engine1', 'my-token')
      
      const stored = JSON.parse(localStorage.getItem(ENGINE_TOKENS_STORAGE_KEY))
      expect(stored.engine1).toBe('my-token')
    })

    it('should store token from localStorage if not provided', () => {
      localStorage.setItem('token', 'local-token')
      storeTokenForEngine('engine1')
      
      const stored = JSON.parse(localStorage.getItem(ENGINE_TOKENS_STORAGE_KEY))
      expect(stored.engine1).toBe('local-token')
    })

    it('should store token from sessionStorage if not provided', () => {
      sessionStorage.setItem('token', 'session-token')
      storeTokenForEngine('engine1')
      
      const stored = JSON.parse(localStorage.getItem(ENGINE_TOKENS_STORAGE_KEY))
      expect(stored.engine1).toBe('session-token')
    })

    it('should not store if no token available', () => {
      storeTokenForEngine('engine1')
      expect(localStorage.getItem(ENGINE_TOKENS_STORAGE_KEY)).toBeNull()
    })

    it('should preserve existing tokens when adding new one', () => {
      const existingTokens = { engine1: 'token1' }
      localStorage.setItem(ENGINE_TOKENS_STORAGE_KEY, JSON.stringify(existingTokens))
      
      storeTokenForEngine('engine2', 'token2')
      
      const stored = JSON.parse(localStorage.getItem(ENGINE_TOKENS_STORAGE_KEY))
      expect(stored).toEqual({ engine1: 'token1', engine2: 'token2' })
    })
  })

  describe('removeTokenForEngine', () => {
    it('should not fail if no engineId', () => {
      expect(() => removeTokenForEngine(null)).not.toThrow()
    })

    it('should remove token for engine', () => {
      const tokens = { engine1: 'token1', engine2: 'token2' }
      localStorage.setItem(ENGINE_TOKENS_STORAGE_KEY, JSON.stringify(tokens))
      
      removeTokenForEngine('engine1')
      
      const stored = JSON.parse(localStorage.getItem(ENGINE_TOKENS_STORAGE_KEY))
      expect(stored).toEqual({ engine2: 'token2' })
    })

    it('should not fail if engine not in tokens', () => {
      const tokens = { engine1: 'token1' }
      localStorage.setItem(ENGINE_TOKENS_STORAGE_KEY, JSON.stringify(tokens))
      
      expect(() => removeTokenForEngine('engine2')).not.toThrow()
    })
  })

  describe('clearAllEngineTokens', () => {
    it('should remove all engine tokens', () => {
      const tokens = { engine1: 'token1', engine2: 'token2' }
      localStorage.setItem(ENGINE_TOKENS_STORAGE_KEY, JSON.stringify(tokens))
      
      clearAllEngineTokens()
      
      expect(localStorage.getItem(ENGINE_TOKENS_STORAGE_KEY)).toBeNull()
    })
  })

  describe('hasTokenForEngine', () => {
    it('should return false when no token', () => {
      expect(hasTokenForEngine('engine1')).toBe(false)
    })

    it('should return true when token exists', () => {
      const tokens = { engine1: 'token1' }
      localStorage.setItem(ENGINE_TOKENS_STORAGE_KEY, JSON.stringify(tokens))
      
      expect(hasTokenForEngine('engine1')).toBe(true)
    })
  })

  describe('restoreTokenForEngine', () => {
    it('should return null when no cached token', () => {
      expect(restoreTokenForEngine('engine1')).toBeNull()
    })

    it('should restore token to sessionStorage when session token exists', () => {
      const tokens = { engine1: 'cached-token' }
      localStorage.setItem(ENGINE_TOKENS_STORAGE_KEY, JSON.stringify(tokens))
      sessionStorage.setItem('token', 'old-session-token')
      
      const result = restoreTokenForEngine('engine1')
      
      expect(result).toBe('cached-token')
      expect(sessionStorage.getItem('token')).toBe('cached-token')
    })

    it('should restore token to localStorage when local token exists', () => {
      const tokens = { engine1: 'cached-token' }
      localStorage.setItem(ENGINE_TOKENS_STORAGE_KEY, JSON.stringify(tokens))
      localStorage.setItem('token', 'old-local-token')
      
      const result = restoreTokenForEngine('engine1')
      
      expect(result).toBe('cached-token')
      expect(localStorage.getItem('token')).toBe('cached-token')
    })

    it('should default to localStorage when no existing token storage', () => {
      const tokens = { engine1: 'cached-token' }
      localStorage.setItem(ENGINE_TOKENS_STORAGE_KEY, JSON.stringify(tokens))
      
      const result = restoreTokenForEngine('engine1')
      
      expect(result).toBe('cached-token')
      expect(localStorage.getItem('token')).toBe('cached-token')
    })
  })
})
