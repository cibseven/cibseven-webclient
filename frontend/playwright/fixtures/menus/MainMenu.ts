import type { Page, Locator } from '@playwright/test'
import { BaseMenu } from './BaseMenu'

export class MainMenu extends BaseMenu {

  constructor(public readonly page: Page) {
    super(page);
  }

  async toggle() {
    // TODO
  }

}
