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

function badRequest(type) {
  return { response: { status: 400, data: { type } } }
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

  it('opens the error dialog for an unreachable engine', async () => {
    const root = rootWithErrorDialog()

    await expect(handleAxiosError(router, root, serverError('EngineNotReachableException'))).rejects.toBeDefined()

    expect(root.$refs.error.show).toHaveBeenCalledWith({ type: 'EngineNotReachableException' })
  })

  it('opens the error dialog for an engine that is not configured', async () => {
    const root = rootWithErrorDialog()

    await expect(handleAxiosError(router, root, badRequest('UnknownEngineException'))).rejects.toBeDefined()

    expect(root.$refs.error.show).toHaveBeenCalledWith({ type: 'UnknownEngineException' })
  })

  it('opens the error dialog for a permission the engine refused', async () => {
    const root = rootWithErrorDialog()
    const data = { type: 'PermissionDeniedException', params: ['demo may not update task-1'] }

    await expect(handleAxiosError(router, root, { response: { status: 403, data } })).rejects.toBeDefined()

    expect(root.$refs.error.show).toHaveBeenCalledWith(data)
  })

  it('leaves the other refusals to the caller', async () => {
    const root = rootWithErrorDialog()

    await expect(handleAxiosError(router, root, { response: { status: 403, data: { type: 'AccessDeniedException' } } }))
      .rejects.toBeDefined()

    expect(root.$refs.error.show).not.toHaveBeenCalled()
  })

  it('opens no dialog for a refused permission when the caller reports its own errors', async () => {
    const root = rootWithErrorDialog()
    const error = { config: { reportsOwnErrors: true }, response: { status: 403, data: { type: 'PermissionDeniedException' } } }

    await expect(handleAxiosError(router, root, error)).rejects.toBe(error)

    expect(root.$refs.error.show).not.toHaveBeenCalled()
  })

  it('leaves other bad requests to the caller', async () => {
    const root = rootWithErrorDialog()

    await expect(handleAxiosError(router, root, badRequest('InvalidAttributeValueException'))).rejects.toBeDefined()

    expect(root.$refs.error.show).not.toHaveBeenCalled()
  })

  it('opens no dialog for an engine that is not configured inside a deployed form', async () => {
    const root = rootWithErrorDialog()
    const formRouter = { currentRoute: { value: { name: 'deployed-form' } } }

    await expect(handleAxiosError(formRouter, root, badRequest('UnknownEngineException'))).rejects.toBeDefined()

    expect(root.$refs.error.show).not.toHaveBeenCalled()
  })

  it('opens no dialog for an unknown type either', async () => {
    const root = rootWithErrorDialog()

    await expect(handleAxiosError(router, root, serverError('SomethingUnmapped'))).rejects.toBeDefined()

    expect(root.$refs.error.show).not.toHaveBeenCalled()
  })
})
