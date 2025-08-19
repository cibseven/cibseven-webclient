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
