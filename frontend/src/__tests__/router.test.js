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

  // The tabs of the profile pages were once picked with ?tab=, which old links and bookmarks still carry
  describe('profile page tabs', () => {
    let router

    // The guards read the module's router, which every createAppRouter call replaces
    beforeEach(() => {
      router = createAppRouter(appRoutes)
      router.setRoot({
        user: { id: 'demo' },
        config: { permissions: {}, layout: { showUserSettings: true } },
        applicationPermissions: () => true
      })
    })

    it.each([
      ['/seven/auth/account/demo?tab=preferences', 'accountPreferences', '/seven/auth/account/demo/preferences'],
      ['/seven/auth/account/demo', 'accountProfile', '/seven/auth/account/demo/profile'],
      // A tab this deployment does not have (e.g. one of an extension) opens the default one
      ['/seven/auth/account/demo?tab=unknown', 'accountProfile', '/seven/auth/account/demo/profile'],
      ['/seven/auth/admin/user/john?tab=groups', 'adminUserGroups', '/seven/auth/admin/user/john/groups'],
      ['/seven/auth/admin/group/sales', 'adminGroupInformation', '/seven/auth/admin/group/sales/information'],
      ['/seven/auth/admin/group/sales?tab=tenants', 'adminGroupTenants', '/seven/auth/admin/group/sales/tenants']
    ])('opens %s as the tab %s', async (link, name, path) => {
      await router.push(link)
      expect(router.currentRoute.value).toMatchObject({ name, path, query: {} })
    })

    it('keeps the rest of the query and the hash', async () => {
      await router.push('/seven/auth/account/demo?tab=groups&lang=de#top')
      expect(router.currentRoute.value).toMatchObject({ name: 'accountGroups', query: { lang: 'de' }, hash: '#top' })
    })

    it('opens a tab by its own link without a redirect', async () => {
      await router.push({ name: 'adminUserTenants', params: { userId: 'john' } })
      expect(router.currentRoute.value.fullPath).toBe('/seven/auth/admin/user/john/tenants')
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
    it.todo('no hardcoded URLs in *.js|*.vue files', () => {
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
