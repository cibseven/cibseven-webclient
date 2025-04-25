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

import { AdminService } from "@/services.js"

const UserStore = {
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

export default UserStore
