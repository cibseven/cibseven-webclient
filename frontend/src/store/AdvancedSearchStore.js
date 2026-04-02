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
          const storedData = localStorage.getItem('_advancedSearch')
          const parsedData = JSON.parse(storedData)
          ctx.commit('initializeAdvancedSearch', parsedData)
      }
    },
  getters: {
      formatedCriteriaData(state) {
           const result = {}
          state.criterias.forEach(criteria => {
            const { key, name, operator, value } = criteria
            if (!result[key]) result[key] = []
            result[key].push({ name, operator, value })
          })
          return result
      }
    }
}

export default AdvancedSearchStore
