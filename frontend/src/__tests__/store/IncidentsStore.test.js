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
import { it, expect, vi, beforeEach } from 'vitest'
import IncidentsStore from '../../store/IncidentsStore.js'
import { IncidentService } from '@/services.js'
import { createStoreTestSuite } from './store-test-utils.js'

vi.mock('@/services.js', () => ({
  IncidentService: {
    findIncidents: vi.fn(),
    fetchHistoricIncidents: vi.fn()
  }
}))

const incident = (overrides = {}) => ({
  id: 'inc-1',
  configuration: 'job-1',
  incidentMessage: 'boom',
  annotation: null,
  ...overrides
})

beforeEach(() => {
  vi.clearAllMocks()
})

createStoreTestSuite('IncidentsStore', IncidentsStore, {
  initialState: (getState) => {
    it('should start with no incidents', () => {
      expect(getState().incidents).toEqual([])
    })
  },

  mutations: {
    setIncidents: (mutation, getState) => {
      it('should replace the incident list', () => {
        const state = getState()
        state.incidents = [incident({ id: 'old' })]

        mutation(state, [incident({ id: 'new' })])

        expect(state.incidents).toEqual([incident({ id: 'new' })])
      })
    },

    removeIncident: (mutation, getState) => {
      it('should remove by incident id', () => {
        const state = getState()
        state.incidents = [incident({ id: 'a' }), incident({ id: 'b', configuration: 'job-b' })]

        mutation(state, 'a')

        expect(state.incidents.map(i => i.id)).toEqual(['b'])
      })

      // Callers that only know the job id (the incident's `configuration`) must be able to
      // remove it too — that is how retry/delete-job flows clear the incident.
      it('should remove by configuration (job) id', () => {
        const state = getState()
        state.incidents = [incident({ id: 'a', configuration: 'job-a' }), incident({ id: 'b', configuration: 'job-b' })]

        mutation(state, 'job-b')

        expect(state.incidents.map(i => i.id)).toEqual(['a'])
      })

      it('should leave the list untouched for an unknown id', () => {
        const state = getState()
        state.incidents = [incident({ id: 'a' })]

        mutation(state, 'missing')

        expect(state.incidents.map(i => i.id)).toEqual(['a'])
      })
    },

    updateIncidentAnnotation: (mutation, getState) => {
      it('should set the annotation on the matching incident', () => {
        const state = getState()
        state.incidents = [incident({ id: 'a' }), incident({ id: 'b' })]

        mutation(state, { incidentId: 'b', annotation: 'looked at it' })

        expect(state.incidents.find(i => i.id === 'b').annotation).toBe('looked at it')
        expect(state.incidents.find(i => i.id === 'a').annotation).toBeNull()
      })

      it('should be a no-op when the incident is not present', () => {
        const state = getState()
        state.incidents = [incident({ id: 'a' })]

        expect(() => mutation(state, { incidentId: 'missing', annotation: 'x' })).not.toThrow()
        expect(state.incidents.find(i => i.id === 'a').annotation).toBeNull()
      })
    }
  },

  getters: {
    incidents: (getter, getState) => {
      it('should expose the incident list', () => {
        const state = getState()
        state.incidents = [incident()]
        expect(getter(state)).toEqual([incident()])
      })
    }
  },

  actions: {
    loadRuntimeIncidents: (action, getContext) => {
      it('should append fetched runtime incidents to the existing list', async () => {
        const context = getContext()
        context.state.incidents = [incident({ id: 'existing' })]
        IncidentService.findIncidents.mockResolvedValue([incident({ id: 'fetched' })])

        const result = await action(context, { processInstanceId: 'pi-1' })

        expect(IncidentService.findIncidents).toHaveBeenCalledWith({ processInstanceId: 'pi-1' })
        expect(context.commit).toHaveBeenCalledWith('setIncidents', [
          incident({ id: 'existing' }),
          incident({ id: 'fetched' })
        ])
        expect(result).toEqual([incident({ id: 'fetched' })])
      })

      // The `...response || []` guard exists because the endpoint answers 204 with no body.
      it('should keep the existing list when the service returns nothing', async () => {
        const context = getContext()
        context.state.incidents = [incident({ id: 'existing' })]
        IncidentService.findIncidents.mockResolvedValue(undefined)

        await action(context, {})

        expect(context.commit).toHaveBeenCalledWith('setIncidents', [incident({ id: 'existing' })])
      })

      it('should propagate service failures', async () => {
        const context = getContext()
        IncidentService.findIncidents.mockRejectedValue(new Error('Service error'))

        await expect(action(context, {})).rejects.toThrow('Service error')
      })
    },

    loadHistoryIncidents: (action, getContext) => {
      it('should append fetched historic incidents to the existing list', async () => {
        const context = getContext()
        context.state.incidents = [incident({ id: 'existing' })]
        IncidentService.fetchHistoricIncidents.mockResolvedValue([incident({ id: 'historic' })])

        const result = await action(context, { processInstanceId: 'pi-1' })

        expect(IncidentService.fetchHistoricIncidents).toHaveBeenCalledWith({ processInstanceId: 'pi-1' })
        expect(context.commit).toHaveBeenCalledWith('setIncidents', [
          incident({ id: 'existing' }),
          incident({ id: 'historic' })
        ])
        expect(result).toEqual([incident({ id: 'historic' })])
      })

      it('should keep the existing list when the service returns nothing', async () => {
        const context = getContext()
        context.state.incidents = [incident({ id: 'existing' })]
        IncidentService.fetchHistoricIncidents.mockResolvedValue(null)

        await action(context, {})

        expect(context.commit).toHaveBeenCalledWith('setIncidents', [incident({ id: 'existing' })])
      })
    },

    setIncidents: (action, getContext) => {
      it('should forward the list to the mutation', () => {
        const context = getContext()
        action(context, [incident()])
        expect(context.commit).toHaveBeenCalledWith('setIncidents', [incident()])
      })
    },

    removeIncident: (action, getContext) => {
      it('should forward the id to the mutation', () => {
        const context = getContext()
        action(context, 'inc-1')
        expect(context.commit).toHaveBeenCalledWith('removeIncident', 'inc-1')
      })
    },

    updateIncidentAnnotation: (action, getContext) => {
      it('should forward the annotation payload to the mutation', () => {
        const context = getContext()
        action(context, { incidentId: 'inc-1', annotation: 'note' })
        expect(context.commit).toHaveBeenCalledWith('updateIncidentAnnotation', { incidentId: 'inc-1', annotation: 'note' })
      })
    }
  }
})
