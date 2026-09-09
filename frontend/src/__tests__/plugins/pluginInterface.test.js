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
import fs from 'node:fs'
import path from 'node:path'
import * as vue from 'vue'
import * as runtime from '@/plugin-runtime.js'
import { loadPlugins } from '@/plugins/pluginLoader.js'
import { setPluginContext } from '@/plugins/pluginContext.js'
import { resetPlugins } from '@/plugins/pluginsConfig.js'
import { setServicesBasePath } from '@/services.js'
import { findComponents } from '../utils.js'

/**
 * Freezes the interface plugins are built against, which the rest of this folder
 * only tests the behaviour of.
 *
 * A plugin is built separately and deployed as a jar, so it meets whichever
 * webclient it is installed into. PLUGIN_API_VERSION is what tells the two
 * whether they fit, and it is only worth anything if it rises whenever this
 * interface changes. Every list below therefore has to be updated in the same
 * commit as the change it describes - and raising the minor version in
 * frontend/package.json is part of that change, not an afterthought.
 *
 * Nothing here may change unnoticed: a renamed export, a dropped slot prop, a new
 * manifest field and a new slot are all reasons for a new version.
 */

// eslint-disable-next-line no-undef
const srcDir = path.resolve(__dirname, '../../../src')

/** What '@cibseven/plugin-runtime' exports, next to the bare re-exports of vue. */
const RUNTIME_EXPORTS = [
  'PLUGIN_API_VERSION',
  'axios',
  'getContext',
  'getPlugin',
  'getRuntimeInfo',
  'i18n',
  'mergeTranslations',
  'navigation',
  'registerPlugin',
  'services',
  'vue'
]

/** What 'register' is called with, once, while the plugin is loaded. */
const REGISTER_ARGUMENT = ['id', 'baseUrl', 'registerPlugin']

const NAVIGATION = ['push', 'replace', 'currentRoute']

const CONTEXT = ['config']

/** The manifest fields the loader acts on; 'slots' is documentation only. */
const MANIFEST_FIELDS = ['apiVersion', 'entry', 'id', 'styles', 'translations']

/** The slots the application renders, and the props each hands a contribution. */
const SLOTS = {
  'process-instance-tab': ['instance', 'process', 'tenantId'],
  'decision-definition-tab': ['decision', 'tenantId']
}

const BUMP = 'the plugin interface changed - raise PLUGIN_API_VERSION (the minor in frontend/package.json) and update this list'

describe('plugin interface', () => {
  beforeEach(() => {
    setServicesBasePath('services/v1')
    setPluginContext({ config: {} })
    resetPlugins()
    vi.spyOn(console, 'info').mockImplementation(() => {})
  })

  it('exports what plugins import, and nothing else', () => {
    // Vue's own names arrive through the bare re-export and are not ours to freeze
    const own = Object.keys(runtime).filter(name => !(name in vue))

    expect(own.sort(), BUMP).toEqual([...RUNTIME_EXPORTS].sort())
  })

  /**
   * A plugin built from single-file components imports 'vue', which the import
   * map resolves to the runtime - so every name Vue offers has to arrive through
   * it, whatever a Vue upgrade adds.
   */
  it('re-exports the whole of vue', () => {
    const missing = Object.keys(vue).filter(name => name !== 'default' && !(name in runtime))

    expect(missing, BUMP).toEqual([])
  })

  it('calls register with the documented argument', async () => {
    const register = vi.fn()

    await loadPlugins(
      [{ id: 'demo', entry: 'index.js', apiVersion: runtime.PLUGIN_API_VERSION }],
      'en',
      () => Promise.resolve({ register }))

    expect(Object.keys(register.mock.calls[0][0]), BUMP).toEqual(REGISTER_ARGUMENT)
  })

  it('offers the documented navigation capability', () => {
    expect(Object.keys(runtime.navigation), BUMP).toEqual(NAVIGATION)
  })

  it('hands over the documented context', () => {
    expect(Object.keys(runtime.getContext()), BUMP).toEqual(CONTEXT)
  })

  /**
   * Read from the loader rather than listed twice: a field it stops acting on is
   * one deployed plugins still declare, and one it starts acting on is a manifest
   * older webclients cannot serve.
   */
  it('acts on the documented manifest fields', () => {
    const loader = fs.readFileSync(path.join(srcDir, 'plugins/pluginLoader.js'), 'utf-8')

    const read = new Set([...loader.matchAll(/manifest\??\.(\w+)/g)].map(match => match[1]))

    expect([...read].sort(), BUMP).toEqual([...MANIFEST_FIELDS].sort())
  })

  /**
   * Taken from the templates, so a renamed slot or a dropped prop fails here
   * instead of in a plugin that silently renders nothing any more.
   */
  it('renders the documented slots, with the documented props', () => {
    const rendered = {}

    findComponents(srcDir, '.vue')
      .filter(file => !file.includes('__tests__'))
      .forEach(file => {
        const source = fs.readFileSync(file, 'utf-8')
        for (const [tag] of source.matchAll(/<PluginSlot\b[^>]*>/g)) {
          const name = tag.match(/name="([^"]+)"/)?.[1]
          // The params are a flat object literal, one key per prop handed over
          const params = tag.match(/:params="\{([^}]*)\}"/)?.[1]
          expect(name, `a PluginSlot in ${path.basename(file)} names no slot`).toBeTruthy()
          expect(params, `the PluginSlot "${name}" hands over no params`).toBeTruthy()
          rendered[name] = [...params.matchAll(/(\w+)\s*:/g)].map(match => match[1])
        }
      })

    expect(rendered, BUMP).toEqual(SLOTS)
  })
})
