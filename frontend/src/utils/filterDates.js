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

import { moment } from '@/globals.js'

/** Task filter criteria the engine parses as a date (the `...Expression` variants are not dates). */
export const DATE_CRITERIA_KEYS = [
  'createdOn', 'createdBefore', 'createdAfter',
  'dueDate', 'dueBefore', 'dueAfter',
  'followUpDate', 'followUpBefore', 'followUpAfter', 'followUpBeforeOrNotExistent',
  'updatedAfter'
]

/** Format the engine requires for dates in a filter query, e.g. 2013-01-23T14:42:45.000+0200. */
export const ENGINE_DATE_FORMAT = 'YYYY-MM-DDTHH:mm:ss.SSSZZ'

const ACCEPTED_FORMATS = [
  moment.ISO_8601,
  'YYYY.MM.DD HH:mm:ss', 'YYYY.MM.DD HH:mm', 'YYYY.MM.DD',
  'YYYY/MM/DD HH:mm:ss', 'YYYY/MM/DD HH:mm', 'YYYY/MM/DD',
  'DD.MM.YYYY HH:mm:ss', 'DD.MM.YYYY HH:mm', 'DD.MM.YYYY'
]

/**
 * Example inputs for the "invalid date" hint, built from the current date so the hint shows a real, valid value.
 *
 * @param {moment.Moment} [now]
 * @returns {{ iso: string, dotted: string, german: string, isoTime: string }}
 */
export function dateExamples(now = moment()) {
  return {
    iso: now.format('YYYY-MM-DD'),
    dotted: now.format('YYYY.MM.DD'),
    german: now.format('DD.MM.YYYY'),
    isoTime: now.format('YYYY-MM-DD[T]HH:mm:ss')
  }
}

export function isDateCriteriaKey(key) {
  return DATE_CRITERIA_KEYS.includes(key)
}

/**
 * Convert a date typed by the user into the format the engine requires.
 * Values without a time part start at 00:00:00 local time; values without an offset use the local offset.
 *
 * @param {string} value - e.g. "2026.03.03", "03.03.2026 14:30" or "2013-01-23T14:42:45.000+0200"
 * @returns {string|null} engine date, or null when the value is not a recognizable date
 */
export function toEngineDate(value) {
  const text = String(value ?? '').trim()
  if (!text) return null
  const parsed = moment(text, ACCEPTED_FORMATS, true)
  return parsed.isValid() ? parsed.format(ENGINE_DATE_FORMAT) : null
}
