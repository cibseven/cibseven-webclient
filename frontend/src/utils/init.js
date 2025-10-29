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
import { axios } from '@/globals.js'
import { getServicesBasePath } from '@/services.js'

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
  var title = productName
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

export function handleAxiosError(router, root, error) {
  if (error.response) {
    var res = error.response
    if (res.status === 401) { // Unauthorized
      if (res.data && res.data.type === 'TokenExpiredException' && res.data.params && res.data.params.length > 0) {
        console && console.info('Prolonged token')
        if (sessionStorage.getItem('token')) sessionStorage.setItem('token', res.data.params[0])
        else if (localStorage.getItem('token')) localStorage.setItem('token', res.data.params[0])
        axios.defaults.headers.common.authorization = root.user.authToken = res.data.params[0]
        // Repeat last request
        error.config.headers.authorization = res.data.params[0]
        return axios.request(error.config)
      } else {
        console && console.warn('Not authenticated, redirecting ...')
        sessionStorage.getItem('token') ? sessionStorage.removeItem('token') : sessionStorage.removeItem('token')
        axios.defaults.headers.common.authorization = ''
        root.user = null
        if (router.currentRoute.path !== '/seven/login') {
          let url = '/seven/login'
          if (router.currentRoute.path) url += '?nextUrl=' + router.currentRoute.path
          router.push(url)
        }
        // root.$refs.error.show(res.data || res.statusText)
      }
      return Promise.reject(error)
    } else if (res.status === 500) {
      var exceptions = ['NoObjectFoundException', 'InvalidAttributeValueException', 'SubmitDeniedException',
        'UnsupportedTypeException', 'ExpressionEvaluationException', 'ExistingUserRequestException',
        'ExistingGroupRequestException', 'AccessDeniedException', 'SystemException', 'InvalidUserIdException', 'InvalidValueHistoryTimeToLive',
        'VariableModificationException', 'WrongDeploymenIdException', 'NoRessourcesFoundException', 'DmnTransformationException']
      if (res.data.type && exceptions.indexOf(res.data.type) !== -1)
        root.$refs.error.show(res.data)
      //root.$refs.report.show(res.data)
      return Promise.reject(error)
    } else {
      //root.$refs.error.show(res.data || res.statusText)
      return Promise.reject(error)
    }
  } else { // for example "Network Error" - doesn't work with spaces in translations.json
    console && console.error('Strange AJAX error', error)
    var message = error.message.replace(' ', '_')
    if (message !== 'Request_aborted') root.$refs.error.show({ type: message })
    return Promise.reject(error)
  }
}

export function applyTheme(theme) {

  var css = document.createElement('Link')
  css.setAttribute('rel', 'stylesheet')
  css.setAttribute('type', 'text/css')
  css.setAttribute('href', 'themes/' + theme + '/styles.css')

  var favicon = document.createElement('Link')
  favicon.setAttribute('rel', 'icon')
  favicon.setAttribute('type', 'image/x-icon')
  favicon.setAttribute('href', 'themes/' + theme + '/favicon.ico')

  document.head.appendChild(css)
  document.head.appendChild(favicon)
}

export function fetchAndStoreProcesses(app, store, config, extraInfo) {
  const method = extraInfo ? 'findProcessesWithInfo' : 'findProcesses'
  return store.dispatch(method).then((result) => {
    const processes = app.processesByPermissions(config.permissions.startProcess, result)
    processes.forEach((process) => {
      process.loading = false
    })
    store.commit('setProcesses', { processes })

    // Sync favorites from local storage with the process store
    if (localStorage.getItem('favorites')) {
      store.dispatch('setFavorites', { favorites: JSON.parse(localStorage.getItem('favorites')) })
    }
  })
}

export async function fetchDecisionsIfEmpty(store) {
  if (store.state.decision.list.length < 1) {
    const decisions = await store.dispatch('getDecisionList', { latestVersion: true })
    const reduced = decisions.map(d => ({ key: d.key, id: d.id, name: d.name, latestVersion: d.latestVersion }))
    store.commit('setDecisions', { decisions: reduced })
    return reduced
  } else {
    return store.state.decision.list
  }
}

export function setupTaskNotifications(app, root, theme) {
  if (window.Worker && localStorage.getItem('tasksCheckNotificationsDisabled') !== 'true' &&
    root.config.notifications.tasks.enabled && Notification.permission === 'granted') {
    const taskWorker = new Worker(new URL('../task-worker.js', import.meta.url), { type: 'module' })
    const authToken = sessionStorage.getItem('token') || localStorage.getItem('token')
    // Construct the full URL for the worker since it runs in a different context
    const baseUrl = window.location.origin
    const servicesPath = getServicesBasePath()
    const fullServicesUrl = baseUrl + '/' + servicesPath
    taskWorker.postMessage({
      type: 'setup', 
      interval: root.config.notifications.tasks.interval,
      authToken: authToken, 
      userId: root.user.id,
      servicesBasePath: fullServicesUrl
    })
    taskWorker.postMessage({ type: 'checkNewTasks' })
    taskWorker.addEventListener('message', event => {
      if (event.data && event.data.type === 'sendNotification') {
        const options = {
          body: app.$t('notification.newTasks'),
          tag: 'cib-flow-check-new-tasks',
          icon: 'themes/' + theme + '/notification-icon.svg'
        }
        const notification = new Notification(app.$t('notification.newTasksTitle'), options)
        notification.onclick = () => {
          notification.close()
          window.focus()
        }
      }
    })
  }
}