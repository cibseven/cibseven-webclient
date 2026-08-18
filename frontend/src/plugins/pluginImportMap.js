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

/**
 * Vite plugin that injects the import map plugins are resolved through.
 *
 * A plugin is built separately and imports 'vue' and '@cibseven/plugin-runtime'
 * by name, without bundling them. The map is what turns those names into the
 * application's own modules; losing an entry gives the plugin a second Vue, and
 * reactivity, provide/inject and the slot registry then stop working without an
 * error. The map is generated rather than written into index.html because the
 * runtime is served from a different URL while developing.
 *
 * @param {string} builtUrl - URL of the runtime chunk in a build
 * @param {string} servedUrl - URL of the runtime while developing
 */
export function pluginRuntimeImportMap(builtUrl, servedUrl = '/src/plugin-runtime.js') {
  return {
    name: 'cibseven-plugin-runtime-import-map',
    transformIndexHtml(html, ctx) {
      if (!ctx.path.endsWith('/index.html')) return
      const url = ctx.server ? servedUrl : builtUrl
      const importMap = {
        imports: {
          '@cibseven/plugin-runtime': url,
          // A plugin built from single-file components imports 'vue'; it must
          // resolve to our instance, never to a second Vue runtime.
          vue: url
        }
      }
      return {
        html,
        tags: [{
          tag: 'script',
          attrs: { type: 'importmap' },
          children: JSON.stringify(importMap, null, 2),
          injectTo: 'head-prepend'
        }]
      }
    }
  }
}
