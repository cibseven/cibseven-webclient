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
import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { nextTick } from 'vue'
import { createAppRouter } from '@/router.js'

const Page = { template: '<div />' }

// The same nesting as appRoutes: seven > login, and seven > auth > page > child
const routes = [{
  path: '/seven', name: 'seven', component: Page, children: [
    { path: 'login', name: 'login', component: Page },
    {
      path: 'auth', name: 'auth', component: Page, children: [
        { path: 'tasks', name: 'tasks', component: Page, children: [{ path: ':filterId', name: 'tasklist', component: Page }] },
        { path: 'processes', name: 'processes', component: Page }
      ]
    }
  ]
}]

describe('router focus on page change', () => {
  let router

  beforeEach(async () => {
    document.body.innerHTML = '<a id="nav" href="#">Processes</a><main id="main-content" tabindex="-1"><input id="field"></main>'
    router = createAppRouter(routes)
    await router.push('/seven/auth/tasks/f1')
  })

  afterEach(() => {
    document.body.innerHTML = ''
  })

  async function navigate(path) {
    await router.push(path)
    await nextTick()
  }

  it('leaves the focus alone on the first load', () => {
    expect(document.activeElement).toBe(document.body)
  })

  it('moves the focus to the main content when another page opens', async () => {
    document.getElementById('nav').focus()

    await navigate('/seven/auth/processes')

    expect(document.activeElement.id).toBe('main-content')
  })

  it('moves the focus to the main content after signing in', async () => {
    await navigate('/seven/login')
    document.getElementById('nav').focus()

    await navigate('/seven/auth/tasks/f1')

    expect(document.activeElement.id).toBe('main-content')
  })

  // Switching filters (or tasks, or tabs) is navigation within the page: the keyboard user
  // must stay in the list they are working in.
  it('keeps the focus on navigation within the same page', async () => {
    document.getElementById('nav').focus()

    await navigate('/seven/auth/tasks/f2')

    expect(document.activeElement.id).toBe('nav')
  })

  it('keeps a focus the new view already set inside the main content', async () => {
    document.getElementById('field').focus()

    await navigate('/seven/auth/processes')

    expect(document.activeElement.id).toBe('field')
  })
})
