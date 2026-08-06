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
import { axios } from '@/globals.js'
import { i18n } from '@/i18n'
import { getPluginContext } from './pluginContext.js'
import { PLUGIN_API_VERSION } from './pluginsConfig.js'

// Plugins are discovered by the backend on its classpath and served by it
const PLUGINS_ENDPOINT = 'info/plugins'
const PLUGINS_BASE_PATH = 'plugins/'

/**
 * Resolves a path relative to the document base, so plugin assets are found
 * both under a context path (/webapp/) and at the root.
 *
 * @param {string} relativePath
 * @returns {string}
 */
function resolveUrl(relativePath) {
  return new URL(relativePath, document.baseURI).href
}

/**
 * Reads the list of plugin manifests from the backend. Having none is a normal
 * state - the webclient ships without plugins and they are disabled by default -
 * so an unavailable endpoint yields an empty list rather than an error.
 *
 * @returns {Promise<Array<object>>}
 */
export async function fetchPluginManifests() {
  try {
    const res = await axios.create().get(resolveUrl(PLUGINS_ENDPOINT))
    const plugins = res.data?.plugins
    if (!Array.isArray(plugins)) {
      console.warn('The plugin endpoint returned no "plugins" array, ignoring it')
      return []
    }
    return plugins
  } catch {
    console.debug('No plugin endpoint available, continuing without plugins')
    return []
  }
}

/**
 * Merges the translations a plugin ships under the 'plugins.<id>' namespace, so
 * plugin keys can never collide with application or theme keys.
 *
 * @param {object} manifest
 * @param {string} lang
 */
async function loadPluginTranslations(manifest, lang) {
  const file = manifest.translations?.[lang]
  if (!file) return
  try {
    const res = await axios.create().get(resolveUrl(`${PLUGINS_BASE_PATH}${manifest.id}/${file}`))
    i18n.global.mergeLocaleMessage(lang, { plugins: { [manifest.id]: res.data } })
  } catch {
    console.debug(`Optional plugin translations not found for "${manifest.id}":`, file)
  }
}

/**
 * Adds the stylesheets a plugin ships. Nothing scopes them: a plugin styling
 * shared elements affects the whole page, which is part of trusting it.
 *
 * @param {object} manifest
 */
function loadPluginStyles(manifest) {
  const files = Array.isArray(manifest.styles) ? manifest.styles : []
  files.forEach(file => {
    const link = document.createElement('link')
    link.rel = 'stylesheet'
    link.href = resolveUrl(`${PLUGINS_BASE_PATH}${manifest.id}/${file}`)
    link.dataset.plugin = manifest.id
    document.head.appendChild(link)
  })
}

function isUsable(manifest) {
  if (!manifest?.id || !manifest?.entry) {
    console.warn('Ignoring plugin manifest without "id" or "entry":', manifest)
    return false
  }
  if (manifest.apiVersion !== PLUGIN_API_VERSION) {
    console.warn(
      `Ignoring plugin "${manifest.id}": it declares plugin API version ` +
      `"${manifest.apiVersion}", this webclient provides "${PLUGIN_API_VERSION}"`)
    return false
  }
  return true
}

/**
 * Imports a module by URL. The specifier is only known at runtime: the plugin is
 * not part of this build, so Vite must not try to resolve it.
 *
 * @param {string} url
 * @returns {Promise<object>}
 */
function importModule(url) {
  return import(/* @vite-ignore */ url)
}

/**
 * Imports one plugin and lets it register its contributions. The plugin module
 * is expected to export a 'register' function (named or default).
 *
 * @param {object} manifest
 * @param {string} lang
 * @param {(url: string) => Promise<object>} importer
 * @returns {Promise<boolean>} whether the plugin was loaded
 */
async function loadPlugin(manifest, lang, importer) {
  const url = resolveUrl(`${PLUGINS_BASE_PATH}${manifest.id}/${manifest.entry}`)
  try {
    const module = await importer(url)
    const register = module.register ?? module.default
    if (typeof register !== 'function') {
      console.warn(`Plugin "${manifest.id}" exports no register function, ignoring it`)
      return false
    }
    await loadPluginTranslations(manifest, lang)
    loadPluginStyles(manifest)
    await register({ id: manifest.id, baseUrl: resolveUrl(`${PLUGINS_BASE_PATH}${manifest.id}/`) })
    console.info(`Plugin "${manifest.id}" loaded`)
    return true
  } catch (error) {
    // A broken plugin must never keep the application from starting
    console.error(`Plugin "${manifest.id}" could not be loaded:`, error)
    return false
  }
}

/**
 * Loads every usable plugin of the given manifest list, isolating failures.
 *
 * @param {Array<object>} manifests
 * @param {string} lang
 * @param {(url: string) => Promise<object>} [importer] - Overridable in tests
 * @returns {Promise<Array<string>>} ids of the plugins that were loaded
 */
export async function loadPlugins(manifests, lang, importer = importModule) {
  const usable = (manifests ?? []).filter(isUsable)
  const results = await Promise.all(usable.map(async manifest => {
    const loaded = await loadPlugin(manifest, lang, importer)
    return loaded ? manifest.id : null
  }))
  return results.filter(Boolean)
}

/**
 * Discovers and loads plugins. Called during bootstrap; resolves to an empty
 * array when no plugin is present, which is the default for the webclient.
 *
 * @param {string} lang - Currently active language
 * @param {(url: string) => Promise<object>} [importer] - Overridable in tests
 * @returns {Promise<Array<string>>} ids of the plugins that were loaded
 */
export async function initPlugins(lang, importer = importModule) {
  // Only an explicit false skips the request; a backend not reporting the flag is asked
  if (getPluginContext().config?.pluginsEnabled === false) return []

  const manifests = await fetchPluginManifests()
  if (manifests.length === 0) return []
  return loadPlugins(manifests, lang, importer)
}
