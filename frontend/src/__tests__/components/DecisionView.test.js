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
import DecisionView from '@/components/decision/DecisionView.vue'

vi.mock('@/services.js', () => ({
  DecisionService: { getDecisionDefinitionById: vi.fn(() => Promise.resolve(null)) }
}))

describe('DecisionView - sidebar persistence', () => {
  it('updates leftOpen and persists it under the static decision scope', () => {
    const saveLeftOpen = vi.fn()
    const ctx = { saveLeftOpen }
    DecisionView.methods.onLeftOpenChanged.call(ctx, false)
    expect(ctx.leftOpen).toBe(false)
    expect(saveLeftOpen).toHaveBeenCalledWith('decision', false)
  })
})

describe('DecisionView - tenant (CIB7-2392)', () => {
  const ctx = (overrides = {}) => ({
    tenantId: null,
    getDecisionVersionsByKey: vi.fn(),
    getDecisionVersion: vi.fn(),
    setSelectedDecisionVersion: vi.fn(),
    updateVersion: vi.fn(),
    $router: { push: vi.fn() },
    ...overrides
  })

  it('loads the versions of the tenant from the route', () => {
    const c = ctx({ tenantId: 'T1', getDecisionVersionsByKey: vi.fn(() => new Promise(() => {})) })
    DecisionView.methods.loadDecisionVersionsByKey.call(c, 'dec', '1', false)
    expect(c.getDecisionVersionsByKey).toHaveBeenCalledWith({ key: 'dec', lazyLoad: false, tenantId: 'T1' })
  })

  it('keeps the tenant when it opens the latest version', async () => {
    const latest = { id: 'dd-t1', key: 'dec', version: 3, tenantId: 'T1' }
    const c = ctx({ tenantId: 'T1', getDecisionVersionsByKey: vi.fn().mockResolvedValue([latest]) })
    DecisionView.methods.loadDecisionVersionsByKey.call(c, 'dec', '', false)
    await vi.waitFor(() => expect(c.$router.push).toHaveBeenCalled())
    expect(c.$router.push).toHaveBeenCalledWith({ path: '/seven/auth/decision/dec/3', query: { tenantId: 'T1' } })
  })

  it('opens the latest version without a query when the definition has no tenant', async () => {
    const latest = { id: 'dd-none', key: 'dec', version: 2, tenantId: null }
    const c = ctx({ getDecisionVersionsByKey: vi.fn().mockResolvedValue([latest]) })
    DecisionView.methods.loadDecisionVersionsByKey.call(c, 'dec', '', false)
    await vi.waitFor(() => expect(c.$router.push).toHaveBeenCalled())
    expect(c.$router.push).toHaveBeenCalledWith({ path: '/seven/auth/decision/dec/2', query: {} })
  })
})
