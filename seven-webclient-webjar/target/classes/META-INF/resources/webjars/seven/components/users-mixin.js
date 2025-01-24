var usersMixin = {
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

export { usersMixin }