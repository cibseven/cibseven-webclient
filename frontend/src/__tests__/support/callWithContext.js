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
 * Build a plain object that stands in for a component instance (`this`), so that an
 * Options API component's methods and computed properties can be invoked with `.call()`
 * without mounting it.
 *
 * This is the right tool for components whose children are expensive or impossible to
 * mount in jsdom (bpmn-js / dmn-js viewers, form-js), or which rely on `inject`. It keeps
 * the unit under test to the component's own logic.
 *
 * Defaults cover the plumbing nearly every component in this repo touches; pass
 * `overrides` for the state and collaborators the test actually cares about. Any method
 * from the component that the method under test calls internally must be spread in by the
 * caller — e.g. `context({ ...Component.methods, loadThings: vi.fn() })`.
 */
export function createComponentContext(overrides = {}) {
  return {
    $t: (key) => key,
    $te: () => true,
    $tc: (key) => key,
    $n: (value) => String(value),
    $d: (value) => String(value),
    $emit: vi.fn(),
    $nextTick: (fn) => (fn ? Promise.resolve().then(fn) : Promise.resolve()),
    $refs: {},
    $route: { params: {}, query: {}, name: undefined, path: '/' },
    $router: { push: vi.fn(), replace: vi.fn(), resolve: vi.fn(() => ({ href: '#/' })) },
    $store: { state: {}, getters: {}, commit: vi.fn(), dispatch: vi.fn(() => Promise.resolve()) },
    $root: {
      config: {},
      user: null,
      $refs: {
        error: { show: vi.fn() },
        success: { show: vi.fn() },
        loader: { wait: vi.fn((promise) => promise) }
      }
    },
    ...overrides
  }
}

/**
 * Invoke a computed property's getter (or a plain method) against a context built from
 * `overrides`, returning both the result and the context so the test can assert on the
 * side effects recorded by its spies.
 */
export function callWith(fn, overrides = {}, ...args) {
  const context = createComponentContext(overrides)
  return { result: fn.call(context, ...args), context }
}
