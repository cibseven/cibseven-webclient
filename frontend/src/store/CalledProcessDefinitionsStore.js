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

export default {
  namespaced: true,
  state: {
    calledProcessDefinitions: [],
    allCalledProcessDefinitions: []
  },
  mutations: {
    setCalledProcessDefinitions(state, calledProcessDefinitions) {
      state.calledProcessDefinitions = calledProcessDefinitions
    },
    setAllCalledProcessDefinitions(state, calledProcessDefinitions) {
      state.allCalledProcessDefinitions = calledProcessDefinitions
    }
  },
  actions: {
    async loadCalledProcessDefinitions({ commit, rootState, getters }, { processId, diagramXml }) {
      commit('setCalledProcessDefinitions', [])
      
      try {
        const activities = await HistoryService.findActivitiesInstancesHistoryWithFilter({
          processDefinitionId: processId,
          activityType: 'callActivity'
        })

        if (activities.length === 0) {
          commit('setCalledProcessDefinitions', [])
          commit('setAllCalledProcessDefinitions', [])
          return
        }

        const staticIds = getters.getStaticCallActivityIdsFromXml(diagramXml)
        const activityList = getters.markStaticOrDynamic(activities, staticIds)

        const filteredActivities = activityList.filter(activity => {
          return activity.isStatic || (!activity.endTime && activity.canceled !== true && activity.deleted !== true)
        })

        const instancesIdList = [...new Set(filteredActivities.map(a => a.calledProcessInstanceId))]

        if (instancesIdList.length > 0) {
          const processInstances = await HistoryService.findProcessesInstancesHistory({
            processInstanceIds: instancesIdList
          })
          
          const groupedProcesses = getters.groupCalledProcesses(
            filteredActivities, 
            processInstances, 
            rootState
          )
          
          const calledProcessDefinitionsArray = Object.values(groupedProcesses)
          commit('setCalledProcessDefinitions', calledProcessDefinitionsArray)
          commit('setAllCalledProcessDefinitions', calledProcessDefinitionsArray)
        }
      } catch (error) {
        console.error('Error loading called process definitions:', error)
      }
    },

    filterByActivity({ commit, state, getters }, selectedActivityId) {
      if (selectedActivityId) {
        const filtered = state.allCalledProcessDefinitions
          .filter(cp => cp.activities.some(act => act.activityId === selectedActivityId))
          .map(cp => getters.mapSelectedActivity(cp, selectedActivityId))
        commit('setCalledProcessDefinitions', filtered)
      } else {
        commit('setCalledProcessDefinitions', [...state.allCalledProcessDefinitions])
      }
    }
  },
  getters: {
    calledProcessDefinitions: state => state.calledProcessDefinitions,
    allCalledProcessDefinitions: state => state.allCalledProcessDefinitions,
    getStaticCallActivityIdsFromXml: () => (xmlString) => {
      const parser = new DOMParser()
      const xmlDoc = parser.parseFromString(xmlString, 'application/xml')
      const callActivities = xmlDoc.getElementsByTagName('bpmn:callActivity')
      const staticIds = []
      for (let i = 0; i < callActivities.length; i++) {
        const el = callActivities[i]
        const calledElement = el.getAttribute('calledElement')
        const id = el.getAttribute('id')
        if (calledElement && !calledElement.trim().startsWith('${')) {
          staticIds.push(id)
        }
      }
      return staticIds
    },
    markStaticOrDynamic: () => (activitiesList, staticActivityIds) => {
      return activitiesList.map(activity => ({
        ...activity,
        isStatic: staticActivityIds.includes(activity.activityId)
      }))
    },
    groupCalledProcesses: () => (activities, processInstances, rootState) => {
      const grouped = {}
      processInstances.forEach(instance => {
        const relatedActivities = activities.filter(
          act => act.calledProcessInstanceId === instance.id
        )
        const processDefId = instance.processDefinitionId
        const key = processDefId.split(':')[0]
        const version = processDefId.split(':')[1]
        const foundProcess = rootState.process?.list?.find(p => p.key === key) || null
        relatedActivities.forEach(activity => {
          const groupKey = processDefId
          if (!grouped[groupKey]) {
            grouped[groupKey] = {
              id: groupKey,
              key,
              version,
              name: foundProcess ? foundProcess.name : key,
              activities: [],
              instances: []
            }
          }
          if (!grouped[groupKey].activities.some(a => a.activityId === activity.activityId)) {
            grouped[groupKey].activities.push({
              ...activity,
              isStatic: activity.isStatic
            })
          }
          if (!grouped[groupKey].instances.some(i => i.id === instance.id)) {
            grouped[groupKey].instances.push(instance)
          }
        })
      })
      return grouped
    },
    mapSelectedActivity: () => (cp, activityId) => {
      const selectedActivity = cp.activities.find(act => act.activityId === activityId)
      return {
        ...cp,
        activities: [selectedActivity]
      }
    },
    getCalledProcessState: () => (activities) => {
      const hasRunning = activities.some(act => act.endTime == null && act.canceled !== true)
      const hasReferenced = activities.some(act => act.isStatic)
      if (hasRunning && hasReferenced) return 'process-instance.calledProcessDefinitions.runningAndReferenced'
      if (hasRunning) return 'process-instance.calledProcessDefinitions.running'
      if (hasReferenced) return 'process-instance.calledProcessDefinitions.referenced'
      return null
    }
  }
}
