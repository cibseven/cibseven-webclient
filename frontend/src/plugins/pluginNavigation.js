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
 * Navigation handed to plugins: enough to send the user somewhere, without the
 * router itself. Adding routes or guards stays the application's business, and
 * the plugin contract does not then depend on a vue-router major version.
 *
 * The methods resolve the router when they are called, not when a plugin is
 * loaded: plugins register before the router exists.
 */
let router = null

/** @param {object} appRouter - The application's vue-router instance */
export function setPluginRouter(appRouter) {
  router = appRouter
}

function requireRouter(action) {
  if (!router) {
    console.warn(`Plugin navigation: cannot ${action}, the router is not available yet`)
  }
  return router
}

export const navigation = {
  /** @returns {Promise|undefined} resolves once the navigation is done */
  push(to) {
    return requireRouter('push')?.push(to)
  },

  /** Like push, but without a history entry. */
  replace(to) {
    return requireRouter('replace')?.replace(to)
  },

  /**
   * Where the user is now, as a plain copy - a snapshot to read, not the live
   * route object.
   *
   * @returns {{name, path, params, query, hash}|null}
   */
  currentRoute() {
    const current = router?.currentRoute?.value
    if (!current) return null
    const { name, path, params, query, hash } = current
    return structuredClone({ name: name ?? null, path, params, query, hash })
  }
}
