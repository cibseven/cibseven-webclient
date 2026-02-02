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
import CibSeven from '@/components/CibSeven.vue'

// Mock services
vi.mock('@/services.js', () => ({
  EngineService: {
    getEngines: vi.fn(() => Promise.resolve([{ name: 'default' }]))
  }
}))

describe('CibSeven.vue', () => {
  describe('Methods', () => {
    it('should filter menu items based on show property', () => {
      const items = [
        { show: true, groupTitle: 'Group 1', items: [] },
        { show: false, groupTitle: 'Group 2', items: [] },
        { show: true, groupTitle: 'Group 3', items: [] }
      ]
      
      const result = CibSeven.methods.getVisibleMenuItems(items)
      expect(result.length).toBe(2)
      expect(result[0].groupTitle).toBe('Group 1')
      expect(result[1].groupTitle).toBe('Group 3')
    })

    it('should check if menu item is active based on route path', () => {
      const mockThis = {
        $route: { path: '/seven/auth/tasks/123' }
      }
      
      const activeItem = { active: ['seven/auth/tasks'], to: '/seven/auth/tasks' }
      const inactiveItem = { active: ['seven/auth/processes'], to: '/seven/auth/processes' }
      const noActiveItem = { to: '/seven/auth/admin' }
      
      expect(CibSeven.methods.isMenuItemActive.call(mockThis, activeItem)).toBe(true)
      expect(CibSeven.methods.isMenuItemActive.call(mockThis, inactiveItem)).toBe(false)
      expect(CibSeven.methods.isMenuItemActive.call(mockThis, noActiveItem)).toBe(false)
    })

    it('should check exact match when activeExact is true', () => {
      const mockThis = {
        $route: { path: '/seven/auth/admin' }
      }
      
      const exactItem = { active: ['seven/auth/admin'], activeExact: true }
      expect(CibSeven.methods.isMenuItemActive.call(mockThis, exactItem)).toBe(true)
      
      mockThis.$route.path = '/seven/auth/admin/users'
      expect(CibSeven.methods.isMenuItemActive.call(mockThis, exactItem)).toBe(false)
    })
  })

  describe('Computed Properties', () => {
    it('should generate helpMenuItems with configured links', () => {
      const mockThis = {
        $root: {
          user: { id: '1' },
          config: {
            flowLinkHelp: 'https://help.example.com',
            flowLinkAccessibility: 'https://accessibility.example.com',
            layout: { showSupportInfo: false }
          }
        }
      }
      
      const items = CibSeven.computed.helpMenuItems.call(mockThis)
      expect(items.length).toBeGreaterThan(0)
      expect(items.some(item => item.href === 'https://help.example.com')).toBe(true)
    })
  })
})
