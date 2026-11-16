/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// The bundles carry a content hash in their names, but the files a theme or a customer replaces under their own
// name (the logo, the favicon, the translation files, config.json) do not. A browser or a proxy that kept such a
// file would show the old one after an update, until the user cleared the cache. Their URLs therefore carry the
// build they were requested by, so every new build asks for them anew.
// __BUILD_VERSION__ is set by vite.config.js; without it (tests, a consumer's own build) the URLs stay as they are.
/* global __BUILD_VERSION__ */
export const buildVersion = typeof __BUILD_VERSION__ === 'undefined' ? '' : __BUILD_VERSION__

/**
 * The URL of a file served under a fixed name, with the build version as query parameter `v`.
 * @param {string} url - Relative or absolute URL, with or without a query
 * @param {string} [version] - The version to add, the build's by default
 * @returns {string} The URL with `v=<version>`, or unchanged when there is no version
 */
export function versionedUrl(url, version = buildVersion) {
  if (!version) return url
  const [path, hash] = url.split('#')
  const separator = path.includes('?') ? '&' : '?'
  return `${path}${separator}v=${encodeURIComponent(version)}${hash === undefined ? '' : `#${hash}`}`
}
