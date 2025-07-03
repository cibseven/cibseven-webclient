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

import { ProcessService } from '@/services.js'

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
    async loadCalledProcessDefinitions({ commit, getters, rootGetters }, { processId, activitiesHistory, diagramXml, chunkSize = 50 }) {
      commit('setCalledProcessDefinitions', [])
      commit('setAllCalledProcessDefinitions', [])

      const staticIds = getters.getStaticCallActivityIdsFromXml(diagramXml)
    
      // Filter activitiesHistory to get only callActivity activities that have calledProcessInstanceId
      const callActivities = activitiesHistory.filter(a =>
        a.activityType === 'callActivity' && a.calledProcessInstanceId
      )

      // Get unique calledProcessInstanceIds from callActivities
      const calledProcessIds = [...new Set(callActivities.map(a => a.calledProcessInstanceId))]
      
      // Group by definitionKey and version
      // This will hold the grouped process definitions
      const groupedMap = {}

      const addLabelsToDefinitions = (definitions) => {
        const versionsByKey = {}
        for (const item of definitions) {
          if (!versionsByKey[item.definitionKey]) versionsByKey[item.definitionKey] = new Set()
          versionsByKey[item.definitionKey].add(item.version)
        }

        for (const item of definitions) {
          const versions = versionsByKey[item.definitionKey]
          item.label = versions.size > 1
            ? `${item.definitionKey}:${item.version}`
            : item.definitionKey
        }
      }

      // Helper function to update the grouped definitions with labels
      const updateGroupedWithLabels = () => {
        const grouped = Object.values(groupedMap)
        addLabelsToDefinitions(grouped)        
        commit('setCalledProcessDefinitions', [...grouped])
      }

      // Process callActivities in chunks to avoid performance issues
      // and to allow partial updates
      // This is especially useful if there are many call activities
      // and we want to avoid blocking the UI for too long
      let isFirstChunk = true
      for (let i = 0; i < calledProcessIds.length; i += chunkSize) {
        const chunk = calledProcessIds.slice(i, i + chunkSize)
        const chunkInstances = await ProcessService.findCurrentProcessesInstances({ processInstanceIds: chunk })
        const instanceMap = new Map(chunkInstances.map(i => [i.id, i]))
      
        // Enrich callActivities with the called process instance details for this chunk
        // and mark if the activity is static or dynamic based on the staticIds      
        const enrichedDefinitions = callActivities
          .filter(activity => chunk.includes(activity.calledProcessInstanceId))
          .flatMap(activity => {
            const instance = instanceMap.get(activity.calledProcessInstanceId)
            if (!instance) return []
      
            return [{
              id: instance.id,
              definitionId: instance.definitionId,
              definitionKey: instance.definitionId.split(':')[0],
              version: instance.definitionId.split(':')[1] || '',
              activityId: activity.activityId,
              activityName: activity.activityName,
              processInstanceId: activity.processInstanceId,
              ended: instance.ended,
              isStatic: staticIds.includes(activity.activityId)
            }]
          })

        // Add the enriched definitions to the grouped map
        for (const def of enrichedDefinitions) {
          const key = `${def.definitionKey}:${def.version}`
          if (!groupedMap[key]) {
            groupedMap[key] = {
              id: def.id,
              definitionId: def.definitionId,
              definitionKey: def.definitionKey,
              version: def.version,
              activities: [],
              instances: []
            }
          }

          // Add activity details to the grouped map if it doesn't already exist
          const exists = groupedMap[key].activities.some(a => a.activityId === def.activityId)
          if (!exists) {
            groupedMap[key].activities.push({
              activityId: def.activityId,
              activityName: def.activityName,
              isStatic: def.isStatic,
              processInstanceId: def.processInstanceId,
              ended: def.ended
            })
          }

          if (!groupedMap[key].instances.includes(def.id)) {
            groupedMap[key].instances.push(def.id)
          }
        }

        // Update the grouped definitions with labels
        // This will ensure that the UI is responsive and updates incrementally
        // instead of waiting for all chunks to be processed
        if (!isFirstChunk) {
          updateGroupedWithLabels()
        }
        isFirstChunk = false
      }

      // Add latest called process definitions
      // This will include the latest version of each called process definition
      const latestCalledProcesses = await ProcessService.findCalledProcessDefinitions(processId)

      for (const def of latestCalledProcesses) {
        const key = `${def.key}:${def.version}`
        if (!groupedMap[key]) {
          groupedMap[key] = {
            id: def.id,
            definitionId: def.id,
            definitionKey: def.key,
            version: String(def.version),
            name: def.name,
            activities: def.calledFromActivityIds.map(activityId => ({
              activityId,
              activityName: rootGetters['getProcessActivities'][activityId],
              processInstanceId: null,
              ended: true,
              isStatic: staticIds.includes(activityId)
            })),
            instances: [],
            latestVersion: true
          }
        } else {
          groupedMap[key].latestVersion = true
          groupedMap[key].name = def.name || groupedMap[key].name
          groupedMap[key].activities.forEach(activity => {
            if (activity.isStatic === undefined) {
              activity.isStatic = staticIds.includes(activity.activityId)
            }
          })
        }
      }      

      // This will be the final list of called process definitions
      const grouped = Object.values(groupedMap)

      addLabelsToDefinitions(grouped)
    
      commit('setCalledProcessDefinitions', grouped)
      commit('setAllCalledProcessDefinitions', grouped)
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
    getStaticCallActivityIdsFromXml: () => (xml) => {
      const parser = new DOMParser()
      const xmlDoc = parser.parseFromString(xml, 'application/xml')
      const nodes = xmlDoc.getElementsByTagName('bpmn:callActivity')
      const staticIds = []
      for (let i = 0; i < nodes.length; i++) {
        const el = nodes[i]
        const called = el.getAttribute('calledElement')
        if (called && !called.startsWith('${')) {
          staticIds.push(el.getAttribute('id'))
        }
      }
      return staticIds
    },
    mapSelectedActivity: () => (cp, activityId) => {
      const selectedActivity = cp.activities.find(act => act.activityId === activityId)
      return {
        ...cp,
        activities: [selectedActivity]
      }
    },
    getCalledProcessState: () => (processDefinition) => {
      // An activity is considered "running" if:
      // 1. It is not ended (ended === false)
      const hasRunning = processDefinition.activities.some(act => act.ended === false)  
      // An activity is considered "referenced" if:
      // 1. It is not ended and has no processInstanceId (meaning it is still running)
      // 2. Or if it is the latest version and the activity is static
      const hasReferenced = processDefinition.activities.some(act => 
        (act.ended === false && act.processInstanceId == null) ||
        (processDefinition.latestVersion && act.isStatic)
      )

      if (hasRunning && hasReferenced) return 'process-instance.calledProcessDefinitions.runningAndReferenced'
      if (hasRunning) return 'process-instance.calledProcessDefinitions.running'
      if (hasReferenced) return 'process-instance.calledProcessDefinitions.referenced'
      return 'process-instance.calledProcessDefinitions.unknown'
    }
  }
}
