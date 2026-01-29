import type { Page, Locator } from '@playwright/test'

export class BaseMenu {

  constructor(public readonly page: Page) {
  }

  async toggle() {
    throw new Error('BaseMenu.close method should not be called');
  }

}
