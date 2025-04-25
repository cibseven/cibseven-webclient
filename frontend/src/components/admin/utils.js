export function notEmpty(value) {
  if (value === null) return null
  if (value.length < 1) return false
  return value != null && value.length > 0
}

export function same(value, value2) {
  if (value === null || value2 === null) return null
  if (value.length < 1 || value2.length < 1) return false
  if (value != null && value2 != null && value === value2) return true
  return false
}

export function isValidId(value) {
  if (value === null) return null
  if (value.indexOf(' ') >= 0) return false
  return notEmpty(value)
}

export function isValidEmail(value) {
  if (value === null || value === '') return null
  if (/^(?!.*\.\.)[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(value)) return true
  // Regular expression for validating an email address with an IP address in the domain
  if (/^[a-zA-Z0-9._%+-]+@(\d{1,3}\.){3}\d{1,3}$/.test(value)) return true
  return false
}

export function getStringObjByKeys(keys, obj) {
  var result = ''
  keys.forEach(key => {
    if (key === 'userIdGroupId') {
      if (obj.userId) result += obj.userId + ';'
      else result += obj.groupId + ';'
    } else result += obj[key] + ';'
  })
  return result.slice(0, -1)
}
