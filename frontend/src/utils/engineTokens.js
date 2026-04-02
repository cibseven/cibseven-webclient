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

import { ENGINE_TOKENS_STORAGE_KEY } from '@/constants.js'

/**
 * Utility functions for managing authentication tokens per engine.
 * This allows users to switch between engines without re-authenticating
 * if they have a valid cached token for the target engine.
 */

/**
 * Get all stored engine tokens from localStorage
 * @returns {Object} Map of engineId -> token
 */
export function getEngineTokens() {
  try {
    const stored = localStorage.getItem(ENGINE_TOKENS_STORAGE_KEY)
    return stored ? JSON.parse(stored) : {}
  } catch (e) {
    console.warn('Failed to parse engine tokens from localStorage', e)
    return {}
  }
}

/**
 * Save engine tokens to localStorage
 * @param {Object} tokens - Map of engineId -> token
 */
function saveEngineTokens(tokens) {
  try {
    localStorage.setItem(ENGINE_TOKENS_STORAGE_KEY, JSON.stringify(tokens))
  } catch (e) {
    console.warn('Failed to save engine tokens to localStorage', e)
  }
}

/**
 * Get the token for a specific engine
 * @param {string} engineId - The engine identifier
 * @returns {string|null} The token or null if not found
 */
export function getTokenForEngine(engineId) {
  const tokens = getEngineTokens()
  return tokens[engineId] || null
}

/**
 * Store the current token for a specific engine.
 * This should be called after successful authentication.
 * @param {string} engineId - The engine identifier
 * @param {string} token - The authentication token (optional, will read from storage if not provided)
 */
export function storeTokenForEngine(engineId, token = null) {
  if (!engineId) return
  
  // If no token provided, get from current session/local storage
  const tokenToStore = token || sessionStorage.getItem('token') || localStorage.getItem('token')
  if (!tokenToStore) return
  
  const tokens = getEngineTokens()
  tokens[engineId] = tokenToStore
  saveEngineTokens(tokens)
}

/**
 * Remove the token for a specific engine.
 * This should be called when logging out.
 * @param {string} engineId - The engine identifier
 */
export function removeTokenForEngine(engineId) {
  if (!engineId) return
  
  const tokens = getEngineTokens()
  delete tokens[engineId]
  saveEngineTokens(tokens)
}

/**
 * Clear all stored engine tokens.
 * This should be called on full logout/session clear.
 */
export function clearAllEngineTokens() {
  localStorage.removeItem(ENGINE_TOKENS_STORAGE_KEY)
}

/**
 * Check if a token exists for a specific engine
 * @param {string} engineId - The engine identifier
 * @returns {boolean} True if a token exists for this engine
 */
export function hasTokenForEngine(engineId) {
  return !!getTokenForEngine(engineId)
}

/**
 * Restore the token for a specific engine to the active session.
 * Sets the token in both the current storage (session/local) and returns it.
 * @param {string} engineId - The engine identifier
 * @returns {string|null} The restored token or null if not found
 */
export function restoreTokenForEngine(engineId) {
  const token = getTokenForEngine(engineId)
  if (!token) return null
  
  // Determine where to store the token based on where previous token was stored
  const useSession = !!sessionStorage.getItem('token')
  const useLocal = !!localStorage.getItem('token')
  
  if (useSession) {
    sessionStorage.setItem('token', token)
  } else if (useLocal) {
    localStorage.setItem('token', token)
  } else {
    // Default to localStorage for "remember me" behavior
    localStorage.setItem('token', token)
  }
  
  return token
}
