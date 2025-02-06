/* globals moment, setInterval, fetch */
/* jshint worker: true */

importScripts('webjars/momentjs/2.29.4/min/moment.min.js')

import appConfig from './appConfig.js'	

var authToken = ''
var createdAfter = ''
var interval = ''
var userId = ''

var Services = {
	findTasksPost: function(body) {
		'use strict';
		const url = appConfig.servicesBasePath + '/task'
		return fetch(url, {
            method: 'POST',
            headers: {
                'Authorization': authToken,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(body)
        })
	}
}

self.addEventListener('message', event => {
	'use strict';
	if (event.data && event.data.type === 'setup') {
		authToken = event.data.authToken
		userId = event.data.userId
		interval = event.data.interval ? event.data.interval : null
	} else if (event.data && event.data.type === 'checkNewTasks') {
		createdAfter = moment().format('YYYY-MM-DDTHH:mm:ss.SSSZZ')
		if (interval) {
			setInterval(() => {
				checkNewTasks()
			}, interval)
		}
    }
})

function checkNewTasks() {
	'use strict';
	var body = { assignee: userId }
	if (createdAfter) body.createdAfter = createdAfter
	Services.findTasksPost(body).then(response => {
		createdAfter = moment().format('YYYY-MM-DDTHH:mm:ss.SSSZZ')
		return response.json()
	}).then(data => {
		if (data) {
			if (data.type === 'TokenExpiredException' && data.params) authToken = data.params[0]
			else if (data.length > 0) {
				self.postMessage({ type: 'sendNotification', tasks: data })
			}
		}
	})
}