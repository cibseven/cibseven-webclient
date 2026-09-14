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
import { describe, it, expect } from 'vitest'
import { TaskPool } from '@/taskpool.js'

/**
 * A task whose promise this test controls: `resolve`/`reject` settle it on demand, and
 * `calls` records how often the pool invoked it.
 */
function deferredTask() {
  const state = { calls: 0, args: [] }
  const gates = []
  const task = (...args) => {
    state.calls++
    state.args.push(args)
    return new Promise((resolve, reject) => gates.push({ resolve, reject }))
  }
  task.state = state
  task.settle = (index, value) => gates[index].resolve(value)
  task.fail = (index, error) => gates[index].reject(error)
  return task
}

/** Let queued promise callbacks run. */
const tick = () => new Promise((resolve) => setTimeout(resolve, 0))

describe('TaskPool', () => {
  it('should expose only add, addPrio and clear', () => {
    expect(Object.keys(TaskPool(1)).sort()).toEqual(['add', 'addPrio', 'clear'])
  })

  it('should run a task and resolve with its value', async () => {
    const pool = TaskPool(1)
    const task = deferredTask()

    const promise = pool.add(task)
    await tick()
    task.settle(0, 'done')

    await expect(promise).resolves.toBe('done')
  })

  it('should forward the given parameters to the task', async () => {
    const pool = TaskPool(1)
    const task = deferredTask()

    pool.add(task, ['a', 'b'])
    await tick()

    expect(task.state.args[0]).toEqual(['a', 'b'])
  })

  it('should call a task with no arguments when none are given', async () => {
    const pool = TaskPool(1)
    const task = deferredTask()

    pool.add(task)
    await tick()

    expect(task.state.args[0]).toEqual([])
  })

  it('should reject with the task error', async () => {
    const pool = TaskPool(1)
    const task = deferredTask()

    const promise = pool.add(task)
    await tick()
    task.fail(0, new Error('boom'))

    await expect(promise).rejects.toThrow('boom')
  })

  // The whole point of the pool: never more than `limit` tasks in flight.
  it('should not exceed the concurrency limit', async () => {
    const pool = TaskPool(2)
    const task = deferredTask()

    pool.add(task)
    pool.add(task)
    pool.add(task)
    await tick()

    expect(task.state.calls).toBe(2)
  })

  it('should start the next task once a running one resolves', async () => {
    const pool = TaskPool(1)
    const task = deferredTask()

    pool.add(task)
    pool.add(task)
    await tick()
    expect(task.state.calls).toBe(1)

    task.settle(0, 'first')
    await tick()

    expect(task.state.calls).toBe(2)
  })

  // A failing task must not stall the queue behind it.
  it('should start the next task even when a running one rejects', async () => {
    const pool = TaskPool(1)
    const task = deferredTask()

    const failing = pool.add(task)
    pool.add(task)
    await tick()

    task.fail(0, new Error('boom'))
    await expect(failing).rejects.toThrow('boom')
    await tick()

    expect(task.state.calls).toBe(2)
  })

  it('should drain a queue longer than the limit', async () => {
    const pool = TaskPool(2)
    const task = deferredTask()
    const promises = [pool.add(task), pool.add(task), pool.add(task), pool.add(task)]

    for (let i = 0; i < 4; i++) {
      await tick()
      if (task.state.calls > i) task.settle(i, i)
    }
    await tick()

    await expect(Promise.all(promises)).resolves.toEqual([0, 1, 2, 3])
    expect(task.state.calls).toBe(4)
  })

  describe('deduplication by id', () => {
    // Two components asking for the same resource should share one request.
    it('should return the existing promise for a queued id instead of enqueuing again', async () => {
      const pool = TaskPool(1)
      const blocker = deferredTask()
      const task = deferredTask()

      pool.add(blocker)
      const first = pool.add(task, [], 'same')
      const second = pool.add(task, [], 'same')

      expect(second).toBe(first)
      await tick()
      expect(task.state.calls).toBe(0)
    })

    it('should enqueue separately for different ids', async () => {
      const pool = TaskPool(1)
      const blocker = deferredTask()
      const task = deferredTask()

      pool.add(blocker)
      const first = pool.add(task, [], 'a')
      const second = pool.add(task, [], 'b')

      expect(second).not.toBe(first)
    })

    it.each([[undefined], [null]])('should not deduplicate when the id is %j', async (id) => {
      const pool = TaskPool(1)
      const blocker = deferredTask()
      const task = deferredTask()

      pool.add(blocker)
      const first = pool.add(task, [], id)
      const second = pool.add(task, [], id)

      expect(second).not.toBe(first)
    })

    // An id of 0 is a legitimate id: the guard checks against null, not falsiness.
    it('should treat 0 as a real id', async () => {
      const pool = TaskPool(1)
      const blocker = deferredTask()
      const task = deferredTask()

      pool.add(blocker)
      const first = pool.add(task, [], 0)
      const second = pool.add(task, [], 0)

      expect(second).toBe(first)
    })
  })

  describe('addPrio', () => {
    it('should run a prioritised task ahead of already queued ones', async () => {
      const pool = TaskPool(1)
      const blocker = deferredTask()
      const normal = deferredTask()
      const urgent = deferredTask()

      pool.add(blocker)
      pool.add(normal)
      pool.addPrio(urgent)
      await tick()

      blocker.settle(0, 'unblocked')
      await tick()

      expect(urgent.state.calls).toBe(1)
      expect(normal.state.calls).toBe(0)
    })

    it('should resolve with the task value', async () => {
      const pool = TaskPool(1)
      const task = deferredTask()

      const promise = pool.addPrio(task)
      await tick()
      task.settle(0, 'done')

      await expect(promise).resolves.toBe('done')
    })

    it('should reject with the task error', async () => {
      const pool = TaskPool(1)
      const task = deferredTask()

      const promise = pool.addPrio(task)
      await tick()
      task.fail(0, new Error('boom'))

      await expect(promise).rejects.toThrow('boom')
    })

    it('should call the task with the given parameters', async () => {
      const pool = TaskPool(1)
      const task = deferredTask()

      pool.addPrio(task, ['x'])
      await tick()

      expect(task.state.args[0]).toEqual(['x'])
    })

    // Re-prioritising something already queued moves it to the front rather than running
    // it a second time.
    it('should move an already queued task to the front of the queue', async () => {
      const pool = TaskPool(1)
      const blocker = deferredTask()
      const first = deferredTask()
      const second = deferredTask()

      pool.add(blocker)
      pool.add(first, [], 'first')
      pool.add(second, [], 'second')

      const promise = pool.addPrio(first, [], 'first')
      await tick()

      blocker.settle(0, 'unblocked')
      await tick()

      expect(first.state.calls).toBe(1)
      expect(second.state.calls).toBe(0)
      await expect(Promise.race([promise, Promise.resolve('pending')])).resolves.toBe('pending')
    })

    // Deduplication only covers entries still *queued*: `start` shifts an entry out of the
    // pool before running it, so once a task is in flight its id is no longer findable and
    // a same-id request starts a second run. Worth pinning, because it is easy to assume
    // the id deduplicates for the whole lifetime of the task.
    it('should start a second run for an id whose task is already in flight', async () => {
      const pool = TaskPool(1)
      const task = deferredTask()

      const first = pool.add(task, [], 'same')
      await tick()
      expect(task.state.calls).toBe(1)

      const second = pool.addPrio(task, [], 'same')
      await tick()

      // A distinct entry, queued behind the in-flight one because the limit is reached.
      expect(second).not.toBe(first)
      expect(task.state.calls).toBe(1)

      task.settle(0, 'first')
      await tick()

      expect(task.state.calls).toBe(2)
    })

    // Re-prioritising a queued entry leaves it in the pool twice (it is unshifted without
    // being removed). While the first copy runs, the second copy is still queued and
    // carries `isExecuted`, which is the only way to reach that lookup branch.
    it('should return the running promise for a re-prioritised entry still queued', async () => {
      const pool = TaskPool(1)
      const blocker = deferredTask()
      const task = deferredTask()

      pool.add(blocker)
      const first = pool.add(task, [], 'same')
      pool.addPrio(task, [], 'same')

      blocker.settle(0, 'unblocked')
      await tick()
      expect(task.state.calls).toBe(1)

      const third = pool.addPrio(task, [], 'same')

      expect(third).toBe(first)
      expect(task.state.calls).toBe(1)
    })

    it.each([[undefined], [null]])('should not deduplicate when the id is %j', async (id) => {
      const pool = TaskPool(1)
      const blocker = deferredTask()
      const task = deferredTask()

      pool.add(blocker)
      const first = pool.addPrio(task, [], id)
      const second = pool.addPrio(task, [], id)

      expect(second).not.toBe(first)
    })
  })

  describe('clear', () => {
    it('should drop queued tasks so they never run', async () => {
      const pool = TaskPool(1)
      const blocker = deferredTask()
      const dropped = deferredTask()

      pool.add(blocker)
      pool.add(dropped)
      await tick()

      pool.clear()
      blocker.settle(0, 'unblocked')
      await tick()

      expect(dropped.state.calls).toBe(0)
    })

    // The running task is already in flight and is unaffected by clearing the queue.
    it('should not affect a task that is already running', async () => {
      const pool = TaskPool(1)
      const task = deferredTask()

      const promise = pool.add(task)
      await tick()
      pool.clear()
      task.settle(0, 'done')

      await expect(promise).resolves.toBe('done')
    })

    it('should allow new tasks to run after clearing', async () => {
      const pool = TaskPool(1)
      const blocker = deferredTask()
      const later = deferredTask()

      pool.add(blocker)
      pool.add(deferredTask())
      pool.clear()
      blocker.settle(0, 'unblocked')
      await tick()

      const promise = pool.add(later)
      await tick()
      later.settle(0, 'ok')

      await expect(promise).resolves.toBe('ok')
    })
  })

  // A task can be dequeued and re-queued, so `start` guards against running the same
  // entry twice and simply moves on to the next one.
  it('should skip an entry that has already been executed', async () => {
    const pool = TaskPool(1)
    const blocker = deferredTask()
    const shared = deferredTask()
    const other = deferredTask()

    pool.add(blocker)
    pool.add(shared, [], 'shared')
    pool.addPrio(shared, [], 'shared')
    pool.add(other)

    blocker.settle(0, 'unblocked')
    await tick()
    expect(shared.state.calls).toBe(1)

    shared.settle(0, 'shared done')
    await tick()

    expect(other.state.calls).toBe(1)
    expect(shared.state.calls).toBe(1)
  })

  it('should isolate separate pools from each other', async () => {
    const first = TaskPool(1)
    const second = TaskPool(1)
    const task = deferredTask()

    first.add(task)
    second.add(task)
    await tick()

    expect(task.state.calls).toBe(2)
  })
})
