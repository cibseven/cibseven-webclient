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

// The load in flight per store, so callers asking for the same definition share one request
const pendingStaticLoads = new WeakMap()
// The latest table load per store; an older one stops committing once a newer one has started
const currentTableLoads = new WeakMap()

async function fetchStaticCalledProcessDefinitions({ commit, state }, pending) {
  const { processDefinitionId } = pending
  // Never keep the data of another definition while the new one loads; null first, so it is never shown under the new id
  if (state.staticCalledProcessDefinitionsId !== processDefinitionId) {
    commit('setStaticCalledProcessDefinitions', null)
    commit('setStaticCalledProcessDefinitionsId', processDefinitionId)
  }
  try {
    const staticCalledProcessDefinitions = await ProcessService.findCalledProcessDefinitions(processDefinitionId)
    // A newer request (another definition, or this one again after quick switches) replaced this one: its answer is older
    if (pendingStaticLoads.get(state) !== pending) return
    commit('setStaticCalledProcessDefinitions', staticCalledProcessDefinitions)
  } catch (error) {
    // Only the call activity links depend on this: they stay "unknown", the views keep working
    console.error('Failed to load the called process definitions:', error)
  } finally {
    // Only its own entry: a newer load of the same definition may have replaced it meanwhile
    if (pendingStaticLoads.get(state) === pending) pendingStaticLoads.delete(state)
  }
}

export default {
  namespaced: true,
  state: {
    calledProcessDefinitions: [],
    allCalledProcessDefinitions: [],
    // null until they have loaded for staticCalledProcessDefinitionsId
    staticCalledProcessDefinitions: null,
    staticCalledProcessDefinitionsId: null
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
    setStaticCalledProcessDefinitionsId: function (state, processDefinitionId) {
      state.staticCalledProcessDefinitionsId = processDefinitionId
    },
  },
  actions: {
    loadStaticCalledProcessDefinitions(context, { processDefinitionId }) {
      const current = pendingStaticLoads.get(context.state)
      if (current?.processDefinitionId === processDefinitionId) return current.promise
      const pending = { processDefinitionId }
      pendingStaticLoads.set(context.state, pending)
      pending.promise = fetchStaticCalledProcessDefinitions(context, pending)
      return pending.promise
    },

    // Leaving the process pages: the next visit loads them again, so a newer deployment is seen then.
    // A load still running no longer commits
    clearStaticCalledProcessDefinitions({ commit, state }) {
      pendingStaticLoads.delete(state)
      commit('setStaticCalledProcessDefinitions', null)
      commit('setStaticCalledProcessDefinitionsId', null)
    },

    // A definition switch before the new table can load: drop the previous rows and outdate a load still running for them
    resetCalledProcessDefinitions({ commit, state }) {
      currentTableLoads.delete(state)
      commit('setCalledProcessDefinitions', [])
      commit('setAllCalledProcessDefinitions', [])
    },

    async loadCalledProcessDefinitions({ commit, dispatch, getters, rootGetters, state }, { processId, chunkSize = 50 }) {
      // e.g. after switching definitions, a slow load of the previous one must not fill the table
      const load = {}
      currentTableLoads.set(state, load)
      const isOutdated = () => currentTableLoads.get(state) !== load

      commit('setCalledProcessDefinitions', [])
      commit('setAllCalledProcessDefinitions', [])

      // Not loaded yet does not mean "calls nothing statically", and a refresh in flight may bring a newer list: wait for it
      const refreshing = pendingStaticLoads.get(state)?.processDefinitionId === processId
      if (processId && (refreshing || !getters.getStaticCalledProcessDefinitionsFor(processId))) {
        await dispatch('loadStaticCalledProcessDefinitions', { processDefinitionId: processId })
        if (isOutdated()) return
      }
      const loadedStaticDefinitions = getters.getStaticCalledProcessDefinitionsFor(processId)
      // Their load failed: a table without them would wrongly say the definition calls nothing statically
      if (processId && !loadedStaticDefinitions) throw new Error('The static called process definitions could not be loaded')
      const staticDefinitions = loadedStaticDefinitions || []
      const historicStats = rootGetters.getHistoricActivityStatistics(processId) || []
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
            tenantId: def.tenantId,
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

        for (const activityId of def.calledFromActivityIds || []) {
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

      const staticActivityIds = new Set(
        staticDefinitions.flatMap(def => def.calledFromActivityIds || [])
      )
      const hasDynamicCalls = historicStats.some(stat => {
        const activity = getActivityName[stat.id]
        return activity && !staticActivityIds.has(stat.id)
      })

      // 2. Dynamic processes
      if (processId && hasDynamicCalls) {
        const activitiesHistory = await HistoryService.findActivitiesProcessDefinitionHistory(processId, {
          activityType: 'callActivity',
          unfinished: true
        })
        if (isOutdated()) return

        const filtered = activitiesHistory.filter(a =>
          a.calledProcessInstanceId && !a.endTime
        )

        const instanceIds = [...new Set(filtered.map(a => a.calledProcessInstanceId))]

        const activityByInstanceId = new Map()
        for (const a of filtered) {
          if (!activityByInstanceId.has(a.calledProcessInstanceId)) {
            activityByInstanceId.set(a.calledProcessInstanceId, a)
          }
        }

        let isFirstChunk = true

        for (let i = 0; i < instanceIds.length; i += chunkSize) {
          const chunk = instanceIds.slice(i, i + chunkSize)
          const definitionsInfo = await HistoryService.findProcessesInstancesHistory({ processInstanceIds: chunk })
          if (isOutdated()) return

          for (const inst of definitionsInfo) {
            const key = `${inst.processDefinitionKey}:${inst.processDefinitionVersion}`

            if (!groupedMap.has(key)) {
              groupedMap.set(key, {
                id: inst.processDefinitionId,
                definitionId: inst.processDefinitionId,
                definitionKey: inst.processDefinitionKey,
                tenantId: inst.tenantId,
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

            const match = activityByInstanceId.get(inst.id)
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
            const merged = buildSnapshot()
            commit('setCalledProcessDefinitions', merged)
            commit('setAllCalledProcessDefinitions', merged)
          }

          isFirstChunk = false
        }
      }

      // 3. Final merge
      const finalMerged = buildSnapshot()

      commit('setCalledProcessDefinitions', finalMerged)
      commit('setAllCalledProcessDefinitions', finalMerged)

      // Helpers
      // Returns detached copies with final labels, so committed state is never mutated afterwards
      function buildSnapshot() {
        const snapshot = mergeAndSplitByDefinition([...groupedMap.values()]).map(def => ({
          ...def,
          instances: [...def.instances],
          activities: def.activities.map(a => ({
            ...a,
            instances: a.instances.map(i => ({ ...i }))
          }))
        }))

        const versionsByKey = {}
        for (const def of snapshot) {
          if (!versionsByKey[def.definitionKey]) {
            versionsByKey[def.definitionKey] = new Set()
          }
          versionsByKey[def.definitionKey].add(def.version)
        }

        for (const def of snapshot) {
          const versions = versionsByKey[def.definitionKey]
          def.label = versions.size > 1
            ? `${def.definitionKey}:${def.version}`
            : def.definitionKey
        }

        return snapshot
      }

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
    // Kept for library consumers; it does not say which definition the list belongs to, use getStaticCalledProcessDefinitionsFor
    getStaticCalledProcessDefinitions: state => state.staticCalledProcessDefinitions || [],
    // The static definitions once they have loaded for the given process definition; null while they are unknown
    getStaticCalledProcessDefinitionsFor: state => processDefinitionId =>
      processDefinitionId && state.staticCalledProcessDefinitionsId === processDefinitionId
        ? state.staticCalledProcessDefinitions
        : null
  }
}
