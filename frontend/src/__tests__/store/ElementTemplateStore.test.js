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
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createStore } from 'vuex'

vi.mock('@/services.js', () => ({ ElementTemplateService: { findAll: vi.fn() } }))

import ElementTemplateStore from '@/store/ElementTemplateStore.js'
import { ElementTemplateService } from '@/services.js'

const template = (templateId, content, active = true) => ({ templateId, active, content })

const newStore = () => createStore({ modules: { elementTemplates: { ...ElementTemplateStore, state: { templates: [] } } } })

describe('ElementTemplateStore', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('reads the element templates', async () => {
    ElementTemplateService.findAll.mockResolvedValue([template('mail', '{"id":"mail"}')])
    const store = newStore()

    await store.dispatch('elementTemplates/fetchAllElementTemplates')

    expect(ElementTemplateService.findAll).toHaveBeenCalledTimes(1)
    expect(store.getters['elementTemplates/allElementTemplateContents']).toEqual([{ id: 'mail' }])
  })

  /** Without the modeler backend the request answers with the application page, which has no templates. */
  it('keeps no templates when the answer is not a list', async () => {
    ElementTemplateService.findAll.mockResolvedValue('<!doctype html>')
    const store = newStore()

    await store.dispatch('elementTemplates/fetchAllElementTemplates')

    expect(store.state.elementTemplates.templates).toEqual([])
  })

  it('hands the renderer only the active templates whose content can be read', async () => {
    ElementTemplateService.findAll.mockResolvedValue([
      template('mail', '{"id":"mail"}'),
      template('hidden', '{"id":"hidden"}', false),
      template('empty', '  '),
      template('missing', null),
      template('broken', '{not json')
    ])
    const store = newStore()

    await store.dispatch('elementTemplates/fetchAllElementTemplates')

    expect(store.getters['elementTemplates/allElementTemplateContents']).toEqual([{ id: 'mail' }])
  })

  it('lets a failed request reach the caller', async () => {
    ElementTemplateService.findAll.mockRejectedValue(new Error('403'))
    const store = newStore()

    await expect(store.dispatch('elementTemplates/fetchAllElementTemplates')).rejects.toThrow('403')
  })
})
