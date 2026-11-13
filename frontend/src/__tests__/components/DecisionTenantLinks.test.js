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
import DecisionList from '@/components/decisions/list/DecisionList.vue'
import DecisionVersionListSidebar from '@/components/decision/DecisionVersionListSidebar.vue'
import { appRoutes, createAppRouter } from '@/router.js'

// A decision key can be deployed for several tenants and without a tenant. Each row and each version
// link must carry its own tenant, otherwise every row opens the same definition (CIB7-2392).
describe('Decision links keep the tenant (CIB7-2392)', () => {
  it('opens a list row with its tenant', () => {
    const ctx = { $router: { push: vi.fn() } }
    DecisionList.methods.goToDecision.call(ctx, { key: 'dec', latestVersion: 1, tenantId: 'T1' })
    expect(ctx.$router.push).toHaveBeenCalledWith({ path: '/seven/auth/decision/dec/1', query: { tenantId: 'T1' } })
  })

  it('opens a list row without a tenant without the query', () => {
    const ctx = { $router: { push: vi.fn() } }
    DecisionList.methods.goToDecision.call(ctx, { key: 'dec', latestVersion: 1, tenantId: null })
    expect(ctx.$router.push).toHaveBeenCalledWith({ path: '/seven/auth/decision/dec/1', query: {} })
  })

  it('opens a version from the sidebar with its tenant', () => {
    const ctx = { $router: { push: vi.fn() } }
    DecisionVersionListSidebar.methods.selectVersion.call(ctx, { key: 'dec', version: 2, tenantId: 'T1' })
    expect(ctx.$router.push).toHaveBeenCalledWith({
      name: 'decision-version', params: { decisionKey: 'dec', versionIndex: 2 }, query: { tenantId: 'T1' }
    })
  })

  it('passes the tenant of the query to the decision view', () => {
    const route = createAppRouter(appRoutes).resolve('/seven/auth/decision/dec/1?tenantId=T1')
    const decisionRoute = route.matched.find(r => r.name === 'decision')
    expect(decisionRoute.props.default(route)).toEqual({ decisionKey: 'dec', versionIndex: '1', tenantId: 'T1' })
  })
})
