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
