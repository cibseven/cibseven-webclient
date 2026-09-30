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
import { vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { readFileSync } from 'node:fs'
import { resolve, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { i18n } from '@/i18n'

const here = dirname(fileURLToPath(import.meta.url))

/**
 * Load the real English translations into the shared i18n instance.
 *
 * Call this from `beforeAll` when a test asserts on user-visible text. Without it, `$t`
 * returns the key, which is fine for tests that assert on keys instead (see
 * `translationStubs` below) — both styles are in use in this suite.
 *
 * `bcomponents.*` keys live in '@cib/common-frontend', not in this repo's translation
 * files, so the handful the shared components need are merged in here.
 */
export function loadEnglishTranslations(extra = {}) {
  const translations = {
    ...JSON.parse(readFileSync(resolve(here, '../../assets/translations_en.json'), 'utf-8')),
    bcomponents: { ariaLabelClose: 'Close' },
    ...extra
  }
  i18n.global.locale = 'en'
  i18n.global.setLocaleMessage('en', translations)
  return translations
}

/**
 * Mocks that make `$t` and friends echo their key, for tests that assert on translation
 * keys rather than rendered prose. Cheaper than loading the real messages and immune to
 * wording changes.
 */
export const translationStubs = {
  $t: (key) => key,
  $te: () => true,
  $tc: (key) => key,
  $n: (value) => String(value),
  $d: (value) => String(value)
}

/**
 * Template stubs for the Bootstrap components from '@cib/bootstrap-components' that show
 * up across the app shell. Each one renders its default slot, so assertions can still
 * reach the content the component under test provides; the interactive ones expose the
 * imperative methods (`show`/`hide`) that our components call as spies.
 *
 * Prefer these over `true` (which drops slot content) when the test needs to see inside,
 * and prefer the *real* component from '@cib/common-frontend' when the assertion is about
 * that component's own DOM or accessibility behaviour.
 */
export function createBootstrapStubs() {
  const slotted = (tag, extra = {}) => ({ template: `<${tag}><slot></slot></${tag}>`, ...extra })
  return {
    'b-navbar': slotted('nav'),
    'b-navbar-nav': slotted('ul'),
    'b-nav-item': slotted('li'),
    'b-nav-item-dropdown': slotted('li'),
    'b-dropdown': slotted('div'),
    'b-dropdown-item': slotted('a'),
    'b-dropdown-item-button': slotted('button'),
    'b-collapse': { template: '<div><slot></slot></div>', props: ['modelValue'] },
    'b-modal': {
      template: '<div><slot></slot></div>',
      props: ['modelValue', 'title'],
      methods: { show: vi.fn(), hide: vi.fn() }
    },
    'b-button': { template: '<button @click="$emit(\'click\', $event)"><slot></slot></button>', emits: ['click'] },
    'b-badge': slotted('span'),
    'b-alert': { template: '<div role="alert"><slot></slot></div>', props: ['modelValue', 'variant'] },
    'b-tabs': slotted('div'),
    'b-tab': slotted('div'),
    'b-form-group': { template: '<div><slot></slot></div>', props: ['label', 'labelFor'] },
    'b-waiting-box': true,
    'router-link': { template: '<a><slot></slot></a>', props: ['to'] },
    'router-view': true
  }
}

/**
 * `mount` with this repo's usual global configuration already in place.
 *
 * Pass `global` to add to (not replace) the defaults — `stubs`, `mocks` and `provide` are
 * merged key-wise, so a test can override a single stub without restating the map. Set
 * `i18nPlugin: false` to rely purely on the `$t` key-echo mocks.
 */
export function mountWithDefaults(component, options = {}) {
  const { global: globalOptions = {}, i18nPlugin = true, ...rest } = options
  return mount(component, {
    ...rest,
    global: {
      ...globalOptions,
      plugins: [...(i18nPlugin ? [i18n] : []), ...(globalOptions.plugins ?? [])],
      stubs: { ...createBootstrapStubs(), ...(globalOptions.stubs ?? {}) },
      mocks: { ...translationStubs, ...(globalOptions.mocks ?? {}) },
      provide: { isMobile: false, currentLanguage: 'en', ...(globalOptions.provide ?? {}) },
      directives: { 'b-popover': {}, 'b-tooltip': {}, ...(globalOptions.directives ?? {}) }
    }
  })
}
