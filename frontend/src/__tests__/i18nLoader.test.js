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
import { flushPromises } from '@vue/test-utils'
import { mergeModelerTranslations } from 'cibseven-modeler'
import { i18n, switchLanguage, registerTranslationLoader } from '@/i18n'

vi.mock('@/globals.js', () => ({
  // Every optional translation file is absent, which the loaders treat as normal
  axios: { create: () => ({ get: () => Promise.reject(new Error('404')) }), defaults: { headers: { common: {} } } },
  moment: { locale: vi.fn() }
}))

vi.mock('@cib/common-frontend', () => ({ mergeLocaleMessage: vi.fn() }))
vi.mock('cibseven-modeler', () => ({ mergeModelerTranslations: vi.fn() }))

const config = { supportedLanguages: ['en', 'de', 'es'] }

describe('loadTranslations', () => {
  /**
   * The libraries merge their messages synchronously, and the first switch happens
   * during startup: one of them throwing must not leave the application unstarted.
   */
  it('switches the language and keeps loading when a source throws', async () => {
    const error = vi.spyOn(console, 'error').mockImplementation(() => {})
    mergeModelerTranslations.mockImplementationOnce(() => {
      throw new Error('modeler messages unavailable')
    })
    const reached = []
    registerTranslationLoader(lang => {
      reached.push(lang)
      return Promise.resolve()
    })

    await expect(switchLanguage({ supportedLanguages: ['en', 'nl'] }, 'nl')).resolves.toBe('nl')

    expect(i18n.global.locale).toBe('nl')
    // The sources after the failing one still run
    expect(reached).toContain('nl')
    expect(error).toHaveBeenCalled()
    error.mockRestore()
  })

  /** Half-loaded messages are worth another attempt, so the language is not remembered. */
  it('loads the sources again on the next switch to a language that failed', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    mergeModelerTranslations.mockImplementationOnce(() => {
      throw new Error('modeler messages unavailable')
    })
    const languages = { supportedLanguages: ['en', 'pt'] }

    await switchLanguage(languages, 'pt')
    mergeModelerTranslations.mockClear()
    await switchLanguage(languages, 'pt')

    expect(mergeModelerTranslations).toHaveBeenCalledWith(i18n, 'pt')
    console.error.mockRestore()
  })

  it('remembers a language whose sources all loaded', async () => {
    await switchLanguage({ supportedLanguages: ['en', 'it'] }, 'it')
    mergeModelerTranslations.mockClear()

    await switchLanguage({ supportedLanguages: ['en', 'it'] }, 'it')

    expect(mergeModelerTranslations).not.toHaveBeenCalled()
  })
})

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

  /**
   * The loaders fetch files of their own - a plugin's translations - and the first
   * language switch happens during startup, so a failing one must not take it down.
   */
  it('switches the language even when a registered source fails', async () => {
    const error = vi.spyOn(console, 'error').mockImplementation(() => {})
    const reached = []
    registerTranslationLoader(() => Promise.reject(new Error('plugin translations unreachable')))
    registerTranslationLoader(lang => {
      reached.push(lang)
      return Promise.resolve()
    })

    // A language nothing loaded yet, so the sources are asked as part of the switch
    await expect(switchLanguage({ supportedLanguages: ['en', 'fr'] }, 'fr')).resolves.toBe('fr')

    expect(i18n.global.locale).toBe('fr')
    // The sources are independent, so one failing does not skip the next
    expect(reached).toContain('fr')
    expect(error).toHaveBeenCalled()
    error.mockRestore()
  })

  it('reports a source that fails while catching up on a loaded language', async () => {
    const error = vi.spyOn(console, 'error').mockImplementation(() => {})
    await switchLanguage(config, 'de')

    registerTranslationLoader(() => Promise.reject(new Error('plugin translations unreachable')))
    await flushPromises()

    expect(error).toHaveBeenCalled()
    error.mockRestore()
  })
})
