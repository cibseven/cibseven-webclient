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
import { pluginRuntimeImportMap } from '@/plugins/pluginImportMap.js'

/**
 * The import map is what makes a separately built plugin share the application's
 * Vue instead of bundling a second one. Losing an entry breaks every deployed
 * plugin at once, and silently: reactivity, inject and the slot registry stop
 * working without an error. Hence a test on the generated tag.
 */
function mapOf(ctx) {
  const result = pluginRuntimeImportMap('./plugin-runtime.js').transformIndexHtml('<html></html>', ctx)
  return JSON.parse(result.tags[0].children)
}

const BUILD = { path: '/index.html' }
const DEV = { path: '/index.html', server: {} }

describe('plugin runtime import map', () => {

  it('maps the runtime and vue to the application build', () => {
    expect(mapOf(BUILD).imports).toEqual({
      '@cibseven/plugin-runtime': './plugin-runtime.js',
      vue: './plugin-runtime.js'
    })
  })

  /** 'vue' has to resolve here too: a compiled component imports it by name. */
  it('points vue at the same module as the runtime', () => {
    const imports = mapOf(BUILD).imports
    expect(imports.vue).toBe(imports['@cibseven/plugin-runtime'])
  })

  /** While developing the runtime is served from src, so the URL differs. */
  it('uses the served source while developing', () => {
    expect(mapOf(DEV).imports).toEqual({
      '@cibseven/plugin-runtime': '/src/plugin-runtime.js',
      vue: '/src/plugin-runtime.js'
    })
  })

  it('is injected into the head, before the application scripts run', () => {
    const tag = pluginRuntimeImportMap('./plugin-runtime.js').transformIndexHtml('<html></html>', BUILD).tags[0]

    expect(tag.tag).toBe('script')
    expect(tag.attrs.type).toBe('importmap')
    expect(tag.injectTo).toBe('head-prepend')
  })

  it('adds nothing to pages other than index.html', () => {
    expect(pluginRuntimeImportMap('./plugin-runtime.js').transformIndexHtml('<html></html>', { path: '/sso-login.html' }))
      .toBeUndefined()
  })
})
