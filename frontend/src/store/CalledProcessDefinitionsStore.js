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

import { ProcessService, HistoryService } from '@/services.js'

export default {
  namespaced: true,
  state: {
    calledProcessDefinitions: [],
    allCalledProcessDefinitions: [],
    staticCalledProcessDefinitions: [] 
  },
  mutations: {
    setCalledProcessDefinitions(state, calledProcessDefinitions) {
      state.calledProcessDefinitions = calledProcessDefinitions
    },
    setAllCalledProcessDefinitions(state, calledProcessDefinitions) {
      state.allCalledProcessDefinitions = calledProcessDefinitions
    },    
    setStaticCalledProcessDefinitions: function (state, params) {
      state.staticCalledProcessDefinitions = params
    },
  },
  actions: {    
    async loadStaticCalledProcessDefinitions({ commit }, { processDefinitionId }) {
      const staticCalledProcessDefinitions = await ProcessService.findCalledProcessDefinitions(processDefinitionId)
      commit('setStaticCalledProcessDefinitions', staticCalledProcessDefinitions)
    },

    async loadCalledProcessDefinitions({ commit, getters, rootGetters }, { processId, chunkSize = 50 }) {
      commit('setCalledProcessDefinitions', [])
      commit('setAllCalledProcessDefinitions', [])

      const staticDefinitions = getters.getStaticCalledProcessDefinitions || []
      const historicStats = rootGetters.getHistoricActivityStatistics || []
      const getActivityName = rootGetters.getProcessActivities || {}

      const groupedMap = new Map()

      // 1. Static processes
      for (const def of staticDefinitions) {
        const key = `${def.key}:${def.version}`
        if (!groupedMap.has(key)) {
          groupedMap.set(key, {
            id: def.id,
            definitionId: def.id,
            definitionKey: def.key,
            version: String(def.version),
            name: def.name,
            activities: [],
            instances: [],
            latestVersion: true,
            label: `${def.key}:${def.version}`,
            isStatic: true,
            isRunning: false
          })
        }

        const group = groupedMap.get(key)

        for (const activityId of def.calledFromActivityIds) {
          const stat = historicStats.find(s => s.id === activityId)
          const ended = stat ? stat.instances === 0 : true
          const name = getActivityName[activityId] || activityId

          let activity = group.activities.find(a => a.activityId === activityId)
          if (!activity) {
            activity = {
              activityId,
              activityName: name,
              isStatic: true,
              instances: []
            }
            group.activities.push(activity)
          }

          activity.instances.push({
            processInstanceId: null,
            ended
          })
        }
      }

      // 2. Dynamic processes
      if (processId) {
        const activitiesHistory = await HistoryService.findActivitiesProcessDefinitionHistory(processId, {
          activityType: 'callActivity',
          unfinished: true
        })

        const filtered = activitiesHistory.filter(a =>
          a.calledProcessInstanceId && !a.endTime
        )

        const instanceIds = [...new Set(filtered.map(a => a.calledProcessInstanceId))]

        let isFirstChunk = true

        for (let i = 0; i < instanceIds.length; i += chunkSize) {
          const chunk = instanceIds.slice(i, i + chunkSize)
          const definitionsInfo = await HistoryService.findProcessesInstancesHistory({ processInstanceIds: chunk })

          for (const inst of definitionsInfo) {
            const key = `${inst.processDefinitionKey}:${inst.processDefinitionVersion}`

            if (!groupedMap.has(key)) {
              groupedMap.set(key, {
                id: inst.processDefinitionId,
                definitionId: inst.processDefinitionId,
                definitionKey: inst.processDefinitionKey,
                version: String(inst.processDefinitionVersion),
                name: inst.processDefinitionName,
                activities: [],
                instances: [],
                latestVersion: false,
                label: key,
                isStatic: false,
                isRunning: true
              })
            }

            const def = groupedMap.get(key)
            def.isRunning = true

            const match = filtered.find(a => a.calledProcessInstanceId === inst.id)
            if (match) {
              let activity = def.activities.find(a => a.activityId === match.activityId)
              if (!activity) {
                activity = {
                  activityId: match.activityId,
                  activityName: match.activityName || match.activityId,
                  isStatic: false,
                  instances: []
                }
                def.activities.push(activity)
              }

              activity.instances.push({
                processInstanceId: inst.id,
                ended: false
              })
            }
          }

          if (!isFirstChunk || instanceIds.length <= chunkSize) {
            const merged = mergeAndSplitByDefinition([...groupedMap.values()])
            commit('setCalledProcessDefinitions', merged)
            commit('setAllCalledProcessDefinitions', merged)
          }

          isFirstChunk = false
        }
      }

      // 3. Final merge
      const finalMerged = mergeAndSplitByDefinition([...groupedMap.values()])

      const versionsByKey = {}
      for (const def of finalMerged) {
        if (!versionsByKey[def.definitionKey]) {
          versionsByKey[def.definitionKey] = new Set()
        }
        versionsByKey[def.definitionKey].add(def.version)
      }

      for (const def of finalMerged) {
        const versions = versionsByKey[def.definitionKey]
        def.label = versions.size > 1
          ? `${def.definitionKey}:${def.version}`
          : def.definitionKey
      }

      commit('setCalledProcessDefinitions', finalMerged)
      commit('setAllCalledProcessDefinitions', finalMerged)

      // Helper
      function mergeAndSplitByDefinition(definitions) {
        const map = new Map()

        for (const def of definitions) {
          const key = `${def.definitionKey}:${def.version}`
          if (!map.has(key)) {
            map.set(key, { base: def, static: null, dynamic: null })
          }

          const entry = map.get(key)
          if (def.isStatic) entry.static = def
          if (def.isRunning) entry.dynamic = def
        }

        const result = []
        for (const { static: stat, dynamic: dyn } of map.values()) {
          if (dyn) result.push(dyn)
          if (stat && !dyn) result.push(stat)
        }

        return result
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
    mapSelectedActivity: () => (cp, activityId) => {
      const selectedActivity = cp.activities.find(act => act.activityId === activityId)
      return {
        ...cp,
        activities: [selectedActivity]
      }
    },
    getCalledProcessState: () => (processDefinition) => {
      const hasRunning = processDefinition.activities
        .filter(act => !act.isStatic)
        .some(act => act.instances.some(inst => inst.ended === false))

      const hasReferenced = processDefinition.activities
        .some(act => act.isStatic)

      if (hasRunning && hasReferenced) return 'process-instance.calledProcessDefinitions.runningAndReferenced'
      if (hasRunning) return 'process-instance.calledProcessDefinitions.running'
      if (hasReferenced) return 'process-instance.calledProcessDefinitions.referenced'
      return 'process-instance.calledProcessDefinitions.unknown'
    },
    getStaticCalledProcessDefinitions: state => state.staticCalledProcessDefinitions
  }
}
