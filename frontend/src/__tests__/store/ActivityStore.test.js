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
import { it, expect, vi } from 'vitest'
import ActivityStore from '../../store/ActivityStore.js'
import { HistoryService } from '@/services.js'
import { createStoreTestSuite } from './store-test-utils.js'

// Mock the HistoryService
vi.mock('@/services.js', () => ({
  HistoryService: {
    findActivitiesInstancesHistoryWithFilter: vi.fn()
  }
}))

createStoreTestSuite('ActivityStore', ActivityStore, {
  initialState: (getState) => {
    it('should have empty processActivities array', () => {
      expect(getState().processActivities).toEqual([])
    })

    it('should have empty selectedActivityId', () => {
      expect(getState().selectedActivityId).toBe('')
    })

    it('should have default listMode as "all"', () => {
      expect(getState().selectedActivityInstancesListMode).toBe('all')
    })

    it('should have null highlightedElement', () => {
      expect(getState().highlightedElement).toBeNull()
    })

    it('should have empty activitiesInstanceHistory array', () => {
      expect(getState().activitiesInstanceHistory).toEqual([])
    })

    it('should have null diagramXml', () => {
      expect(getState().diagramXml).toBeNull()
    })
  },

  mutations: {
    setProcessActivities: (mutation, getState) => {
      it('should set process activities', () => {
        const state = getState()
        const activities = [
          { id: '1', name: 'Activity 1' },
          { id: '2', name: 'Activity 2' }
        ]
        mutation(state, activities)
        expect(state.processActivities).toEqual(activities)
      })

      it('should replace existing activities', () => {
        const state = getState()
        state.processActivities = [{ id: 'old', name: 'Old Activity' }]
        const newActivities = [{ id: 'new', name: 'New Activity' }]
        
        mutation(state, newActivities)
        expect(state.processActivities).toEqual(newActivities)
      })
    },

    setSelectedActivityId: (mutation, getState) => {
      it('should set activity id and list mode', () => {
        const state = getState()
        const payload = { activityId: 'activity-1', listMode: 'executed' }
        mutation(state, payload)
        expect(state.selectedActivityId).toBe('activity-1')
        expect(state.selectedActivityInstancesListMode).toBe('executed')
      })

      it('should use default listMode when not provided', () => {
        const state = getState()
        mutation(state, { activityId: 'activity-2' })
        expect(state.selectedActivityId).toBe('activity-2')
        expect(state.selectedActivityInstancesListMode).toBe('all')
      })

      it('should handle empty payload', () => {
        const state = getState()
        mutation(state, {})
        expect(state.selectedActivityId).toBeUndefined()
        expect(state.selectedActivityInstancesListMode).toBe('all')
      })
    },

    setHighlightedElement: (mutation, getState) => {
      it('should set highlighted element', () => {
        const state = getState()
        const element = { id: 'element-1', type: 'activity' }
        mutation(state, element)
        expect(state.highlightedElement).toEqual(element)
      })

      it('should handle null element', () => {
        const state = getState()
        mutation(state, null)
        expect(state.highlightedElement).toBeNull()
      })
    },

    clearActivitySelection: (mutation, getState) => {
      it('should clear all activity selection data', () => {
        const state = getState()
        // Set some data first
        state.selectedActivityId = 'test-id'
        state.selectedActivityInstancesListMode = 'executed'
        state.highlightedElement = { id: 'element-1' }

        mutation(state)

        expect(state.selectedActivityId).toBe('')
        expect(state.selectedActivityInstancesListMode).toBe('all')
        expect(state.highlightedElement).toBeNull()
      })
    },

    setActivitiesInstanceHistory: (mutation, getState) => {
      it('should set activities instance history', () => {
        const state = getState()
        const history = [
          { id: '1', activityId: 'act1', startTime: '2023-01-01' },
          { id: '2', activityId: 'act2', startTime: '2023-01-02' }
        ]
        mutation(state, history)
        expect(state.activitiesInstanceHistory).toEqual(history)
      })
    },

    setDiagramXml: (mutation, getState) => {
      it('should set diagram XML', () => {
        const state = getState()
        const xml = '<bpmn:definitions>...</bpmn:definitions>'
        mutation(state, xml)
        expect(state.diagramXml).toBe(xml)
      })
    }
  },

  getters: {
    selectedActivityId: (getter, getState) => {
      it('should return selected activity id', () => {
        const state = getState()
        state.selectedActivityId = 'test-activity'
        expect(getter(state)).toBe('test-activity')
      })

      it('should return empty string when no activity selected', () => {
        const state = getState()
        state.selectedActivityId = ''
        expect(getter(state)).toBe('')
      })
    },

    selectedActivityInstancesListMode: (getter, getState) => {
      it('should return current list mode', () => {
        const state = getState()
        state.selectedActivityInstancesListMode = 'executed'
        expect(getter(state)).toBe('executed')
      })
    },

    highlightedElement: (getter, getState) => {
      it('should return highlighted element', () => {
        const state = getState()
        const element = { id: 'element-1', type: 'activity' }
        state.highlightedElement = element
        expect(getter(state)).toEqual(element)
      })
    },

    getProcessActivities: (getter, getState) => {
      it('should return process activities', () => {
        const state = getState()
        const activities = [{ id: '1' }, { id: '2' }]
        state.processActivities = activities
        expect(getter(state)).toEqual(activities)
      })
    },

    activitiesInstanceHistory: (getter, getState) => {
      it('should return activities instance history', () => {
        const state = getState()
        const history = [{ id: '1', activityId: 'act1' }]
        state.activitiesInstanceHistory = history
        expect(getter(state)).toEqual(history)
      })
    },

    diagramXml: (getter, getState) => {
      it('should return diagram XML', () => {
        const state = getState()
        const xml = '<bpmn:definitions>...</bpmn:definitions>'
        state.diagramXml = xml
        expect(getter(state)).toBe(xml)
      })
    }
  },

  actions: {
    selectActivity: (action, getContext) => {
      it('should commit setSelectedActivityId with payload', () => {
        const context = getContext()
        const payload = { activityId: 'activity-1', listMode: 'executed' }
        action(context, payload)
        expect(context.commit).toHaveBeenCalledWith('setSelectedActivityId', payload)
      })

      it('should use default payload when none provided', () => {
        const context = getContext()
        action(context)
        expect(context.commit).toHaveBeenCalledWith('setSelectedActivityId', { activityId: 0, listMode: 'all' })
      })
    },

    setHighlightedElement: (action, getContext) => {
      it('should commit setHighlightedElement', () => {
        const context = getContext()
        const element = { id: 'element-1', type: 'activity' }
        action(context, element)
        expect(context.commit).toHaveBeenCalledWith('setHighlightedElement', element)
      })
    },

    clearActivitySelection: (action, getContext) => {
      it('should commit clearActivitySelection', () => {
        const context = getContext()
        action(context)
        expect(context.commit).toHaveBeenCalledWith('clearActivitySelection')
      })
    },

    loadActivitiesInstanceHistory: (action, getContext) => {
      it('should load activities instance history and commit result', async () => {
        const context = getContext()
        const mockData = [
          { id: '1', activityId: 'act1', startTime: '2023-01-01' },
          { id: '2', activityId: 'act2', startTime: '2023-01-02' }
        ]
        const params = { processInstanceId: 'process-1', activityId: 'activity-1' }
        
        HistoryService.findActivitiesInstancesHistoryWithFilter.mockResolvedValue(mockData)

        const result = await action(context, params)

        expect(HistoryService.findActivitiesInstancesHistoryWithFilter).toHaveBeenCalledWith(params)
        expect(context.commit).toHaveBeenCalledWith('setActivitiesInstanceHistory', mockData)
        expect(result).toEqual(mockData)
      })

      it('should handle service errors', async () => {
        const context = getContext()
        const params = { processInstanceId: 'process-1' }
        const error = new Error('Service error')
        
        HistoryService.findActivitiesInstancesHistoryWithFilter.mockRejectedValue(error)

        await expect(action(context, params)).rejects.toThrow('Service error')
        expect(HistoryService.findActivitiesInstancesHistoryWithFilter).toHaveBeenCalledWith(params)
      })
    },

    setDiagramXml: (action, getContext) => {
      it('should commit setDiagramXml', () => {
        const context = getContext()
        const xml = '<bpmn:definitions>...</bpmn:definitions>'
        action(context, xml)
        expect(context.commit).toHaveBeenCalledWith('setDiagramXml', xml)
      })
    }
  }
})