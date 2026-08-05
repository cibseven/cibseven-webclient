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
 * Public runtime API for webclient plugins.
 *
 * This file is a separate entry of the *application* build, not of the library
 * build. That is what makes it work: because it is bundled together with
 * 'app.js', both entries share the same chunks, so a plugin importing from here
 * receives the very same Vue, axios and service instances the application uses.
 * A plugin that imported 'vue' from its own bundle would get a second Vue
 * runtime, and neither reactivity nor provide/inject would cross that boundary.
 *
 * Everything exported here is an official interface: it is what plugins compile
 * against and may not change without a bump of PLUGIN_API_VERSION.
 */
import { i18n } from './i18n.js'
import { PLUGIN_API_VERSION } from './plugins/pluginsConfig.js'

// Bare re-exports, so an import map can point "vue" at this module and a plugin
// built from single-file components keeps working unchanged.
export * from 'vue'

// Grouped re-exports, for plugins written as plain ES modules without a build.
export * as vue from 'vue'
export * as services from './services.js'
export { axios } from './globals.js'
export { i18n }
export { registerPlugin, getPlugin, PLUGIN_API_VERSION } from './plugins/pluginsConfig.js'
export { getPluginContext as getContext } from './plugins/pluginContext.js'

/**
 * Merges translations of a plugin under the 'plugins.<id>' namespace, so plugin
 * keys cannot collide with application or theme keys.
 *
 * @param {string} pluginId
 * @param {string} lang
 * @param {object} messages
 */
export function mergeTranslations(pluginId, lang, messages) {
  i18n.global.mergeLocaleMessage(lang, { plugins: { [pluginId]: messages } })
}

/**
 * Identifies this runtime instance. Two different values observed by
 * application and plugin would mean the plugin loaded a second copy of the
 * runtime, i.e. the import map or the plugin build is misconfigured.
 *
 * @returns {{ apiVersion: string, instance: object }}
 */
const instance = Object.freeze({})
export function getRuntimeInfo() {
  return { apiVersion: PLUGIN_API_VERSION, instance }
}
