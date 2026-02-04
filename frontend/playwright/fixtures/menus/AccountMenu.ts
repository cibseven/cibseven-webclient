import type { Page, Locator } from '@playwright/test'
import { BaseMenu } from './BaseMenu'
import { LoginPage } from '../LoginPage'

export class AccountMenu extends BaseMenu {

  constructor(public readonly page: Page) {
    super(page)
  }

  async toggle() {
    await this.page.locator('.mdi-account').locator('xpath=..').click()
  }

  async logout() {
    await this.page.locator('.dropdown-menu-end .dropdown-item').filter({ hasText: 'Logout' }).click()
    return new LoginPage(this.page)
  }
}
