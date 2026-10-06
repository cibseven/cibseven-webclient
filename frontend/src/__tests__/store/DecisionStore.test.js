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
import { describe, it, expect, vi, beforeEach } from 'vitest'
import DecisionStore from '../../store/DecisionStore.js'
import { DecisionService } from '@/services.js'
import { createStoreTestSuite } from './store-test-utils.js'

vi.mock('@/services.js', () => ({
  DecisionService: {
    getDecisionList: vi.fn(),
    getDecisionByKey: vi.fn(),
    getDecisionByKeyAndTenant: vi.fn(),
    getDecisionVersionsByKey: vi.fn(),
    evaluateByKey: vi.fn(),
    evaluateByKeyAndTenant: vi.fn(),
    evaluateById: vi.fn(),
    getDiagramByKey: vi.fn(),
    getDiagramById: vi.fn(),
    getDiagramByKeyAndTenant: vi.fn(),
    getXmlByKey: vi.fn(),
    getXmlByKeyAndTenant: vi.fn(),
    getXmlById: vi.fn(),
    updateHistoryTTLByKey: vi.fn(),
    updateHistoryTTLByKeyAndTenant: vi.fn(),
    updateHistoryTTLById: vi.fn(),
    getHistoricDecisionInstances: vi.fn(),
    getHistoricDecisionInstanceCount: vi.fn(),
    getHistoricDecisionInstanceById: vi.fn(),
    deleteHistoricDecisionInstances: vi.fn(),
    setHistoricDecisionInstanceRemovalTime: vi.fn()
  }
}))

const version = (overrides = {}) => ({ id: 'dd-1', version: 1, ...overrides })
const decision = (overrides = {}) => ({ key: 'invoice', id: 'dd-1', name: 'Invoice', versions: [version()], ...overrides })

// The declared parameter count is what makes the bug visible: a Vuex action can only
// ever receive two arguments, so any action declaring three has a dead parameter.
const storeModuleActionArity = (fn) => fn.length

beforeEach(() => {
  vi.clearAllMocks()
})

createStoreTestSuite('DecisionStore', DecisionStore, {
  initialState: (getState) => {
    it('should start empty and not loading', () => {
      expect(getState()).toEqual({
        list: [],
        selectedDecisionVersion: null,
        selectedInstance: null,
        instances: [],
        isLoading: false
      })
    })
  },

  mutations: {
    setDecisions: (mutation, getState) => {
      it('should replace the decision list', () => {
        const state = getState()
        mutation(state, { decisions: [decision()] })
        expect(state.list).toEqual([decision()])
      })
    },

    setLoading: (mutation, getState) => {
      it('should track the loading flag', () => {
        const state = getState()
        mutation(state, true)
        expect(state.isLoading).toBe(true)
        mutation(state, false)
        expect(state.isLoading).toBe(false)
      })
    },

    setHistoricInstances: (mutation, getState) => {
      it('should replace the flat instance list', () => {
        const state = getState()
        mutation(state, [{ id: 'di-1' }])
        expect(state.instances).toEqual([{ id: 'di-1' }])
      })
    },

    setSelectedInstance: (mutation, getState) => {
      it('should store the selected instance', () => {
        const state = getState()
        mutation(state, { id: 'di-1' })
        expect(state.selectedInstance).toEqual({ id: 'di-1' })
      })
    },

    setDecisionVersions: (mutation, getState) => {
      it('should attach versions to the matching decision', () => {
        const state = getState()
        state.list = [decision({ key: 'invoice' })]

        mutation(state, { key: 'invoice', versions: [version({ version: 2 })] })

        expect(state.list[0].versions).toEqual([version({ version: 2 })])
      })

      it('should be a no-op for an unknown key', () => {
        const state = getState()
        state.list = [decision({ key: 'invoice' })]

        mutation(state, { key: 'missing', versions: [] })

        expect(state.list[0].versions).toEqual([version()])
      })
    },

    setHistoricInstancesForKey: (mutation, getState) => {
      // Version is compared as a string because it arrives from the URL as one.
      it('should attach instances to the matching version, comparing loosely', () => {
        const state = getState()
        state.list = [decision({ versions: [version({ version: 2 })] })]

        mutation(state, { key: 'invoice', version: '2', instances: [{ id: 'di-1' }] })

        expect(state.list[0].versions[0].historicInstances).toEqual([{ id: 'di-1' }])
      })

      it('should be a no-op for an unknown decision key', () => {
        const state = getState()
        state.list = [decision()]

        expect(() => mutation(state, { key: 'missing', version: '1', instances: [] })).not.toThrow()
        expect(state.list[0].versions[0].historicInstances).toBeUndefined()
      })

      it('should be a no-op for an unknown version', () => {
        const state = getState()
        state.list = [decision({ versions: [version({ version: 1 })] })]

        mutation(state, { key: 'invoice', version: '99', instances: [{ id: 'di-1' }] })

        expect(state.list[0].versions[0].historicInstances).toBeUndefined()
      })
    },

    setSelectedDecisionVersion: (mutation, getState) => {
      it('should select the matching version', () => {
        const state = getState()
        state.list = [decision({ versions: [version({ version: 1 }), version({ id: 'dd-2', version: 2 })] })]

        mutation(state, { key: 'invoice', version: '2' })

        expect(state.selectedDecisionVersion).toEqual(version({ id: 'dd-2', version: 2 }))
      })

      it('should be a no-op for an unknown version', () => {
        const state = getState()
        state.list = [decision()]

        mutation(state, { key: 'invoice', version: '99' })

        expect(state.selectedDecisionVersion).toBeNull()
      })

      it('should be a no-op when the decision has no versions loaded', () => {
        const state = getState()
        state.list = [{ key: 'invoice' }]

        expect(() => mutation(state, { key: 'invoice', version: '1' })).not.toThrow()
        expect(state.selectedDecisionVersion).toBeNull()
      })
    },

    updateVersion: (mutation, getState) => {
      it('should replace the version with the same id', () => {
        const state = getState()
        state.list = [decision({ versions: [version({ id: 'dd-1', version: 1 })] })]

        mutation(state, { key: 'invoice', newVersion: version({ id: 'dd-1', version: 1, historyTimeToLive: 30 }) })

        expect(state.list[0].versions[0].historyTimeToLive).toBe(30)
        expect(state.list[0].versions).toHaveLength(1)
      })

      it('should be a no-op when no version has that id', () => {
        const state = getState()
        state.list = [decision()]

        mutation(state, { key: 'invoice', newVersion: version({ id: 'other' }) })

        expect(state.list[0].versions).toEqual([version()])
      })

      it('should be a no-op for an unknown decision key', () => {
        const state = getState()
        state.list = [decision()]

        expect(() => mutation(state, { key: 'missing', newVersion: version() })).not.toThrow()
      })
    }
  },

  getters: {
    getFilteredDecisions: (getter, getState) => {
      it('should sort by name when no filter is given', () => {
        const state = getState()
        state.list = [decision({ key: 'b', name: 'Zebra' }), decision({ key: 'a', name: 'Alpha' })]

        expect(getter(state)().map(d => d.name)).toEqual(['Alpha', 'Zebra'])
      })

      it('should filter case-insensitively on key or name', () => {
        const state = getState()
        state.list = [
          decision({ key: 'invoice', name: 'Invoice' }),
          decision({ key: 'order', name: 'Order' })
        ]

        expect(getter(state)('INVO').map(d => d.key)).toEqual(['invoice'])
        expect(getter(state)('orde').map(d => d.key)).toEqual(['order'])
      })

      it('should treat a whitespace-only filter as no filter', () => {
        const state = getState()
        state.list = [decision({ key: 'a', name: 'Alpha' })]

        expect(getter(state)('   ')).toHaveLength(1)
      })

      it('should match on key when the decision has no name', () => {
        const state = getState()
        state.list = [{ key: 'nameless', versions: [] }]

        expect(getter(state)('NAMELESS').map(d => d.key)).toEqual(['nameless'])
      })

      it('should return an empty array when the list is unset', () => {
        const state = getState()
        state.list = null

        expect(getter(state)('x')).toEqual([])
      })
    },

    getDecisionByKey: (getter, getState) => {
      it('should find the decision by key', () => {
        const state = getState()
        state.list = [decision()]
        expect(getter(state)('invoice')).toEqual(decision())
      })

      it('should return undefined for an unknown key', () => {
        expect(getter(getState())('missing')).toBeUndefined()
      })
    },

    getDecisionInstances: (getter, getState) => {
      it('should return the instances stored on the version', () => {
        const state = getState()
        state.list = [decision({ versions: [version({ version: 1, historicInstances: [{ id: 'di-1' }] })] })]

        expect(getter(state)('invoice', '1')).toEqual([{ id: 'di-1' }])
      })

      it('should return an empty array when the version has none', () => {
        const state = getState()
        state.list = [decision()]

        expect(getter(state)('invoice', '1')).toEqual([])
      })

      it('should return undefined for an unknown decision', () => {
        expect(getter(getState())('missing', '1')).toBeUndefined()
      })
    },

    getDecisionVersions: (getter, getState) => {
      it('should return the versions of the decision', () => {
        const state = getState()
        state.list = [decision()]
        expect(getter(state)('invoice')).toEqual([version()])
      })

      it('should return an empty array for an unknown decision', () => {
        expect(getter(getState())('missing')).toEqual([])
      })
    },

    getSelectedDecisionVersion: (getter, getState) => {
      it('should return the selected version', () => {
        const state = getState()
        state.selectedDecisionVersion = version()
        expect(getter(state)()).toEqual(version())
      })
    },

    getDecisionVersion: (getter, getState) => {
      it('should find a version by key and version, comparing loosely', () => {
        const state = getState()
        state.list = [decision({ versions: [version({ version: 3 })] })]

        expect(getter(state)({ key: 'invoice', version: '3' })).toEqual(version({ version: 3 }))
      })

      it('should return null for an unknown version', () => {
        const state = getState()
        state.list = [decision()]

        expect(getter(state)({ key: 'invoice', version: '99' })).toBeNull()
      })

      it('should return null for an unknown decision', () => {
        expect(getter(getState())({ key: 'missing', version: '1' })).toBeNull()
      })
    },

    decisionInstances: (getter, getState) => {
      it('should expose the flat instance list', () => {
        const state = getState()
        state.instances = [{ id: 'di-1' }]
        expect(getter(state)).toEqual([{ id: 'di-1' }])
      })
    },

    isLoading: (getter, getState) => {
      it('should expose the loading flag', () => {
        const state = getState()
        state.isLoading = true
        expect(getter(state)).toBe(true)
      })
    }
  },

  actions: {
    getDecisionList: (action, getContext) => {
      it('should reduce each definition to the fields the list needs', async () => {
        const context = getContext()
        DecisionService.getDecisionList.mockResolvedValue([{
          key: 'invoice',
          id: 'dd-1',
          name: 'Invoice',
          version: 4,
          tenantId: 't-1',
          decisionRequirementsDefinitionId: 'drd-1',
          decisionRequirementsDefinitionKey: 'drd',
          extraFieldWeDoNotWant: true
        }])

        const result = await action(context, { latestVersion: true })

        expect(DecisionService.getDecisionList).toHaveBeenCalledWith({ latestVersion: true })
        expect(result).toEqual([{
          key: 'invoice',
          id: 'dd-1',
          name: 'Invoice',
          latestVersion: 4,
          tenantId: 't-1',
          decisionRequirementsDefinitionId: 'drd-1',
          decisionRequirementsDefinitionKey: 'drd'
        }])
        expect(context.commit).toHaveBeenCalledWith('setDecisions', { decisions: result })
      })

      it('should toggle the loading flag around the request', async () => {
        const context = getContext()
        DecisionService.getDecisionList.mockResolvedValue([])

        await action(context, {})

        expect(context.commit).toHaveBeenNthCalledWith(1, 'setLoading', true)
        expect(context.commit).toHaveBeenLastCalledWith('setLoading', false)
      })

      // The `finally` block must clear the flag even when the request blows up, or the
      // list stays stuck on its spinner.
      it('should clear the loading flag when the request fails', async () => {
        const context = getContext()
        DecisionService.getDecisionList.mockRejectedValue(new Error('Service error'))

        await expect(action(context, {})).rejects.toThrow('Service error')
        expect(context.commit).toHaveBeenLastCalledWith('setLoading', false)
      })
    },

    getDecisionByKey: (action, getContext) => {
      it('should resolve from the cached list without calling the service', async () => {
        const context = getContext()
        context.state.list = [decision({ key: 'invoice' })]

        const result = await action(context, { key: 'invoice' })

        expect(result).toEqual(decision({ key: 'invoice' }))
        expect(DecisionService.getDecisionList).not.toHaveBeenCalled()
      })

      it('should reload the list and return the match found there', async () => {
        const context = getContext()
        DecisionService.getDecisionList.mockResolvedValue([decision({ key: 'invoice' })])

        const result = await action(context, { key: 'invoice' })

        expect(DecisionService.getDecisionList).toHaveBeenCalledWith({ key: 'invoice' })
        expect(result).toEqual(decision({ key: 'invoice' }))
        expect(DecisionService.getDecisionByKey).not.toHaveBeenCalled()
      })

      it('should fall back to a direct lookup when the reload does not contain it', async () => {
        const context = getContext()
        DecisionService.getDecisionList.mockResolvedValue([])
        DecisionService.getDecisionByKey.mockResolvedValue(decision({ key: 'invoice' }))

        const result = await action(context, { key: 'invoice' })

        expect(DecisionService.getDecisionByKey).toHaveBeenCalledWith('invoice')
        expect(result).toEqual(decision({ key: 'invoice' }))
      })

      it('should reload when the cached list holds a different key', async () => {
        const context = getContext()
        context.state.list = [decision({ key: 'other' })]
        DecisionService.getDecisionList.mockResolvedValue([decision({ key: 'invoice' })])

        const result = await action(context, { key: 'invoice' })

        expect(DecisionService.getDecisionList).toHaveBeenCalled()
        expect(result).toEqual(decision({ key: 'invoice' }))
      })
    },

    getDecisionVersionsByKey: (action, getContext) => {
      it('should fetch and store the versions', async () => {
        const context = getContext()
        DecisionService.getDecisionVersionsByKey.mockResolvedValue([version()])

        const result = await action(context, { key: 'invoice', lazyLoad: false })

        expect(DecisionService.getDecisionVersionsByKey).toHaveBeenCalledWith('invoice', false)
        expect(context.commit).toHaveBeenCalledWith('setDecisionVersions', { key: 'invoice', versions: [version()] })
        expect(result).toEqual([version()])
      })

      // Lazy loading skips the instance counts, so they are shown as a placeholder until
      // the real numbers are fetched.
      it('should placeholder the instance count when lazy loading', async () => {
        const context = getContext()
        DecisionService.getDecisionVersionsByKey.mockResolvedValue([version(), version({ id: 'dd-2' })])

        const result = await action(context, { key: 'invoice', lazyLoad: true })

        expect(result.every(v => v.allInstances === '-')).toBe(true)
      })
    },

    refreshDecisionVersions: (action, getContext) => {
      it('should merge newly appeared versions, newest first', async () => {
        const context = getContext()
        context.getters.getDecisionVersions = () => [version({ id: 'dd-1', version: 1 })]
        DecisionService.getDecisionVersionsByKey.mockResolvedValue([
          version({ id: 'dd-1', version: 1 }),
          version({ id: 'dd-2', version: 2 })
        ])

        await action(context, 'invoice')

        expect(context.commit).toHaveBeenCalledWith('setDecisionVersions', {
          key: 'invoice',
          versions: [version({ id: 'dd-2', version: 2 }), version({ id: 'dd-1', version: 1 })]
        })
      })

      it('should not commit when nothing new appeared', async () => {
        const context = getContext()
        context.getters.getDecisionVersions = () => [version({ id: 'dd-1' })]
        DecisionService.getDecisionVersionsByKey.mockResolvedValue([version({ id: 'dd-1' })])

        await action(context, 'invoice')

        expect(context.commit).not.toHaveBeenCalled()
      })

      // KNOWN BUG (pinned, not fixed): the action is declared as
      // `refreshDecisionVersions({ commit, getters }, key, lazyLoad)`, but Vuex invokes
      // actions as `(context, payload)` only — a third argument is never supplied. So
      // `lazyLoad` is always `undefined` and the service is always called in non-lazy
      // mode, whatever the caller intended. Dispatching the way Vuex does shows it.
      it('should always pass lazyLoad as undefined to the service (known bug)', async () => {
        const context = getContext()
        context.getters.getDecisionVersions = () => []
        DecisionService.getDecisionVersionsByKey.mockResolvedValue([])

        // Exactly what `store.dispatch('refreshDecisionVersions', 'invoice')` results in.
        await action(context, 'invoice')

        expect(DecisionService.getDecisionVersionsByKey).toHaveBeenCalledWith('invoice', undefined)
        expect(storeModuleActionArity(action)).toBeGreaterThan(2)
      })
    },

    getDecisionById: (action, getContext) => {
      it('should return the first definition matching the id', async () => {
        DecisionService.getDecisionList.mockResolvedValue([decision({ id: 'dd-1' })])

        const result = await action(getContext(), 'dd-1')

        expect(DecisionService.getDecisionList).toHaveBeenCalledWith({ decisionDefinitionId: 'dd-1' })
        expect(result).toEqual(decision({ id: 'dd-1' }))
      })

      it.each([[[]], [null], [undefined]])('should return null when the service answers %j', async (answer) => {
        DecisionService.getDecisionList.mockResolvedValue(answer)

        expect(await action(getContext(), 'dd-1')).toBeNull()
      })
    },

    getHistoricDecisionInstances: (action, getContext) => {
      it('should store the instances both per version and flat', async () => {
        const context = getContext()
        DecisionService.getHistoricDecisionInstances.mockResolvedValue([{ id: 'di-1' }])

        const result = await action(context, { key: 'invoice', version: '1', params: { maxResults: 10 } })

        expect(DecisionService.getHistoricDecisionInstances).toHaveBeenCalledWith({ maxResults: 10 })
        expect(context.commit).toHaveBeenCalledWith('setHistoricInstancesForKey', {
          key: 'invoice', version: '1', instances: [{ id: 'di-1' }]
        })
        expect(context.commit).toHaveBeenCalledWith('setHistoricInstances', [{ id: 'di-1' }])
        expect(result).toEqual([{ id: 'di-1' }])
      })
    },

    getHistoricDecisionInstanceCount: (action, getContext) => {
      // KNOWN BUG (pinned, not fixed): `setHistoricCount` is not among this module's
      // mutations, so Vuex logs an unknown-mutation error and the count is dropped.
      it('should commit the non-existent setHistoricCount mutation (known bug)', async () => {
        const context = getContext()
        DecisionService.getHistoricDecisionInstanceCount.mockResolvedValue({ count: 5 })

        const result = await action(context, { key: 'invoice' })

        expect(context.commit).toHaveBeenCalledWith('setHistoricCount', 5)
        expect(DecisionStore.mutations.setHistoricCount).toBeUndefined()
        expect(result).toEqual({ count: 5 })
      })

      it.each([[{}], [null]])('should skip the commit when the response is %j', async (answer) => {
        const context = getContext()
        DecisionService.getHistoricDecisionInstanceCount.mockResolvedValue(answer)

        await action(context, {})

        expect(context.commit).not.toHaveBeenCalled()
      })
    },

    getHistoricDecisionInstanceById: (action, getContext) => {
      // KNOWN BUG (pinned, not fixed): `setHistoricInstance` (singular) is not a mutation
      // of this module either — the existing one is `setSelectedInstance`.
      it('should commit the non-existent setHistoricInstance mutation (known bug)', async () => {
        const context = getContext()
        DecisionService.getHistoricDecisionInstanceById.mockResolvedValue({ id: 'di-1' })

        const result = await action(context, { id: 'di-1', params: { includeInputs: true } })

        expect(DecisionService.getHistoricDecisionInstanceById).toHaveBeenCalledWith('di-1', { includeInputs: true })
        expect(context.commit).toHaveBeenCalledWith('setHistoricInstance', { id: 'di-1' })
        expect(DecisionStore.mutations.setHistoricInstance).toBeUndefined()
        expect(result).toEqual({ id: 'di-1' })
      })
    }
  },

  additional: (storeModule) => {
    // The remaining actions are one-line delegations. A table keeps them verified without
    // twenty near-identical blocks.
    describe('delegating actions', () => {
      const cases = [
        ['getDecisionByKeyAndTenant', { key: 'invoice', tenant: 't-1' }, 'getDecisionByKeyAndTenant', ['invoice', 't-1']],
        ['evaluateByKey', { key: 'invoice', data: { a: 1 } }, 'evaluateByKey', ['invoice', { a: 1 }]],
        ['evaluateByKeyAndTenant', { key: 'invoice', tenant: 't-1', data: { a: 1 } }, 'evaluateByKeyAndTenant', ['invoice', 't-1', { a: 1 }]],
        ['evaluateById', { id: 'dd-1', data: { a: 1 } }, 'evaluateById', ['dd-1', { a: 1 }]],
        ['getDiagramByKey', 'invoice', 'getDiagramByKey', ['invoice']],
        ['getDiagramById', 'dd-1', 'getDiagramById', ['dd-1']],
        ['getDiagramByKeyAndTenant', { key: 'invoice', tenant: 't-1' }, 'getDiagramByKeyAndTenant', ['invoice', 't-1']],
        ['getXmlByKey', 'invoice', 'getXmlByKey', ['invoice']],
        ['getXmlByKeyAndTenant', { key: 'invoice', tenant: 't-1' }, 'getXmlByKeyAndTenant', ['invoice', 't-1']],
        ['getXmlById', 'dd-1', 'getXmlById', ['dd-1']],
        ['updateHistoryTTLByKey', { key: 'invoice', data: { historyTimeToLive: 5 } }, 'updateHistoryTTLByKey', ['invoice', { historyTimeToLive: 5 }]],
        ['updateHistoryTTLByKeyAndTenant', { key: 'invoice', tenant: 't-1', data: { historyTimeToLive: 5 } }, 'updateHistoryTTLByKeyAndTenant', ['invoice', 't-1', { historyTimeToLive: 5 }]],
        ['updateHistoryTTLById', { id: 'dd-1', data: { historyTimeToLive: 5 } }, 'updateHistoryTTLById', ['dd-1', { historyTimeToLive: 5 }]],
        ['deleteHistoricDecisionInstances', { decisionInstanceIds: ['di-1'] }, 'deleteHistoricDecisionInstances', [{ decisionInstanceIds: ['di-1'] }]],
        ['setHistoricDecisionInstanceRemovalTime', { calculatedRemovalTime: true }, 'setHistoricDecisionInstanceRemovalTime', [{ calculatedRemovalTime: true }]]
      ]

      it.each(cases)('%s should call DecisionService.%s with the unpacked payload', async (actionName, payload, serviceMethod, expectedArgs) => {
        const answer = { ok: actionName }
        DecisionService[serviceMethod].mockResolvedValue(answer)

        const result = await storeModule.actions[actionName]({ commit: vi.fn(), state: storeModule.state }, payload)

        expect(DecisionService[serviceMethod]).toHaveBeenCalledWith(...expectedArgs)
        expect(result).toEqual(answer)
      })
    })
  }
})
