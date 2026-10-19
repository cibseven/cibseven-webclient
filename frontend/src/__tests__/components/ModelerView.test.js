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
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { axios } from '@/globals.js'
import { setServicesBasePath } from '@/services.js'

vi.mock('cibseven-modeler', () => ({
  CibsevenModeler: { name: 'CibsevenModeler', template: '<div class="cibseven-modeler-stub" />' },
  setAxiosInstance: vi.fn(),
  setServicesBasePath: vi.fn()
}))
vi.mock('cibseven-modeler/dist/cibseven-modeler.css', () => ({}))

import ModelerView from '@/components/modeler/ModelerView.vue'
import { setAxiosInstance, setServicesBasePath as setModelerServicesBasePath } from 'cibseven-modeler'

function createWrapper({ dispatch = vi.fn().mockResolvedValue() } = {}) {
  return mount(ModelerView, {
    global: {
      mocks: {
        $store: { dispatch }
      },
      mixins: [{
        data() {
          return {
            config: { productNamePageTitle: 'CIB seven' }
          }
        }
      }],
      provide: {
        currentLanguage: 'en'
      }
    }
  })
}

describe('ModelerView.vue', () => {
  let wrapper

  beforeEach(() => {
    vi.clearAllMocks()
    setServicesBasePath('/services/v1')
  })

  afterEach(() => {
    wrapper?.unmount()
    wrapper = null
    setServicesBasePath('')
  })

  it('configures the modeler to use the webclient axios instance and services base path', () => {
    wrapper = createWrapper()
    expect(setAxiosInstance).toHaveBeenCalledWith(axios)
    expect(setModelerServicesBasePath).toHaveBeenCalledWith('/services/v1')
  })

  it('dispatches fetchAllElementTemplates on created', () => {
    const dispatch = vi.fn().mockResolvedValue()
    wrapper = createWrapper({ dispatch })
    expect(dispatch).toHaveBeenCalledWith('modeler/elementTemplates/fetchAllElementTemplates')
  })

  it('warns without throwing when fetchAllElementTemplates fails', async () => {
    const error = new Error('boom')
    const dispatch = vi.fn().mockRejectedValue(error)
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {})

    wrapper = createWrapper({ dispatch })
    await flushPromises()

    expect(warnSpy).toHaveBeenCalledWith('Could not load element templates:', error)
    warnSpy.mockRestore()
  })

  it('rewrites requests to the engine REST prefix to the services base path', () => {
    const useSpy = vi.spyOn(axios.interceptors.request, 'use')
    wrapper = createWrapper()

    expect(useSpy).toHaveBeenCalledTimes(1)
    const handler = useSpy.mock.calls[0][0]
    const config = { url: '/client/cibseven-engine/process-definition' }

    expect(handler(config)).toEqual({ url: '/services/v1/process-definition' })
  })

  it('leaves requests outside the engine REST prefix untouched', () => {
    const useSpy = vi.spyOn(axios.interceptors.request, 'use')
    wrapper = createWrapper()

    const handler = useSpy.mock.calls[0][0]
    const config = { url: '/other/path' }

    expect(handler(config)).toEqual(config)
  })

  it('ejects the interceptor when the view is unmounted', () => {
    const interceptorId = 42
    vi.spyOn(axios.interceptors.request, 'use').mockReturnValue(interceptorId)
    const ejectSpy = vi.spyOn(axios.interceptors.request, 'eject')

    wrapper = createWrapper()
    wrapper.unmount()
    wrapper = null

    expect(ejectSpy).toHaveBeenCalledWith(interceptorId)
  })
})
