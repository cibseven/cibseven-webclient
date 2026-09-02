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

// Letters, digits, '-' and '_' only: safe unencoded both as a JSON string value
// and as a URL query parameter value (RFC 3986 "unreserved" characters, minus
// '.' and '~' which are left out so the id can't be confused with the '.'
// nesting used by the deepLinks.<section>.<id>.title i18n key).
const ID_PATTERN = /^[a-zA-Z0-9_-]+$/

/**
 * Reads and validates the config.deepLinks[section] array, dropping any
 * entry with an invalid id, a missing url, an id that collides with a
 * built-in tab id, or a duplicate id.
 * @param {Object} config - the merged application config (this.$root.config)
 * @param {String} section - one of processDefinition, processInstance, decisionDefinition, decisionInstance
 * @param {String[]} reservedIds - built-in tab ids for this section that a deep link may not shadow
 * @returns {Array<{id: String, url: String}>}
 */
export function getDeepLinkEntries(config, section, reservedIds = []) {
  const entries = config?.deepLinks?.[section]
  if (entries === undefined) return []
  if (!Array.isArray(entries)) {
    console.warn(`Invalid deepLinks.${section}: expected an array`, entries)
    return []
  }

  const seen = new Set()
  return entries.filter(entry => {
    if (!entry || typeof entry.id !== 'string' || !ID_PATTERN.test(entry.id)) {
      console.warn(`Invalid deepLinks.${section} entry: id must match ${ID_PATTERN}`, entry)
      return false
    }
    if (typeof entry.url !== 'string' || !entry.url) {
      console.warn(`Invalid deepLinks.${section} entry "${entry.id}": url is missing`, entry)
      return false
    }
    if (reservedIds.includes(entry.id) || seen.has(entry.id)) {
      console.warn(`Invalid deepLinks.${section} entry: id "${entry.id}" is reserved or duplicated`, entry)
      return false
    }
    seen.add(entry.id)
    return true
  }).map(entry => ({
    id: entry.id,
    url: entry.url,
    text: `deepLinks.${section}.${entry.id}.title`
  }))
}

/**
 * Resolves the display label for a deep link entry: the translation for
 * entry.text if one exists, otherwise the entry's id. vue-i18n's $t()
 * returns the key unchanged when no translation is found, which is how a
 * missing translation is detected here.
 * @param {Function} t - a $t-like translation function (key) => String
 * @param {{id: String, text: String}} entry - a deep link entry as returned by getDeepLinkEntries
 * @returns {String}
 */
export function resolveDeepLinkLabel(t, entry) {
  if (!entry) return ''
  const translated = t(entry.text)
  return translated === entry.text ? entry.id : translated
}

/**
 * Appends predefined context parameters to a deep link URL's query string.
 * @param {String} url - the deep link's configured URL
 * @param {Object} params - key/value pairs to append; nullish/empty values are skipped
 * @returns {String} the resolved URL, or the original url if it could not be parsed
 */
export function buildDeepLinkUrl(url, params = {}) {
  let resolved
  try {
    resolved = new URL(url)
  } catch (error) {
    console.warn(`Invalid deep link URL "${url}"`, error)
    return url
  }

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      resolved.searchParams.set(key, value)
    }
  })

  return resolved.toString()
}
