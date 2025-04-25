
const AdvancedSearchStore = {
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

export default AdvancedSearchStore
