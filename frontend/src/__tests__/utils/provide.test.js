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
import { createProvideObject } from '@/utils/provide.js'
import { i18n, switchLanguage } from '@/i18n'
import { fetchAndStoreProcesses, fetchDecisionsIfEmpty, isMobile } from '@/utils/init'
import { AuthService } from '@/services.js'

// Mock the dependencies
vi.mock('@/i18n', () => ({
  i18n: {
    global: {
      locale: 'en'
    }
  },
  switchLanguage: vi.fn()
}))

vi.mock('@/utils/init', () => ({
  fetchAndStoreProcesses: vi.fn(),
  fetchDecisionsIfEmpty: vi.fn(),
  isMobile: vi.fn()
}))

vi.mock('@/services.js', () => ({
  AuthService: {
    fetchAuths: vi.fn()
  }
}))

describe('createProvideObject', () => {
  let config
  let vueInstance
  let store
  let provideObject

  beforeEach(() => {
    config = { theme: 'cib' }
    vueInstance = { $store: {} }
    store = { state: {}, dispatch: vi.fn() }
    
    // Reset mocks
    vi.clearAllMocks()
    
    provideObject = createProvideObject(config, vueInstance, store)
  })

  describe('currentLanguage', () => {
    it('should return the current locale from i18n', () => {
      expect(provideObject.currentLanguage()).toBe('en')
    })

    it('should return updated locale when i18n locale changes', () => {
      i18n.global.locale = 'de'
      expect(provideObject.currentLanguage()).toBe('de')
      // Reset for other tests
      i18n.global.locale = 'en'
    })
  })

  describe('setCurrentLanguage', () => {
    it('should call switchLanguage with config and lang', async () => {
      switchLanguage.mockResolvedValue(undefined)
      
      await provideObject.setCurrentLanguage('de')
      
      expect(switchLanguage).toHaveBeenCalledWith(config, 'de')
    })

    it('should return the current locale after switching', async () => {
      switchLanguage.mockResolvedValue(undefined)
      i18n.global.locale = 'fr'
      
      const result = await provideObject.setCurrentLanguage('fr')
      
      expect(result).toBe('fr')
      // Reset for other tests
      i18n.global.locale = 'en'
    })

    it('should not call switchLanguage when lang is falsy', async () => {
      const result = await provideObject.setCurrentLanguage(null)
      
      expect(switchLanguage).not.toHaveBeenCalled()
      expect(result).toBe('en')
    })

    it('should not call switchLanguage when lang is empty string', async () => {
      const result = await provideObject.setCurrentLanguage('')
      
      expect(switchLanguage).not.toHaveBeenCalled()
      expect(result).toBe('en')
    })
  })

  describe('loadProcesses', () => {
    it('should call fetchAndStoreProcesses with correct parameters', () => {
      const extraInfo = { someData: 'test' }
      
      provideObject.loadProcesses(extraInfo)
      
      expect(fetchAndStoreProcesses).toHaveBeenCalledWith(vueInstance, store, config, extraInfo)
    })

    it('should return the result from fetchAndStoreProcesses', () => {
      const expectedResult = { processes: [] }
      fetchAndStoreProcesses.mockReturnValue(expectedResult)
      
      const result = provideObject.loadProcesses()
      
      expect(result).toBe(expectedResult)
    })

    it('should work without extraInfo parameter', () => {
      provideObject.loadProcesses()
      
      expect(fetchAndStoreProcesses).toHaveBeenCalledWith(vueInstance, store, config, undefined)
    })
  })

  describe('loadDecisions', () => {
    it('should call fetchDecisionsIfEmpty with store', async () => {
      fetchDecisionsIfEmpty.mockResolvedValue([])
      
      await provideObject.loadDecisions()
      
      expect(fetchDecisionsIfEmpty).toHaveBeenCalledWith(store)
    })

    it('should return the result from fetchDecisionsIfEmpty', async () => {
      const expectedResult = [{ id: 1, name: 'Decision 1' }]
      fetchDecisionsIfEmpty.mockResolvedValue(expectedResult)
      
      const result = await provideObject.loadDecisions()
      
      expect(result).toEqual(expectedResult)
    })
  })

  describe('isMobile', () => {
    it('should expose isMobile function', () => {
      expect(provideObject.isMobile).toBe(isMobile)
    })
  })

  describe('AuthService', () => {
    it('should expose AuthService', () => {
      expect(provideObject.AuthService).toBe(AuthService)
    })

    it('should have AuthService with expected methods', () => {
      expect(provideObject.AuthService).toHaveProperty('fetchAuths')
    })
  })

  describe('provide object structure', () => {
    it('should return an object with all expected properties', () => {
      expect(provideObject).toHaveProperty('currentLanguage')
      expect(provideObject).toHaveProperty('setCurrentLanguage')
      expect(provideObject).toHaveProperty('loadProcesses')
      expect(provideObject).toHaveProperty('loadDecisions')
      expect(provideObject).toHaveProperty('isMobile')
      expect(provideObject).toHaveProperty('AuthService')
    })

    it('should have functions for language methods', () => {
      expect(typeof provideObject.currentLanguage).toBe('function')
      expect(typeof provideObject.setCurrentLanguage).toBe('function')
    })

    it('should have functions for data loading methods', () => {
      expect(typeof provideObject.loadProcesses).toBe('function')
      expect(typeof provideObject.loadDecisions).toBe('function')
    })
  })
})
