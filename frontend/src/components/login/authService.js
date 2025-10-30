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
import { getServicesBasePath } from '@/services.js'
import { axios } from '@/globals.js'
import { ENGINE_STORAGE_KEY } from '@/constants.js'

export default {
    login: function(params, remember) {
        const engineName = localStorage.getItem(ENGINE_STORAGE_KEY)
        const headers = engineName ? { 'X-Process-Engine': engineName } : {}
        return axios.create().post(getServicesBasePath() + '/auth/login', params, { 
            params: { source: 'WEBSITE' },
            headers: headers
        }).then(function(user) {
            axios.defaults.headers.common.authorization = user.data.authToken
            ;(remember ? localStorage : sessionStorage).setItem('token', user.data.authToken)
            return user.data
        })
    },

       update: function(params) {
        return axios.patch(getServicesBasePath() + '/auth/user', params, { params: { source: 'WEBSITE' } }).then(function(user) {
            axios.defaults.headers.common.authorization = user.authToken
            if (sessionStorage.getItem('token') || !localStorage.getItem('token')) sessionStorage.setItem('token', user.authToken)
            else localStorage.setItem('token', user.authToken)
            return user
        })
    },

    'delete': function(id) { return axios['delete'](getServicesBasePath() + '/auth/user/' + id + '?source=WEBSITE') },

    requestPasswordReset: function(params) {
        params.source = 'WEBSITE'
        return axios.create().post(getServicesBasePath() + '/auth/reset', null, { params: params })
    },

    poll4otp: function(userId) { return axios.get(getServicesBasePath() + '/auth/otp/' + userId + '?source=WEBSITE') },
}