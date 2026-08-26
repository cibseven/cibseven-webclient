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
import { version } from '../../package.json'

/**
 * The webclient release plugins are built against, pre-release suffixes aside. A plugin
 * lists the versions it was tested with, and the loader rejects a manifest that does not
 * name this one instead of loading it and failing unpredictably later.
 */
export const PLUGIN_API_VERSION = version.replace(/-.*$/, '')

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
  // Ids reach the UI (a tab id becomes ?tab=<id>), so a second registration under
  // the same id would render twice and select ambiguously.
  if (meta.id && slot.value.some(contribution => contribution.id === meta.id)) {
    console.warn(`Ignoring a second contribution with id "${meta.id}" in slot "${slotName}"`)
    return
  }
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
