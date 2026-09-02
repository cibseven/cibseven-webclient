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
import { getDeepLinkEntries, buildDeepLinkUrl, resolveDeepLinkLabel } from '@/utils/deepLinks.js'

describe('deepLinks utility', () => {
  let warnSpy

  beforeEach(() => {
    warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {})
  })

  afterEach(() => {
    warnSpy.mockRestore()
  })

  describe('getDeepLinkEntries', () => {
    it('returns an empty array when config has no deepLinks section', () => {
      expect(getDeepLinkEntries(undefined, 'processInstance')).toEqual([])
      expect(getDeepLinkEntries({}, 'processInstance')).toEqual([])
    })

    it('returns an empty array when the section is not an array', () => {
      const config = { deepLinks: { processInstance: 'not-an-array' } }
      expect(getDeepLinkEntries(config, 'processInstance')).toEqual([])
      expect(warnSpy).toHaveBeenCalled()
    })

    it('returns valid entries unchanged', () => {
      const config = { deepLinks: { processInstance: [
        { id: 'myExternalLinkId', url: 'https://external.example/app' },
        { id: 'abc123', url: 'https://external.example/app2' }
      ] } }
      expect(getDeepLinkEntries(config, 'processInstance')).toEqual([
        { id: 'myExternalLinkId', url: 'https://external.example/app', text: 'deepLinks.processInstance.myExternalLinkId.title' },
        { id: 'abc123', url: 'https://external.example/app2', text: 'deepLinks.processInstance.abc123.title' }
      ])
      expect(warnSpy).not.toHaveBeenCalled()
    })

    it('drops entries with an id that does not match ^[a-zA-Z0-9]+$', () => {
      const config = { deepLinks: { processInstance: [
        { id: 'valid1', url: 'https://external.example' },
        { id: 'has-dash', url: 'https://external.example' },
        { id: 'has space', url: 'https://external.example' },
        { id: '', url: 'https://external.example' },
        { url: 'https://external.example' }
      ] } }
      const result = getDeepLinkEntries(config, 'processInstance')
      expect(result).toEqual([{ id: 'valid1', url: 'https://external.example', text: 'deepLinks.processInstance.valid1.title' }])
      expect(warnSpy).toHaveBeenCalledTimes(4)
    })

    it('drops entries with a missing or blank url', () => {
      const config = { deepLinks: { processInstance: [
        { id: 'noUrl' },
        { id: 'blankUrl', url: '' },
        { id: 'good', url: 'https://external.example' }
      ] } }
      expect(getDeepLinkEntries(config, 'processInstance')).toEqual([{ id: 'good', url: 'https://external.example', text: 'deepLinks.processInstance.good.title' }])
    })

    it('drops entries whose id collides with a reserved built-in tab id', () => {
      const config = { deepLinks: { processInstance: [
        { id: 'variables', url: 'https://external.example' },
        { id: 'custom', url: 'https://external.example' }
      ] } }
      const result = getDeepLinkEntries(config, 'processInstance', ['variables', 'incidents'])
      expect(result).toEqual([{ id: 'custom', url: 'https://external.example', text: 'deepLinks.processInstance.custom.title' }])
    })

    it('drops later duplicate ids, keeping the first occurrence', () => {
      const config = { deepLinks: { processInstance: [
        { id: 'dup', url: 'https://first.example' },
        { id: 'dup', url: 'https://second.example' }
      ] } }
      const result = getDeepLinkEntries(config, 'processInstance')
      expect(result).toEqual([{ id: 'dup', url: 'https://first.example', text: 'deepLinks.processInstance.dup.title' }])
    })
  })

  describe('buildDeepLinkUrl', () => {
    it('appends params as query string on a plain URL', () => {
      const url = buildDeepLinkUrl('https://external.example/app', { processInstanceId: 'pi-1', lang: 'en' })
      const parsed = new URL(url)
      expect(parsed.origin + parsed.pathname).toBe('https://external.example/app')
      expect(parsed.searchParams.get('processInstanceId')).toBe('pi-1')
      expect(parsed.searchParams.get('lang')).toBe('en')
    })

    it('merges into an existing query string without dropping it', () => {
      const url = buildDeepLinkUrl('https://external.example/app?existing=1', { lang: 'en' })
      const parsed = new URL(url)
      expect(parsed.searchParams.get('existing')).toBe('1')
      expect(parsed.searchParams.get('lang')).toBe('en')
    })

    it('skips undefined, null and empty-string param values', () => {
      const url = buildDeepLinkUrl('https://external.example/app', { a: undefined, b: null, c: '', d: 'kept' })
      const parsed = new URL(url)
      expect(parsed.searchParams.has('a')).toBe(false)
      expect(parsed.searchParams.has('b')).toBe(false)
      expect(parsed.searchParams.has('c')).toBe(false)
      expect(parsed.searchParams.get('d')).toBe('kept')
    })

    it('returns the original url and warns on an unparsable URL', () => {
      const result = buildDeepLinkUrl('not-a-valid-url', { lang: 'en' })
      expect(result).toBe('not-a-valid-url')
      expect(warnSpy).toHaveBeenCalled()
    })
  })

  describe('resolveDeepLinkLabel', () => {
    it('falls back to the entry id when $t returns the key unchanged (no translation found)', () => {
      const entry = { id: 'vhvOutput', url: 'https://external.example', text: 'deepLinks.processInstance.vhvOutput.title' }
      expect(resolveDeepLinkLabel(key => key, entry)).toBe('vhvOutput')
    })

    it('returns the translated label when $t resolves it to something other than the key', () => {
      const entry = { id: 'vhvOutput', url: 'https://external.example', text: 'deepLinks.processInstance.vhvOutput.title' }
      expect(resolveDeepLinkLabel(() => 'VHV Output', entry)).toBe('VHV Output')
    })
  })
})
