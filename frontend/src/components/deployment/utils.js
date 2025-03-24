export function sortDeployments(a, b, sorting, order) {
  if (!a[sorting] && b[sorting]) return -1
  else if (a[sorting] && !b[sorting]) return 1
  a = a[sorting].toLowerCase()
  b = b[sorting].toLowerCase()
  if (order === 'asc') return a < b ? -1 : a > b ? 1 : 0
  else return a < b ? 1 : a > b ? -1 : 0
}
