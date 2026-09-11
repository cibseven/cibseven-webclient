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

/**
 * task-worker.js is a Web Worker entry point: it registers a `message` listener on `self`
 * at import time and keeps its state in module-level variables. So `self` and `fetch` have
 * to exist *before* the module is imported, and the module registry has to be reset between
 * tests to get the state back to its initial values.
 */
async function loadWorker() {
  vi.resetModules()

  const postMessage = vi.fn()
  let handler
  vi.stubGlobal('self', {
    addEventListener: (type, fn) => {
      if (type === 'message') handler = fn
    },
    postMessage
  })

  const fetchMock = vi.fn()
  vi.stubGlobal('fetch', fetchMock)

  await import('@/task-worker.js')

  return {
    postMessage,
    fetch: fetchMock,
    send: (data) => handler({ data })
  }
}

/** A fetch Response stub. */
const response = (body, { ok = true, status = 200 } = {}) => ({
  ok,
  status,
  json: () => Promise.resolve(body)
})

/** Let the promise chain inside checkNewTasks settle. */
const settle = async (times = 6) => {
  for (let i = 0; i < times; i++) await Promise.resolve()
}

const SETUP = {
  type: 'setup',
  authToken: 'tok-123',
  userId: 'demo',
  interval: 1000,
  servicesBasePath: 'http://host/services/v1'
}

beforeEach(() => {
  vi.useFakeTimers()
  vi.spyOn(console, 'error').mockImplementation(() => {})
})

afterEach(() => {
  vi.useRealTimers()
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('task-worker', () => {
  it('should register a message listener on import', async () => {
    const worker = await loadWorker()

    expect(() => worker.send({ type: 'setup' })).not.toThrow()
  })

  it.each([[undefined], [null], [{}], [{ type: 'unknown' }]])('should ignore the message %j', async (data) => {
    const worker = await loadWorker()

    worker.send(data)
    await vi.advanceTimersByTimeAsync(5000)

    expect(worker.fetch).not.toHaveBeenCalled()
  })

  describe('polling', () => {
    // Setup only records the configuration; polling starts on the first checkNewTasks.
    it('should not poll until asked to check', async () => {
      const worker = await loadWorker()

      worker.send(SETUP)
      await vi.advanceTimersByTimeAsync(5000)

      expect(worker.fetch).not.toHaveBeenCalled()
    })

    it('should poll on the configured interval', async () => {
      const worker = await loadWorker()
      worker.fetch.mockResolvedValue(response([]))

      worker.send(SETUP)
      worker.send({ type: 'checkNewTasks' })

      await vi.advanceTimersByTimeAsync(1000)
      expect(worker.fetch).toHaveBeenCalledTimes(1)

      await vi.advanceTimersByTimeAsync(1000)
      expect(worker.fetch).toHaveBeenCalledTimes(2)
    })

    it('should query the task endpoint with the token and the assignee', async () => {
      const worker = await loadWorker()
      worker.fetch.mockResolvedValue(response([]))

      worker.send(SETUP)
      worker.send({ type: 'checkNewTasks' })
      await vi.advanceTimersByTimeAsync(1000)

      const [url, options] = worker.fetch.mock.calls[0]
      expect(url).toBe('http://host/services/v1/task')
      expect(options.method).toBe('POST')
      expect(options.headers).toEqual({ Authorization: 'tok-123', 'Content-Type': 'application/json' })
      expect(JSON.parse(options.body)).toMatchObject({ assignee: 'demo' })
    })

    // Only tasks created since the last check should count as new.
    it('should scope the query to tasks created after the last check', async () => {
      const worker = await loadWorker()
      worker.fetch.mockResolvedValue(response([]))

      worker.send(SETUP)
      worker.send({ type: 'checkNewTasks' })
      await vi.advanceTimersByTimeAsync(1000)

      expect(JSON.parse(worker.fetch.mock.calls[0][1].body).createdAfter).toBeTruthy()
    })

    it('should not start an interval when none was configured', async () => {
      const worker = await loadWorker()

      worker.send({ ...SETUP, interval: undefined })
      worker.send({ type: 'checkNewTasks' })
      await vi.advanceTimersByTimeAsync(5000)

      expect(worker.fetch).not.toHaveBeenCalled()
    })

    // Two checkNewTasks messages must not leave two intervals running.
    it('should replace an existing interval rather than adding a second', async () => {
      const worker = await loadWorker()
      worker.fetch.mockResolvedValue(response([]))

      worker.send(SETUP)
      worker.send({ type: 'checkNewTasks' })
      worker.send({ type: 'checkNewTasks' })

      await vi.advanceTimersByTimeAsync(1000)

      expect(worker.fetch).toHaveBeenCalledTimes(1)
    })

    it('should stop polling on a stop message', async () => {
      const worker = await loadWorker()
      worker.fetch.mockResolvedValue(response([]))

      worker.send(SETUP)
      worker.send({ type: 'checkNewTasks' })
      await vi.advanceTimersByTimeAsync(1000)

      worker.send({ type: 'stop' })
      await vi.advanceTimersByTimeAsync(5000)

      expect(worker.fetch).toHaveBeenCalledTimes(1)
    })

    it('should tolerate a stop message when nothing is running', async () => {
      const worker = await loadWorker()

      expect(() => worker.send({ type: 'stop' })).not.toThrow()
    })
  })

  describe('reporting results', () => {
    const poll = async (worker) => {
      worker.send(SETUP)
      worker.send({ type: 'checkNewTasks' })
      await vi.advanceTimersByTimeAsync(1000)
      await settle()
    }

    it('should notify the app when tasks come back', async () => {
      const worker = await loadWorker()
      const tasks = [{ id: 't1' }]
      worker.fetch.mockResolvedValue(response(tasks))

      await poll(worker)

      expect(worker.postMessage).toHaveBeenCalledWith({ type: 'sendNotification', tasks })
    })

    it('should stay quiet when there are no new tasks', async () => {
      const worker = await loadWorker()
      worker.fetch.mockResolvedValue(response([]))

      await poll(worker)

      expect(worker.postMessage).not.toHaveBeenCalled()
    })

    it('should stay quiet when the response body is empty', async () => {
      const worker = await loadWorker()
      worker.fetch.mockResolvedValue(response(null))

      await poll(worker)

      expect(worker.postMessage).not.toHaveBeenCalled()
    })

    // The worker holds its own copy of the token and has to refresh it itself, since it
    // cannot see the app's axios interceptors.
    it('should adopt a prolonged token and retry with it', async () => {
      const worker = await loadWorker()
      worker.fetch
        .mockResolvedValueOnce(response({ type: 'TokenExpiredException', params: ['tok-new'] }))
        .mockResolvedValueOnce(response([{ id: 't1' }]))

      await poll(worker)

      expect(worker.fetch).toHaveBeenCalledTimes(2)
      expect(worker.fetch.mock.calls[1][1].headers.Authorization).toBe('tok-new')
      expect(worker.postMessage).toHaveBeenCalledWith({ type: 'sendNotification', tasks: [{ id: 't1' }] })
    })

    it('should not retry an expired token without a replacement', async () => {
      const worker = await loadWorker()
      worker.fetch.mockResolvedValue(response({ type: 'TokenExpiredException' }))

      await poll(worker)

      expect(worker.fetch).toHaveBeenCalledTimes(1)
    })

    it('should report an HTTP failure back to the app', async () => {
      const worker = await loadWorker()
      worker.fetch.mockResolvedValue(response(null, { ok: false, status: 503 }))

      await poll(worker)

      expect(worker.postMessage).toHaveBeenCalledWith({
        type: 'error',
        error: 'HTTP error! status: 503'
      })
      expect(console.error).toHaveBeenCalled()
    })

    it('should report a transport failure back to the app', async () => {
      const worker = await loadWorker()
      worker.fetch.mockRejectedValue(new Error('Network Error'))

      await poll(worker)

      expect(worker.postMessage).toHaveBeenCalledWith({ type: 'error', error: 'Network Error' })
    })
  })
})
