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
const TaskStore = {
  namespaced: true,
  state: {
    selectedAssignee: { taskId: null, assignee: null }
  },
  mutations: {
    setSelectedAssignee(state, { taskId, assignee }) {
      state.selectedAssignee = { taskId, assignee }
    },
    clearSelectedAssignee(state) {
      state.selectedAssignee = { taskId: null, assignee: null }
    }
  },
  getters: {
    getSelectedAssignee: (state) => state.selectedAssignee,
    getAssigneeByTaskId: (state) => (taskId) => {
      if (state.selectedAssignee && state.selectedAssignee.taskId === taskId) {
        return state.selectedAssignee.assignee
      }
      return null
    }
  },
  actions: {
    setSelectedAssignee({ commit }, { taskId, assignee }) {
      if (taskId) {
        commit('setSelectedAssignee', { taskId, assignee })
      } else {
        commit('clearSelectedAssignee')
      }
    },
    clearSelectedAssignee({ commit }) {
      commit('clearSelectedAssignee')
    }
  }
}

export default TaskStore


