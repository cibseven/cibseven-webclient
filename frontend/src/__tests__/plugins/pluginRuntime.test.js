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
import { describe, it, expect, beforeEach, vi } from 'vitest'
import * as runtime from '@/plugin-runtime.js'
import * as vue from 'vue'
import { ref, reactive, nextTick } from 'vue'
import { axios } from '@/globals.js'
import { ProcessService } from '@/services.js'
import { getPlugin, resetPlugins, PLUGIN_API_VERSION } from '@/plugins/pluginsConfig.js'
import { setPluginContext } from '@/plugins/pluginContext.js'
import { setPluginRouter } from '@/plugins/pluginNavigation.js'
import { i18n } from '@/i18n'

describe('plugin-runtime', () => {
  beforeEach(() => {
    resetPlugins()
  })

  describe('shared instances', () => {
    // The whole design rests on this: a plugin importing from the runtime must
    // receive the application's own instances, not a second copy.
    it('exposes the same Vue instance the application uses', () => {
      expect(runtime.vue.ref).toBe(vue.ref)
      expect(runtime.ref).toBe(vue.ref)
      expect(runtime.vue.defineComponent).toBe(vue.defineComponent)
    })

    it('exposes the application axios instance, with its interceptors', () => {
      expect(runtime.axios).toBe(axios)
    })

    it('exposes the application services', () => {
      expect(runtime.services.ProcessService).toBe(ProcessService)
      expect(typeof runtime.services.getServicesBasePath).toBe('function')
    })

    it('exposes the application i18n instance', () => {
      expect(runtime.i18n).toBe(i18n)
    })

    it('shares reactivity across the runtime boundary', async () => {
      // Would fail with two Vue runtimes: the effect would never be triggered
      const state = reactive({ count: 0 })
      const seen = []
      runtime.vue.watchEffect(() => seen.push(state.count))
      state.count = 1
      await nextTick()

      expect(seen).toEqual([0, 1])
    })

    it('reports a stable runtime identity', () => {
      expect(runtime.getRuntimeInfo().apiVersion).toBe(PLUGIN_API_VERSION)
      expect(runtime.getRuntimeInfo().instance).toBe(runtime.getRuntimeInfo().instance)
    })
  })

  describe('registry access', () => {
    it('registers into the same registry the application reads', () => {
      const component = { name: 'FromPlugin' }
      runtime.registerPlugin('demo', component, { pluginId: 'demo' })

      expect(getPlugin('demo').value).toMatchObject([{ component, pluginId: 'demo' }])
    })

    it('sees slot updates reactively', async () => {
      const slot = runtime.getPlugin('demo')
      const seen = []
      runtime.vue.watch(slot, value => seen.push(value.length))
      runtime.registerPlugin('demo', { name: 'FromPlugin' })
      await nextTick()

      expect(seen).toEqual([1])
    })
  })

  describe('context', () => {
    it('hands the application config to plugins', () => {
      setPluginContext({ config: { theme: 'cib' } })

      expect(runtime.getContext().config).toEqual({ theme: 'cib' })
    })

    /** A plugin reads how the application is configured; it does not reconfigure it. */
    it('hands over a copy, so a plugin cannot change the application config', () => {
      const config = { theme: 'cib', nested: { pluginsEnabled: true } }
      setPluginContext({ config })

      const handed = runtime.getContext().config
      expect(handed).not.toBe(config)
      expect(handed.nested).not.toBe(config.nested)
      expect(() => { handed.theme = 'other' }).toThrow()
      expect(() => { handed.nested.pluginsEnabled = false }).toThrow()
      expect(config).toEqual({ theme: 'cib', nested: { pluginsEnabled: true } })
    })

    /** Freezing only the config would still let a plugin swap the whole of it. */
    it('hands over a context a plugin cannot put another config on', () => {
      setPluginContext({ config: { theme: 'cib' } })

      const handed = runtime.getContext()
      expect(() => { handed.config = { theme: 'other' } }).toThrow()
      expect(runtime.getContext().config).toEqual({ theme: 'cib' })
    })

    /** The store is product state: plugins go through the services instead. */
    it('does not expose the store', () => {
      setPluginContext({ config: {}, store: { commit: () => {} } })

      expect(runtime.getContext().store).toBeUndefined()
    })
  })

  describe('navigation', () => {
    function routerStub() {
      return {
        push: vi.fn(() => Promise.resolve()),
        replace: vi.fn(() => Promise.resolve()),
        currentRoute: { value: { name: 'process', path: '/seven/auth/process', params: { id: '1' }, query: {}, hash: '' } }
      }
    }

    it('sends the user somewhere through the application router', async () => {
      const router = routerStub()
      setPluginRouter(router)

      await runtime.navigation.push({ name: 'process', params: { id: '1' } })
      await runtime.navigation.replace('/seven/auth/tasks')

      expect(router.push).toHaveBeenCalledWith({ name: 'process', params: { id: '1' } })
      expect(router.replace).toHaveBeenCalledWith('/seven/auth/tasks')
    })

    /** A snapshot to read: mutating it must not reach the live route. */
    it('reports the current route as a copy', () => {
      const router = routerStub()
      setPluginRouter(router)

      const route = runtime.navigation.currentRoute()
      expect(route).toEqual({ name: 'process', path: '/seven/auth/process', params: { id: '1' }, query: {}, hash: '' })
      route.params.id = 'tampered'
      expect(router.currentRoute.value.params.id).toBe('1')
    })

    /** Plugins register before the router exists, so the methods resolve it lazily. */
    it('warns instead of failing when used before the router exists', () => {
      setPluginRouter(null)
      const warn = vi.spyOn(console, 'warn').mockImplementation(() => {})

      expect(runtime.navigation.push('/somewhere')).toBeUndefined()
      expect(runtime.navigation.currentRoute()).toBeNull()
      expect(warn).toHaveBeenCalled()
      warn.mockRestore()
    })

    it('does not hand over the router itself', () => {
      expect(runtime.navigation.router).toBeUndefined()
      expect(Object.keys(runtime.navigation)).toEqual(['push', 'replace', 'currentRoute'])
    })
  })

  describe('mergeTranslations', () => {
    it('namespaces plugin messages so they cannot collide with ours', () => {
      const spy = vi.spyOn(i18n.global, 'mergeLocaleMessage').mockImplementation(() => {})
      runtime.mergeTranslations('demo', 'en', { title: 'Demo' })

      expect(spy).toHaveBeenCalledWith('en', { plugins: { demo: { title: 'Demo' } } })
      spy.mockRestore()
    })
  })

  describe('vue exports', () => {
    it('re-exports vue bare, so a plugin built from single-file components works', () => {
      // An import map maps 'vue' to this module; a precompiled component's
      // imports must therefore resolve here.
      expect(runtime.ref).toBe(ref)
      expect(runtime.h).toBe(vue.h)
      expect(runtime.createElementBlock).toBe(vue.createElementBlock)
    })
  })
})
