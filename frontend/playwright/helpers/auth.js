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
import process from 'node:process'

/**
 * Login with environment-aware default credentials
 * @param {import('@playwright/test').Page} page
 */
export async function loginDefault(page) {
  const env = process.env.ENV
  // console.log('process.env.ENV=', env)

  if (env === 'stage') {
    const username = process.env.username
    const password = process.env.password
    await login(page, username, password)
  } else {
    await login(page, 'demo', 'demo', 'Demo Demo')
  }
}

/**
 * Login with specified credentials
 * @param {import('@playwright/test').Page} page
 * @param {string} username
 * @param {string} password
 * @param {string} [displayName] - Expected display name after login (defaults to capitalized username)
 */
export async function login(page, username, password, displayName) {
  await page.goto('#/seven/login?_=' + Date.now(), { waitUntil: 'networkidle' })
  
  // Wait for the login form to be fully ready
  await page.locator('button[type=submit]').waitFor({ state: 'visible' })
  
  // Fill in username - using more robust selectors
  await page.locator(':nth-child(3) > form > :nth-child(1) > .row > .col > .form-control').fill(username)

  // Fill in password
  await page.locator('.input-group > .form-control').fill(password)

  // Click login button
  await page.locator('button[type=submit]').click()

  // Wait for navigation to complete
  await page.waitForLoadState('networkidle')

  // Capitalize username for user name display
  const capitalize = (s) => s && s[0].toUpperCase() + s.slice(1)
  displayName = displayName || capitalize(username)

  // Wait for the user account dropdown with the account icon to be visible
  const accountDropdown = page.locator('.mdi-account').locator('xpath=..')
  await accountDropdown.waitFor({ state: 'visible', timeout: 1000 })

  // Check that the user name is correct
  await accountDropdown.locator(`text=${displayName}`).waitFor({ state: 'visible' })
}

/**
 * Logout from the application
 * @param {import('@playwright/test').Page} page
 */
export async function logout(page) {
  // Click the account dropdown
  await page.locator('.mdi-account').locator('xpath=..').click()

  // Click logout
  await page.locator('.dropdown-menu-end .dropdown-item').filter({ hasText: 'Logout' }).click()

  // Verify we're back at login page
  await page.locator('button[type=submit]').filter({ hasText: 'Login' }).waitFor({ state: 'visible' })
  await page.getByRole('heading', { name: 'CIB seven', level: 1 }).waitFor({ state: 'visible' })
}

/**
 * Attempt login with credentials and verify failure
 * @param {import('@playwright/test').Page} page
 * @param {string} email
 * @param {string} password
 */
export async function loginFail(page, email, password) {
  await page.goto('#/seven/login')
  
  // Wait for the login form to be fully ready
  await page.locator('button[type=submit]').waitFor({ state: 'visible' })

  // Fill in username - using more robust selectors
  await page.locator(':nth-child(3) > form > :nth-child(1) > .row > .col > .form-control').first().fill(email)

  // Fill in password
  await page.locator('.input-group > .form-control').fill(password)

  // Click login button
  await page.locator('button[type=submit]').click()

  // Verify error message is visible
  const errorMessage = page.locator('text=/Authentication data is wrong|Username or password is incorrect/')
  await errorMessage.waitFor({ state: 'visible' })

  await page.locator('.modal.fade.show > .modal-dialog > .modal-content > .modal-header > button.btn-close').click()

  // Verify error message is no longer visible
  const count = await page.locator('.modal.fade.show > .modal-dialog > .modal-content > .modal-header > button.btn-close').count()
  if (count > 0) {
    // failed to close the dialog
    throw new Error('Failed to close the error dialog after failed login')
  }
}
