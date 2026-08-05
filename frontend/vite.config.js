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
import fs from 'node:fs'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

const backendUrl = 'http://localhost:8080/webapp'

// Detect build mode
/* eslint-disable no-undef */
const isLibrary = process.env.BUILD_MODE === 'library'
/* eslint-enable no-undef */

// https://flaviocopes.com/fix-dirname-not-defined-es-module-scope/
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

console.log('isLibrary', isLibrary)

// Plugins are loaded at runtime and are therefore not part of this build. They
// import the application's Vue, axios and services from 'plugin-runtime.js', and
// resolve that bare specifier through an import map. The map has to be injected
// here rather than written into index.html, because the runtime lives at a
// different URL while developing (served from src) than after a build.
const pluginRuntimeUrl = isLibrary ? null : './plugin-runtime.js'

function pluginRuntimeImportMap() {
  return {
    name: 'cibseven-plugin-runtime-import-map',
    transformIndexHtml(html, ctx) {
      if (!ctx.path.endsWith('/index.html')) return
      const url = ctx.server ? '/src/plugin-runtime.js' : pluginRuntimeUrl
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

// https://vite.dev/config/
export default defineConfig({
  base: './',
  plugins: [
    vue(),
    vueDevTools(),
    pluginRuntimeImportMap()
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
      // Plugin files are served by the backend from its classpath. The pattern is
      // a regex on purpose: a plain '/plugins' prefix would also catch
      // '/plugins.json', the manifest of a locally deployed plugin.
      '^/plugins/': {
        target: backendUrl,
        changeOrigin: true,
        secure: false,
        // A plugin copied into public/ wins, so plugins can be tried out without
        // packaging them for the backend.
        bypass: (req) => {
          const filePath = path.join(__dirname, 'public', req.url.split('?')[0])
          return fs.existsSync(filePath) ? req.url : undefined
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
