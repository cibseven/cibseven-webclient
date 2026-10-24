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
import { describe, it, expect } from 'vitest'
import { normalizeLanguage, resolveLanguage } from '@/utils/language.js'

describe('language utility', () => {
  describe('normalizeLanguage', () => {
    it('should map every traditional Chinese tag to zh-tw', () => {
      ['zh-TW', 'zh-HK', 'zh-MO', 'zh-Hant', 'zh-Hant-TW', 'zh_TW'].forEach(tag => {
        expect(normalizeLanguage(tag), tag).toBe('zh-tw')
      })
    })

    it('should map every simplified Chinese tag to zh', () => {
      ['zh', 'zh-CN', 'zh-Hans', 'zh-SG', 'ZH_cn'].forEach(tag => {
        expect(normalizeLanguage(tag), tag).toBe('zh')
      })
    })

    it('should reduce a regional tag to its main language', () => {
      expect(normalizeLanguage('de-DE')).toBe('de')
      expect(normalizeLanguage('en-US')).toBe('en')
      expect(normalizeLanguage('UA')).toBe('ua')
      expect(normalizeLanguage('ru-RU')).toBe('ru')
    })

    it('should ignore surrounding whitespace and underscores', () => {
      expect(normalizeLanguage(' zh-TW ')).toBe('zh-tw')
      expect(normalizeLanguage('pt_BR')).toBe('pt')
    })

    it('should fall back to en for a missing language', () => {
      [undefined, null, '', '   '].forEach(language => {
        expect(normalizeLanguage(language)).toBe('en')
      })
    })
  })

  describe('resolveLanguage', () => {
    const supported = ['de', 'en', 'es', 'ua', 'ru', 'zh', 'zh-tw']

    it('should resolve a full tag to the configured short code', () => {
      expect(resolveLanguage(supported, 'zh-TW')).toBe('zh-tw')
      expect(resolveLanguage(supported, 'zh-Hant-HK')).toBe('zh-tw')
      expect(resolveLanguage(supported, 'zh-CN')).toBe('zh')
      expect(resolveLanguage(supported, 'en-US')).toBe('en')
    })

    it('should leave a short code untouched', () => {
      expect(resolveLanguage(['en', 'nl'], 'nl')).toBe('nl')
      expect(resolveLanguage(supported, 'zh-tw')).toBe('zh-tw')
    })

    it('should match a configuration that spells the tag in full', () => {
      expect(resolveLanguage(['en', 'zh-TW'], 'zh-tw')).toBe('zh-tw')
      expect(resolveLanguage(['en', 'zh-TW'], 'zh-HK')).toBe('zh-tw')
    })

    it('should fall back to the first configured language', () => {
      expect(resolveLanguage(['en', 'de'], 'zh-TW')).toBe('en')
      expect(resolveLanguage(['de'], 'fr')).toBe('de')
    })

    it('should keep the requested language when nothing is configured', () => {
      expect(resolveLanguage([], 'de-DE')).toBe('de')
      expect(resolveLanguage(undefined, 'zh-TW')).toBe('zh-tw')
    })
  })
})
