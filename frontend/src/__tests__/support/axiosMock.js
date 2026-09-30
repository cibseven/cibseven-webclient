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

/**
 * A stand-in for the axios instance exported by '@/globals.js'.
 *
 * Every verb is a spy resolving to `{ data: undefined }` so that service methods which
 * `await` the response and read `.data` do not blow up on the default path. `create()`
 * returns the *same* spy set, which matters because a handful of services deliberately
 * call `axios.create()` to obtain an instance without the app's interceptors — tests can
 * still assert on `get`/`post` there, and additionally on `create` having been called.
 */
export function createAxiosMock() {
  const respond = () => Promise.resolve({ data: undefined })
  const instance = {
    get: vi.fn(respond),
    post: vi.fn(respond),
    put: vi.fn(respond),
    delete: vi.fn(respond),
    patch: vi.fn(respond),
    request: vi.fn(respond),
    defaults: { headers: { common: {} } },
    interceptors: {
      request: { use: vi.fn(), eject: vi.fn() },
      response: { use: vi.fn(), eject: vi.fn() }
    }
  }
  instance.create = vi.fn(() => instance)
  return instance
}

/**
 * Reset every spy on a mock built by `createAxiosMock()` back to resolving `{ data }`.
 * `vi.clearAllMocks()` clears call history but also drops mockResolvedValue-style
 * implementations, so call this in `beforeEach` to get the default behaviour back.
 */
export function resetAxiosMock(instance) {
  const respond = () => Promise.resolve({ data: undefined })
  for (const verb of ['get', 'post', 'put', 'delete', 'patch', 'request']) {
    instance[verb].mockReset()
    instance[verb].mockImplementation(respond)
  }
  instance.create.mockReset()
  instance.create.mockImplementation(() => instance)
}
