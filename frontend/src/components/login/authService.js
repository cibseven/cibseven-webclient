import appConfig from '@/appConfig.js'
import { axios } from '@/globals.js'

export default {
    login: function(params, remember) {
        return axios.create().post(appConfig.servicesBasePath + '/auth/login', params, { params: { source: 'WEBSITE' } }).then(function(user) {
            axios.defaults.headers.common.authorization = user.data.authToken
            ;(remember ? localStorage : sessionStorage).setItem('token', user.data.authToken)
            return user.data
        })
    },

       update: function(params) {
        return axios.patch(appConfig.servicesBasePath + '/auth/user', params, { params: { source: 'WEBSITE' } }).then(function(user) {
            axios.defaults.headers.common.authorization = user.authToken
            if (sessionStorage.getItem('token') || !localStorage.getItem('token')) sessionStorage.setItem('token', user.authToken)
            else localStorage.setItem('token', user.authToken)
            return user
        })
    },

    'delete': function(id) { return axios['delete'](appConfig.servicesBasePath + '/auth/user/' + id + '?source=WEBSITE') },

    requestPasswordReset: function(params) {
        params.source = 'WEBSITE'
        return axios.create().post(appConfig.servicesBasePath + '/auth/reset', null, { params: params })
    },

    poll4otp: function(userId) { return axios.get(appConfig.servicesBasePath + '/auth/otp/' + userId + '?source=WEBSITE') },
}