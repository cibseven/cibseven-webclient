import { axios } from './globals.js'
import { sha256 } from 'js-sha256'
			
axios.interceptors.response.use(function(res) { return res.data; }, handler)
const BASE_URL = import.meta.env.BASE_URL
import { InfoService } from '@/services.js'
import appConfig from './appConfig.js'

function handler(error) {
    if (error.response) {
        var res = error.response
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
    debugger
    var ssoCallback = parseParams(location.hash.substr(1))
    var callbackState = JSON.parse(sessionStorage.getItem('callbackState') || '{}')
    var queryParams = parseParams(location.search.substr(1))
    var redirectTo = callbackState.redirectTo || BASE_URL + './#' + (queryParams.nextUrl || '').replace(/^(.*\/\/.*?\/|\/)/, '')
    
    if (!(callbackState.state && ssoCallback.state === callbackState.state)) {
        ssoLogin(callbackState, redirectTo)
    } else {
        history.pushState('', document.title, location.pathname + location.search); //Delete hashParams
        axios.post(appConfig.servicesBasePath + '/auth/login', {
            type: 'org.cibseven.webapp.auth.sso.SSOLogin',
            code: ssoCallback.code,
            nonce: callbackState.nonce,
            redirectUrl: location.origin + location.pathname
        }).then(function(user) {
            localStorage.setItem('token', user.authToken)
            sessionStorage.removeItem('callbackState')
            location.href = redirectTo
        })
    }
    
    function ssoLogin(callbackState, redirectTo) {
        var queryParams = parseParams(location.search.substr(1))
        var state = createUUID()
        var nonce = createUUID()
        var request = config.authorizationEndpoint +
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

function createUUID() { //keycloak.js unique identifier for state and nonce
    var s = [];
    var hexDigits = '0123456789abcdef';
    for (var i = 0; i < 36; i++) {
        s[i] = hexDigits.substr(Math.floor(Math.random() * 0x10), 1);
    }
    s[14] = '4';
    s[19] = hexDigits.substr((s[19] & 0x3) | 0x8, 1);
    s[8] = s[13] = s[18] = s[23] = '-';
    var uuid = s.join('');
    return uuid;
}