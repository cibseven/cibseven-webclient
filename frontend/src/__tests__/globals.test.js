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
import { createUUID } from '@/globals'

describe('globals.js', () => {

  describe('createUUID', () => {
    it('creates a valid UUID v4', () => {
      const uuid = createUUID()
      // Check the format of the UUID v4
      const uuidV4Regex = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/
      expect(uuid).toMatch(uuidV4Regex)
    })

    it('creates unique UUIDs', () => {
      const uuidSet = new Set()
      const iterations = 1000
      for (let i = 0; i < iterations; i++) {
        uuidSet.add(createUUID())
      }
      expect(uuidSet.size).toBe(iterations)
    })
  })
})
