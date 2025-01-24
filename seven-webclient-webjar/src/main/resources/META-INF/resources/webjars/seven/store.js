/* globals localStorage */

	function buildProcessStore(ProcessService) {
		return {
			state: { list: [] },
			mutations: {
				setProcesses: function (state, param) {
					state.list = param.processes
				},
				setFavorite: function (state, params) {
					params.process.favorite = params.value
				},
				setStatistics: function (state, params) {
					params.process.statistics = params.statistics
				},
				setSuspended: function (state, params) {
					params.process.suspended = params.suspended
				}
			},
			actions: {
				findProcesses: function () {
					return ProcessService.findProcesses()
				},
				findProcessesWithInfo: function () {
					return ProcessService.findProcessesWithInfo()
				},
				getProcessByDefinitionKey: function (ctx, params) {
					var process = ctx.state.list.find(process => { return process.key === params.key })
					if (process) return Promise.resolve(process)	
					else return ProcessService.findProcessByDefinitionKey(params.key)
				},
				getProcessById: function (ctx, params) {
					var process = ctx.state.list.find(process => { return process.id === params.id })
					if (process) return Promise.resolve(process)	
					else return ProcessService.findProcessById(params.id)
				},
				setFavorites: function (ctx, params) {
					ctx.state.list.forEach(process => {
						if (params.favorites.indexOf(process.key) !== -1) ctx.commit('setFavorite', { process: process, value: true }) 
						else ctx.commit('setFavorite', { process: process, value: false }) 
					})
				},
				setStatistics: function(ctx, params) {
					ctx.commit('setStatistics', { process: params.process, statistics: params.statistics }) 
				},
				setSuspended: function (ctx, params) {
					ctx.commit('setSuspended', { process: params.process, suspended: params.suspended })
				}
			}	
		}
	}
	
	function buildFilterStore(FilterService) {
		if (localStorage.getItem('filterSettings')) localStorage.removeItem('filterSettings')
		var settings = JSON.parse(localStorage.getItem('addFilterSettings') || JSON.stringify({
			reminder: false,
			dueDate: false
		}))
		if (typeof settings.reminder !== 'boolean') {
			settings.reminder = false
			settings.dueDate = false
			localStorage.setItem('filterSettings', JSON.stringify(settings))
		}
		return {
			state: { 
				list: [],
				selected: { 
					id: null,
					resourceType: 'Task', 
					name: '',
					owner: null, 
					query: {}, 
			        properties: {
			            color: '#555555',
			            showUndefinedVariable: false,
			            description: '',
			            refresh: true,
			            priority: 50
			        }
				},
				settings: settings
			},
			mutations: {
				setFilters: function(state, params) {
					var favorites = localStorage.getItem('favoriteFilters') ? 
						JSON.parse(localStorage.getItem('favoriteFilters')) : []
					params.filters.forEach(f => {
						if (favorites.includes(f.id)) f.favorite = true
						else f.favorite = false
					})
					favorites = favorites.filter(fid => {
						return params.filters.find(fl => {
							return fl.id === fid
						})
					})
					localStorage.setItem('favoriteFilters', JSON.stringify(favorites))
					state.list = params.filters
				},
				updateFilter: function(state, params) {
					state.list[params.index] = params.filter
				},
				addFilter: function(state, params) {
					state.list.push(params.filter)
				},
				deleteFilter: function(state, params) {
					state.list = state.list.filter(filter => {
						return filter.id !== params.filterId
					})
				},
				deleteFavoriteFilter: function(state, params) {
					var indx = state.list.findIndex(f => {
						return f.id === params.filterId
					})
					state.list[indx].favorite = false
					var favorites = localStorage.getItem('favoriteFilters')
					if (favorites) {
						favorites = JSON.parse(favorites)
						favorites = favorites.filter(f => {
							return f !== params.filterId
						})
						localStorage.setItem('favoriteFilters', JSON.stringify(favorites))
					} 
				},
				addFavoriteFilter: function(state, params) {
					var indx = state.list.findIndex(f => {
						return f.id === params.filterId
					})
					state.list[indx].favorite = true
					var favorites = localStorage.getItem('favoriteFilters') ? 
						JSON.parse(localStorage.getItem('favoriteFilters')) : []
					favorites.push(params.filterId)
					localStorage.setItem('favoriteFilters', JSON.stringify(favorites))
				}
			},
			actions: {
				findFilters: function() {
					return FilterService.findFilters()
				},
				updateFilter: function(ctx, params) {
					var selectedFilterIndx = ctx.state.list.findIndex(filter => {
						return filter.id === params.filter.id
					})
					if (selectedFilterIndx > -1) {
						return FilterService.updateFilter(params.filter).then(() => {
							ctx.commit('updateFilter', { index: selectedFilterIndx, filter: params.filter })
							return Promise.resolve()
						})
					} else return Promise.reject()
				},
				createFilter: function(ctx, params) {
					return FilterService.createFilter(params.filter).then(filter => {
						ctx.commit('addFilter', { filter: filter })
						return Promise.resolve(filter)
					})
				},
				deleteFilter: function(ctx, params) {
					return FilterService.deleteFilter(params.filterId).then(() => {
						ctx.commit('deleteFilter', { filterId: params.filterId })
					})
				},
				addFavoriteFilter: function(ctx, params) {
					ctx.commit('addFavoriteFilter', { filterId: params.filterId })
				},
				deleteFavoriteFilter: function(ctx, params) {
					ctx.commit('deleteFavoriteFilter', { filterId: params.filterId })
				}
			}	
		}
	}
	
	function buildAdvancedSearchStore() {
		return {
			state: { 
				matchAllCriteria: true,
				criterias: []
			},
			mutations: {				
		    	initializeAdvancedSearch: function(state, params) {
		      		state.matchAllCriteria = params ? params.matchAllCriteria : true
		      		state.criterias = params ? params.criterias : []
			    },
		    	setAdvancedSearch: function(state, params) {
		     		state.matchAllCriteria = params.matchAllCriteria
		      		state.criterias = params.criterias
		      		if (state.criterias.length > 0) {
				      	localStorage.setItem('_advancedSearch', JSON.stringify({
				        	matchAllCriteria: state.matchAllCriteria,
				        	criterias: state.criterias
				      	}))
			      	} else localStorage.removeItem('_advancedSearch')
		    	}
		  	},
		  	actions: {
		    	updateAdvancedSearch: function(ctx, params) {
		      		ctx.commit('setAdvancedSearch', params)
		    	},
		    	loadAdvancedSearchData: function(ctx) {		
		      		var storedData = localStorage.getItem('_advancedSearch')
			        var parsedData = JSON.parse(storedData)
			        ctx.commit('initializeAdvancedSearch', parsedData)
			    }
		  	},
			getters: {
		    	formatedCriteriaData(state) {
		       		var result = {}
			      	state.criterias.forEach(criteria => {
				        var { key, name, operator, value } = criteria
				        if (!result[key]) result[key] = []
			        	result[key].push({ name, operator, value })
			      	})	
		      		return result
			    }
		  	}
		}
	}
	
	function buildUserStore(AdminService) {
		return {
			state: {
				listCandidates: [],
				searchUsers: []
			},
			mutations: {
				setCandidateUsers: function(state, users) {
					state.listCandidates = users
				},
				concatCandidateUsers: function(state, users) {
					state.listCandidates = state.listCandidates.concat(users)
				},
				setSearchUsers: function(state, users) {
					state.searchUsers = users
				},
				concatSearchUsers: function(state, users) {
					state.searchUsers = state.searchUsers.concat(users)
				}
			},
			actions: {
				findUsersByCandidates: function(ctx, params) {
					if (params.idIn.length > 0) {
						var idIn = params.idIn.join(',')
						return AdminService.findUsers({ idIn: idIn }).then(users => {
							ctx.commit('concatCandidateUsers', users)
							ctx.commit('concatSearchUsers', users)
						})
					}
				},
				findUsers: function(ctx, params) {
					if (ctx.state.listCandidates.length > 0) {
						var fn = params.filter.toLowerCase()
						var users = ctx.state.listCandidates.filter(u => {
							return u.id.toLowerCase() === fn || 
							(u.firstName && u.firstName.toLowerCase().includes(fn)) || 
							(u.lastName && u.lastName.toLowerCase().includes(fn))
						})
						ctx.commit('setSearchUsers', users)
						return Promise.resolve()
					} else {
						var firstNameLike = { firstNameLike: '*' + params.filter + '*', maxResults: params.maxResults }
						var lastNameLike = { lastNameLike: '*' + params.filter + '*', maxResults: params.maxResults }
						var id = { id: params.filter }
						return Promise.all([AdminService.findUsers(firstNameLike), AdminService.findUsers(lastNameLike), AdminService.findUsers(id)])
						.then(users => {
							users = users[0].concat(users[1]).concat(users[2])
							ctx.commit('setSearchUsers', users)
						})
					}
				}
			}	
		}
	}
	
	export { buildProcessStore, buildFilterStore, buildUserStore, buildAdvancedSearchStore }