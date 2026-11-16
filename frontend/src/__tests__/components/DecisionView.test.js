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
import { describe, it, expect, vi, afterEach } from 'vitest'
import DecisionView from '@/components/decision/DecisionView.vue'
import { DecisionService } from '@/services.js'

vi.mock('@/services.js', () => ({
  DecisionService: {
    getDecisionDefinitionById: vi.fn(() => Promise.resolve(null)),
    getHistoricDecisionInstances: vi.fn(() => Promise.resolve([])),
    getDecisionVersionsByKey: vi.fn(() => Promise.resolve([]))
  }
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
  const { loadDecisionVersionsByKey, tenantToOpen, replaceTenant, selectVersion } = DecisionView.methods

  // The view's methods on a plain context: the route, the router and the store actions are stubs
  const ctx = ({ params = {}, query = {}, versions = [], ...overrides } = {}) => {
    const c = {
      tenantId: query.tenantId ?? null,
      loadRequest: 0,
      versionLoaded: false,
      $route: { params, query },
      $router: { replace: vi.fn() },
      getDecisionVersionsByKey: vi.fn().mockResolvedValue(versions),
      getDecisionVersion: vi.fn(({ version }) => versions.find(v => String(v.version) === version) ?? null),
      setSelectedDecisionVersion: vi.fn(),
      updateVersion: vi.fn(),
      ...overrides
    }
    return Object.assign(c, { tenantToOpen, replaceTenant, selectVersion })
  }
  const load = (c, versionIndex) => loadDecisionVersionsByKey.call(c, 'dec', versionIndex, false)
  const v = (version, tenantId = null) => ({ id: `dd-${tenantId ?? 'none'}-${version}`, key: 'dec', version, tenantId })

  afterEach(() => vi.clearAllMocks())

  it('loads the versions of the tenant in the link and selects the requested one', async () => {
    const c = ctx({ query: { tenantId: 'T1' }, versions: [v(2, 'T1'), v(1, 'T1')] })

    await load(c, '1')

    expect(c.getDecisionVersionsByKey).toHaveBeenCalledWith({ key: 'dec', lazyLoad: false, tenantId: 'T1' })
    expect(DecisionService.getHistoricDecisionInstances).not.toHaveBeenCalled()
    expect(c.$router.replace).not.toHaveBeenCalled()
    expect(c.setSelectedDecisionVersion).toHaveBeenCalledWith({ key: 'dec', version: '1' })
    expect(DecisionService.getDecisionDefinitionById).toHaveBeenCalledWith('dd-T1-1', true)
    expect(c.versionLoaded).toBe(true)
  })

  it('loads the versions without a tenant for a link without one', async () => {
    const c = ctx({ versions: [v(2)] })

    await load(c, '2')

    expect(c.getDecisionVersionsByKey).toHaveBeenCalledWith({ key: 'dec', lazyLoad: false, tenantId: null })
    expect(c.setSelectedDecisionVersion).toHaveBeenCalledWith({ key: 'dec', version: '2' })
  })

  it('opens the latest version of the tenant in place, keeping the query, when it has no such version', async () => {
    const c = ctx({ params: { decisionKey: 'dec', versionIndex: '7' }, query: { tenantId: 'T1', tab: 'details' }, versions: [v(2, 'T1')] })

    await load(c, '7')

    expect(c.$router.replace).toHaveBeenCalledWith({
      name: 'decision-version',
      params: { decisionKey: 'dec', versionIndex: '2' },
      query: { tenantId: 'T1', tab: 'details' }
    })
    expect(c.setSelectedDecisionVersion).toHaveBeenCalledWith({ key: 'dec', version: '2' })
    expect(c.versionLoaded).toBe(true)
  })

  // Review point 1: an instance link without the tenant used to land on another definition
  it("takes the tenant of an instance link from the instance, keeping the rest of the link", async () => {
    DecisionService.getHistoricDecisionInstances.mockResolvedValueOnce([{ id: 'inst-1', tenantId: 'T1' }])
    const c = ctx({ params: { decisionKey: 'dec', versionIndex: '5', instanceId: 'inst-1' }, query: { tab: 'details' } })

    await load(c, '5')

    expect(DecisionService.getHistoricDecisionInstances).toHaveBeenCalledWith({ decisionInstanceId: 'inst-1', maxResults: 1 })
    expect(c.$router.replace).toHaveBeenCalledWith({ query: { tab: 'details', tenantId: 'T1' } })
    // the tenant watcher loads again once the link carries the tenant
    expect(c.getDecisionVersionsByKey).not.toHaveBeenCalled()
  })

  it('removes a wrong tenant from an instance link whose instance has none', async () => {
    DecisionService.getHistoricDecisionInstances.mockResolvedValueOnce([{ id: 'inst-1', tenantId: null }])
    const c = ctx({ params: { instanceId: 'inst-1' }, query: { tenantId: 'T1' } })

    await load(c, '5')

    expect(c.$router.replace).toHaveBeenCalledWith({ query: {} })
  })

  it('loads the instance link as it is when the tenant matches the instance', async () => {
    DecisionService.getHistoricDecisionInstances.mockResolvedValueOnce([{ id: 'inst-1', tenantId: 'T1' }])
    const c = ctx({ params: { instanceId: 'inst-1' }, query: { tenantId: 'T1' }, versions: [v(5, 'T1')] })

    await load(c, '5')

    expect(c.$router.replace).not.toHaveBeenCalled()
    expect(c.setSelectedDecisionVersion).toHaveBeenCalledWith({ key: 'dec', version: '5' })
  })

  // Review point 4: an outdated tenant used to leave an empty page
  it('drops a tenant that has no version of the key any more', async () => {
    const c = ctx({ query: { tenantId: 'gone', tab: 'details' } })

    await load(c, '1')

    expect(c.$router.replace).toHaveBeenCalledWith({ query: { tab: 'details' } })
    expect(c.versionLoaded).toBe(false)
  })

  it('opens the tenant of the newest version of a key deployed only for tenants', async () => {
    DecisionService.getDecisionVersionsByKey.mockResolvedValueOnce([v(3, 'T2'), v(1, 'T1')])
    const c = ctx()

    await load(c, '')

    expect(DecisionService.getDecisionVersionsByKey).toHaveBeenCalledWith('dec', true)
    expect(c.$router.replace).toHaveBeenCalledWith({ query: { tenantId: 'T2' } })
  })

  it('stays quiet for a key that does not exist', async () => {
    const c = ctx()

    await load(c, '1')

    expect(c.$router.replace).not.toHaveBeenCalled()
    expect(c.versionLoaded).toBe(false)
  })

  // Review point 5: an answer to an older load must not overwrite a newer one
  it('ignores the answer to a load that a newer one replaced', async () => {
    let answerFirst
    const c = ctx({ query: { tenantId: 'T1' }, versions: [v(1, 'T1')] })
    c.getDecisionVersionsByKey = vi.fn()
      .mockImplementationOnce(() => new Promise(resolve => { answerFirst = resolve }))
      .mockResolvedValueOnce([v(1, 'T1')])

    const first = load(c, '1')
    await vi.waitFor(() => expect(c.getDecisionVersionsByKey).toHaveBeenCalledTimes(1))
    await load(c, '1')
    answerFirst([v(9, 'T9')])
    await first

    expect(c.setSelectedDecisionVersion).toHaveBeenCalledTimes(1)
    expect(c.setSelectedDecisionVersion).toHaveBeenCalledWith({ key: 'dec', version: '1' })
  })

  describe('route watcher', () => {
    const watcher = DecisionView.watch.routeSelection

    it('loads again when the tenant changes, even if the version changes with it', () => {
      const c = { decisionKey: 'dec', versionIndex: '3', $root: { config: { lazyLoadHistory: true } }, loadDecisionVersionsByKey: vi.fn(), getDecisionVersion: vi.fn() }

      watcher.call(c, { tenantId: 'T2', versionIndex: '3' }, { tenantId: 'T1', versionIndex: '2' })

      expect(c.loadDecisionVersionsByKey).toHaveBeenCalledWith('dec', '3', true)
      expect(c.getDecisionVersion).not.toHaveBeenCalled()
    })

    it('only selects another version of the same tenant', () => {
      const version3 = v(3, 'T1')
      const c = { decisionKey: 'dec', loadDecisionVersionsByKey: vi.fn(), getDecisionVersion: vi.fn(() => version3), selectVersion: vi.fn() }

      watcher.call(c, { tenantId: 'T1', versionIndex: '3' }, { tenantId: 'T1', versionIndex: '2' })

      expect(c.loadDecisionVersionsByKey).not.toHaveBeenCalled()
      expect(c.selectVersion).toHaveBeenCalledWith('dec', version3)
    })

    it('leaves the selection alone when the version is not in the list', () => {
      const c = { decisionKey: 'dec', loadDecisionVersionsByKey: vi.fn(), getDecisionVersion: vi.fn(() => null), selectVersion: vi.fn() }

      watcher.call(c, { tenantId: null, versionIndex: '5' }, { tenantId: null, versionIndex: '2' })

      expect(c.selectVersion).not.toHaveBeenCalled()
    })
  })
})
