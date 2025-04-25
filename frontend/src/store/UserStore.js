
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
