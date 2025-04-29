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
/* jshint worker: true */

import moment from 'moment'
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
