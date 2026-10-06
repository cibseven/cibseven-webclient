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
 * Application context handed to plugins. Set once during bootstrap, before any
 * plugin is loaded, so a plugin never sees a half-initialised application.
 *
 * A plugin gets the context through 'plugin-runtime.js' rather than importing
 * application internals, which keeps the supported surface explicit.
 *
 * Note that this is a compatibility contract, not a security boundary: plugin
 * code runs in the page and could reach application internals by other means.
 * Handing over a copy keeps honest plugins from corrupting the application by
 * accident.
 */
let context = Object.freeze({
  config: null
})

/**
 * The config is handed over as a frozen deep copy, and so is the context holding
 * it: a plugin reads how the application is configured without being able to
 * reconfigure it, or to replace what another plugin then reads. The store is
 * deliberately not exposed - a plugin uses the services, so product state stays
 * ours to change.
 *
 * @param {object} update - { config }
 */
export function setPluginContext(update) {
  const config = 'config' in update ? freezeDeep(structuredClone(update.config)) : context.config
  context = Object.freeze({ config })
}

/** @returns {{ config: object|null }} */
export function getPluginContext() {
  return context
}

function freezeDeep(value) {
  if (value && typeof value === 'object' && !Object.isFrozen(value)) {
    Object.freeze(value)
    Object.values(value).forEach(freezeDeep)
  }
  return value
}
