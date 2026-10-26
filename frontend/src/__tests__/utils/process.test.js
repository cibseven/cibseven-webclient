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
import { isStartableInTasklist, hasStartableProcesses } from '@/utils/process.js'

const startable = { startableInTasklist: true, revoked: false, suspended: 'false' }

describe('isStartableInTasklist', () => {
  it('accepts a startable, active process', () => {
    expect(isStartableInTasklist(startable)).toBe(true)
  })

  it.each([
    ['not flagged startable', { ...startable, startableInTasklist: false }],
    ['missing the startable flag', { revoked: false, suspended: 'false' }],
    ['revoked', { ...startable, revoked: true }],
    ['suspended', { ...startable, suspended: 'true' }]
  ])('rejects a process that is %s', (_, process) => {
    expect(isStartableInTasklist(process)).toBe(false)
  })
})

describe('hasStartableProcesses', () => {
  it('is true when at least one process is startable', () => {
    expect(hasStartableProcesses([{ ...startable, suspended: 'true' }, startable])).toBe(true)
  })

  it('is false when none is startable', () => {
    expect(hasStartableProcesses([{ ...startable, revoked: true }])).toBe(false)
  })

  it('is false for a list that is not loaded yet', () => {
    expect(hasStartableProcesses(null)).toBe(false)
    expect(hasStartableProcesses(undefined)).toBe(false)
  })
})
