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

import { i18n, switchLanguage } from '@/i18n'
import { fetchAndStoreProcesses, fetchDecisionsIfEmpty, isMobile } from './init'
import { AuthService } from '@/services.js'

/**
 * Creates the provide object for Vue app with shared functionality
 * @param {Object} config - Application configuration
 * @param {Object} vueInstance - Vue instance (this)
 * @param {Object} store - Vuex store
 * @returns {Object} Provide object
 */
export function createProvideObject(config, vueInstance, store) {
  return {
    currentLanguage() {
      return i18n.global.locale
    },
    async setCurrentLanguage(lang) {
      if (lang) {
        await switchLanguage(config, lang)
      }
      return i18n.global.locale
    },
    loadProcesses(extraInfo) {
      return fetchAndStoreProcesses(vueInstance, store, config, extraInfo)
    },
    async loadDecisions() {
      return fetchDecisionsIfEmpty(store)
    },
    isMobile: isMobile,
    AuthService: AuthService
  }
}
