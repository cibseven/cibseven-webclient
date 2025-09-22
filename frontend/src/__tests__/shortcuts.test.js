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
import { describe, it, expect, beforeEach } from 'vitest'
import {
  getEnabledShortcuts,
  getShortcutsForModal,
  getGlobalNavigationShortcuts,
  getTaskEventShortcuts,
  checkKeyMatch
} from '@/utils/shortcuts.js'

describe('shortcuts utilities', () => {
  let mockConfig

  beforeEach(() => {
    mockConfig = {
      shortcuts: {
        global: {
          dashboard: {
            enabled: true,
            keys: ['ctrl', 'd'],
            description: 'Go to dashboard',
            route: '/dashboard'
          },
          tasks: {
            enabled: true,
            keys: ['ctrl', 't'],
            description: 'Go to tasks',
            route: '/tasks'
          },
          disabled: {
            enabled: false,
            keys: ['ctrl', 'x'],
            description: 'Disabled shortcut'
          }
        },
        tasks: {
          complete: {
            enabled: true,
            keys: ['ctrl', 'enter'],
            description: 'Complete task',
            event: 'complete'
          },
          cancel: {
            enabled: true,
            keys: ['esc'],
            description: 'Cancel task',
            event: 'cancel'
          },
          disabled: {
            enabled: false,
            keys: ['ctrl', 'r'],
            description: 'Disabled task shortcut'
          }
        }
      }
    }
  })

  describe('getEnabledShortcuts', () => {
    it('should return enabled shortcuts for valid category', () => {
      const result = getEnabledShortcuts(mockConfig, 'global')
      
      expect(result).toHaveLength(2)
      expect(result[0]).toEqual({
        id: 'dashboard',
        enabled: true,
        keys: ['ctrl', 'd'],
        description: 'Go to dashboard',
        route: '/dashboard'
      })
      expect(result[1]).toEqual({
        id: 'tasks',
        enabled: true,
        keys: ['ctrl', 't'],
        description: 'Go to tasks',
        route: '/tasks'
      })
    })

    it('should filter out disabled shortcuts', () => {
      const result = getEnabledShortcuts(mockConfig, 'global')
      
      expect(result.find(s => s.id === 'disabled')).toBeUndefined()
    })

    it('should return empty array for missing config', () => {
      expect(getEnabledShortcuts(null, 'global')).toEqual([])
      expect(getEnabledShortcuts(undefined, 'global')).toEqual([])
      expect(getEnabledShortcuts({}, 'global')).toEqual([])
    })

    it('should return empty array for missing category', () => {
      expect(getEnabledShortcuts(mockConfig, 'nonexistent')).toEqual([])
    })

    it('should return empty array for missing shortcuts property', () => {
      const configWithoutShortcuts = { other: 'data' }
      expect(getEnabledShortcuts(configWithoutShortcuts, 'global')).toEqual([])
    })
  });

  describe('getShortcutsForModal', () => {
    it('should format shortcuts for modal display', () => {
      const result = getShortcutsForModal(mockConfig, 'global')
      
      expect(result).toHaveLength(2)
      expect(result[0]).toEqual({
        buttons: ['Ctrl', '+', 'D'],
        description: 'Go to dashboard'
      })
      expect(result[1]).toEqual({
        buttons: ['Ctrl', '+', 'T'],
        description: 'Go to tasks'
      })
    })

    it('should handle arrow keys in formatting', () => {
      const configWithArrows = {
        shortcuts: {
          navigation: {
            left: {
              enabled: true,
              keys: ['left'],
              description: 'Move left'
            },
            rightShift: {
              enabled: true,
              keys: ['shift', 'right'],
              description: 'Select right'
            }
          }
        }
      }
      
      const result = getShortcutsForModal(configWithArrows, 'navigation')
      
      expect(result[0].buttons).toEqual(['🠄'])
      expect(result[1].buttons).toEqual(['Shift', '+', '🠆'])
    })

    it('should return empty array for no enabled shortcuts', () => {
      const result = getShortcutsForModal(mockConfig, 'nonexistent')
      expect(result).toEqual([])
    })
  });

  describe('getGlobalNavigationShortcuts', () => {
    it('should return only shortcuts with route property', () => {
      const result = getGlobalNavigationShortcuts(mockConfig)
      
      expect(result).toHaveLength(2)
      expect(result.every(s => s.route)).toBe(true)
    })

    it('should return empty array when no shortcuts have routes', () => {
      const configWithoutRoutes = {
        shortcuts: {
          global: {
            action: {
              enabled: true,
              keys: ['ctrl', 'a'],
              description: 'Some action'
            }
          }
        }
      }
      
      const result = getGlobalNavigationShortcuts(configWithoutRoutes)
      expect(result).toEqual([])
    })
  });

  describe('getTaskEventShortcuts', () => {
    it('should return only shortcuts with event property', () => {
      const result = getTaskEventShortcuts(mockConfig)
      
      expect(result).toHaveLength(2)
      expect(result.every(s => s.event)).toBe(true)
      expect(result[0].event).toBe('complete')
      expect(result[1].event).toBe('cancel')
    })

    it('should return empty array when no shortcuts have events', () => {
      const configWithoutEvents = {
        shortcuts: {
          tasks: {
            action: {
              enabled: true,
              keys: ['ctrl', 'a'],
              description: 'Some action'
            }
          }
        }
      }
      
      const result = getTaskEventShortcuts(configWithoutEvents)
      expect(result).toEqual([])
    })
  });

  describe('checkKeyMatch', () => {
    it('should match modifier keys correctly', () => {
      const ctrlEvent = { ctrlKey: true, altKey: false, shiftKey: false }
      const altEvent = { ctrlKey: false, altKey: true, shiftKey: false }
      const shiftEvent = { ctrlKey: false, altKey: false, shiftKey: true }
      
      expect(checkKeyMatch(ctrlEvent, ['ctrl'])).toBe(true)
      expect(checkKeyMatch(altEvent, ['alt'])).toBe(true)
      expect(checkKeyMatch(shiftEvent, ['shift'])).toBe(true)
      
      expect(checkKeyMatch(ctrlEvent, ['alt'])).toBe(false)
      expect(checkKeyMatch(altEvent, ['ctrl'])).toBe(false)
    })

    it('should match arrow keys correctly', () => {
      const leftEvent = { key: 'ArrowLeft', ctrlKey: false, altKey: false, shiftKey: false }
      const rightEvent = { key: 'ArrowRight', ctrlKey: false, altKey: false, shiftKey: false }
      const upEvent = { key: 'ArrowUp', ctrlKey: false, altKey: false, shiftKey: false }
      const downEvent = { key: 'ArrowDown', ctrlKey: false, altKey: false, shiftKey: false }
      
      expect(checkKeyMatch(leftEvent, ['left'])).toBe(true)
      expect(checkKeyMatch(rightEvent, ['right'])).toBe(true)
      expect(checkKeyMatch(upEvent, ['up'])).toBe(true)
      expect(checkKeyMatch(downEvent, ['down'])).toBe(true)
      
      expect(checkKeyMatch(leftEvent, ['right'])).toBe(false)
    })

    it('should match regular keys by event.key', () => {
      const aEvent = { key: 'a', ctrlKey: false, altKey: false, shiftKey: false }
      const AEvent = { key: 'A', ctrlKey: false, altKey: false, shiftKey: false }
      
      expect(checkKeyMatch(aEvent, ['a'])).toBe(true)
      expect(checkKeyMatch(AEvent, ['a'])).toBe(true)
      expect(checkKeyMatch(AEvent, ['A'])).toBe(true)
    })

    it('should match regular keys by event.code', () => {
      const aEvent = { key: 'a', code: 'KeyA', ctrlKey: false, altKey: false, shiftKey: false }
      const digitEvent = { key: '1', code: 'Digit1', ctrlKey: false, altKey: false, shiftKey: false }
      
      expect(checkKeyMatch(aEvent, ['a'])).toBe(true)
      expect(checkKeyMatch(digitEvent, ['1'])).toBe(true)
    })

    it('should match key combinations', () => {
      const ctrlAEvent = { 
        key: 'a', 
        code: 'KeyA',
        ctrlKey: true, 
        altKey: false, 
        shiftKey: false 
      }
      
      const ctrlShiftLeftEvent = {
        key: 'ArrowLeft',
        ctrlKey: true,
        altKey: false,
        shiftKey: true
      }
      
      expect(checkKeyMatch(ctrlAEvent, ['ctrl', 'a'])).toBe(true)
      expect(checkKeyMatch(ctrlShiftLeftEvent, ['ctrl', 'shift', 'left'])).toBe(true)
      
      expect(checkKeyMatch(ctrlAEvent, ['ctrl', 'b'])).toBe(false)
      expect(checkKeyMatch(ctrlAEvent, ['alt', 'a'])).toBe(false)
    })

    it('should handle empty keys array', () => {
      const event = { key: 'a', ctrlKey: false, altKey: false, shiftKey: false }
      expect(checkKeyMatch(event, [])).toBe(true)
    })

    it('should handle Enter key', () => {
      const enterEvent = { 
        key: 'Enter', 
        code: 'Enter',
        ctrlKey: false, 
        altKey: false, 
        shiftKey: false 
      }
      
      expect(checkKeyMatch(enterEvent, ['enter'])).toBe(true)
      expect(checkKeyMatch(enterEvent, ['Enter'])).toBe(true)
    })

    it('should handle Escape key', () => {
      const escEvent = { 
        key: 'Escape', 
        code: 'Escape',
        ctrlKey: false, 
        altKey: false, 
        shiftKey: false 
      }
      
      expect(checkKeyMatch(escEvent, ['esc'])).toBe(false) // 'esc' != 'Escape'
      expect(checkKeyMatch(escEvent, ['Escape'])).toBe(true)
      expect(checkKeyMatch(escEvent, ['escape'])).toBe(true)
    })
  })
})
