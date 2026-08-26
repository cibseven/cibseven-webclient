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
import { fetchPluginManifests, loadPlugins, initPlugins } from '@/plugins/pluginLoader.js'
import { setPluginContext } from '@/plugins/pluginContext.js'
import { axios } from '@/globals.js'
import { i18n } from '@/i18n'

vi.mock('@/globals.js', () => ({
  axios: { create: vi.fn() }
}))

vi.mock('@/i18n', () => ({
  i18n: { global: { mergeLocaleMessage: vi.fn() } }
}))

const validManifest = { id: 'demo', entry: 'index.js', apiVersion: '1' }

// Serves the manifest file and, optionally, plugin translation files
function mockHttp(responses) {
  const get = vi.fn(url => {
    const match = Object.keys(responses).find(key => url.includes(key))
    if (!match) return Promise.reject(new Error('404'))
    return Promise.resolve({ data: responses[match] })
  })
  axios.create.mockReturnValue({ get })
  return get
}

describe('pluginLoader', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setPluginContext({ config: null })
    document.head.querySelectorAll('link[data-plugin]').forEach(link => link.remove())
    vi.spyOn(console, 'warn').mockImplementation(() => {})
    vi.spyOn(console, 'error').mockImplementation(() => {})
    vi.spyOn(console, 'debug').mockImplementation(() => {})
    vi.spyOn(console, 'info').mockImplementation(() => {})
  })

  describe('fetchPluginManifests', () => {
    it('reads the plugins the backend discovered on its classpath', async () => {
      const get = mockHttp({ 'info/plugins': { plugins: [validManifest] } })

      await expect(fetchPluginManifests()).resolves.toEqual([validManifest])
      expect(get).toHaveBeenCalledWith(expect.stringContaining('info/plugins'))
    })

    it('returns an empty list when the backend reports no plugins', async () => {
      mockHttp({ 'info/plugins': { plugins: [] } })

      await expect(fetchPluginManifests()).resolves.toEqual([])
    })

    it('returns an empty list when the endpoint is unavailable', async () => {
      mockHttp({})

      await expect(fetchPluginManifests()).resolves.toEqual([])
    })

    it('returns an empty list when the response has no plugins array', async () => {
      mockHttp({ 'info/plugins': { plugins: 'not-an-array' } })

      await expect(fetchPluginManifests()).resolves.toEqual([])
      expect(console.warn).toHaveBeenCalled()
    })
  })

  describe('loadPlugins', () => {
    it('imports the plugin and lets it register its contributions', async () => {
      mockHttp({})
      const register = vi.fn()
      const importer = vi.fn(() => Promise.resolve({ register }))

      await expect(loadPlugins([validManifest], 'en', importer)).resolves.toEqual(['demo'])
      expect(importer).toHaveBeenCalledWith(expect.stringContaining('plugins/demo/index.js'))
      expect(register).toHaveBeenCalledWith({
        id: 'demo',
        baseUrl: expect.stringContaining('plugins/demo/')
      })
    })

    /**
     * Loading happens before the first render, so a plugin whose file never
     * arrives would otherwise leave the application on a blank page for good.
     */
    it('gives up on a plugin whose import never settles', async () => {
      vi.useFakeTimers()
      mockHttp({})
      const importer = vi.fn(() => new Promise(() => {}))

      const loading = loadPlugins([validManifest], 'en', importer)
      await vi.advanceTimersByTimeAsync(11000)

      await expect(loading).resolves.toEqual([])
      vi.useRealTimers()
    })

    it('accepts a default export as register function', async () => {
      mockHttp({})
      const register = vi.fn()

      await expect(loadPlugins([validManifest], 'en', () => Promise.resolve({ default: register })))
        .resolves.toEqual(['demo'])
      expect(register).toHaveBeenCalled()
    })

    it('rejects a plugin built against a different API version', async () => {
      mockHttp({})
      const importer = vi.fn()

      await expect(loadPlugins([{ ...validManifest, apiVersion: '2' }], 'en', importer))
        .resolves.toEqual([])
      expect(importer).not.toHaveBeenCalled()
      expect(console.warn).toHaveBeenCalledWith(expect.stringContaining('"2"'))
    })

    // One published build can serve several webclient versions, so a plugin may name
    // every version it was tested against.
    it('accepts a plugin that lists this API version among several', async () => {
      mockHttp({})
      const register = vi.fn()

      await expect(loadPlugins([{ ...validManifest, apiVersion: ['0', '1'] }], 'en',
        () => Promise.resolve({ register }))).resolves.toEqual(['demo'])
      expect(register).toHaveBeenCalled()
    })

    it('rejects a plugin whose list leaves this API version out', async () => {
      mockHttp({})
      const importer = vi.fn()

      await expect(loadPlugins([{ ...validManifest, apiVersion: ['2', '3'] }], 'en', importer))
        .resolves.toEqual([])
      expect(importer).not.toHaveBeenCalled()
      expect(console.warn).toHaveBeenCalledWith(expect.stringContaining('"2", "3"'))
    })

    it('skips a manifest without id or entry', async () => {
      mockHttp({})
      const importer = vi.fn()

      await expect(loadPlugins([{ apiVersion: '1' }], 'en', importer)).resolves.toEqual([])
      expect(importer).not.toHaveBeenCalled()
    })

    it('skips a plugin that exports no register function', async () => {
      mockHttp({})

      await expect(loadPlugins([validManifest], 'en', () => Promise.resolve({})))
        .resolves.toEqual([])
      expect(console.warn).toHaveBeenCalled()
    })

    it('survives a plugin that cannot be imported', async () => {
      mockHttp({})

      await expect(loadPlugins([validManifest], 'en', () => Promise.reject(new Error('boom'))))
        .resolves.toEqual([])
      expect(console.error).toHaveBeenCalled()
    })

    it('survives a plugin whose register function throws', async () => {
      mockHttp({})
      const register = vi.fn(() => { throw new Error('boom') })

      await expect(loadPlugins([validManifest], 'en', () => Promise.resolve({ register })))
        .resolves.toEqual([])
      expect(console.error).toHaveBeenCalled()
    })

    it('loads the remaining plugins when one of them fails', async () => {
      mockHttp({})
      const other = { id: 'other', entry: 'index.js', apiVersion: '1' }
      const importer = vi.fn(url => url.includes('/demo/')
        ? Promise.reject(new Error('boom'))
        : Promise.resolve({ register: vi.fn() }))

      await expect(loadPlugins([validManifest, other], 'en', importer)).resolves.toEqual(['other'])
    })

    it('adds the stylesheets a plugin ships', async () => {
      mockHttp({})
      const manifest = { ...validManifest, styles: ['styles.css', 'extra.css'] }

      await loadPlugins([manifest], 'en', () => Promise.resolve({ register: vi.fn() }))

      const links = [...document.head.querySelectorAll('link[data-plugin="demo"]')]
      expect(links.map(link => link.getAttribute('rel'))).toEqual(['stylesheet', 'stylesheet'])
      expect(links[0].href).toContain('plugins/demo/styles.css')
      expect(links[1].href).toContain('plugins/demo/extra.css')
    })

    it('adds no stylesheet when the plugin ships none', async () => {
      mockHttp({})

      await loadPlugins([validManifest], 'en', () => Promise.resolve({ register: vi.fn() }))

      expect(document.head.querySelector('link[data-plugin="demo"]')).toBeNull()
    })

    it('adds the stylesheets before the plugin registers anything', async () => {
      mockHttp({})
      const manifest = { ...validManifest, styles: ['styles.css'] }
      let stylesWhenRegistering = 0
      const register = vi.fn(() => {
        stylesWhenRegistering = document.head.querySelectorAll('link[data-plugin="demo"]').length
      })

      await loadPlugins([manifest], 'en', () => Promise.resolve({ register }))

      expect(stylesWhenRegistering).toBe(1)
    })

    it('merges plugin translations under the plugins namespace', async () => {
      mockHttp({ 'translations_de.json': { title: 'Demo' } })
      const manifest = { ...validManifest, translations: { de: 'translations_de.json' } }

      await loadPlugins([manifest], 'de', () => Promise.resolve({ register: vi.fn() }))

      expect(i18n.global.mergeLocaleMessage).toHaveBeenCalledWith('de', {
        plugins: { demo: { title: 'Demo' } }
      })
    })

    it('loads a plugin whose translations are missing for the active language', async () => {
      mockHttp({})
      const manifest = { ...validManifest, translations: { de: 'translations_de.json' } }

      await expect(loadPlugins([manifest], 'de', () => Promise.resolve({ register: vi.fn() })))
        .resolves.toEqual(['demo'])
      expect(i18n.global.mergeLocaleMessage).not.toHaveBeenCalled()
    })
  })

  describe('initPlugins', () => {
    it('asks for nothing when the backend reports plugins as disabled', async () => {
      const get = mockHttp({ 'info/plugins': { plugins: [validManifest] } })
      setPluginContext({ config: { pluginsEnabled: false } })
      const importer = vi.fn()

      await expect(initPlugins('en', importer)).resolves.toEqual([])
      expect(get).not.toHaveBeenCalled()
      expect(importer).not.toHaveBeenCalled()
    })

    it('asks when the backend does not report the flag at all', async () => {
      mockHttp({ 'info/plugins': { plugins: [validManifest] } })
      setPluginContext({ config: {} })

      await expect(initPlugins('en', () => Promise.resolve({ register: vi.fn() })))
        .resolves.toEqual(['demo'])
    })

    it('loads the plugins the backend reports', async () => {
      mockHttp({ 'info/plugins': { plugins: [validManifest] } })
      const register = vi.fn()

      await expect(initPlugins('en', () => Promise.resolve({ register }))).resolves.toEqual(['demo'])
      expect(register).toHaveBeenCalled()
    })

    it('does nothing when no plugin is deployed', async () => {
      mockHttp({})
      const importer = vi.fn()

      await expect(initPlugins('en', importer)).resolves.toEqual([])
      expect(importer).not.toHaveBeenCalled()
    })
  })
})
