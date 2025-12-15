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
const TaskPool = function(limit) {
	let pool = []
	let runningInstances = 0
	const start = () => {
		if (pool.length > 0) {
			let newInstance = pool.shift()
			if (!newInstance.isExecuted) {
				newInstance.isExecuted = true
				newInstance.params = newInstance.params || []
				newInstance.func(...newInstance.params).then(data => {
					newInstance.resolve(data)
					start()
				}, e => { 
					newInstance.reject(e)
				 	start() 
				})
			} else start()
		} else runningInstances--
	}
	const add = (func, params, id) => {
		if (id != null) {
			const instance = findInstanceById(id)
			if (instance) {
				return instance.promise
			}
		}
		let newEntry
		const prom = new Promise((resolve, reject) => {
			newEntry = {
				func: func, params: params, id: id, resolve: resolve, reject: reject
			}
			pool.push(newEntry)
		})
		newEntry.promise = prom
		if (runningInstances < limit) {
			runningInstances++
			start()
		}
		return prom
	}
	const addPrio = (func, params, id) => {
		if (id != null) {
			const instance = findInstanceById(id)
			if (instance && instance.isExecuted) {
				return instance.promise
			} else if (instance) {
				pool.unshift(instance)
				return instance.promise
			}
		}
		let newEntry
		const prom = new Promise((resolve, reject) => {
			newEntry = {
				func: func, params: params, id: id, resolve: resolve, reject: reject
			}
			pool.unshift(newEntry)
		})
		newEntry.promise = prom
		if (runningInstances < limit) {
			runningInstances++
			start()
		}
		return prom
	}
	const clear = () => {
		pool = []
	}
	const findInstanceById = (id) => {
		//Make copy so the data pool is not modified during the search and prevent wrong search
		const copyPool = [...pool]
		return copyPool.find(p => {
			return p.id === id
		})
	}
	
	return {
	    add: add,
		addPrio: addPrio,
		clear: clear
  	}
}

export { TaskPool }