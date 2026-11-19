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

const modelerPackage = vi.hoisted(() => ({
  CibsevenModeler: { name: 'CibsevenModeler', template: '<div class="cibseven-modeler-stub" />' },
  setAxiosInstance: vi.fn(),
  setServicesBasePath: vi.fn()
}))
vi.mock('@/utils/modelerLoader.js', () => ({ loadModeler: vi.fn() }))
vi.mock('@cib/common-frontend', () => ({ BWaitingBox: { name: 'BWaitingBox', template: '<span />' } }))

import ModelerView from '@/components/modeler/ModelerView.vue'
import { loadModeler } from '@/utils/modelerLoader.js'

function createWrapper({ dispatch = vi.fn().mockResolvedValue() } = {}) {
  const $store = { dispatch }
  return mount(ModelerView, {
    global: {
      mocks: {
        $store,
        $t: key => key
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
    loadModeler.mockResolvedValue(modelerPackage)
    setServicesBasePath('/services/v1')
  })

  afterEach(() => {
    wrapper?.unmount()
    wrapper = null
    setServicesBasePath('')
  })

  /** The modeler is loaded when the view opens, so there is something to show meanwhile. */
  it('shows that the modeler is loading, then the modeler', async () => {
    wrapper = createWrapper()

    expect(wrapper.find('[role="status"]').text()).toContain('start.modeler.loading')
    expect(wrapper.find('.cibseven-modeler-stub').exists()).toBe(false)

    await flushPromises()

    expect(loadModeler).toHaveBeenCalledWith(wrapper.vm.$store)
    expect(wrapper.find('.cibseven-modeler-stub').exists()).toBe(true)
    expect(wrapper.find('[role="status"]').exists()).toBe(false)
  })

  it('configures the modeler to use the webclient axios instance and services base path', async () => {
    wrapper = createWrapper()
    await flushPromises()

    expect(modelerPackage.setAxiosInstance).toHaveBeenCalledWith(axios)
    expect(modelerPackage.setServicesBasePath).toHaveBeenCalledWith('/services/v1')
  })

  it('dispatches fetchAllElementTemplates once the modeler is loaded', async () => {
    const dispatch = vi.fn().mockResolvedValue()
    wrapper = createWrapper({ dispatch })
    expect(dispatch).not.toHaveBeenCalled()

    await flushPromises()

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

  /** A chunk that cannot be fetched, after a deployment for instance, must not leave a spinner forever. */
  it('says so when the modeler cannot be loaded', async () => {
    const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
    loadModeler.mockRejectedValue(new Error('Failed to fetch dynamically imported module'))
    const dispatch = vi.fn()

    wrapper = createWrapper({ dispatch })
    await flushPromises()

    expect(wrapper.find('[role="alert"]').text()).toBe('start.modeler.loadFailed')
    expect(wrapper.find('[role="status"]').exists()).toBe(false)
    expect(modelerPackage.setAxiosInstance).not.toHaveBeenCalled()
    expect(dispatch).not.toHaveBeenCalled()
    errorSpy.mockRestore()
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
