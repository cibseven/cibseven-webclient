/*
 * @return subfolder for the active theme
 */
export function getTheme(config) {
  return config.theme || "generic"
}

export function hasHeader() {
  function parseParams(paramString) {
    return paramString.split('&').reduce((params, param) => {
      params[param.split('=')[0].replace('?', '')] = decodeURIComponent(param.split('=')[1])
      return params
    }, {})
  }

  var params = parseParams(window.location.hash)
  var header = params.header || 'true'
  return header
}

import platform from 'platform'
export function isMobile() {
  if ((platform.os.family === 'Android') || (platform.os.family === 'iOS')) return true
  else return false
}

export function checkExternalReturn(href, hash) {
  //var hrefAux = href
  var hashAux = hash

  if (hashAux.includes('token=')) {
    var token = ''

    var tokenStartPos = hashAux.indexOf('token=') + 'token='.length

    if(hashAux.indexOf('&', tokenStartPos) > -1)
      token = hashAux.substring(tokenStartPos, hashAux.indexOf('&', tokenStartPos))
    else
      token = hashAux.substring(tokenStartPos)
    localStorage.setItem('token', decodeURIComponent(token))

    // navigate to the new URL, which leads to a page reload
    window.location.href = hashAux.replace('&token=' + token, '')
  }
}

export function updateAppTitle(productName, sectionName = undefined, taskName = undefined) {
  var title = productName || 'CIB seven'
  if (sectionName) {
    title += ' | '
    title += sectionName

    if (taskName) {
      title += ' | '
      title += taskName
    }
  }
  document.title = title
}
