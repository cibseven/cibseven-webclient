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

// Display label for a definition's history time to live (TTL), shared by
// ProcessDefinitionDetails, DecisionDefinitionDetails and EditHistoryTimeToLiveModal.
//
// A plain function rather than a mixin: it only depends on the translation function and the
// value passed in, so components call it from their own computed properties, e.g.
//   historyTimeToLiveDisplay() {
//     return formatHistoryTimeToLive(this.$t, this.historyTimeToLive)
//   }
//
// The translation function parameter is named `$t` on purpose, so that i18n.test.js (which
// looks for `$t('key'`) recognizes the keys used here.

/**
 * Label of a process or decision definition's own history time to live.
 *
 * - `null` / `undefined`: the definition has no TTL, so its history is kept indefinitely.
 *   The engine-wide default does <b>not</b> apply here: the engine copies it onto a definition
 *   only at deployment time (for models without their own TTL), and history cleanup skips
 *   definitions without a TTL with either cleanup strategy (removal-time based: no removal
 *   time is set; end-time based: definitions with a `NULL` TTL are filtered out). So a
 *   definition without a TTL is unlimited, whatever the engine default is today.
 * - `0`: history is removed by the next cleanup run. It has its own label and is never shown
 *   like a missing TTL: `0` and `null` have opposite meanings for the engine.
 * - `n > 0`: `n` days.
 *
 * @param {Function} $t the translation function (`this.$t`)
 * @param {number|null|undefined} historyTimeToLive the definition's TTL in days
 * @returns {string} the translated label, e.g. "30 days"
 */
export function formatHistoryTimeToLive($t, historyTimeToLive) {
  if (historyTimeToLive === null || historyTimeToLive === undefined) {
    return $t('historyTimeToLive.choiceUnlimited')
  }
  if (historyTimeToLive === 0) {
    return $t('historyTimeToLive.choiceZero')
  }
  return `${historyTimeToLive} ${$t('historyTimeToLive.days')}`
}
