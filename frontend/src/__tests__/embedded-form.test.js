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
import { describe, it, expect, beforeEach, vi } from 'vitest'

// Shared spies for the mocked bpm-sdk. `vi.hoisted` so they exist before vi.mock runs.
const { ClientMock, FormMock, httpGetCalls, state } = vi.hoisted(() => {
  const httpGetCalls = []
  const state = { formKey: 'deployment:my-form' }
  const ClientMock = vi.fn(function (config) {
    this.apiUri = config.apiUri
    this.http = {
      config: { headers: config.headers },
      get: (path, opts) => {
        httpGetCalls.push({ apiUri: config.apiUri, path, opts })
        if (opts && typeof opts.done === 'function') opts.done(null, '<form></form>')
      }
    }
    this.resource = () => ({
      startForm: (_o, cb) => cb(null, { key: state.formKey }),
      form: (_id, cb) => cb(null, { key: state.formKey }),
      deployedForm: (_id, cb) => cb(null, '<form></form>'),
      http: this.http
    })
  })
  const FormMock = vi.fn(function (config) {
    if (config && typeof config.done === 'function') config.done(null, { submit() {}, store() {} })
  })
  return { ClientMock, FormMock, httpGetCalls, state }
})

vi.mock('bpm-sdk', () => ({ default: { Client: ClientMock, Form: FormMock } }))
vi.mock('jquery', () => ({ default: (el) => el }))
vi.mock('@/services', () => ({ InfoService: { getProperties: vi.fn() } }))
vi.mock('@/i18n', () => ({ switchLanguage: vi.fn(), i18n: { global: { t: (k) => k } } }))
vi.mock('@/utils/init', () => ({ getTheme: vi.fn(), loadTheme: vi.fn() }))
vi.mock('@/utils/error', () => ({
  extractErrorMessage: (e) => String(e),
  isDeployedFormNotFoundError: () => false,
  extractDeployedFormName: () => '',
  isFormElementError: () => false
}))

import { loadEmbeddedForm } from '@/embedded-form/embedded-form.js'

const BASE = '/webapp/services/v1'

function run(isStartForm, referenceId) {
  return loadEmbeddedForm(isStartForm, false, referenceId, document.createElement('div'),
    document.createElement('div'), { authToken: 'Bearer x' }, { servicesBasePath: BASE })
}

// Every resource bpm-sdk requests at runtime is served by the middleware, so the sdk client stays
// pointed at servicesBasePath. Pointing it at a sub-path broke file downloads, because the sdk
// derives variable data URLs from client.baseUrl (see CIB7-1707).
describe('embedded-form: single sdk client on the middleware base path', () => {
  beforeEach(() => {
    ClientMock.mockClear()
    FormMock.mockClear()
    httpGetCalls.length = 0
    state.formKey = 'deployment:my-form'
  })

  it('builds a single sdk client pointed at the middleware base path', async () => {
    await run(true, 'pd:1')

    expect(ClientMock).toHaveBeenCalledTimes(1)
    expect(ClientMock.mock.instances[0].apiUri).toBe(BASE)
  })

  it('does not point the client at an engine-rest sub-path', async () => {
    // Guards the regression that broke task-form file downloads: the sdk builds
    // `${baseUrl}/task/{id}/variables/{name}/data`, which must resolve on the middleware.
    await run(false, 'task-1')

    expect(ClientMock.mock.instances[0].apiUri).not.toContain('engine-rest')
  })

  it('passes the client to CamSDK.Form (start form)', async () => {
    await run(true, 'pd:1')

    expect(FormMock).toHaveBeenCalledTimes(1)
    const formConfig = FormMock.mock.calls[0][0]
    expect(formConfig.client).toBe(ClientMock.mock.instances[0])
    expect(formConfig.client.apiUri).toBe(BASE)
    expect(formConfig.processDefinitionId).toBe('pd:1')
  })

  it('passes the client to CamSDK.Form (task form)', async () => {
    await run(false, 'task-1')

    expect(FormMock).toHaveBeenCalledTimes(1)
    const formConfig = FormMock.mock.calls[0][0]
    expect(formConfig.client.apiUri).toBe(BASE)
    expect(formConfig.taskId).toBe('task-1')
  })

  it('loads the deployed start form via the middleware process path', async () => {
    await run(true, 'pd:1')

    const call = httpGetCalls.find((c) => c.path === 'process/pd:1/deployed-start-form')
    expect(call).toBeDefined()
    expect(call.apiUri).toBe(BASE)
  })

  it('fetches form-proxy through the sdk client, not a separate http client', async () => {
    // an external embedded form (no deployment:/rendered-form key) goes through the form-proxy branch
    state.formKey = 'embedded:app:forms/my-form.html'

    await run(false, 'task-9')

    const call = httpGetCalls.find((c) => c.path === 'task/form-proxy')
    expect(call).toBeDefined()
    expect(call.apiUri).toBe(BASE)
    expect(call.opts.data).toEqual({ referenceId: 'task-9', isStartForm: false })
    expect(call.opts.headers.authorization).toBe('Bearer x')
  })

  it('loads a generated (rendered-form) start form via the client', async () => {
    // a Camunda-generated form: key contains /rendered-form (and not deployment:)
    state.formKey = 'app:default:/rendered-form'

    await run(true, 'pd:1')

    const call = httpGetCalls.find((c) => c.path === 'process-definition/pd:1/rendered-form')
    expect(call).toBeDefined()
    expect(call.apiUri).toBe(BASE)
    expect(FormMock.mock.calls[0][0].client.apiUri).toBe(BASE)
  })

  it('sets the X-Process-Engine header on the client when an engine id is provided', async () => {
    await loadEmbeddedForm(true, false, 'pd:1', document.createElement('div'),
      document.createElement('div'), { authToken: 'Bearer x', engineId: 'myengine' },
      { servicesBasePath: BASE })

    const config = ClientMock.mock.calls[0][0]
    expect(config.headers['X-Process-Engine']).toBe('myengine')
    expect(config.apiUri).toBe(BASE)
  })
})
