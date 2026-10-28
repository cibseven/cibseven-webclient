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
import { createStore } from 'vuex'

const registerTranslationLoader = vi.fn()
const messages = {}
const i18n = {
  global: {
    getLocaleMessage: lang => messages[lang] || {},
    setLocaleMessage: (lang, value) => { messages[lang] = value }
  }
}
const modelerPackage = {
  processStore: { namespaced: true, state: () => ({ processes: [] }) },
  formStore: { namespaced: true, state: () => ({ forms: [] }) },
  elementTemplateStore: { namespaced: true, state: () => ({ elementTemplates: [] }) },
  xmlStore: { namespaced: true, state: () => ({ xml: '' }) },
  modelerTranslations: {
    en: { version: 'Version', modeler: { save: 'Save' } },
    de: { version: 'Version (de)', modeler: { save: 'Speichern' } }
  }
}

/** A fresh loader per test, since it keeps the loaded package for the whole page. */
async function freshLoader({ packageFails = false } = {}) {
  vi.resetModules()
  vi.doMock('@/i18n', () => ({ i18n, registerTranslationLoader }))
  vi.doMock('cibseven-modeler/dist/cibseven-modeler.css', () => ({}))
  let fail = packageFails
  vi.doMock('cibseven-modeler', () => {
    if (fail) {
      fail = false
      throw new Error('Failed to fetch dynamically imported module')
    }
    return modelerPackage
  })
  return import('@/utils/modelerLoader.js')
}

describe('modelerLoader', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    Object.keys(messages).forEach(lang => delete messages[lang])
  })

  it('registers the modeler store modules once the package has loaded', async () => {
    const { loadModeler } = await freshLoader()
    const store = createStore({ modules: {} })

    const loaded = await loadModeler(store)

    expect(loaded.processStore).toBe(modelerPackage.processStore)
    expect(store.hasModule('modeler')).toBe(true)
    expect(store.state.modeler.processes).toEqual({ processes: [] })
    expect(store.state.modeler.elementTemplates).toEqual({ elementTemplates: [] })
  })

  /** The view is opened again and again; the store and the translations are set up the first time only. */
  it('sets the store and the translations up only once', async () => {
    const { loadModeler } = await freshLoader()
    const store = createStore({ modules: {} })
    const registerModule = vi.spyOn(store, 'registerModule')

    await loadModeler(store)
    await loadModeler(store)

    expect(registerModule).toHaveBeenCalledTimes(1)
    expect(registerTranslationLoader).toHaveBeenCalledTimes(1)
  })

  /** Every language, the current one and each one switched to later, gets the modeler's messages. */
  it('merges the modeler translations for each language through the registered loader', async () => {
    const { loadModeler } = await freshLoader()

    await loadModeler(createStore({ modules: {} }))
    const merge = registerTranslationLoader.mock.calls[0][0]
    merge('de')
    merge('fr')

    expect(messages.de).toEqual({ version: 'Version (de)', modeler: { save: 'Speichern' } })
    // A language the modeler does not speak gets its English
    expect(messages.fr.modeler.save).toBe('Save')
  })

  /**
   * The modeler's messages arrive last, so they go underneath: a theme or the application that
   * rewords one of its keys keeps its wording, as when the modeler was merged at startup.
   */
  it('keeps what is already there for a modeler key', async () => {
    const { loadModeler } = await freshLoader()
    messages.en = { modeler: { save: 'Store' }, start: { title: 'Start' } }

    await loadModeler(createStore({ modules: {} }))
    registerTranslationLoader.mock.calls[0][0]('en')

    expect(messages.en).toEqual({ version: 'Version', modeler: { save: 'Store' }, start: { title: 'Start' } })
  })

  /** An edition layers its own messages over the modeler's, and still underneath what is there. */
  it('layers the messages of an edition over those of the modeler', async () => {
    const { loadModeler } = await freshLoader()
    messages.en = { modeler: { save: 'Store' } }

    await loadModeler(createStore({ modules: {} }), { messages: () => ({ version: 'Edition version', modeler: { save: 'Edition save' } }) })
    registerTranslationLoader.mock.calls[0][0]('en')

    expect(messages.en).toEqual({ version: 'Edition version', modeler: { save: 'Store' } })
  })

  it('leaves a store alone that has the modeler modules already', async () => {
    const { installModeler } = await freshLoader()
    const store = createStore({ modules: { modeler: { namespaced: true, state: () => ({ own: true }) } } })

    installModeler(store, modelerPackage)

    expect(store.state.modeler).toEqual({ own: true })
  })

  /** A chunk that failed to load, after a deployment for instance, is fetched again on the next opening. */
  it('tries the package again after it failed to load', async () => {
    const { loadModeler } = await freshLoader({ packageFails: true })
    const store = createStore({ modules: {} })

    await expect(loadModeler(store)).rejects.toThrow()
    expect(store.hasModule('modeler')).toBe(false)

    await expect(loadModeler(store)).resolves.toBeDefined()
    expect(store.hasModule('modeler')).toBe(true)
  })
})
