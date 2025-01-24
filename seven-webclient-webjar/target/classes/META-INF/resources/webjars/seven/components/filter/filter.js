/* globals localStorage, permissionsMixin, console, window, setInterval, clearInterval, TaskPool */
	
	import { TaskPool } from "../../taskpool.js"
	import { permissionsMixin } from '../../permissions.js';
	import { TaskService, AdminService } from '../../services.js';

	var candidateOptions = ['candidateGroup', 'candidateGroupExpression', 
		'candidateGroups', 'candidateGroupsExpression', 'candidateUser', 'candidateUserExpression']
		
	const MIN_TASKNUMBER_INTERVAL = 10000
	
	const CamundaFilter = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/filter/camunda-filter.html').then(function(html) {
			return { 
				template: html,
				mixins: [permissionsMixin],
				props: { tasks: Array, processes: Array, layout: Boolean },
				data: function() {
					return {
						selectedFilterId: null,
						selectedFilterName: ''						
					}
				},
				computed: {
					filtersSorted: function() {
						return this.$store.state.filter.list.sort((objA, objB) => {
							return objA.properties.priority < objB.properties.priority ? -1 : 
								objA.properties.priority > objB.properties.priority ? 1 : 0
						})
					}
				},
				created: function() {
					this.$store.dispatch('findFilters').then(response => {
						this.$store.commit('setFilters', 
							{ filters: this.filtersByPermissions(this.$root.config.permissions.displayFilter, response) })
						this.$emit('filters-loaded', null)
						this.loadFilter()
					})
				},
				methods: { // TODO: Refactor, many methods and unnecessary structures
					loadFilter: function() {
						var filterID = this.$route.query.externalMode !== undefined ? this.$store.state.filter.list[0].id :
							localStorage.getItem('filter') || this.$store.state.filter.list[0].id
						if (this.$store.state.filter.list.length > 0) {
							// TODO: I would like to move this to filter-nav-bar component and not here.
							if (this.$route.query.filtername !== undefined) {
								var selectedFilter = this.$store.state.filter.list.find(filter => {
									if (filter.name === this.$route.query.filtername) return filter
								})
								if (selectedFilter !== undefined) {
									this.selectFilter(selectedFilter.id)
									delete this.$route.query.filtername
									var query = '?'
									Object.keys(this.$route.query).forEach(key => {
										query += key + '=' + this.$route.query[key]
									})							
									this.$router.replace('/flow/auth/tasks/' + selectedFilter.id + '/' + (this.$route.params.taskId || '') + query)
								}	
							} else {
								this.selectFilter(filterID)								
							}
						}
					},
					showFilterDialog: function(mode) {
						this.$refs.filterModal.showFilterDialog(mode)
					},
					selectFilter: function(value) {
						var selectedFilter = this.$store.state.filter.list.find(filter => {
							if (filter.id === value) return filter
						})
						this.$store.state.filter.selected = selectedFilter || this.$store.state.filter.list[0]
						this.selectedFilterId = this.$store.state.filter.selected.id
						this.selectedFilterName = this.$store.state.filter.selected.name
						this.$emit('set-filter', this.selectedFilterId)
						localStorage.setItem('filter', this.selectedFilterId)
					},
					deleteFilter: function() {
						this.$store.dispatch('deleteFilter', { filterId: this.$store.state.filter.selected.id }).then(() => {
							this.$emit('filter-alert', { message: 'msgFilterDeleted', filter: this.selectedFilterName })
							this.$store.state.filter.selected = null
							this.$emit('deleted-filter')
							if (this.$store.state.filter.list.length > 0) this.$emit('set-filter', this.$store.state.filter.list[0].id)
						})
					}			
				}
			}
		})
	})

	const FilterModal = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/filter/filter-modal.html').then(function(html) {
			return { 
				template: html,
				props: { tasks: Array, processes: Array, layout2: Boolean },
				mixins: [permissionsMixin],
				data: function () {
					return {
						mode: 'create',
						matchAllCriteria: true,
						filters: [],
						criterias: [],
						criteriasGrouped: [],
						selectedFilterId: null,
						selectedFilterName: '',
						selectedFilterPriority: 0,
						selectedCriteriaKey: null,
						selectedCriteriaValue: null,
						selectedCriteriaType: null,
						selectedCriteriaVariable: [{ name: '', operator: 'eq', value: '' }],
						criteriasToAdd: [],
						includeAssigned: false,
						isEditing: false,
						criteriaEdited: { key: null, rowIndex: null},
					}
				},
				watch: {
					'$store.state.filter.selected': function() {
						this.selectedFilterId = this.$store.state.filter.selected.id
						this.selectedFilterName = this.$store.state.filter.selected.name
						this.selectedFilterPriority = this.$store.state.filter.selected.properties.priority
					},
					selectedCriteriaKey: function(newValue) {
						this.selectCriteria(newValue)
					}
				},
				computed: {
					existFilter: function() {
						var checkNotSelected = this.mode === 'edit' ? this.$store.state.filter.selected.name !== this.selectedFilterName : true
						var checkExists = this.$store.state.filter.list.find(row => { return row.name === this.selectedFilterName })
						return checkExists && checkNotSelected
					},
					checkValidity: function() {
						var invalidFilter = this.criteriasToAdd.find(row => { return row.key === null })
						return !!this.existFilter || !!invalidFilter || this.selectedFilterName.length === 0
					},
					variableOperators: function() {
						return  [
							{ value: 'eq', text: this.$t('nav-bar.filters.operators.txteq') },
							{ value: 'neq', text: this.$t('nav-bar.filters.operators.txtneq') },
							{ value: 'gt', text: this.$t('nav-bar.filters.operators.txtgt') },
							{ value: 'gteq', text: this.$t('nav-bar.filters.operators.txtgteq') },
							{ value: 'lt', text: this.$t('nav-bar.filters.operators.txtlt') },
							{ value: 'lteq', text: this.$t('nav-bar.filters.operators.txtlteq') }
						]
					},
					existCandidateSelected: function() {
						return this.criteriasToAdd.some(criteria => {
							return candidateOptions.includes(criteria.key)
						})
					}
				},
				created: function() {
					this.selectFilter(this.$store.state.filter.selected.id)
				},
				methods: { // TODO: Refactor, many methods and unnecessary structur,,
					selectFilter: function(value) {
						var selectedFilter = this.$store.state.filter.list.find(filter => {
							return filter.id === value
						})
						if (selectedFilter) {							
							this.selectedFilterId = selectedFilter.id
							this.selectedFilterName = selectedFilter.name
							this.selectedFilterPriority = selectedFilter.properties.priority
						}
					},
					createFilter: function() {
						var query = {}
						if (this.matchAllCriteria) {
							this.criteriasToAdd.forEach(criteria => {
								query[criteria.key] = criteria.value
							})
							if (this.existCandidateSelected) query.includeAssignedTasks = this.includeAssigned
						} else {
							query.orQueries = []
							query.orQueries.push({})
							this.criteriasToAdd.forEach(criteria => {
								query.orQueries[0][criteria.key] = criteria.value
							})
							if (this.existCandidateSelected) query.orQueries[0].includeAssignedTasks = this.includeAssigned
						}
						if (this.mode === 'edit') {
							this.$store.state.filter.selected.name = this.selectedFilterName
							this.$store.state.filter.selected.properties.priority = this.selectedFilterPriority || 0
							this.$store.state.filter.selected.query = query
							this.$store.dispatch('updateFilter', { filter: this.$store.state.filter.selected }).then(() => {
								this.$emit('filter-alert', { message: 'msgFilterUpdated', filter: this.selectedFilterName })
								this.$refs.filterHandler.hide()
								this.$emit('set-filter', this.$store.state.filter.selected.id)
								this.selectedFilterId = this.$store.state.filter.selected.id
								this.$emit('filter-updated', this.selectedFilter)
							}, () => {
								this.$root.$refs.error.show({ type: 'filterSaveError' })
							})
						} else {
							var filterCreate = { 
								id: null,
								resourceType: 'Task', 
								name: this.selectedFilterName,
								owner: null, 
								query: query, 
						        properties: {
						            color: '#555555',
						            showUndefinedVariable: false,
						            description: '',
						            refresh: true,
						            priority: this.selectedFilterPriority || 0
						        }
							}
							this.$store.dispatch('createFilter', { filter: filterCreate }).then(filter => {
								this.$emit('filter-alert', { message: 'msgFilterCreated', filter: this.selectedFilterName })
								this.$refs.filterHandler.hide()
								this.$emit('set-filter', filter.id)
								this.$emit('select-filter', filter)
								this.$store.state.filter.selected = filter
								this.selectedFilterId = filter.id
								localStorage.setItem('filter', JSON.stringify(this.$store.state.filter.selected))
							}, () => {
								this.$root.$refs.error.show({ type: 'filterSaveError' })
							})
						}
					},
					addProcessVariable: function() {
						this.selectedCriteriaVariable.push({ name: '', operator: 'eq', value: '' })
					},
					deleteProcessVariable: function(index) {
						if (this.selectedCriteriaVariable.length > index)
						this.selectedCriteriaVariable.splice(index, 1)
					},
					rowClass: function(item) {
						let stylesForRow = ['row']
						if (item.key === this.criteriaEdited.key ) stylesForRow.push('table-active')  
						return stylesForRow
					},
					addCriteria: function() {
						var valueToAdd = []
						if (this.selectedCriteriaType === 'variable') {
							valueToAdd = this.selectedCriteriaVariable
						} else if (this.selectedCriteriaType === 'array') {
							this.selectedCriteriaValue.split(',').forEach(value => {
								valueToAdd.push(value.trim())
							})
						} else valueToAdd = this.selectedCriteriaValue
						this.criteriasToAdd.push({
							key: this.selectedCriteriaKey,
							name: this.$t('nav-bar.filters.keys.' + this.selectedCriteriaKey), 
							value: valueToAdd, type: this.selectedCriteriaType
						})
						this.selectedCriteriaKey = null
						this.selectedCriteriaValue = null
						this.selectedCriteriaType = null
						this.selectedCriteriaVariable = [{ name: '', operator: 'eq', value: '' }]
					},
					updateCriteria: function() {
						var valueToAdd = []
						if (this.selectedCriteriaType === 'variable') {
							valueToAdd = this.selectedCriteriaVariable
						} else if (this.selectedCriteriaType === 'array') {
							this.selectedCriteriaValue.split(',').forEach(value => {
							valueToAdd.push(value.trim())
							})
						} else valueToAdd = this.selectedCriteriaValue											

							this.criteriasToAdd.splice(this.criteriaEdited.rowIndex, 1, {
							  key: this.selectedCriteriaKey,
							  name: this.$t('nav-bar.filters.keys.' + this.selectedCriteriaKey),
							  value: valueToAdd,
							  type: this.selectedCriteriaType
							})
							this.selectedCriteriaKey = null
							this.selectedCriteriaValue = null
							this.selectedCriteriaType = null
							this.selectedCriteriaVariable = [{ name: '', operator: 'eq', value: '' }]
							
							this.cancelEditCriteria()
					},
					deleteCriteria: function(index) {
						this.criteriasToAdd.splice(index, 1)
					},
					editCriteria: function(index) {
						this.isEditing = true
						// so the row of the table doesnt change too when the inputs are modified
						let criteriaToEdit = JSON.parse(JSON.stringify(this.criteriasToAdd[index])) 					
						this.criteriaEdited = { key: criteriaToEdit.key, rowIndex: index }
						this.selectedCriteriaKey = criteriaToEdit.key
						this.selectCriteria(this.selectedCriteriaKey)
						if(this.selectedCriteriaType === 'array' && criteriaToEdit.value.length >0) {
							this.selectedCriteriaValue = criteriaToEdit.value[0]
						}
						else if ( this.selectedCriteriaType === 'variable' || Array.isArray(criteriaToEdit.value) && criteriaToEdit.value.length > 0) {
							this.selectedCriteriaVariable = criteriaToEdit.value
							this.selectedCriteriaValue = criteriaToEdit.value[0].value
						} else {
						   this.selectedCriteriaValue = criteriaToEdit.value
						}
					},
					cancelEditCriteria: function() {
						this.isEditing = false
						this.selectedCriteriaKey = null
						this.selectedCriteriaValue = null
						this.selectedCriteriaType = null
						this.criteriaEdited = { key: null, rowIndex: null }
					},
					selectCriteria: function(evt) {
						var criteria = this.criterias.find(option => {
							return option.value === evt
						})
						if (criteria) this.selectedCriteriaType = criteria.type
					},
					showFilterDialog: function(mode) {
						this.mode = mode
						this.criterias = []
						this.criteriasGrouped = []
						this.criteriasToAdd = []
						this.selectedFilterName = ''
						this.selectedFilterPriority = 0
						this.selectedCriteriaKey = null
						this.selectedCriteriaValue = null
						this.selectedCriteriaType = null
						this.includeAssigned = false
						this.selectedCriteriaVariable = [{ name: '', operator: 'eq', value: '' }]
						this.isEditing = false,
						this.criteriaEdited = { key: null, rowIndex: null}

						// Prepared criterias
						var auxCriterias = {}
						this.$root.config.filters.forEach(filter => {
							if (filter.group) {
								if (auxCriterias[filter.group] === undefined) {
									auxCriterias[filter.group] = { "label": this.$t('nav-bar.filters.keys.' + filter.group), "options" : [] }
								} 
								auxCriterias[filter.group].options.push(
									{ value: filter.key, text: this.$t('nav-bar.filters.keys.' + filter.key), type: filter.type })
							} else {
								this.criteriasGrouped.push({ value: filter.key, text: this.$t('nav-bar.filters.keys.' + filter.key), type: filter.type })
							}
							this.criterias.push({ value: filter.key, text: this.$t('nav-bar.filters.keys.' + filter.key), type: filter.type })
						})
						
						Object.keys(auxCriterias).forEach(group => {
							this.criteriasGrouped.push(auxCriterias[group])
						})
						
						if(this.criteriasGrouped.length <= 0) this.criteriasGrouped = this.criterias
						
						if (this.mode === 'edit') {
							this.selectedFilterId = this.$store.state.filter.selected.id
							this.selectedFilterName = this.$store.state.filter.selected.name
							this.selectedFilterPriority = this.$store.state.filter.selected.properties.priority
							
							// Not matched all criterias.
							if (!this.$store.state.filter.selected.query) return 
							if (this.$store.state.filter.selected.query.orQueries && this.$store.state.filter.selected.query.orQueries.length > 0) {
								this.matchAllCriteria = false
								Object.keys(this.$store.state.filter.selected.query.orQueries[0]).forEach(key => {
									var filterVal = this.$store.state.filter.selected.query.orQueries[0][key]
									if (key === 'includeAssignedTasks') this.includeAssigned = filterVal
									if (!filterVal || (filterVal && filterVal.length === 0)) return
									var index = this.criterias.findIndex(item => {
										return item.value === key
									})
									if (index > -1) {										
										this.criteriasToAdd.push({
											key: key, 
											name: this.$t('nav-bar.filters.keys.' + key), 
											value: filterVal
										})
									}
								})
							} else {
								//Match all criterias.
								Object.keys(this.$store.state.filter.selected.query).forEach(key => {
									var filterVal = this.$store.state.filter.selected.query[key]
									if (key === 'includeAssignedTasks') this.includeAssigned = filterVal
									if (!filterVal || (filterVal && filterVal.length === 0)) return
									var index = this.criterias.findIndex(item => {
										return item.value === key
									})
									if (index > -1) {										
										this.criteriasToAdd.push({
											key: key, 
											name: this.$t('nav-bar.filters.keys.' + key), 
											value: filterVal
										})
									}
								})
							}
						}
						this.$refs.filterHandler.show()
					},
					formatCriteria: function(value) {
						return Array.isArray(value) ? value.join(', ') : value
					}
				}
			}
		})
	})

	const FilterNavBar = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/filter/filter-nav-bar.html').then(function(html) {
			return { 
				template: html,
				mixins: [permissionsMixin],
				data: function () {
					return { 
						filter: '',
						currentSorting: {},
						showFilters: true,
						selected: null,
						focused: null,
						sortOrder: '',
						interval: null,
						taskpool: new TaskPool(5)
					}
				},
				watch: {
					'$route.params.filterId': function(to) {
						if (this.$route.query.filtername) {
							this.setFilterByName()
						} else this.checkFilterIdInUrl(to)
					},
					'$store.state.filter.settings': {
						deep: true,
						handler: function() {
							localStorage.setItem('addFilterSettings', JSON.stringify(this.$store.state.filter.settings))
						}
					}			
				},
				computed: {
					filtersFiltered: function() {
						var filters = []
						if (this.$store.state.filter.list) {					
							filters = this.$store.state.filter.list.filter(filter => {
								return (filter.name) ? filter.name.toUpperCase().includes(this.filter.toUpperCase()) : false 
							})
						}
						this.$emit('n-filters-shown', filters.length)
						return filters.sort((a, b) => {
							if (!this.$root.config.filtersSearch || !this.sortOrder) {
								return a.properties.priority - b.properties.priority
							}
							const nameA = a.name.toLowerCase()
    						const nameB = b.name.toLowerCase()
					        let comparison = nameA.localeCompare(nameB)
							if (comparison === 0) {
							    comparison = a.properties.priority - b.properties.priority
							}
							return this.sortOrder === 'desc' ? comparison : -comparison
						})
					}		
				},
				mounted: function() {
					this.fetchFilters()
				},
				methods: {
					fetchFilters: function() {
							this.$refs.filterLoader.done = false
							this.$store.dispatch('findFilters').then(response => {
							this.$store.commit('setFilters', 
								{ filters: this.filtersByPermissions(this.$root.config.permissions.displayFilter, response) })
							if (this.$root.config.taskFilter.tasksNumber.enabled) {
								const interval = this.$root.config.taskFilter.tasksNumber.interval < MIN_TASKNUMBER_INTERVAL ? 
									MIN_TASKNUMBER_INTERVAL : this.$root.config.taskFilter.tasksNumber.interval
								this.setTasksNumber()
								this.interval = setInterval(() => { this.setTasksNumber() }, interval)
							}
							if (this.$route.query.filtername) {
								this.setFilterByName()
							} else this.checkFilterIdInUrl(this.$route.params.filterId)
							this.$refs.filterLoader.done = true
						})
					},
					setTasksNumber: function() {
						this.$store.state.filter.list.forEach(f => {
							this.taskpool.add(TaskService.findTasksCountByFilter, [f.id, {}]).then(tasksNumber => {
								f.tasksNumber = tasksNumber
							})
						})
					},
					setFilterByName: function() {
						var selectedFilter = this.$store.state.filter.list.find(filter => {
							return filter.name === this.$route.query.filtername
						})
						if (selectedFilter) {
							this.selectFilter(selectedFilter)
						} else this.checkFilterIdInUrl(this.$route.params.filterId)
					},
					checkFilterIdInUrl: function(filterId) {
						if (this.$store.state.filter.list.length > 0 && filterId) {
							var filterStore = this.$store.state.filter.list.find(filter => {
								return filter.id === filterId
							})
							if (filterStore) this.selectFilter(filterStore)
							else this.selectFilter({})
						} else this.selectFilter({})
					},
					selectFilter: function(filter) {
						if (this.$store.state.filter.list.length > 0) {
							var taskId = this.$route.query.filtername ? this.$route.params.filterId : this.$route.params.taskId
							var selectedFilter = this.$store.state.filter.list.find(f => {
								return f.id === filter.id
							})
							try {
								//Use of '*' as special character when we don't specify the filter on a link
								selectedFilter = (!this.$route.params.filterId || this.$route.params.filterId === '*') && localStorage.getItem('filter') ? 
									JSON.parse(localStorage.getItem('filter')) : selectedFilter
							} catch(error) {
								console.error('Filter format wrong: corrected')
							}
							if ((!this.$route.params.filterId || selectedFilter) || !selectedFilter) {
								this.$store.state.filter.selected = selectedFilter || this.$store.state.filter.list[0]
								this.$emit('selected-filter', this.$store.state.filter.selected.id)
								localStorage.setItem('filter', JSON.stringify(this.$store.state.filter.selected))
								if (this.$route.params.filterId === '*') return this.handleTaskLink(taskId)
								var path = '/flow/auth/tasks/' + this.$store.state.filter.selected.id + 
									(taskId ? '/' + taskId : '')
								if (this.$route.path !== path) this.$router.replace(path)
							}
						}
					},
					getClasses: function(filter) {
						var classes = []
						if (filter !== this.focused) classes.push('invisible')
						return classes
					},
					deleteFilter: function() {
						this.$store.dispatch('deleteFilter', { filterId: this.$store.state.filter.selected.id }).then(() => {
							this.$emit('filter-alert', { message: 'msgFilterDeleted', filter: this.$store.state.filter.selected.name })
							localStorage.removeItem('filter')
							if (this.$store.state.filter.list[0]) this.selectFilter(this.$store.state.filter.list[0])
							else this.$router.push('/flow/auth/tasks')
						})
					},
					showFilterDialog: function(mode) {
						this.$refs.filterModal.showFilterDialog(mode)
					},
					setFavoriteFilter: function(filter) {
						this.$store.dispatch('addFavoriteFilter', { filterId: filter.id })
					},
					deleteFavoriteFilter: function(filter) {
						this.$store.dispatch('deleteFavoriteFilter', { filterId: filter.id })
					},
					handleTaskLink: function(taskId) {
						TaskService.findTaskById(taskId).then(task => {
							if (task.assignee && (task.assignee.toLowerCase() === this.$root.user.userID.toLowerCase())) 
								return this.$emit('selected-task', task)
							else {
								TaskService.findIdentityLinks(taskId).then(identityLinks => {
									var userIdLink = identityLinks.find(i => {
										return i.type === 'candidate' && i.userId && i.userId.toLowerCase() === this.$root.user.userID.toLowerCase()
									})
									if (userIdLink) return this.$emit('selected-task', task)
									this.manageCandidateGroups(identityLinks, task)
								})
							}
						})
					},
					manageCandidateGroups: function(identityLinks, task) {
						var promises = []
						for (var i in identityLinks) {
							if (identityLinks[i].type === 'candidate' && identityLinks[i].groupId) {
								var promise = AdminService.findUsers({ memberOfGroup: identityLinks[i].groupId }).then(users => {
									return users.some(u => {
										return u.id.toLowerCase() === this.$root.user.userID.toLowerCase()
									})
								})
								promises.push(promise)
							}
						}
						Promise.all(promises).then(results => {
							if (results.some(r => { return r })) this.$emit('selected-task', task)
							else {
								this.$root.$refs.error.show({ type: 'AccessDeniedException', params: [task.id] })
								this.$router.push('/flow/auth/tasks/' + this.$store.state.filter.selected.id)
							}
						})
					}
				},
				beforeDestroy: function() {
					clearInterval(this.interval)
					this.taskpool.clear()
				}
			}
		})
	})
	
	const FilterNavCollapsed = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/filter/filter-nav-collapsed.html').then(function(html) {
			return { 
				template: html,
				props: { leftOpen: Boolean },
				data: function() {
					return {
						sizes: { filter: 140, arrow: 40, header: 55, dots: 40 }
					}
				},
				watch: {
					favoriteFilters: function() {
						this.setDisplayFavorites()
					}
				},
				computed: {
					favoriteFilters: function() {
						var filters = this.$store.state.filter.list.filter(f => {
							return f.favorite
						})
						return filters.sort(function(a, b) {
							return a.properties.priority - b.properties.priority
						})
					},
					favoritesDisplayed: function() {
						return this.favoriteFilters.filter(filter => {
							return filter.display
						})
					},
					favoritesNoDisplayed: function() {
						return this.favoriteFilters.filter(filter => {
							return !filter.display
						})
					}
				},
				mounted: function() {
					this.setDisplayFavorites()
					window.addEventListener('resize', this.setDisplayFavorites)
				},
				methods: {
					setDisplayFavorites: function() {
						this.favoriteFilters.forEach((filter, key) => {
							if ((key * this.sizes.filter + this.sizes.arrow + this.sizes.header + 
								this.sizes.filter + this.sizes.dots) < window.innerHeight) {
									filter.display = true
							} else filter.display = false
						})
					},
					getStyles: function(filter, key) {
						var styles = { top: key * this.sizes.filter + this.sizes.arrow + 'px', width: this.sizes.filter + 'px' }
						if (this.$store.state.filter.selected.id === filter.id) {
							styles['border-top'] = '5px solid!important'
							styles['border-top-color'] = 'var(--primary)!important'
						}
						return styles
					},
					getDotsStyle: function() {
						var styles = { width: this.sizes.dots + 'px' }
						styles.top = this.favoritesDisplayed.length * this.sizes.filter + this.sizes.arrow + 'px'
						return styles
					},
					selectFilter: function(filter) {
						var selectedFilter = this.$store.state.filter.list.find(f => {
							return f.id === filter.id
						})
						if (selectedFilter) {
							this.$store.state.filter.selected = selectedFilter
							this.$emit('selected-filter', selectedFilter.id)
							localStorage.setItem('filter', JSON.stringify(selectedFilter))
							var path = '/flow/auth/tasks/' + selectedFilter.id + 
								(this.$route.params.taskId ? '/' + this.$route.params.taskId : '')
							if (this.$route.path !== path) this.$router.replace(path)
						}
					}
				},
				beforeDestroy: function() {
			        window.removeEventListener('resize', this.setDisplayFavorites)
			    }
			}
		})
	})
	
	export { CamundaFilter, FilterModal, FilterNavBar, FilterNavCollapsed }