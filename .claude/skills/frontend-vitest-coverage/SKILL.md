---
name: frontend-vitest-coverage
description: Write or extend Vitest unit tests for the Vue 3 frontend in `frontend/`, and measure the coverage effect. Use when adding tests, raising coverage, testing a Vuex store, a service, a mixin or a `.vue` component, or when a coverage threshold fails.
---

# Frontend Vitest coverage

House recipes for testing `frontend/`. Follow these instead of inventing a new mocking
style — every pattern below is already used by the suite, so a new spec reads like its
neighbours.

## Measure first, then write

Coverage is reported by istanbul into `frontend/target/coverage/`. Configuration lives in
`frontend/vitest.config.js` (jsdom, setup file `src/__tests__/vitest.setup.js`, `@` aliased
to `frontend/src`).

```bash
cd frontend
npm run test                # whole suite
npm run test:coverage       # whole suite + coverage (this is what the thresholds gate)
npx vitest run src/__tests__/store/FilterStore.test.js          # one file
```

Scope the coverage report to what you touched — the full table is 190 files:

```bash
npx vitest run <test files> --coverage \
  --coverage.include="src/<changed file or glob>" --coverage.reporter=text
```

**Rank targets by absolute uncovered lines, not by percentage.** A 40%-covered 300-line
file is worth ten 40%-covered 20-line files:

```bash
awk '/^SF:/{sf=substr($0,4)} /^LF:/{lf=substr($0,4)} /^LH:/{lh=substr($0,4);
  printf "%6d uncovered  %6.1f%%  %s\n", lf-lh, lh*100/lf, sf}' \
  target/coverage/lcov.info | sort -rn | head -30
```

Overall figure in one line:

```bash
awk '/^LF:/{lf=substr($0,4)} /^LH:/{lh=substr($0,4); F+=lf; H+=lh} \
  END{printf "%.2f%% (%d/%d)\n", H*100/F, H, F}' target/coverage/lcov.info
```

## Thresholds

`vitest.config.js` sets a global floor plus 80% glob thresholds for `src/store/**`,
`src/mixins/**`, `src/utils/**`, `src/plugins/**` and `src/services.js`. A file matched by
a glob key is checked against that glob and **excluded from the global numbers**, so the
global values describe the remainder (`src/components`, `src/embedded-form`, entry
modules). When you raise a directory above 80% durably, add a glob for it and lift the
global floor to match what is left. Never lower a threshold to make a change pass.

## Recipe 1 — Vuex store module

`src/__tests__/store/store-test-utils.js` already exists. `createStoreTestSuite` generates
the `describe`, reclones `state` before each test, asserts every mutation/getter/action
exists, and *invokes every getter* — so getter bodies are covered almost for free.

```js
import { it, expect, vi, beforeEach } from 'vitest'
import IncidentsStore from '../../store/IncidentsStore.js'
import { IncidentService } from '@/services.js'
import { createStoreTestSuite } from './store-test-utils.js'

vi.mock('@/services.js', () => ({
  IncidentService: { findIncidents: vi.fn() }   // declare only what the store calls
}))

beforeEach(() => { vi.clearAllMocks() })

createStoreTestSuite('IncidentsStore', IncidentsStore, {
  initialState: (getState) => { /* it(...) using getState() */ },
  mutations: { setIncidents: (mutation, getState) => { /* mutation(state, payload) */ } },
  getters:   { incidents:    (getter, getState)   => { /* getter(state) */ } },
  actions:   { loadRuntimeIncidents: (action, getContext) => {
    it('...', async () => {
      const context = getContext()   // { commit, dispatch, state, getters, rootGetters } spies
      IncidentService.findIncidents.mockResolvedValue([{ id: 'i1' }])
      await action(context, { processInstanceId: 'pi1' })
      expect(context.commit).toHaveBeenCalledWith('setIncidents', [{ id: 'i1' }])
    })
  } },
  additional: (storeModule) => { /* it.each tables over pass-through actions */ }
})
```

Gotchas:
- The auto-generated getter test asserts `toBeDefined()`. A getter returning `undefined`
  on empty state fails it — give that getter a custom case with state set up.
- `state` is recloned with `JSON.parse(JSON.stringify(...))`, so it must stay
  JSON-serialisable.
- A module reading `rootGetters` needs them assigned on the context object returned by
  `getContext()` (it is the same object each call, so mutate it in place).
- Module-level side effects (e.g. `FilterStore` migrating `localStorage` on import) need a
  separate `describe` using `vi.resetModules()` + `await import(...)` with storage primed.

## Recipe 2 — a service or any module using axios

Mock `@/globals.js`, never `axios` itself. `src/__tests__/support/axiosMock.js` provides
the spy set; `create()` returns the same instance, which matters because a few services
call `axios.create()` to bypass the app's interceptors.

```js
import { createAxiosMock, resetAxiosMock } from './support/axiosMock.js'

const axios = createAxiosMock()
vi.mock('@/globals.js', async (importOriginal) => ({ ...await importOriginal(), axios }))

// Import the module under test *after* the mock is registered.
const { TaskService, setServicesBasePath } = await import('@/services.js')

beforeEach(() => { resetAxiosMock(axios); localStorage.clear() })
```

Assert the whole request — URL, params, body, headers — so a renamed endpoint or a dropped
query parameter fails here rather than against a live backend. For flat one-line methods a
table is far more readable than one block each:

```js
describe('TaskService', () => {
  itMatchesEndpoints([
    { name: 'findTaskById', verb: 'get', call: () => TaskService.findTaskById('t1'),
      args: [`${BASE}/task/t1`] },
  ])
})
```

(See `src/__tests__/services.test.js` for `itMatchesEndpoints`. Keep the `describe` title a
string literal — the `vitest/valid-title` ESLint rule rejects a variable.)

## Recipe 3 — a pure util or a mixin

No mounting. Call the function, or `.call()` the mixin's method/computed against a
hand-built `this`:

```js
import usersMixin from '@/mixins/usersMixin.js'

const vm = { $t: k => k, $root: { user: { id: 'me' }, config: {} }, $store: { ... } }
expect(usersMixin.computed.getCompleteName.call(vm)).toBe('me')
```

## Recipe 4 — a `.vue` component, without mounting (the default for big ones)

Exercise `methods` / `computed` / `data` against a stubbed `this`. This is the right tool
whenever the component nests expensive children (`bpmn-js`, `dmn-js`, form-js), uses
`inject`, or reaches into `$refs` of its children — mounting it would mean standing all of
that up for no gain. Worked examples: `TasksContent.test.js`, `DeploymentsView.test.js`,
`ProcessDefinitionViewLogic.test.js`, `BpmnViewer.test.js`.

`src/__tests__/support/callWithContext.js` gives sensible defaults; most components want a
local `context()` factory instead, shaped like this:

```js
function context(overrides = {}) {
  const vm = {
    $t: (key) => key, $emit: vi.fn(),
    $root: { config: structuredClone(DEFAULT_CONFIG), user: { id: 'demo' },
             $refs: { error: { show: vi.fn() } } },
    $route: { params: {}, query: {} }, $router: { push: vi.fn() },
    $store: { state: { ... }, getters: {}, commit: vi.fn(), dispatch: vi.fn(() => Promise.resolve()) },
    $refs: { /* the children's refs the methods poke at */ },
    /* data fields, and stubs for mapped store helpers */
    ...overrides
  }
  // Bind only what the context does not already provide, so the stubs above survive.
  for (const [name, method] of Object.entries(Component.methods)) {
    if (typeof method === 'function' && !(name in vm)) vm[name] = method.bind(vm)
  }
  for (const [name, def] of Object.entries(Component.computed)) {
    if (name in vm || typeof def !== 'function') continue
    Object.defineProperty(vm, name, { get: () => def.call(vm), configurable: true })
  }
  return vm
}
```

**The `!(name in vm)` guard is the part that bites.** Guarding on `overrides[name]` instead
lets the loop overwrite the intentional stubs for `mapActions`/`mapGetters` helpers and for
imported methods such as `formatDate`, producing confusing failures.

Other notes:
- `data()` often reads `localStorage` and `this.someMethod()`; call it as
  `Component.data.call({ someMethod: () => true })`.
- A `debounce`d method needs `vi.useFakeTimers()` and an explicit `advanceTimersByTime`.
- Let promise chains settle with a small `await Promise.resolve()` loop.

## Recipe 5 — a `.vue` component, mounted

Mount when the assertion is genuinely about rendered DOM or accessibility. Use
`src/__tests__/support/mountWithDefaults.js` (bootstrap stub map, `$t` stubs, i18n
bootstrap) and a `createWrapper(props)` factory at the top of the file.

```js
import { mountWithDefaults, loadEnglishTranslations } from '../support/mountWithDefaults.js'

beforeAll(() => { loadEnglishTranslations() })   // only if asserting on real wording

const createWrapper = (props = {}) => mountWithDefaults(WarningBox, {
  props, global: { stubs: { 'b-alert': RealBAlert } }   // merges over the defaults
})
```

Two i18n styles are both in use: mock `$t: k => k` and assert on translation keys (cheaper,
immune to wording changes), or load the real messages and assert on prose. Prefer the real
component from `@cib/common-frontend` over a template stub when the test is about that
component's own DOM.

## House rules

- Apache-2.0 header on **every** new file — `src/__tests__/package.test.js` enforces it.
  Copy it from a neighbouring file.
- Name specs `*.test.js` (the suite has no `*.spec.js`; those are Playwright's).
- Options API only, matching the components.
- Assert behaviour: mock call arguments, returned route objects, state after a mutation. No
  snapshot tests, no assertion-free padding — `AGENTS.md` is explicit about this.
- Helper modules belong in `src/__tests__/support/`; nothing there is collected as a test
  and `src/__tests__/**` is already excluded from coverage.
- Explain a non-obvious test in a comment above it — why the branch matters, not what the
  code does. That is the existing style.
- `npm run lint` must pass. The ESLint flat config declares no Node globals, so `__dirname`
  needs `// eslint-disable-next-line no-undef` (or use `import.meta.url` +
  `fileURLToPath`).

## jsdom limits worth knowing

- **No layout.** `clientWidth`, `scrollWidth` and `getBoundingClientRect()` are all zero on
  real elements, so scroll/size logic must be driven through hand-built plain-object
  "elements". That is easier, not harder — each geometry case is set up exactly.
- `window.getComputedStyle` only accepts real Elements; spy on it when walking plain-object
  node graphs (`utils/iframe.js`, `RenderTemplate.handleScrollIframe`).
- `ResizeObserver` is stubbed as a no-op in `vitest.setup.js`; call the observed callback
  directly to test resize behaviour.
- **Navigation throws.** Anything assigning `location.href` needs
  `delete window.location; window.location = { href: '' }` and restoring it afterwards.
- `FormData.append(name, blob)` wraps a bare `Blob` in a new `File`, so use a `File` when
  asserting identity.
- Web Worker entry points (`task-worker.js`) register listeners on `self` at import time:
  `vi.stubGlobal('self', ...)` and `vi.stubGlobal('fetch', ...)` **before** `await import()`,
  and `vi.resetModules()` between tests to reset module-level state.

## Deliberately not unit-tested

`src/app.js` and `src/sso-login.js` are excluded in `vitest.config.js`: their module scope
boots the application (mounting the SPA, assigning `location.href`). They are covered by
the Playwright suite in `frontend/playwright/`. Also out of reach: the inline anonymous
`auth` route component in `router.js`, which needs the real component tree.
