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
import { test } from '@playwright/test'
import { LoginPage } from '../fixtures/LoginPage.js'
import process from 'node:process'

test.describe('Auth tests', () => {
  test('visits the app root url @smoke', async ({ page }) => {
    const loginPage = new LoginPage(page)
    await loginPage.goto()
  })

  test('login @auth', async ({ page }) => {

    if (process.env.ENV === 'stage') {
      const loginPage = new LoginPage(page)
      await loginPage.goto()
      const startPage = await loginPage.loginSuccess('demo', 'demo')
      const accountMenu = await startPage.clickAccountMenu()
      await accountMenu.logout()
    } else {
      let loginPage = new LoginPage(page)
      await loginPage.goto()

      let startPage = await loginPage.loginSuccess('demo', 'demo')
      let accountMenu = await startPage.clickAccountMenu()
      loginPage = await accountMenu.logout()
      await loginPage.ensureOnPage()

      startPage = await loginPage.loginSuccess('mary', 'mary')
      accountMenu = await startPage.clickAccountMenu()
      loginPage = await accountMenu.logout()
      await loginPage.ensureOnPage()
    }

  })

  test('loginDefault @auth', async ({ page }) => {
    const loginPage = new LoginPage(page)
    await loginPage.goto()
    const startPage = await loginPage.loginDefault()
    const accountMenu = await startPage.clickAccountMenu()
    await accountMenu.logout()
  })

  test('failed login 1 @auth', async ({ page }) => {
    const loginPage = new LoginPage(page)
    await loginPage.goto()

    const failedLoginPage = await loginPage.loginFail('john', 'wrong password')
    await failedLoginPage.ensureOnPage()
  })

  test('failed login 2 @auth', async ({ page }) => {
    const loginPage = new LoginPage(page)
    await loginPage.goto()

    const failedLoginPage = await loginPage.loginFail('john-non-existing', 'john')
    await failedLoginPage.ensureOnPage()
  })
})
