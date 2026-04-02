import type { Page, Locator } from '@playwright/test';
import { AuthedPage } from './base/AuthedPage';

export class StartPage extends AuthedPage {
  private readonly startProcess: Locator
  private readonly taskList: Locator

  constructor(public readonly page: Page) {
    super(page)
    this.startProcess = this.page.locator('')
    this.taskList = this.page.locator('')
  }

  async goto() {
    await super.goto('#/seven/auth/start?_=' + Date.now())
  }

  async clickStartProcess() {
    // Wait for the start process button to be fully ready
    await this.startProcess.waitFor({ state: 'visible' })
    
    // Click start process button
    await this.startProcess.click()

    // Wait for navigation to complete
    await this.page.waitForLoadState('networkidle')

    return new AuthedPage(this.page);
  }

  async clickTaskList() {
    // Wait for the task list button to be fully ready
    await this.taskList.waitFor({ state: 'visible' })
    
    // Click task list button
    await this.taskList.click()

    // Wait for navigation to complete
    await this.page.waitForLoadState('networkidle')

    return new AuthedPage(this.page);
  }

}
