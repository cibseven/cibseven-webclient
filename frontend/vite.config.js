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
/* eslint-disable no-unused-vars */

import { fileURLToPath, URL } from 'node:url'
import path from 'node:path'

import { defineCibConfig } from '@cib/frontend-preset/vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import { pluginRuntimeImportMap } from './src/plugins/pluginImportMap.js'

const backendUrl = 'http://localhost:8080/webapp'

// Detect build mode
/* eslint-disable no-undef */
const isLibrary = process.env.BUILD_MODE === 'library'
/* eslint-enable no-undef */

// https://flaviocopes.com/fix-dirname-not-defined-es-module-scope/
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

console.log('isLibrary', isLibrary)
// Plugins are loaded at runtime and are therefore not part of this build: they
// resolve the application's Vue and services through the import map below.
const pluginRuntimeUrl = isLibrary ? null : './plugin-runtime.js'

// https://vite.dev/config/
export default defineCibConfig({
  test: {
    server: {
      deps: {
        inline: [/cibseven-modeler/, /@bpmn-io\/form-js/],
      },
    },
    setupFiles: ['src/__tests__/vitest.setup.js'],
    coverage: {
      exclude: [
        // built on its own, against the import map: its imports do not resolve here
        'plugin-example/**',

        // Entry-point scripts: their module scope boots the application, so importing
        // them in jsdom has no meaningful unit under test. 'app.js' mounts the SPA
        // ('createApp(...).mount("#app")'), 'sso-login.js' assigns 'location.href',
        // which jsdom refuses to navigate. Both are covered by the Playwright E2E suite.
        'src/app.js',
        'src/sso-login.js',
      ],
      // A floor, not a target. The values sit a couple of points under what the suite
      // currently achieves so an unrelated change cannot quietly erode coverage, which
      // is what AGENTS.md asks for ("never reduce overall coverage") but nothing
      // enforced before.
      //
      // Note how Vitest applies these: files matched by a glob key are checked against
      // that glob and are *excluded* from the global numbers. So the global values below
      // describe the remainder — src/components, src/embedded-form and the entry-level
      // modules under src/ — while the well-covered directories are held to 80% each.
      // 'perFile' is deliberately left off: the thresholds apply to each group as a
      // whole, so one legitimately hard-to-test file cannot fail the build on its own.
      thresholds: {
        lines: 40,
        statements: 40,
        functions: 23,
        branches: 27,

        // Brought to ~99% by the unit tests; 80% is the AGENTS.md bar for changed files.
        'src/store/**/*.js': { lines: 80, statements: 80, functions: 80, branches: 70 },
        'src/mixins/**/*.js': { lines: 80, statements: 80, functions: 80, branches: 70 },
        'src/utils/**/*.js': { lines: 80, statements: 80, functions: 80, branches: 70 },
        'src/plugins/**/*.js': { lines: 80, statements: 80, functions: 80, branches: 70 },
        'src/services.js': { lines: 80, statements: 80, functions: 80, branches: 70 },
      },
    },
  },
  base: './',
  plugins: [
    vue(),
    vueDevTools(),
    pluginRuntimeImportMap(pluginRuntimeUrl)
  ],
  resolve: {
    dedupe: ['bootstrap'],
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      vue: 'vue/dist/vue.esm-bundler.js',
    },
  },
  css: {
    preprocessorOptions: {
      scss: {
        // Suppress deprecation warnings from Bootstrap
        quietDeps: true,
        silenceDeprecations: ['legacy-js-api', 'import', 'global-builtin'],
        loadPaths: ['node_modules']
      }
    }
  },
  server: {
    proxy: {
      '/info': {
        target: backendUrl,
        changeOrigin: true,
        secure: false,
        configure: (proxy, _options) => {
          proxy.on('error', (err, _req, _res) => {
            console.log('proxy error', err)
          })
          proxy.on('proxyReq', (proxyReq, req, _res) => {
            //console.log('Sending Request to the Target:', req.method, backendUrl + req.url)
          })
          proxy.on('proxyRes', (proxyRes, req, _res) => {
            //console.log('Received Response from the Target:', proxyRes.statusCode, backendUrl + req.url)
          })
        },
      },
		  '/services': {
        target: backendUrl,
        changeOrigin: true,
        secure: false,
        configure: (proxy, _options) => {
          proxy.on('error', (err, _req, _res) => {
            console.log('proxy error', err)
          })
          proxy.on('proxyReq', (proxyReq, req, _res) => {
            //console.log('Sending Request to the Target:', req.method, backendUrl + req.url)
          })
          proxy.on('proxyRes', (proxyRes, req, _res) => {
            //console.log('Received Response from the Target:', proxyRes.statusCode, backendUrl + req.url)
          })
        },
      },
    },
  },
  build: isLibrary
    ? {
        lib: {
          entry: path.resolve(__dirname, 'src/library.js'),
          name: 'cibseven-components',
          formats: ['es', 'umd'],
          fileName: (format) => `cibseven-components.${format}.js`,
        },
        rollupOptions: {
          external: [
            /^\/assets\/images\//,
            'axios',
            'bootstrap',
            /^cibseven-modeler/,
            'vue',
            'vue-i18n',
            'vue-router',
          ],
          output: {
            globals: {
              vue: 'Vue',
              bootstrap: 'bootstrap',
              'vue-i18n': 'VueI18n',
              'vue-router': 'VueRouter',
              axios: 'axios',
              'cibseven-modeler': 'CibsevenModeler',
            },
            // Ensure CSS is extracted and placed in the dist folder
            assetFileNames: 'cibseven-components.[ext]',
            inlineDynamicImports: true,
          },
        },
        cssCodeSplit: true, // Ensure CSS is extracted into a separate file
        outDir: 'dist', // The output directory
      }
    : {
      rollupOptions: {
        input: {
          main: path.resolve(__dirname, 'index.html'),
          ssoLogin: path.resolve(__dirname, 'sso-login.html'),
          embeddedForms: path.resolve(__dirname, 'embedded-forms.html'),
          // Public API for runtime-loaded plugins. As an entry of this build it
          // shares Vue, axios and services with the application instead of
          // bundling its own copies.
          pluginRuntime: path.resolve(__dirname, 'src/plugin-runtime.js'),
        },
        // The html entries export nothing, so Vite defaults to dropping entry
        // exports - which would strip and rename the plugin runtime's API.
        // Keeping signatures strict makes its exports survive the build.
        preserveEntrySignatures: 'strict',
        output: {
          // Plugins reference the runtime through a fixed URL, so this one entry
          // must not be content-hashed.
          entryFileNames: chunk =>
            chunk.name === 'pluginRuntime' ? 'plugin-runtime.js' : 'assets/[name]-[hash].js',
        }
      }
    }
})
