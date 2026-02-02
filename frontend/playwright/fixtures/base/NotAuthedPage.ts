import type { Page, Locator } from '@playwright/test';
import { BasePage } from './BasePage';
import { LoginPage } from '../LoginPage';

export class NotAuthedPage extends BasePage {
  constructor(public readonly page: Page) {
    super(page)
  }

  async ensureOnPage() {
    await super.ensureOnPage()
  }

  async clickHome(): Promise<LoginPage> {
    await super.clickHome()
    const loginPage = new LoginPage(this.page)
    await loginPage.ensureOnPage()
    return loginPage
  }
}
