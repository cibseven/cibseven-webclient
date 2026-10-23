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

// Display labels for history time to live (TTL) values, shared by ProcessDefinitionDetails,
// DecisionDefinitionDetails and EditHistoryTimeToLiveModal.
//
// Plain functions rather than a mixin: they only depend on the translation function and the
// values passed in, so components call them from their own computed properties, e.g.
//   historyTimeToLiveDisplay() {
//     return formatHistoryTimeToLive(this.$t, this.historyTimeToLive, this.$root.config)
//   }
//
// The translation function parameter is named `$t` on purpose, so that i18n.test.js (which
// looks for `$t('key'`) recognizes the keys used here.

/**
 * Label of the engine-wide default history time to live, as reported by `/info`
 * (`config.historyTimeToLive`, passed through from the engine's configuration endpoint).
 *
 * - `null` / `undefined` (or any non-number): the engine does not report it (engines before
 *   3.0.0) -> "unknown". It must not be mistaken for one of the real values below.
 * - `-1` (any negative number): the engine has no default configured -> unlimited.
 * - `0`: history is removed by the next cleanup run.
 * - `n > 0`: `n` days.
 *
 * @param {Function} $t the translation function (`this.$t`)
 * @param {Object} [config] the engine configuration from `/info` (`this.$root.config`)
 * @returns {string} the translated label, e.g. "Default to system (30 days)"
 */
export function formatEngineDefaultHistoryTimeToLive($t, config) {
  const engineDefault = config?.historyTimeToLive
  if (typeof engineDefault !== 'number' || Number.isNaN(engineDefault)) {
    return $t('historyTimeToLive.choiceDefaultUnknown')
  }
  if (engineDefault < 0) {
    return $t('historyTimeToLive.choiceDefaultUnlimited')
  }
  if (engineDefault === 0) {
    return $t('historyTimeToLive.choiceDefaultZero')
  }
  return $t('historyTimeToLive.choiceDefault', { value: engineDefault })
}

/**
 * Label of a process or decision definition's own history time to live.
 *
 * - `null` / `undefined`: the definition has no TTL of its own -> the engine default label
 *   (see {@link formatEngineDefaultHistoryTimeToLive}).
 * - `0`: history is removed by the next cleanup run. It has its own label and is never shown
 *   like a missing TTL: `0` and `null` have opposite meanings for the engine.
 * - `n > 0`: `n` days.
 *
 * @param {Function} $t the translation function (`this.$t`)
 * @param {number|null|undefined} historyTimeToLive the definition's TTL in days
 * @param {Object} [config] the engine configuration from `/info` (`this.$root.config`)
 * @returns {string} the translated label, e.g. "30 days"
 */
export function formatHistoryTimeToLive($t, historyTimeToLive, config) {
  if (historyTimeToLive === null || historyTimeToLive === undefined) {
    return formatEngineDefaultHistoryTimeToLive($t, config)
  }
  if (historyTimeToLive === 0) {
    return $t('historyTimeToLive.choiceZero')
  }
  return `${historyTimeToLive} ${$t('historyTimeToLive.days')}`
}
