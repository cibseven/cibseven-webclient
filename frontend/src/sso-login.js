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
import { axios, createUUID } from './globals.js'
import { sha256 } from 'js-sha256'
import { ENGINE_STORAGE_KEY } from '@/constants.js'

axios.interceptors.response.use(function(res) { return res.data; }, handler)
const BASE_URL = import.meta.env.BASE_URL
import { InfoService } from '@/services.js'

function handler(error) {
    if (error.response) {
        const res = error.response
        console && console.error('Strange AJAX error', error)
        alert('Technical error: ' + (res.data.type || 'SystemException'))
    } else {
        console && console.error('Strange AJAX error', error)
        alert('Technical error occurred')
    }
    return Promise.reject(error)
}

InfoService.getProperties().then(function(config) {
    // Debug point will be left here because it is hard to debug when it is redirected all the time
    // eslint-disable-next-line no-debugger
    debugger
    const ssoCallback = parseParams(location.hash.substr(1))
    const callbackState = JSON.parse(sessionStorage.getItem('callbackState') || '{}')
    const queryParams = parseParams(location.search.substr(1))
    const redirectTo = callbackState.redirectTo || BASE_URL + './#' + (queryParams.nextUrl || '').replace(/^(.*\/\/.*?\/|\/)/, '')

    if (!(callbackState.state && ssoCallback.state === callbackState.state)) {
        ssoLogin(callbackState, redirectTo)
    } else {
        history.pushState('', document.title, location.pathname + location.search); //Delete hashParams
        const engineName = localStorage.getItem(ENGINE_STORAGE_KEY) || 'default'
        const headers = engineName ? { 'X-Process-Engine': engineName } : {}
        axios.post(config.servicesBasePath + '/auth/login', {
            type: 'org.cibseven.webapp.auth.sso.SSOLogin',
            code: ssoCallback.code,
            nonce: callbackState.nonce,
            redirectUrl: location.origin + location.pathname
        }, { headers: headers }).then(function(user) {
            localStorage.setItem('token', user.authToken)
            sessionStorage.removeItem('callbackState')
            location.href = redirectTo
        })
    }

    function ssoLogin(callbackState, redirectTo) {
        const queryParams = parseParams(location.search.substr(1))
        const state = createUUID()
        const nonce = createUUID()
        const request = config.authorizationEndpoint +
            '?client_id=' + encodeURIComponent(config.clientId) +
            '&redirect_uri=' + encodeURIComponent(location.origin + location.pathname) +
            '&state=' + encodeURIComponent(state) +
            '&nonce=' + encodeURIComponent(sha256(nonce)) +
            '&response_mode=' + encodeURIComponent('fragment') +
            '&response_type=' + encodeURIComponent('code') +
            '&scope=' + encodeURIComponent(config.scopes)
        sessionStorage.setItem('callbackState', JSON.stringify({
            state: state,
            nonce: nonce,
            redirectTo: redirectTo,
            role: queryParams.role,
            client: queryParams.client
        }))
        location.href = request
    }
})

function parseParams(paramString) {
    return paramString.split('&').reduce(function(params, param) {
        params[param.split('=')[0]] = decodeURIComponent(param.split('=')[1])
        return params
    }, {})
}
