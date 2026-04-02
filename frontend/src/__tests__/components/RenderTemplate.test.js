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
import { describe, it, expect, beforeAll, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { i18n } from '@/i18n'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import RenderTemplate from '@/components/render-template/RenderTemplate.vue'
import { createProvideObject } from '@/utils/provide.js'

function createWrapper(props = {}) {
  const mockConfig = {}
  const mockVueInstance = {}
  const mockStore = {}
  
  return mount(RenderTemplate, {
    props: props,
    global: {
      provide: createProvideObject(mockConfig, mockVueInstance, mockStore),
      mocks: {
        $t: (msg) => msg,
        $te: () => true,
        $route: { query: { q: ''} }
      },
      plugins: [i18n],
      stubs: {
        'b-calendar': { template: '<div><slot></slot></div>' },
        'b-button': { template: '<div><slot></slot></div>' },
        'b-modal': { template: '<div><slot></slot></div>' },
        'b-alert': { template: '<div><slot></slot></div>' },
      }
    }
  })
}

describe('RenderTemplate.vue', () => {
  let wrapper

  beforeAll(() => {
    const translations = JSON.parse(
      // eslint-disable-next-line no-undef
      readFileSync(resolve(__dirname, '../../assets/translations_en.json'), 'utf-8')
    )

    i18n.global.locale = 'en'
    i18n.global.setLocaleMessage('en', translations)
  })

  beforeEach(() => {
    wrapper = createWrapper({
      task: {}
    })
  })

  it('scurrentLanguage', () => {
    expect(wrapper.vm.currentLanguage()).toBe('en')
  })
})
