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

/** Far-past date so the engine returns only tasks that have a due/follow-up set (NULLs are excluded). */
export const DATE_PRESENCE_AFTER = '1970-01-01T00:00:00.000+0000'

/**
 * Apply "has due date" / "has follow-up" presence filters to a task filter query
 * so filtering happens before server-side sort and pagination.
 *
 * @param {object} filters - mutable filter payload sent to findTasksByFilter
 * @param {{ dueDate?: boolean, reminder?: boolean }} settings - additional filter toggles
 */
export function applyDatePresenceFilters(filters, settings = {}) {
  const filterByDueDate = !!settings.dueDate
  const filterByReminder = !!settings.reminder
  if (!filterByDueDate && !filterByReminder) return filters

  if (filterByDueDate && filterByReminder) {
    // Preserve OR semantics; do not flatten into existing search/advanced orQueries.
    if (!filters.orQueries || filters.orQueries.length === 0) {
      filters.orQueries = [
        { dueAfter: DATE_PRESENCE_AFTER },
        { followUpAfter: DATE_PRESENCE_AFTER }
      ]
    }
    return filters
  }
  if (filterByDueDate) filters.dueAfter = DATE_PRESENCE_AFTER
  if (filterByReminder) filters.followUpAfter = DATE_PRESENCE_AFTER
  return filters
}
