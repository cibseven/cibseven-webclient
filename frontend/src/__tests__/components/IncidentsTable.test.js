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

import { describe, it, expect, vi } from 'vitest'
import IncidentsTable from '@/components/process/tables/IncidentsTable.vue'

vi.mock('@/services.js', () => ({
  IncidentService: {
    findIncidents: vi.fn(),
    findIncidentsCount: vi.fn(),
    fetchHistoricIncidents: vi.fn(),
    fetchHistoricIncidentsCount: vi.fn(),
  }
}))

describe('IncidentsTable - filtering by the selected activity', () => {
  // The engine leaves failedActivityId empty in the incidents of external tasks, so selecting an
  // external task in the diagram emptied the list. Everything else keeps matching by failedActivityId.
  describe('activityFilterKey', () => {
    const key = (selectedActivityId, externalTaskActivityIds) =>
      IncidentsTable.computed.activityFilterKey.call({ selectedActivityId, externalTaskActivityIds })

    it('is activityId for an external task', () => {
      expect(key('charge', ['charge', 'ship'])).toBe('activityId')
    })

    it.each([
      ['a job activity', 'approve', ['charge']],
      ['a diagram without external tasks', 'approve', []],
    ])('is failedActivityId for %s', (_, selectedActivityId, externalTaskActivityIds) => {
      expect(key(selectedActivityId, externalTaskActivityIds)).toBe('failedActivityId')
    })
  })

  describe('loadIncidentsData', () => {
    const context = (overrides = {}) => {
      const vm = {
        firstResult: 0,
        maxResults: 50,
        currentSortBy: 'incidentType',
        currentSortDesc: false,
        freeText: '',
        selectedActivityId: '',
        activityFilterKey: 'failedActivityId',
        isInstanceView: true,
        incidents: [],
        setIncidents: vi.fn(),
        fetchCount: vi.fn(async () => 1),
        fetch: vi.fn(async () => []),
        ...overrides
      }
      return vm
    }
    const sentParams = (vm) => vm.fetch.mock.calls[0][0]

    it('sends no activity filter when no activity is selected', async () => {
      const vm = context()

      await IncidentsTable.methods.loadIncidentsData.call(vm, 'pi1', true)

      expect(sentParams(vm)).toEqual({
        firstResult: 0, maxResults: 50, sortBy: 'incidentType', sortOrder: 'desc', processInstanceId: 'pi1'
      })
    })

    it('filters by activityId when an external task is selected', async () => {
      const vm = context({ selectedActivityId: 'charge', activityFilterKey: 'activityId' })

      await IncidentsTable.methods.loadIncidentsData.call(vm, 'pi1', true)

      expect(sentParams(vm)).toMatchObject({ activityId: 'charge', processInstanceId: 'pi1' })
      expect(sentParams(vm)).not.toHaveProperty('failedActivityId')
      // the count uses the same filter as the list
      expect(vm.fetchCount.mock.calls[0][0]).toMatchObject({ activityId: 'charge' })
    })

    it('filters by failedActivityId when any other activity is selected', async () => {
      const vm = context({ selectedActivityId: 'approve' })

      await IncidentsTable.methods.loadIncidentsData.call(vm, 'def1', false)

      expect(sentParams(vm)).toMatchObject({ failedActivityId: 'approve', processDefinitionId: 'def1' })
      expect(sentParams(vm)).not.toHaveProperty('activityId')
    })
  })
})
