import type { Page, Locator } from '@playwright/test'

export class BasePage {
  private readonly homeButton: Locator
  private readonly helpMenu: Locator
  private readonly languageMenu: Locator

  constructor(public readonly page: Page) {
    this.homeButton = this.page.getByRole('link', { name: 'Brand' })
    this.helpMenu = this.page.getByTestId('help-menu')
    this.languageMenu = this.page.getByTestId('language-menu')
  }

  async goto(path: string = '') {
    if (!path) {
      throw new Error('Path must be provided to navigate to the page')
    }
    await this.page.goto(path, { waitUntil: 'networkidle' })
    await this.ensureOnPage();
  }

  async ensureOnPage() {
    throw new Error('ensureOnPage method not implemented in BasePage')
  }

  async clickHome(): Promise<any>{
    await this.ensureOnPage()
    await this.homeButton.click()

    // Wait for specific element on destination page
    await Promise.race([
      // Login page with error
      this.page.getByRole('heading', { name: 'CIB seven', level: 1 }).waitFor({ state: 'visible' }),
      // Start page
      this.page.waitForSelector('.mdi-account', { timeout: 5000 })
    ])

    return null
  }

  async openHelp() {
    await this.ensureOnPage()
    await this.helpMenu.click()
  }

  async changeLanguage(lang: string) {
    await this.ensureOnPage()
    await this.languageMenu.click()
    await this.page.getByRole('menuitem', { name: lang }).click()
  }
}
