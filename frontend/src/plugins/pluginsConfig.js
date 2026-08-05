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
import { shallowRef } from 'vue'

/**
 * Version of the plugin API exposed by 'plugin-runtime.js'. A plugin manifest
 * declaring a different apiVersion is rejected by the loader instead of being
 * loaded and failing in an unpredictable way later.
 */
export const PLUGIN_API_VERSION = '1'

/** @type {Record<string, import('vue').ShallowRef<Array<object>>>} */
const pluginSlots = {}

function ensureSlot(slotName) {
  if (!pluginSlots[slotName]) {
    pluginSlots[slotName] = shallowRef([])
  }
  return pluginSlots[slotName]
}

/**
 * Registers a Vue component as a contribution to a named slot. Several plugins
 * may contribute to the same slot; contributions are kept in registration order.
 *
 * @param {string} slotName - Name of the slot (e.g. 'process-instance-tab')
 * @param {object} component - A Vue component definition
 * @param {object} [meta] - Optional metadata (e.g. { pluginId, id, text })
 */
export function registerPlugin(slotName, component, meta = {}) {
  const slot = ensureSlot(slotName)
  // shallowRef: replace the array so consumers re-render
  slot.value = [...slot.value, { component, ...meta }]
}

/**
 * Returns the reactive ref holding all contributions of a named slot.
 * Reading a slot that nothing contributed to yields an empty array, so callers
 * never need to guard for absence.
 *
 * @param {string} slotName - Name of the slot
 * @returns {import('vue').ShallowRef<Array<object>>}
 */
export function getPlugin(slotName) {
  return ensureSlot(slotName)
}

/** Removes all registered contributions. Intended for tests. */
export function resetPlugins() {
  Object.keys(pluginSlots).forEach(slotName => {
    pluginSlots[slotName].value = []
  })
}
