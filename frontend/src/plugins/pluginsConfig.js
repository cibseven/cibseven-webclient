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
 * The webclient line plugins are built against, as major.minor: a patch release changes
 * nothing a plugin binds to, so it must not invalidate a published one. A plugin lists the
 * lines it was tested with, and the loader rejects a manifest naming none of them rather
 * than loading it and failing unpredictably later.
 */
export const PLUGIN_API_VERSION = version.split('.').slice(0, 2).join('.')

/** @type {Record<string, import('vue').ShallowRef<Array<object>>>} */
const pluginSlots = {}

/** Ids the application itself uses in a slot. @type {Record<string, Set<string>>} */
const reservedSlotIds = {}

/** Counts contributions, to give each one a key of its own. */
let contributions = 0

function ensureSlot(slotName) {
  if (!pluginSlots[slotName]) {
    pluginSlots[slotName] = shallowRef([])
  }
  return pluginSlots[slotName]
}

/**
 * Declares the ids the application itself renders in a slot, so a plugin cannot
 * take one of them. Called where those ids are defined, before plugins load.
 *
 * @param {string} slotName - Name of the slot
 * @param {Array<string>} ids
 */
export function reserveSlotIds(slotName, ids) {
  reservedSlotIds[slotName] = new Set([...(reservedSlotIds[slotName] ?? []), ...ids])
}

/**
 * Registers a Vue component as a contribution to a named slot. Several plugins
 * may contribute to the same slot; contributions are kept in registration order.
 *
 * @param {string} slotName - Name of the slot (e.g. 'process-instance-tab')
 * @param {object} component - A Vue component definition
 * @param {object} [meta] - Optional metadata (e.g. { pluginId, id, text })
 * @returns {object|null} the stored contribution, or null when it was rejected
 */
export function registerPlugin(slotName, component, meta = {}) {
  const slot = ensureSlot(slotName)
  // Ids reach the UI (a tab id becomes ?tab=<id>), so a second registration under
  // the same id would render twice and select ambiguously.
  if (meta.id && reservedSlotIds[slotName]?.has(meta.id)) {
    console.warn(`Ignoring a contribution with id "${meta.id}": slot "${slotName}" uses it itself`)
    return null
  }
  if (meta.id && slot.value.some(contribution => contribution.id === meta.id)) {
    console.warn(`Ignoring a second contribution with id "${meta.id}" in slot "${slotName}"`)
    return null
  }
  // One plugin may register several times in the same slot, so its id is no key.
  // Stamped here rather than in the slot, where only a list position is available.
  const contribution = { component, ...meta, key: `${meta.pluginId ?? 'app'}-${++contributions}` }
  // shallowRef: replace the array so consumers re-render
  slot.value = [...slot.value, contribution]
  return contribution
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

/**
 * Removes all registered contributions. Intended for tests. Reserved ids are kept:
 * they belong to the application, which claims them once while it loads.
 */
export function resetPlugins() {
  Object.keys(pluginSlots).forEach(slotName => {
    pluginSlots[slotName].value = []
  })
}
