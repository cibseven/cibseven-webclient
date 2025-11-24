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
import './assets/main.css'  // Application-specific styles
import '@mdi/font/css/materialdesignicons.css'
import { axios } from './globals.js'

import { createApp } from 'vue'

import store from './store'
import { createAppRouter, appRoutes } from './router.js'
import registerOwnComponents from './register.js'
import { permissionsMixin }  from './permissions.js'

import { InfoService, AuthService, setServicesBasePath } from './services.js'
import { getTheme, hasHeader, isMobile, checkExternalReturn } from './utils/init'
import { applyTheme, handleAxiosError, fetchAndStoreProcesses, fetchDecisionsIfEmpty, setupTaskNotifications } from './utils/init'
import { applyConfigDefaults } from './utils/config.js'
import { i18n, switchLanguage } from './i18n'

// check for token inside hash
// if it exists => redirect to new uri
checkExternalReturn(window.location.href, window.location.hash)

/**
 * Load theme CSS dynamically based on configuration
 * @param {string} themeName - Name of the theme to load
 */
async function loadThemeCSS(themeName) {
  await import(`./styles/themes/${themeName}/${themeName}.js`);
}

Promise.all([
  axios.get('config.json').then(response => {
    // Check if response data is not an object
    if (typeof response.data !== 'object' || response.data === null) {
      console.warn('Received non-object response for config.json, using defaults')
      return { data: {} }
    }
    return response
  }),
  InfoService.getProperties()
]).then(responses => {
  // Apply defaults before merging
  const configFromFile = applyConfigDefaults(responses[0].data)
  Object.assign(configFromFile, responses[1].data)
  var config = configFromFile

  setServicesBasePath(config.servicesBasePath)

  // (Optional) check if possible
  //axios.defaults.baseURL = appConfig.adminBasePath

  // Load theme CSS and static assets (favicon, etc.)
  var theme = getTheme(config)
  loadThemeCSS(theme).then(() => {
    applyTheme(theme)
    switchLanguage(config, i18n.global.locale).then(() => {
      const app = createApp({ /*jshint nonew:false */
        name: 'CIB7App',
        el: '#app',
        mixins: [permissionsMixin],
        provide: function() {
          return {
            currentLanguage(lang) {
              // get language
              if (!lang) return i18n.global.locale
              // set language
              return switchLanguage(config, lang).then(() => {
                return i18n.global.locale
              })
            },
            loadProcesses(extraInfo) {
                return fetchAndStoreProcesses(this, this.$store, config, extraInfo)
            },
            async loadDecisions() {
              return fetchDecisionsIfEmpty(this.$store)
            },
            isMobile: isMobile,
            AuthService: AuthService
          }
        },
        data: function() {
          return {
            user: null,
            config: config,
            consent: localStorage.getItem('consent'),
            logoPath: 'themes/' + theme + '/logo.svg',
            loginImgPath: 'themes/' + theme + '/login-image.svg',
            resetPasswordImgPath: 'webjars/seven/components/password/reset-password.svg',
            theme: theme,
            header: hasHeader(),
            processUpdateInterval: null
          }
        },
        watch: {
          user: function(user) {
            if (user) {
              this.handleTaskWorker()
              this.startProcessAutoUpdate()
            } else {
              this.stopProcessAutoUpdate()
            }
          }
        },
        mounted: function() {
          if ('Notification' in window && this.config.notifications.tasks.enabled &&
            (Notification.permission !== 'granted' || Notification.permission !== 'denied')) {
            Notification.requestPermission()
          }
        },
        methods: {
          remember: function() { localStorage.setItem('consent', true) },
          sendReport: function(data) { axios.post('report', data) },
          handleTaskWorker: function() {
            setupTaskNotifications(this, this.$root, theme)
          },
          startProcessAutoUpdate: function() {
            if (this.config.processes?.autoUpdate?.enabled && this.config.processes?.autoUpdate?.interval) {
              this.stopProcessAutoUpdate() // Clear any existing interval
              const interval = Math.max(this.config.processes.autoUpdate.interval, 15000) // Minimum 15 seconds
              this.processUpdateInterval = setInterval(() => {
                fetchAndStoreProcesses(this, this.$store, this.config, true)
              }, interval)
            }
          },
          stopProcessAutoUpdate: function() {
            if (this.processUpdateInterval) {
              clearInterval(this.processUpdateInterval)
              this.processUpdateInterval = null
            }
          }
        }
      })

      registerOwnComponents(app)

      const router = createAppRouter(appRoutes)
      app.use(router)
      app.use(store)
      app.use(i18n)
      const root = app.mount('#app')
      router.setRoot(root)

      axios.interceptors.response.use(
        res => res.data,
        error => {
          return handleAxiosError(router, root, error)
        }
      )

      return config
    })
  })
})
