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

import { IncidentService } from '@/services.js'

export default {
  namespaced: true,
  state: {
    incidents: []
  },
  mutations: {
    setIncidents(state, incidents) {
      state.incidents = incidents
    },
    removeIncident(state, incidentId) {
      // Remove by incident ID or by configuration ID (job ID)
      state.incidents = state.incidents.filter(incident =>
        incident.id !== incidentId && incident.configuration !== incidentId
      )
    }, updateIncidentAnnotation(state, { incidentId, annotation }) {
      const incident = state.incidents.find(incident => incident.id === incidentId)
      if (incident) {
        incident.annotation = annotation
      }
    }
  },
  actions: {
    async loadIncidents({ commit }, params) {
      const incidents = await IncidentService.findIncidents(params)
      commit('setIncidents', incidents)
      return incidents
    },
    async loadIncidentsByProcessInstance({ dispatch }, processInstanceId) {
      return dispatch('loadIncidents', { processInstanceId })
    },
    async loadIncidentsByProcessDefinition({ dispatch }, processDefinitionId) {
      return dispatch('loadIncidents', { processDefinitionId })
    },
    removeIncident({ commit }, incidentId) {
      commit('removeIncident', incidentId)
    },
    updateIncidentAnnotation({ commit }, { incidentId, annotation }) {
      commit('updateIncidentAnnotation', { incidentId, annotation })
    },
    async deleteIncident({ commit }, incidentId) {
      await IncidentService.deleteIncident(incidentId)
      commit('removeIncident', incidentId)
    }
  },
  getters: {
    incidents: state => state.incidents
  }
}
