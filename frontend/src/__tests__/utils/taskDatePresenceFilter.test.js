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

import { describe, it, expect } from 'vitest'
import { applyDatePresenceFilters, DATE_PRESENCE_AFTER } from '@/utils/taskDatePresenceFilter.js'

describe('applyDatePresenceFilters', () => {
  it('does nothing when neither toggle is active', () => {
    const filters = { sorting: [] }
    applyDatePresenceFilters(filters, { dueDate: false, reminder: false })
    expect(filters).toEqual({ sorting: [] })
  })

  it('adds dueAfter when due date filter is active', () => {
    const filters = { sorting: [{ sortBy: 'created', sortOrder: 'asc' }] }
    applyDatePresenceFilters(filters, { dueDate: true, reminder: false })
    expect(filters.dueAfter).toBe(DATE_PRESENCE_AFTER)
    expect(filters.followUpAfter).toBeUndefined()
    expect(filters.orQueries).toBeUndefined()
  })

  it('adds followUpAfter when reminder filter is active', () => {
    const filters = { sorting: [{ sortBy: 'dueDate', sortOrder: 'desc' }] }
    applyDatePresenceFilters(filters, { dueDate: false, reminder: true })
    expect(filters.followUpAfter).toBe(DATE_PRESENCE_AFTER)
    expect(filters.dueAfter).toBeUndefined()
  })

  it('uses orQueries when both toggles are active and no existing orQueries', () => {
    const filters = { sorting: [{ sortBy: 'name', sortOrder: 'desc' }] }
    applyDatePresenceFilters(filters, { dueDate: true, reminder: true })
    expect(filters.dueAfter).toBeUndefined()
    expect(filters.followUpAfter).toBeUndefined()
    expect(filters.orQueries).toEqual([
      { dueAfter: DATE_PRESENCE_AFTER },
      { followUpAfter: DATE_PRESENCE_AFTER }
    ])
  })

  it('does not flatten into existing search orQueries when both toggles are active', () => {
    const existing = [{ nameLike: '%foo%' }]
    const filters = { sorting: [], orQueries: existing }
    applyDatePresenceFilters(filters, { dueDate: true, reminder: true })
    expect(filters.orQueries).toBe(existing)
    expect(filters.dueAfter).toBeUndefined()
    expect(filters.followUpAfter).toBeUndefined()
  })

  it('still applies dueAfter with any sort field/order combination', () => {
    const sortCases = [
      { sortBy: 'created', sortOrder: 'asc' },
      { sortBy: 'created', sortOrder: 'desc' },
      { sortBy: 'dueDate', sortOrder: 'asc' },
      { sortBy: 'dueDate', sortOrder: 'desc' },
      { sortBy: 'name', sortOrder: 'desc' },
      { sortBy: 'priority', sortOrder: 'desc' }
    ]
    for (const sorting of sortCases) {
      const filters = { sorting: [sorting] }
      applyDatePresenceFilters(filters, { dueDate: true })
      expect(filters.dueAfter).toBe(DATE_PRESENCE_AFTER)
    }
  })
})
