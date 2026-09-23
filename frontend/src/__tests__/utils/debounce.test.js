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
import { debounce } from '@/utils/debounce.js'

describe('debounce', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('should not call the function before the delay has elapsed', () => {
    const fn = vi.fn()
    debounce(200, fn)()
    vi.advanceTimersByTime(199)
    expect(fn).not.toHaveBeenCalled()
  })

  it.each([
    100,
    500,
    1000
  ])('should call the function once the %ims delay has elapsed', (delay) => {
    const fn = vi.fn()
    debounce(delay, fn)()
    vi.advanceTimersByTime(delay)
    expect(fn).toHaveBeenCalledTimes(1)
  })

  it('should forward the arguments to the debounced function', () => {
    const fn = vi.fn()
    debounce(200, fn)('a', 'b', 3)
    vi.advanceTimersByTime(200)
    expect(fn).toHaveBeenCalledWith('a', 'b', 3)
  })

  it('should cancel the previous pending call when invoked again before the delay elapses', () => {
    const fn = vi.fn()
    const debounced = debounce(200, fn)
    debounced('first')
    vi.advanceTimersByTime(100)
    debounced('second')
    vi.advanceTimersByTime(100)
    expect(fn).not.toHaveBeenCalled()
    vi.advanceTimersByTime(100)
    expect(fn).toHaveBeenCalledTimes(1)
    expect(fn).toHaveBeenCalledWith('second')
  })

  it('should invoke the function with the caller as `this`', () => {
    const context = {
      value: 42,
      fn: vi.fn(function () { return this.value })
    }
    context.debounced = debounce(200, context.fn)
    context.debounced()
    vi.advanceTimersByTime(200)
    expect(context.fn.mock.instances[0]).toBe(context)
  })
})
