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
 * Utility functions for handling keyboard shortcuts configuration
 */

/**
 * Get enabled shortcuts from config for a specific category
 * @param {Object} config - The application config object
 * @param {String} category - The shortcuts category ('global' or 'tasks')
 * @returns {Array} Array of enabled shortcut objects
 */
export function getEnabledShortcuts(config, category) {
  if (!config?.shortcuts?.[category]) {
    return []
  }
  
  return Object.entries(config.shortcuts[category])
    .filter(([, shortcut]) => shortcut.enabled)
    .map(([key, shortcut]) => ({
      id: key,
      ...shortcut
    }))
}

/**
 * Get shortcuts formatted for display in the ShortcutsModal
 * @param {Object} config - The application config object
 * @param {String} category - The shortcuts category ('global' or 'tasks')
 * @returns {Array} Array of shortcuts formatted for the modal table
 */
export function getShortcutsForModal(config, category) {
  const shortcuts = getEnabledShortcuts(config, category)
  
  return shortcuts.map(shortcut => ({
    buttons: formatKeysForDisplay(shortcut.keys),
    description: shortcut.description
  }))
}

/**
 * Format key combination for display in the modal
 * @param {Array} keys - Array of key names
 * @returns {Array} Array of formatted keys with proper symbols
 */
function formatKeysForDisplay(keys) {
  const keyMap = {
    'left': '🠄',
    'right': '🠆', 
    'up': '🠅',
    'down': '🠇',
    'ctrl': 'Ctrl',
    'alt': 'Alt',
    'shift': 'Shift'
  }
  
  return keys.map(key => keyMap[key.toLowerCase()] || key.toUpperCase())
    .reduce((acc, key, index) => {
      if (index > 0) acc.push('+')
      acc.push(key)
      return acc
    }, [])
}

/**
 * Get all global navigation shortcuts that should trigger route changes
 * @param {Object} config - The application config object
 * @returns {Array} Array of global shortcuts with route information
 */
export function getGlobalNavigationShortcuts(config) {
  const globalShortcuts = getEnabledShortcuts(config, 'global')
  return globalShortcuts.filter(shortcut => shortcut.route)
}

/**
 * Get all task-specific shortcuts that should trigger events
 * @param {Object} config - The application config object
 * @returns {Array} Array of task shortcuts with event information
 */
export function getTaskEventShortcuts(config) {
  const taskShortcuts = getEnabledShortcuts(config, 'tasks')
  return taskShortcuts.filter(shortcut => shortcut.event)
}

/**
 * Check if the current key event matches a specific key combination
 * @param {KeyboardEvent} event - The keyboard event
 * @param {Array} keys - Array of key names to match
 * @returns {Boolean} True if all keys match the event
 */
export function checkKeyMatch(event, keys) {
  // Check if all required keys are pressed
  const keyMap = {
    'ctrl': event.ctrlKey,
    'alt': event.altKey,
    'shift': event.shiftKey,
    'left': event.key === 'ArrowLeft',
    'right': event.key === 'ArrowRight',
    'up': event.key === 'ArrowUp',
    'down': event.key === 'ArrowDown'
  }
  // Check if all keys in the combination match
  return keys.every(key => {
    const keyLower = key.toLowerCase()
    if (Object.prototype.hasOwnProperty.call(keyMap, keyLower)) {
      return keyMap[keyLower]
    }
    // For regular keys (letters, numbers, etc.), check multiple possibilities
    const matches = [
      event.key === key,                    // Exact match
      event.key.toLowerCase() === keyLower, // Lowercase match
      event.key === keyLower,               // Direct lowercase match
      event.code === `Digit${key}`,         // For numbers: Digit1, Digit2, etc.
      event.code === `Key${key.toUpperCase()}`, // For letters: KeyA, KeyB, etc.
    ]
    const result = matches.some(match => match)
    return result
  })
}
