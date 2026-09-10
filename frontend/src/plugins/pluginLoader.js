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
import { i18n, registerTranslationLoader } from '@/i18n'
import { getServicesBasePath } from '@/services.js'
import { getPluginContext } from './pluginContext.js'
import { PLUGIN_API_VERSION, registerPlugin } from './pluginsConfig.js'

// Plugins are discovered by the backend on its classpath and served by it, from
// the same base path as every other endpoint
const PLUGINS_ENDPOINT = 'plugins'

/** Manifests of the plugins in the page, and the languages already merged for them. */
const loaded = []
const merged = new Set()

// Every step is bounded, so a plugin whose files never arrive is reported and
// dropped rather than left pending for the lifetime of the page.
const REQUEST_TIMEOUT_MS = 5000
const IMPORT_TIMEOUT_MS = 10000

function withTimeout(promise, ms, description) {
  let timer
  const expired = new Promise((_, reject) => {
    timer = setTimeout(() => reject(new Error(`${description} timed out after ${ms}ms`)), ms)
  })
  return Promise.race([promise, expired]).finally(() => clearTimeout(timer))
}

/**
 * Resolves an endpoint below the services base path against the document base, so
 * plugin assets are found both under a context path (/webapp/) and at the root.
 * The base path is known by the time plugins load: the application reads it from
 * '/info' and sets it before it starts them.
 *
 * @param {string} endpoint - Path below the base path, e.g. 'plugins/demo/index.js'
 * @returns {string}
 */
function resolveUrl(endpoint) {
  const basePath = getServicesBasePath()
  return new URL(`${basePath ? `${basePath}/` : ''}${endpoint}`, document.baseURI).href
}

/**
 * URL of a file a plugin ships.
 *
 * @param {object} manifest
 * @param {string} file
 * @returns {string}
 */
function pluginFileUrl(manifest, file) {
  return resolveUrl(`${PLUGINS_ENDPOINT}/${manifest.id}/${file}`)
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
    const res = await axios.create({ timeout: REQUEST_TIMEOUT_MS }).get(resolveUrl(PLUGINS_ENDPOINT))
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
  if (!file || merged.has(`${manifest.id}:${lang}`)) return
  merged.add(`${manifest.id}:${lang}`)
  try {
    const res = await axios.create({ timeout: REQUEST_TIMEOUT_MS }).get(pluginFileUrl(manifest, file))
    i18n.global.mergeLocaleMessage(lang, { plugins: { [manifest.id]: res.data } })
  } catch {
    console.debug(`Optional plugin translations not found for "${manifest.id}":`, file)
  }
}

/**
 * Loads the translations of every plugin for a language, for when the user switches to one
 * the plugins were not loaded in. The application loads its own messages per language the
 * same way, in 'switchLanguage'.
 *
 * @param {string} lang
 */
export async function syncPluginTranslations(lang) {
  await Promise.all(loaded.map(manifest => loadPluginTranslations(manifest, lang)))
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
    link.href = pluginFileUrl(manifest, file)
    link.dataset.plugin = manifest.id
    document.head.appendChild(link)
  })
}

function isUsable(manifest) {
  if (!manifest?.id || !manifest?.entry) {
    console.warn('Ignoring plugin manifest without "id" or "entry":', manifest)
    return false
  }
  // A plugin may name several versions it was built and tested against, so that one
  // published build can serve more than one webclient version.
  const declared = Array.isArray(manifest.apiVersion) ? manifest.apiVersion : [manifest.apiVersion]
  if (!declared.map(String).includes(PLUGIN_API_VERSION)) {
    console.warn(
      `Ignoring plugin "${manifest.id}": it declares plugin API version ` +
      `"${declared.join('", "')}", this webclient provides "${PLUGIN_API_VERSION}"`)
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
  const url = pluginFileUrl(manifest, manifest.entry)
  try {
    const module = await withTimeout(importer(url), IMPORT_TIMEOUT_MS, `Loading plugin "${manifest.id}"`)
    const register = module.register ?? module.default
    if (typeof register !== 'function') {
      console.warn(`Plugin "${manifest.id}" exports no register function, ignoring it`)
      return false
    }
    await loadPluginTranslations(manifest, lang)
    loadPluginStyles(manifest)
    await register({
      id: manifest.id,
      baseUrl: pluginFileUrl(manifest, ''),
      // Handed over rather than imported, so the plugin id is stamped on every
      // contribution and cannot be forgotten or claimed for another plugin
      registerPlugin: (slotName, component, meta = {}) =>
        registerPlugin(slotName, component, { ...meta, pluginId: manifest.id })
    })
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
  loaded.length = 0
  merged.clear()
  const usable = (manifests ?? []).filter(isUsable)
  const results = await Promise.all(usable.map(async manifest => {
    if (!await loadPlugin(manifest, lang, importer)) return null
    // Kept so their translations can follow a later language change
    loaded.push(manifest)
    return manifest.id
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
  const ids = await loadPlugins(manifests, lang, importer)

  // A plugin's labels come from its own files, so a language switch has to fetch them
  registerTranslationLoader(syncPluginTranslations)
  return ids
}
