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
import { describe, it, expect, vi } from 'vitest'
import { handleAxiosError } from '@/utils/init'

const router = { currentRoute: { value: { name: 'process' } } }

function rootWithErrorDialog() {
  return { $refs: { error: { show: vi.fn() } } }
}

function serverError(type) {
  return { response: { status: 500, data: { type } } }
}

describe('handleAxiosError', () => {
  it('opens the error dialog for an unexpected failure', async () => {
    const root = rootWithErrorDialog()

    await expect(handleAxiosError(router, root, serverError('SystemException'))).rejects.toBeDefined()

    expect(root.$refs.error.show).toHaveBeenCalledWith({ type: 'SystemException' })
  })

  /**
   * Its callers report it themselves - the modeler in its deployment toast, the definition
   * details in their own handler - so the dialog would be the second report of one failure.
   */
  it('leaves a missing history time to live to the caller', async () => {
    const root = rootWithErrorDialog()

    await expect(handleAxiosError(router, root, serverError('InvalidValueHistoryTimeToLive')))
      .rejects.toBeDefined()

    expect(root.$refs.error.show).not.toHaveBeenCalled()
  })

  it('rejects so the caller can handle it', async () => {
    const error = serverError('InvalidValueHistoryTimeToLive')

    await expect(handleAxiosError(router, rootWithErrorDialog(), error)).rejects.toBe(error)
  })

  it('opens no dialog for an unknown type either', async () => {
    const root = rootWithErrorDialog()

    await expect(handleAxiosError(router, root, serverError('SomethingUnmapped'))).rejects.toBeDefined()

    expect(root.$refs.error.show).not.toHaveBeenCalled()
  })
})
