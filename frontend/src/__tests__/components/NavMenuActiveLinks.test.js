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
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import NavCluster from '@/components/common-components/navigation/NavCluster.vue'
import NavMobileMenu from '@/components/common-components/navigation/NavMobileMenu.vue'

// Regression: the user menu's account entries all lead to one page and differ only in ?tab=. The app's
// router marks links with linkActiveClass 'active' while ignoring the query, so every entry was marked.
const account = (tab, active = false) => ({
  key: tab, label: tab, active,
  to: { name: 'account', params: { userId: 'ada' }, query: { tab } }
})
const user = { id: 'user', label: 'Ada', avatar: 'A', sections: [[account('profile', true), account('groups'), account('preferences')]] }

const mountWithRouter = async (component, props) => {
  const router = createRouter({
    history: createMemoryHistory(),
    linkActiveClass: 'active',
    routes: [{ path: '/', component: { render: () => null } }, { path: '/account/:userId', name: 'account', component: { render: () => null } }]
  })
  router.push({ name: 'account', params: { userId: 'ada' }, query: { tab: 'profile' } })
  await router.isReady()
  const wrapper = mount(component, { props, global: { plugins: [router], mocks: { $t: key => key } } })
  await flushPromises()
  return wrapper
}

describe('menu links are marked by the menu, not by the router', () => {
  it('marks only the open account tab in the desktop menu', async () => {
    const wrapper = await mountWithRouter(NavCluster, { cluster: user, open: true })
    const items = wrapper.findAll('.cib-nav-menu-item')
    expect(items.map(item => item.classes('active'))).toEqual([true, false, false])
  })

  it('marks only the open account tab in the mobile menu', async () => {
    const wrapper = await mountWithRouter(NavMobileMenu, { utility: [user] })
    await wrapper.find('.cib-nav-mobile-row').trigger('click')
    const items = wrapper.findAll('.cib-nav-mobile-item')
    expect(items.map(item => item.classes('active'))).toEqual([true, false, false])
  })
})
