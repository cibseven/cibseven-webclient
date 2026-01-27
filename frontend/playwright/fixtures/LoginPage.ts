import type { Page, Locator } from '@playwright/test';
import { expect } from '@playwright/test';
import { NotAuthedPage } from './base/NotAuthedPage';
import { StartPage } from './StartPage';

export class LoginPage extends NotAuthedPage {
  private readonly h1Title: Locator
  private readonly usernameInput: Locator
  private readonly passwordInput: Locator
  private readonly loginButton: Locator

  constructor(public readonly page: Page) {
    super(page)
    this.h1Title = this.page.getByRole('heading', { name: 'CIB seven', level: 1 })
    this.usernameInput = this.page.locator(':nth-child(3) > form > :nth-child(1) > .row > .col > .form-control')
    this.passwordInput = this.page.locator('.input-group > .form-control')
    this.loginButton = this.page.locator('button[type=submit]')
  }

  async goto() {
    await super.goto('#/seven/login?nextUrl=/seven/auth/start&_=' + Date.now())
  }

  async ensureOnPage() {
    await expect(this.h1Title).toBeVisible()
  }

  private async login(username: string, password: string) : Promise<StartPage|LoginPage> {
      // Wait for the login form to be fully ready
    await this.loginButton.waitFor({ state: 'visible' })
    
    // Fill in username - using more robust selectors
    await this.usernameInput.fill(username)

    // Fill in password
    await this.passwordInput.fill(password)

    // Click login button
    await this.loginButton.click()

    // Wait for specific element on destination page
    await Promise.race([
      // Login page with error
      this.page.waitForSelector('text=/Authentication data is wrong|Username or password is incorrect/', { timeout: 5000 }),
      // Start page
      this.page.waitForSelector('.mdi-account', { timeout: 5000 })
    ])

    if (this.page.url().includes('/seven/login')) {
      return new LoginPage(this.page);
    } else {
      return new StartPage(this.page);
    }
  }

  async loginSuccess(username: string, password: string) : Promise<StartPage> {
    const page = await this.login(username, password);
    if (page instanceof LoginPage) {
      throw new TypeError('Login failed with provided credentials');
    }
    return page;
  }

  async loginFail(username: string, password: string) : Promise<LoginPage> {
    const page = await this.login(username, password);
    if (page instanceof StartPage) {
      throw new TypeError('Login succeeded but was expected to fail with provided credentials');
    }
    return page;
  }

  async loginDefault() : Promise<StartPage> {
    const defaultUsername = process.env.DEFAULT_USERNAME || 'demo';
    const defaultPassword = process.env.DEFAULT_PASSWORD || 'demo';
    return this.loginSuccess(defaultUsername, defaultPassword);
  }
}
