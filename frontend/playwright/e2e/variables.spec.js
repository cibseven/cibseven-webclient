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
import { loginDefault, logout } from '../helpers/auth.js'

test.describe('Process instance variables tests', () => {
  test('add/remove variable', async ({ page }) => {
    await loginDefault(page)

    await page.goto('#/seven/auth/processes/list')

    if (process.env.ENV === 'stage') {
      // filter test process definition on stage
      await page.locator('.form-control-plaintext').fill('test')

      // sort ASCENDING by number of instances
      // NOTE: These selectors are fragile and should be improved with data-testid attributes
      await page.locator(':nth-child(2) > .d-flex > .sort-icon > .mdi').click()
      // sort DESCENDING by number of instances
      await page.locator('span > .mdi').click()
    }

    // click any first process definition to see its instances
    // NOTE: These selectors are fragile and should be improved with data-testid attributes
    await page.locator(':nth-child(1) > :nth-child(3) > .text-truncate').click()

    // click any first process instance to see its variables
    // NOTE: These selectors are fragile and should be improved with data-testid attributes
    await page.locator(':nth-child(1) > :nth-child(5) > .text-truncate').click()

    // create random variable name
    const variableName = `__cy_test__variable_${Math.floor(Math.random() * 1000000)}`

    // verify table first column has no `variableName` variable
    const tableBody = page.locator('.overflow-y-scroll > .table > tbody')
    await expect(tableBody).not.toContainText(variableName)

    // click add variable button
    await page.locator('.p-3 > .btn').click()

    // fill the form and submit
    const formModal = page.locator('.modal.show > .modal-dialog > .modal-content')
    await formModal.locator('input.form-control').fill(variableName)
    await formModal.locator('textarea.form-control').fill('variable data')
    await formModal.locator('button.btn-primary').click()

    // verify table first column has `variableName` variable
    await expect(tableBody).toContainText(variableName)

    // delete the created variable
    // find the row with the variable name and click delete button in that row
    const variableRow = tableBody.locator('tr').filter({ hasText: variableName })
    await variableRow.locator('.mdi-delete-outline').click()

        // Wait for navigation to complete
    await page.waitForLoadState('networkidle')

    await page.getByRole('button', { name: 'Delete' }).click()
    // or 
    // await page.locator('.modal.fade.show > .modal-dialog > .modal-content > .modal-footer > div > div >button.btn.btn-primary').click()

    // Wait for navigation to complete
    await page.waitForLoadState('networkidle')

    // verify table first column has no `variableName` variable
    await expect(tableBody).not.toContainText(variableName)

    await logout(page)
  })
})
