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
import { describe, it, expect } from 'vitest'
import { appRoutes, createAppRouter } from '@/router.js'

describe('router', () => {

  describe('routes', () => {
    it('no empty names', () => {

      const checkRoutes = (routeList) => {
        routeList.forEach((route) => {
          expect(route.name, `Route with path "${route.path}" is missing a name`).toBeDefined()
          expect(route.name).not.toBe('')
          expect(typeof route.name).toBe('string')
          expect(route.name.length).toBeGreaterThan(0)

          // Recursively check nested routes if they exist
          if (route.children && route.children.length > 0) {
            checkRoutes(route.children)
          }
        })
      }

      const router = createAppRouter(appRoutes);
      const routes = router.getRoutes()
      expect(routes.length).toBeGreaterThan(0)
      checkRoutes(routes)
    })

    it('unique names', () => {
      const nameSet = new Set()
      const duplicateNames = new Set()

      const checkRoutes = (routeList) => {
        routeList.forEach((route) => {
          expect(route.name, `Route with path "${route.path}" is missing a name`).toBeDefined()

          if (route.name) {
            if (nameSet.has(route.name)) {
              duplicateNames.add(route.name)
            } else {
              nameSet.add(route.name)
            }
          }

          // Recursively check nested routes if they exist
          if (route.children && route.children.length > 0) {
            checkRoutes(route.children)
          }
        })
      }

      // Check the defined routes (appRoutes), not the internal router records
      expect(appRoutes.length).toBeGreaterThan(0)
      checkRoutes(appRoutes)

      expect(duplicateNames.size, `Duplicate route names found: ${Array.from(duplicateNames).join(', ')}`).toBe(0)
    })
  })

})
