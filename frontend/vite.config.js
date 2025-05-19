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

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

const backendUrl = 'http://localhost:8080/webapp'
//Needed for Embedded forms
const engineRestUrl = 'http://localhost:8080/'

// Detect build mode
/* eslint-disable no-undef */
const isLibrary = process.env.BUILD_MODE === 'library'
/* eslint-enable no-undef */

// https://flaviocopes.com/fix-dirname-not-defined-es-module-scope/
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

console.log('isLibrary', isLibrary)

// https://vite.dev/config/
export default defineConfig({
  base: './',
  plugins: [
    vue(),
    vueDevTools()
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      vue: 'vue/dist/vue.esm-bundler.js',
    },
  },
  server: {
    proxy: {
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
      '/engine-rest': {
        target: engineRestUrl,
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
	  //Add embedded form root urls
     '/camunda-invoice': {
       target: engineRestUrl,
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
     }
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
          external: ['vue', /^\/assets\/images\//],
          output: {
            globals: {
              vue: 'Vue',
              // No name was provided for external module "/assets/images/start/process.svg" in "output.globals" – guessing "_imports_0".
              "/assets/images/start/process.svg": "ProcessImage",
              "/assets/images/start/task.svg": "TaskImage",
              "/assets/images/start/management.svg": "ManagementImage",
              "/assets/images/start/admin.svg": "AdminImage",
              "/assets/images/start/empty_start_page.svg": "EmptyStartImage",
              "/assets/images/process/empty_processes_list_dark_background.svg": "EmptyProcessesDark",
              "/assets/images/process/empty_processes_list.svg": "EmptyProcesses",
              "/assets/images/decision/empty_processes_list.svg": "EmptyDecisionProcesses",
              "/assets/images/task/no_tasks_pending.svg": "NoTasksPending",
              "/assets/images/task/no_tasks.svg": "NoTasks",
              "/assets/images/task/tasklist_empty.svg": "TasklistEmpty",
              "/assets/images/task/asign_task.svg": "AssignTask"
            },
            // Ensure CSS is extracted and placed in the dist folder
            assetFileNames: 'cibseven-components.[ext]',
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
          embeddedForms: path.resolve(__dirname, 'embedded-forms.html')
        }
      }
    }
})
