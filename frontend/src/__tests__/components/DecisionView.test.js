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
import { DecisionService } from '@/services.js'

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

  it('stops quietly when the tenant has no versions of the key', async () => {
    const getVersions = vi.fn().mockResolvedValue([])
    const c = ctx({ tenantId: 'gone', getDecisionVersionsByKey: getVersions })
    DecisionView.methods.loadDecisionVersionsByKey.call(c, 'dec', '1', false)
    await vi.waitFor(() => expect(getVersions).toHaveBeenCalled())
    await Promise.resolve()
    expect(c.$router.push).not.toHaveBeenCalled()
    expect(c.setSelectedDecisionVersion).not.toHaveBeenCalled()
    expect(c.versionLoaded).toBe(false)
  })

  it('opens the latest version of the tenant when it has no such version number', async () => {
    const latest = { id: 'dd-t1', key: 'dec', version: 2, tenantId: 'T1' }
    const c = ctx({
      tenantId: 'T1',
      getDecisionVersionsByKey: vi.fn().mockResolvedValue([latest]),
      getDecisionVersion: vi.fn(() => null)
    })
    DecisionView.methods.loadDecisionVersionsByKey.call(c, 'dec', '7', false)
    await vi.waitFor(() => expect(c.versionLoaded).toBe(true))
    expect(c.$router.push).toHaveBeenCalledWith({ path: '/seven/auth/decision/dec/2', query: { tenantId: 'T1' } })
    expect(c.setSelectedDecisionVersion).toHaveBeenCalledWith({ key: 'dec', version: '2' })
    expect(DecisionService.getDecisionDefinitionById).toHaveBeenCalledWith('dd-t1', true)
  })

  it('selects the requested version of the tenant', async () => {
    const v1 = { id: 'dd-t1-v1', key: 'dec', version: 1, tenantId: 'T1' }
    const c = ctx({
      tenantId: 'T1',
      getDecisionVersionsByKey: vi.fn().mockResolvedValue([{ ...v1, id: 'dd-t1-v2', version: 2 }, v1]),
      getDecisionVersion: vi.fn(() => v1)
    })
    DecisionView.methods.loadDecisionVersionsByKey.call(c, 'dec', '1', false)
    await vi.waitFor(() => expect(c.versionLoaded).toBe(true))
    expect(c.$router.push).not.toHaveBeenCalled()
    expect(c.setSelectedDecisionVersion).toHaveBeenCalledWith({ key: 'dec', version: '1' })
    expect(DecisionService.getDecisionDefinitionById).toHaveBeenCalledWith('dd-t1-v1', true)
  })

  it('leaves the selection alone when the route version is not in the list yet', () => {
    const c = ctx({
      decisionKey: 'dec',
      versionIndex: '5',
      $store: { state: { decision: { list: [{ key: 'dec' }] } } },
      getDecisionVersion: vi.fn(() => null)
    })
    DecisionView.watch['$route.params.versionIndex'].handler.call(c, '5')
    expect(c.setSelectedDecisionVersion).not.toHaveBeenCalled()
  })
})
