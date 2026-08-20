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
// Persists a SidebarsFlow "left-open" boolean in localStorage under
// `sidebar-left-open:{scope}`. The host component owns its own `leftOpen`
// data and decides what scope(s) to use (a single static scope, or several
// scopes it switches between), calling getSavedLeftOpen/saveLeftOpen directly.
export default {
  methods: {
    getSavedLeftOpen: function(scope) {
      try {
        const saved = localStorage.getItem(`sidebar-left-open:${scope}`)
        return saved === null ? true : saved === 'true'
      } catch {
        return true
      }
    },
    saveLeftOpen: function(scope, isOpen) {
      try {
        localStorage.setItem(`sidebar-left-open:${scope}`, isOpen)
      } catch {
        // localStorage unavailable or quota exceeded - ignore
      }
    }
  }
}
