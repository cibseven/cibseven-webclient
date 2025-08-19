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

// Generic comparator factory for sorting with null/empty handling
// Usage: arr.sort(createSortComparator(getValue, sortDesc))

/**
 * Returns a comparator function for Array.prototype.sort that handles null/empty values.
 * @param {function(any): any} getValue - Function to extract the value to compare from an item.
 * @param {boolean} sortDesc - Whether to sort descending.
 * @returns {function(any, any): number}
 */
export function createSortComparator(getValue, sortDesc) {
  return (a, b) => {
    const aVal = getValue(a)
    const bVal = getValue(b)
    const aEmpty = aVal == null || aVal === ''
    const bEmpty = bVal == null || bVal === ''

    if (aEmpty && bEmpty) return 0
    if (aEmpty) return sortDesc ? 1 : -1
    if (bEmpty) return sortDesc ? -1 : 1

    if (aVal < bVal) return sortDesc ? 1 : -1
    if (aVal > bVal) return sortDesc ? -1 : 1
    return 0
  }
}
