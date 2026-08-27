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
import { i18n, switchLanguage, registerTranslationLoader } from '@/i18n'

vi.mock('@/globals.js', () => ({
  // Every optional translation file is absent, which the loaders treat as normal
  axios: { create: () => ({ get: () => Promise.reject(new Error('404')) }), defaults: { headers: { common: {} } } },
  moment: { locale: vi.fn() }
}))

vi.mock('@cib/common-frontend', () => ({ mergeLocaleMessage: vi.fn() }))
vi.mock('cibseven-modeler', () => ({ mergeModelerTranslations: vi.fn() }))

const config = { supportedLanguages: ['en', 'de', 'es'] }

describe('registerTranslationLoader', () => {
  /**
   * A language is switched to only once its messages are there. A source loading
   * afterwards would render its keys once, until the next switch.
   */
  it('loads a registered source before the language is switched to', async () => {
    const localeWhenCalled = []
    registerTranslationLoader(lang => {
      localeWhenCalled.push({ lang, locale: i18n.global.locale })
      return Promise.resolve()
    })

    await switchLanguage(config, 'es')

    expect(localeWhenCalled.at(-1).lang).toBe('es')
    expect(localeWhenCalled.at(-1).locale).not.toBe('es')
    expect(i18n.global.locale).toBe('es')
  })

  /** A plugin registers once it has loaded, after the first language is long there. */
  it('catches a source up on the languages already loaded', async () => {
    await switchLanguage(config, 'de')
    const loaded = []

    registerTranslationLoader(lang => {
      loaded.push(lang)
      return Promise.resolve()
    })

    expect(loaded).toContain('de')
  })
})
