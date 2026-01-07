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
import { findComponents } from './utils.js'
import fs from 'node:fs'
import path from 'node:path'

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

  describe('no direct URL usage', () => {
    // Find all .js files in /src/

    // eslint-disable-next-line no-undef
    const srcDir = path.resolve(__dirname, '../../src')
    const jsFiles = findComponents(srcDir, '.js')
    const vueFiles = findComponents(srcDir, '.vue')
    const allFiles = jsFiles.concat(vueFiles)

    it('no hardcoded URLs in *.js|*.vue files with next() method', () => {
      allFiles.forEach(f => {
        const routerFilePath = path.resolve(srcDir, f)
        const routerContent = fs.readFileSync(routerFilePath, 'utf-8')

        // Regex to match hardcoded URL paths (e.g., '/some/path')
        const hardcodedUrlRegex = /next\(['"`]\/[a-zA-Z0-9/_-]*['"`]/g
        const matches = routerContent.match(hardcodedUrlRegex) || []

        // Filter out valid cases (like import statements or comments)
        const invalidMatches = matches.filter(match => {
          // Exclude import statements and comments
          return !match.includes('import') && !match.startsWith('//') && !match.startsWith('/*')
        })

        const message = invalidMatches.length > 0
          ? `Hardcoded URLs found in ${f}: ${invalidMatches.join(', ')}. Please use named routes instead.`
          : ''

        expect(message).toBe('')
      })
    })

    // No '/seven/auth/start' direct usage
    // TODO unskip when fixing all occurrences (82 as of Nov 2025)
    it.skip('no hardcoded URLs in *.js|*.vue files', () => {
      allFiles.forEach(f => {
        const routerFilePath = path.resolve(srcDir, f)
        const routerContent = fs.readFileSync(routerFilePath, 'utf-8')

        if (f.includes('router.test.js')) {
          return // skip further checks for router.test.js
        }
        const hardcodedStartUrlRegex = /push\(['"`]\/seven\/auth\//g
        const startMatches = routerContent.match(hardcodedStartUrlRegex) || []

        const startMessage = startMatches.length > 0
          ? `Hardcoded push('/seven/auth/...' URL found in ${f}. Please use named routes instead.`
          : ''

        expect(startMessage).toBe('')
      })
    })
  })
})
