/* globals setTimeout, clearTimeout, Blob, moment, localStorage */
	
	import { AdminService, AuthService } from '../../services.js';

	function debounce(delay, fn) {
		var timeoutID = null
		return function() {
			clearTimeout(timeoutID)
			var args = arguments
			var self = this
			timeoutID = setTimeout(function() { fn.apply(self, args) }, delay)
		}
	}
	
	function notEmpty(value) {
		if (value === null) return null
		if (value.length < 1) return false
		return value != null && value.length > 0
	}
	
	function same(value, value2) {
		if (value === null || value2 === null) return null
		if (value.length < 1 || value2.length < 1) return false
		if (value != null && value2 != null && value === value2) return true
		return false
	}
	
	function isValidId(value) {
		if (value === null) return null
		if (value.indexOf(' ') >= 0) return false
		return notEmpty(value)		
	}
	
	function isValidEmail(value) {
		if (value === null || value === '') return null
		if (/^\w+([\.-]?\w+)*@\w+([\.-]?\w+)*(\.\w{2,3})+$/.test(value)) return true
		return false
	}
	
	function getStringObjByKeys(keys, obj) {
		var result = ''
		keys.forEach(key => {
			if (key === 'userIdGroupId') {
				if (obj.userId) result += obj.userId + ';'
				else result += obj.groupId + ';'
			} else result += obj[key] + ';'
		})
		return result.slice(0, -1)
	}
	
	const AdminUsers = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/admin/admin-users.html').then(function(html) {
			return {
				template: html,
				data: function () {
					return {
						selected: null,
						focused: null,
						filter: '',
						users: [],
						userSelected: null,				
						firstResult: 0,
						maxResults: 40,
						loading: false,
						exporting: false
					}
				},
				created: function() {											
					this.loading = true
					this.loadUsers()
				},
				methods: {
					loadUsers: debounce(800, function (showMore = false) {
						AdminService.findUsers({ firstResult: this.firstResult, maxResults: this.maxResults }).then(response => {
							if (!showMore) this.users = response
							else this.users = this.users.concat(response)
							this.loading = false
						})
					}),
					add: function () {
						this.$router.push('/seven/auth/admin/create-user')
					},
					prepareRemove: function (user) {
						this.userSelected = user
						this.$refs.deleteModal.show()
					},
					remove: function (user) {
						AdminService.deleteUser(user.id).then(() => {
							this.userSelected = null
							this.firstResult = 0
							this.loadUsers()
						})
					},
					edit: function (user) {
						this.$router.push('/seven/auth/admin/user/' + user.id + '?tab=profile')
					},
					showMore: function(el) {
						if (this.firstResult <= this.users.length && !this.loading) {
					      	if ((el.target.offsetHeight + el.target.scrollTop + 1) >= el.target.scrollHeight && this.filter==='') {
						        this.firstResult += this.maxResults										
								this.loading = true						        
								this.loadUsers(true)
					      	}
				      	}
					},
					searchUsers: function() {
						if (this.filter.length > 2) {
							this.users = []
							this.loading = true
							this.findUsers(this.filter)
						} else if (!this.filter || this.filter.length === 0) {
							this.users = []
							this.userSelected = null
							this.firstResult = 0
							this.loading = true
							this.loadUsers()
						}						
					},
					findUsers: debounce(800, function(filter) {
						var firstNameLike = null
						var lastNameLike = null		
						var id = null							
						firstNameLike = ({ firstNameLike: '*' + filter + '*' })
						lastNameLike = ({ lastNameLike: '*' + filter + '*' })
						id = ({ id: filter })
						Promise.all([AdminService.findUsers(firstNameLike), 
							AdminService.findUsers(lastNameLike), AdminService.findUsers(id)])
						.then(users => {				
							users = users[0].concat(users[1]).concat(users[2])								
							// Remove duplicates
							users = users.filter((value, index, self) =>
							  	index === self.findIndex((t) => (
							    t.id === value.id
							  ))
							)								
							this.users = users
							this.loading = false
						})				
					}),
					exportCSV: function() {
						this.exporting = true
						var keys = ['id', 'firstName', 'lastName', 'email']
						var csvContent = keys.map(k => this.$t('admin.users.' + k)).join(';') + '\n'
						AdminService.findUsers().then(users => {
							if (users.length > 0) {
								users.forEach(r => {
									csvContent += getStringObjByKeys(keys, r) + '\n'
								})
								var csvBlob = new Blob([csvContent], { type: 'text/csv' })
								var filename = 'users_' + moment().format('YYYYMMDD_HHmm') + '.csv'
								this.$refs.importPopper.triggerDownload(csvBlob, filename)
							}
							this.exporting = false
						})
					}
				}
			}
        })
	})
		
	const AdminGroups = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/admin/admin-groups.html').then(function(html) {
			return {
				template: html,
				data: function () {
					return {
						selected: null,
						focused: null,
						filter: '',
						groups: [],
						groupSelected: null,				
						firstResult: 0,
						maxResults: 40,
						loading: false,
						exporting: false	
					}
				},
				created: function () {
					this.loading = true
					this.loadGroups()
				},
				methods: {
					loadGroups: debounce(800, function (showMore = false) {
						AdminService.findGroups({ firstResult: this.firstResult, maxResults: this.maxResults }).then(response => {
							if (!showMore) this.groups = response
							else this.groups = this.groups.concat(response)
							this.loading = false
						})
					}),		
					add: function() {
						this.$router.push('/seven/auth/admin/create-group')
					},
					prepareRemove: function(group) {
						this.groupSelected = group
						this.$refs.deleteModal.show()
					},
					remove: function (group) {
						AdminService.deleteGroup(group.id).then(() => {
							this.firstResult = 0
							this.groupSelected = null
							this.loadGroups()
						})
					},
					edit: function(group) {
						this.$router.push('/seven/auth/admin/group/' + group.id + '?tab=information')
					},
					showMore: function(el) {
						if (this.firstResult <= this.groups.length && !this.loading) {
					      	if ((el.target.offsetHeight + el.target.scrollTop + 1) >= el.target.scrollHeight && this.filter==='') {
						        this.firstResult += this.maxResults
						        this.loading = true
								this.loadGroups(true)
					      	}
				      	}
					},
					searchGroups: function() {
						if (this.filter.length > 2) {
							this.groups = []
							this.loading = true
							this.findGroups(this.filter)
						} else if (!this.filter || this.filter.length === 0) {
							this.groups = []
							this.userSelected = null
							this.firstResult = 0
							this.loading = true
							this.loadGroups()
						}						
					},
					findGroups: debounce(800, function(filter) {
						var nameLike = ({ nameLike: '*' + filter + '*' })
						var id = ({ id: filter })						
					
						Promise.all([AdminService.findGroups(nameLike), AdminService.findGroups(id)])
						.then(groups => {				
							groups = groups[0].concat(groups[1])							
							// Remove duplicates
							groups = groups.filter((value, index, self) =>
							  	index === self.findIndex((t) => (
							    t.id === value.id
							  ))
							)								
							this.groups = groups
							this.loading = false
						})
					}),
					exportCSV: function() {
						this.exporting = true
						var keys = ['id', 'name', 'type']
						var csvContent = keys.map(k => this.$t('admin.groups.' + k)).join(';') + '\n'
						AdminService.findGroups().then(groups => {
							if (groups.length > 0) {
								groups.forEach(r => {
									csvContent += getStringObjByKeys(keys, r) + '\n'
								})
								var csvBlob = new Blob([csvContent], { type: 'text/csv' })
								var filename = 'groups_' + moment().format('YYYYMMDD_HHmm') + '.csv'
								this.$refs.importPopper.triggerDownload(csvBlob, filename)
							}
							this.exporting = false
						})
					}
				}
			}
        })
	})
	
	const AdminAuthorizations = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/admin/admin-authorizations.html').then(function(html) {
			return {
				template: html,
				data: function () {
					return {
						leftOpen: true
					}
				}
			}
		})		
	})
	
	const AdminAuthorizationsTable = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/admin/admin-authorizations-table.html').then(function(html) {
			return {
				template: html,
				data: function () {
					return {
						selected: [],
						focused: null,
						filter: '',
						authorizations: [],
						resourcesTypes: this.$root.config.admin.resourcesTypes,
						types: this.$root.config.admin.types,
						edit: null,
						isUserToEdit: true,
						authorizationSelected: null,				
						firstResult: 0,
						maxResults: 40,
						loading: false,
						exporting: false
					}
				},
				watch: {
					'$route.params.resourceTypeId': function(resourceTypeId) {
						this.authorizations = []
						this.firstResult = 0
						this.loading = true
						this.loadAuthorizations(resourceTypeId) 
					}
				},
				mounted: function () {
					if (this.$route.params.resourceTypeId) {
						this.loading = true
						this.loadAuthorizations(this.$route.params.resourceTypeId)
					}
				},
				computed: {
					authorizationFields: function() {
			      		var baseFields = [
				        	{ label: 'type', key: 'type', class: 'col' },
					        { label: 'userIdGroupId', key: 'userIdGroupId', class: 'col' },
					        { label: 'permissions', key: 'permissions', class: 'col' },
					        { label: 'resourceId', key: 'resourceId', class: 'col' },
					        { label: 'actions', key: 'actions', class: 'col', sortable: false, 
					        	thClass: 'justify-content-center', tdClass: 'justify-content-center py-0' }
			      		]				
						if (this.$route.params.resourceTypeId === '5') 
		       				baseFields.splice(3, 0, { label: 'name', key: 'name', class: 'col' })			
				      	return baseFields
				    },
					exportableAuth: function() {
						return ['0', '4', '5', '6', '9'].includes(this.$route.params.resourceTypeId)
					},
					filterNameOptions: function() {
						return this.$store.state.filter.list.map(item => item.name)
					}
				},
				methods: {
					loadAuthorizations: debounce(800, function (resourceTypeId, showMore = false) {
						AdminService.findAuthorizations({ 
								resourceType: resourceTypeId,
							 	firstResult: this.firstResult, 
							 	maxResults: this.maxResults
							}).then(response => {
								if (!showMore) this.authorizations = response
								else this.authorizations = this.authorizations.concat(response)								
								if (resourceTypeId === '5') {									
									this.authorizations.forEach(authorization => {
										var filter = this.$store.state.filter.list.find(obj => obj.id === authorization.resourceId)
										if (filter) authorization.name = filter.name
									})
								}								
								this.loading = false
							})
					}),	
					getClasses: function(authorization) {
						var classes = []
						if (authorization !== this.focused) classes.push('invisible')
						return classes
					},
					prepareEdit: function (authorization) {
						// In case we press edit and we are in the middle to add a new authorization, that extra "unfinished"
						// auth needs to be removed from list.
						if (this.authorizations[0].id === "0") this.authorizations.shift() 
						this.edit = authorization.id
						this.isUserToEdit = (authorization.userId != null) ? true : false
						this.selected = []
						this.selected = authorization.permissions
						this.authorizationSelected = authorization
					},
					prepareRemove: function (authorization) {
						this.authorizationSelected = authorization
						this.$refs.deleteModal.show()
					},
					cancelEdit: function(authorization) {
						this.selected = []
						this.edit = null
						authorization = this.authorizationSelected
						this.authorizationSelected = null
						// If id == 0 then means that we are creating a new authorization.
						if (authorization.id === "0") {
							this.authorizations.shift()
						}
					},
					remove: function(authorization) {
						AdminService.deleteAuthorization(authorization.id).then(() => {
							this.authorizationSelected = null
							this.firstResult = 0
							this.loadAuthorizations(this.$route.params.resourceTypeId)
						})
					},					
					selectAll: function() {
						if (this.selected.length === this.resourcesTypes[this.$route.params.resourceTypeId].permissions.length) {
							this.selected = []
						} else {
							this.selected = this.resourcesTypes[this.$route.params.resourceTypeId].permissions
						}
					},
					save: function (authorization) {
						if ((this.isUserToEdit) && (authorization.userId == null)) {
							authorization.userId = authorization.groupId
							authorization.groupId = null
						} else if ((!this.isUserToEdit) && (authorization.groupId == null)) {
							authorization.groupId = authorization.userId	
							authorization.userId = null						
						}
						
						if (this.selected.length === this.resourcesTypes[this.$route.params.resourceTypeId].permissions.length) {
							authorization.permissions = ['ALL']
						} else if (this.selected.length === 0) {
							authorization.permissions = ['NONE']
						} else {
							authorization.permissions = this.selected 	
						}
						// If id == 0 then means that we are creating a new authorization. and new auth is going to be always in first place.
						if (authorization.id === "0") {
							authorization.id = null
							AdminService.createAuthorization(authorization).then((res) => {
								authorization.id = res.id
							})
						} else {
							AdminService.updateAuthorization(authorization.id, authorization).then(() => {
								this.cancelEdit(authorization)
							})	
						}
					},
					add: function () {
						// If we are already adding a new element, no more should be allowed.
						if (this.authorizations[0].id !== "0") {
							this.authorizations.unshift({
								id: "0",
								type: "1",
								permissions: ["ALL"],
								userId: null,
								groupId: null,
								resourceType: this.$route.params.resourceTypeId,
								resourceId: null
							})
							this.selected = this.authorizations[0].permissions
							this.isUserToEdit = true
							this.authorizationSelected = this.authorizations[0]
							this.edit = this.authorizationSelected.id
						}
					},					
					showMore: function(el) {
						if (this.firstResult <= this.authorizations.length && !this.loading) {
					      	if ((el.target.offsetHeight + el.target.scrollTop + 1) >= el.target.scrollHeight && this.filter==='') {
						        this.firstResult += this.maxResults
						        this.loading = true
								this.loadAuthorizations(this.$route.params.resourceTypeId, true)
					      	}
				      	}
					},
					searchAuthorizations: function() {
						if (this.filter.length > 2) {
							this.authorizations = []
							this.loading = true
							this.findAuthorizations(this.filter)
						} else if (!this.filter || this.filter.length === 0) {
							this.authorizations = []
							this.authorizationSelected = null
							this.firstResult = 0
							this.loading = true
							this.loadAuthorizations(this.$route.params.resourceTypeId)
						}						
					},
					findAuthorizations: debounce(800, function(filter) {
						Promise.all([
							AdminService.findAuthorizations({ resourceType: this.$route.params.resourceTypeId, resourceId: filter }),
							AdminService.findAuthorizations({ resourceType: this.$route.params.resourceTypeId, userIdIn: filter }),
							AdminService.findAuthorizations({ resourceType: this.$route.params.resourceTypeId, groupIdIn: filter })
						])
						.then(authorizations => {				
							authorizations = authorizations[0].concat(authorizations[1]).concat(authorizations[2])
							// Remove duplicates
							authorizations = authorizations.filter((value, index, self) =>
							  	index === self.findIndex((t) => (
							    t.id === value.id
							  ))
							)								
							this.authorizations = authorizations
							this.loading = false
						})	
					}),
					onFilterNameChange: function(item) {
						var filter = this.$store.state.filter.list.find(obj => obj.name === item.name)
						if (filter) item.resourceId = filter.id
						else item.resourceId = '*'
					},
					exportCSV: function() {
						this.exporting = true
						var params = this.$route.params
						var keys = ['type', 'userIdGroupId', 'permissions', 'resourceId']
						var csvContent = keys.map(k => this.$t('admin.authorizations.' + k)).join(';') + '\n'
						AdminService.findAuthorizations({ resourceType: params.resourceTypeId }).then(auths => {
							if (auths.length > 0) {
								auths.forEach(r => {
									csvContent += getStringObjByKeys(keys, r) + '\n'
								})
								var csvBlob = new Blob([csvContent], { type: 'text/csv' })
								var filename = 'authorizations_' + this.$t('admin.authorizations.resourcesTypes.' + params.resourceTypeKey) + 
									'_' + moment().format('YYYYMMDD_HHmm') + '.csv'
								this.$refs.importPopper.triggerDownload(csvBlob, filename)
							}
							this.exporting = false
						})
					}
				}
			}
		})
	})
	
	const AuthorizationsNavBar = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/admin/authorizations-nav-bar.html').then(function(html) {
			return {
				template: html,
				data: function() {
					return {
						resourcesTypes: this.$root.config.admin.resourcesTypes
					}
				}
			}
		})		
	})
	
	const CreateUser = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/admin/create-user.html').then(function(html) {
			return {
				template: html,
				data: function () {
					return {
						profile: { id: null, email: null, firstName: null, lastName: null },
						credentials: { password: null },
						passwordRepeat: null,
						showPassword: false,
						showPassRepeat: false,
						passwordPolicyError: false,
						userIdError: false
					}
				},
				methods: {
					fieldType: function(showPass) {
						if (showPass) 
							return 'text'
						return 'password'
					},
					onSubmit: function(evt) {
						evt.preventDefault()
						if (!same(this.credentials.password, this.passwordRepeat)) return
						AdminService.createUser({ 'profile': this.profile, 'credentials': this.credentials }).then(() => {
							this.passwordPolicyError = false
							this.userIdError = false
							this.$refs.userCreated.show(1)
							setTimeout(() => {
								this.$router.push('/seven/auth/admin/users')			
							}, 1000)
						}, error => {
							var data = error.response.data
							if (data) {
								if (data.type === 'PasswordPolicyException') this.passwordPolicyError = true
								else if (data.type === 'InvalidUserIdException') this.userIdError = true
							}
						})
					},
					onReset: function() {
						this.$router.push('/seven/auth/admin/users')
					},
					notEmpty: function(value) {
						return notEmpty(value)
					},
					same: function(value, value2) {
						return same(value, value2)
					},
					isValidEmail: function(value) {
						return isValidEmail(value)
					}
				}
			}
        })
	})
	
	const CreateGroup = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/admin/create-group.html').then(function(html) {
			return {
				template: html,
				data: function() {
					return {
						group: { id: null, name: null, type: null }
					}
				},
				methods: {
					onSubmit: function() {
						AdminService.createGroup(this.group).then(() => {
							this.$refs.groupCreated.show(1)
							setTimeout(() => {
								this.$router.push('/seven/auth/admin/groups')			
							}, 1000)
						})
					},
					onReset: function() {
						this.$router.push('/seven/auth/admin/groups')
					},
					notEmpty: function(value) {
						return notEmpty(value)
					},
					isValidId: function(value) {
						return isValidId(value)
					}					
				}
			}
        })
	})
	
	const ProfileUser = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/admin/profile-user.html').then(function(html) {
			return {
				template: html,
                props: { 
					editMode: {
			      		type: Boolean,
			      		default: false
				    },
				},	
				data: function () {
					return {
						leftOpen: true,
						user: { id: null, firstName: null,  lastName: null, email: null },
						dirty: false,
						credentials: { authenticatedUserPassword: null, password: null },
						passwordRepeat: null,
						groups: null,
						unAssignedGroups: [],
						selectedGroup: null,
						focusedGroup: null,
						passwordPolicyError: false,
						passwordVisibility: { current: false, new: false, repeat: false },
						sendingEmail: false
					}
				},
				watch: {
					'$route.params.userId': function() {
						if (!this.editMode && this.$route.params.userId !== this.$router.app.user.id) {							
							this.$router.push('/seven/auth/start')
						} else {
							this.loadUser(this.$route.params.userId)
							this.clean()
						}
					},
					'$route.query.tab': function() {
						if (this.$route.query.tab === 'groups') this.loadGroups(this.$route.params.userId) 
					}
				},
				created: function () {
					if (this.$route.params.userId) this.loadUser(this.$route.params.userId)
					if (this.$route.query.tab === 'groups') this.loadGroups(this.$route.params.userId) 
				},
				computed: {
					readOnlyUser: function() {
						return (this.$root.config.userProvider !== 'de.cib.cibflow.auth.CamundaUserProvider')
					},
					tasksCheckNotificationsDisabled: {
						get: function() {
							return localStorage.getItem('tasksCheckNotificationsDisabled') === 'true' || false
						},
						set: function(val) {
							!localStorage.setItem('tasksCheckNotificationsDisabled', val)
						}
					}
				},
				methods: {
					loadUser: function (userId) {
						AdminService.findUsers({ id: userId }).then(response => {
							this.user = response[0]
						})
					},
					loadGroups: function (userId) {
						AdminService.findGroups({ member: userId }).then(response => {
							this.groups = response
						})
					},
					update: function() {
						AdminService.updateUserProfile(this.user.id, this.user).then(() => {
							this.$refs.updateProfile.show(2)
						})
					},
					notEmpty: function(value) {
						return notEmpty(value)
					},
					same: function(value, value2) {
						return same(value, value2)
					},
					changePassword: function (evt) {
						evt.preventDefault()
						if (same(this.credentials.password, this.passwordRepeat) && notEmpty(this.credentials.authenticatedUserPassword)) {
							AdminService.updateUserCredentials(this.user.id, this.credentials.password, 
								this.credentials.authenticatedUserPassword).then(() => {
								this.passwordPolicyError = false
								this.$refs.updatePassword.show(2)
							}, error => {
								var data = error.response.data
								if (data && data.type === 'PasswordPolicyException') {
									this.passwordPolicyError = true
								}
							})	
						}											
					},
					deleteUser: function() {
						AdminService.deleteUser(this.user.id).then(() => {
							this.$refs.deleteUser.show(2)
							this.$router.push('/seven/auth/admin/users')
						})
					},
					unassignGroup: function(group) {
						this.selectedGroup = group
						AdminService.deleteMember(group.id, this.$route.params.userId).then(() => {
							this.$refs.unassignGroup.show(2)
							this.loadGroups(this.$route.params.userId)
						})
					},
					loadUnassignedGroups: function() {
						var userGroups = JSON.parse(JSON.stringify(this.groups))
						this.unAssignedGroups = []						
						AdminService.findGroups().then(allGroups => {	
							allGroups.forEach(group => {								
								var isAssigned = false		
								userGroups.forEach(userGroup => {
									if (group.id === userGroup.id) isAssigned = true 
								})	
								if (!isAssigned){
									group.selected = false
									this.unAssignedGroups.push(group)
								}  
							})
						})
					},
					openAssignGroupModal: function() {
						this.loadUnassignedGroups()
						this.$refs.assignGroupsModal.show()
					},	
					assignGroups: function () {
						this.unAssignedGroups.forEach(unAssignedGroup => {
							if (unAssignedGroup.selected) {
								AdminService.addMember(unAssignedGroup.id, this.user.id).then(() => {									
									this.groups.push(unAssignedGroup)
								})
							}							
						})
					},
					clean: function() {
						this.dirty = false
						this.credentials = { authenticatedUserPassword: null, password: null }
						this.passwordRepeat = null
						this.groups = null						
					},
					onSendEmail: function() {
						this.sendingEmail = true
						AuthService.passwordRecover({ id: this.user.id }).then(() => {
							this.sendingEmail = false
							this.$refs.emailSent.show()
						}, () => {
							this.sendingEmail = false
						})
					}
				}
			}
		})		
	})
	
	const ProfileGroup = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/admin/profile-group.html').then(function(html) {
			return {
				template: html,
				data: function() {
					return {
						leftOpen: true,
						group: { id: null, name: null,  type: null },
						dirty: false,
						users: null,
						selectedUser: null,
						focusedUser: null,
						perPage: 15,
						page: 1
					}
				},
				watch: {
					'$route.params.groupId': function() {
						this.loadGroup(this.$route.params.groupId)
						this.clean()
					},
					'$route.query.tab': function() {
						if (this.$route.query.tab === 'users') this.loadUsers(this.$route.params.groupId) 
					}
				},
				created: function() {
					if (this.$route.params.groupId) this.loadGroup(this.$route.params.groupId)
					if (this.$route.query.tab === 'users') this.loadUsers(this.$route.params.groupId) 
				},
				methods: {
					loadGroup: function(groupId) {
						AdminService.findGroups({ id: groupId }).then(response => {
							this.group = response[0]
						})
					},
					loadUsers: function(groupId) {
						AdminService.findUsers({ memberOfGroup: groupId }).then(response => {
							this.users = response
						})
					},
					update: function() {
						AdminService.updateGroup(this.group.id, this.group).then(() => {
							this.$refs.updateGroup.show(2)
						})
					},
					notEmpty: function(value) {
						return notEmpty(value)
					},
					deleteGroup: function() {
						AdminService.deleteGroup(this.group.id).then(() => {
							this.$refs.deleteGroup.show(2)
							this.$router.push('/seven/auth/admin/groups')
						})
					},
					clean: function () {
						this.dirty = false
						this.users = null						
					}
				}
			}
		})		
	})
	
	export { AdminUsers, AdminGroups, AdminAuthorizations, AdminAuthorizationsTable, 
		AuthorizationsNavBar, CreateUser, CreateGroup, ProfileUser, ProfileGroup }