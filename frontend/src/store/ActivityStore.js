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

import { HistoryService } from '@/services.js'

const ActivityStore = {
  state: { 
    processActivities: [],
    selectedActivityId: '',
    highlightedElement: null,
    activitiesInstanceHistory: [],
    diagramXml: null
  },
  mutations: {
    setProcessActivities: function (state, activities) {
      state.processActivities = activities
    },
    setSelectedActivityId: function (state, activityId) {
      state.selectedActivityId = activityId
    },
    setHighlightedElement: function (state, element) {
      state.highlightedElement = element
    },
    clearActivitySelection: function (state) {
      state.selectedActivityId = ''
      state.highlightedElement = null
    },
    setActivitiesInstanceHistory: function (state, activitiesInstance) {
      state.activitiesInstanceHistory = activitiesInstance
    },
    setDiagramXml: function (state, diagramXml) {
      state.diagramXml = diagramXml
    },
  },
  getters: {
    selectedActivityId: (state) => state.selectedActivityId,
    highlightedElement: (state) => state.highlightedElement,
    getProcessActivities: (state) => state.processActivities,
    activitiesInstanceHistory: (state) => state.activitiesInstanceHistory,
    diagramXml: (state) => state.diagramXml
  },
  actions: {
    selectActivity: function ({ commit }, activityId) {
      commit('setSelectedActivityId', activityId)
    },
    setHighlightedElement: function ({ commit }, element) {
      commit('setHighlightedElement', element)
    },
    clearActivitySelection: function ({ commit }) {
      commit('clearActivitySelection')
    },
    async loadActivitiesInstanceHistory({ commit }, processInstanceId) {
      const activitiesInstace = await HistoryService.findActivitiesInstancesHistory(processInstanceId)
      commit('setActivitiesInstanceHistory', activitiesInstace)
    },
    setDiagramXml: function ({ commit }, diagramXml) {
      commit('setDiagramXml', diagramXml)
    }
  }
}

export default ActivityStore
