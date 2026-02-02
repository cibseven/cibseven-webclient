import type { Page, Locator } from '@playwright/test'
import { BasePage } from './BasePage'
import { MainMenu } from '../menus/MainMenu'
import { AccountMenu } from '../menus/AccountMenu'
import { StartPage } from '../StartPage'

export class AuthedPage extends BasePage {
  private readonly mainMenu: Locator
  private readonly accountMenu: Locator

  constructor(public readonly page: Page) {
    super(page)
    this.mainMenu = this.page.locator('inav .main-menu')
    this.accountMenu = this.page.locator('.mdi-account').locator('xpath=..')
  }

  async ensureOnPage() {
    await this.accountMenu.waitFor({ state: 'visible', timeout: 1000 })
  }

  async clickHome(): Promise<StartPage> {
    await super.clickHome()
    const startPage = new StartPage(this.page)
    await startPage.ensureOnPage()
    return startPage
  }

  async clickMainMenu() {
    await this.mainMenu.click()
    return new MainMenu(this.page)
  }

  async clickAccountMenu() {
    await this.accountMenu.click()
    return new AccountMenu(this.page)
  }
}
