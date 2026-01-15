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
import { test, expect } from '@playwright/test'
import { loginDefault, login, logout, loginFail } from '../helpers/auth.js'

test.describe('Simple tests', () => {
  test('visits the app root url', async ({ page }) => {
    await page.goto('/')
    await expect(page.getByRole('heading', { name: 'CIB seven', level: 1 })).toBeVisible()
  })

  test('login', async ({ page }) => {
    if (process.env.ENV === 'stage') {
      await loginDefault(page)
      await logout(page)
    } else {
      await login(page, 'demo', 'demo')
      await logout(page)

      await login(page, 'mary', 'mary', 'Mary Anne')
      await logout(page)

      await login(page, 'john', 'john', 'John Doe')
      await logout(page)
    }
  })

  test('failed login 1', async ({ page }) => {
    await loginFail(page, 'demo', 'wrong password')
  })

  test('failed login 2', async ({ page }) => {
    await loginFail(page, 'demo-non-existing', 'demo')
  })
})
