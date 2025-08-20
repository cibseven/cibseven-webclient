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
import { createSortComparator } from '../utils/sort.js'

describe('createSortComparator', () => {
  it('sorts numbers ascending, handling null/empty', () => {
    const arr = [3, null, 1, '', 2]
    arr.sort(createSortComparator(x => x, false))
    // Check that empty values come first, then valid values are sorted
    expect(arr.slice(0, 2)).toEqual(expect.arrayContaining([null, '']))
    expect(arr.slice(2)).toEqual([1, 2, 3])
  })

  it('sorts numbers descending, handling null/empty', () => {
    const arr = [3, null, 1, '', 2]
    arr.sort(createSortComparator(x => x, true))
    // For descending sort: valid values come first (descending), then empty values
    expect(arr.slice(0, 3)).toEqual([3, 2, 1])
    expect(arr.slice(3)).toEqual(expect.arrayContaining([null, '']))
  })

  it('sorts strings ascending, handling null/empty', () => {
    const arr = ['b', null, 'a', '', 'c']
    arr.sort(createSortComparator(x => x, false))
    // Check that empty values are first, then valid values are sorted
    expect(arr.slice(0, 2)).toEqual(expect.arrayContaining([null, '']))
    expect(arr.slice(2)).toEqual(['a', 'b', 'c'])
  })

  it('sorts strings descending, handling null/empty', () => {
    const arr = ['b', null, 'a', '', 'c']
    arr.sort(createSortComparator(x => x, true))
    // For descending sort: valid values come first (descending), then empty values
    expect(arr.slice(0, 3)).toEqual(['c', 'b', 'a'])
    expect(arr.slice(3)).toEqual(expect.arrayContaining([null, '']))
  })

  it('sorts objects by property', () => {
    const arr = [
      { v: 2 },
      { v: null },
      { v: 1 },
      { v: '' },
      { v: 3 }
    ]
    arr.sort(createSortComparator(obj => obj.v, false))
    const values = arr.map(x => x.v)
    // Empty values come first, then valid values are sorted
    expect(values.slice(0, 2)).toEqual(expect.arrayContaining([null, '']))
    expect(values.slice(2)).toEqual([1, 2, 3])
  })

  it('sorts objects by property descending', () => {
    const arr = [
      { v: 2 },
      { v: null },
      { v: 1 },
      { v: '' },
      { v: 3 }
    ]
    arr.sort(createSortComparator(obj => obj.v, true))
    const values = arr.map(x => x.v)
    // For descending sort: valid values come first (descending), then empty values
    expect(values.slice(0, 3)).toEqual([3, 2, 1])
    expect(values.slice(3)).toEqual(expect.arrayContaining([null, '']))
  })

  it('treats undefined as empty', () => {
    const arr = [undefined, 2, 1, '', null]
    arr.sort(createSortComparator(x => x, false))
    // Separate empty and non-empty values
    const result = arr
    const emptyValues = result.filter(x => x == null || x === '')
    const nonEmptyValues = result.filter(x => x != null && x !== '')
    
    // Check that we have the right number of each
    expect(emptyValues).toHaveLength(3)
    expect(nonEmptyValues).toEqual([1, 2]) // Should be sorted
    expect(emptyValues).toEqual(expect.arrayContaining([undefined, '', null]))
  })
})
