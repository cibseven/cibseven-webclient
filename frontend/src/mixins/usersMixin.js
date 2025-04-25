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
export default {
	computed: {
		getCompleteName: function() {
			if (this.$root.user.id.toLowerCase() === this.task.assignee.toLowerCase()) return this.$root.user.id // displayName
			else {
				if (this.$store.state.user.listCandidates) {
					var user = this.$store.state.user.listCandidates.find(user => {
						return user.id.toLowerCase() === this.task.assignee.toLowerCase()
					})
					if ((user) && (user.displayName)) return user.displayName
				}
				return this.task.assignee
			}
		}
	},
	methods: {
		findUsers: function(filter) {
			var maxResults = this.$root.config.maxUsersResults || 10
			this.$refs.ariaLiveText.textContent = ''
			this.$store.dispatch('findUsers', { maxResults: maxResults, filter: filter, 
				userProvider: this.$root.config.userProvider }).then(() => {
				if (this.$store.state.user.searchUsers.length > 0) {
					this.$refs.ariaLiveText.textContent = this.$t('task.usersFound')
				}
				this.loadingUsers = false
			}, () => this.loadingUsers = false)
		},
		resetUsers: function(evt) {
			if (evt) this.$store.commit('setSearchUsers', [])
			else this.$store.commit('setSearchUsers', this.$store.state.user.listCandidates)
		}
	}
}