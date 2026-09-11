# Agent Instructions — cibseven-webclient

Guidance for AI coding agents (Claude Code, Copilot, Codex, Cursor, …) working in this repository.

## Project Overview
The main CIB seven BPM web application: a multi-module Maven project with a Vue 3 frontend. It supports **app mode** (SPA) and **library mode** (published as `cibseven-components`, consumed by `cibseven-webclient-ee` as UMD/ES module).

## Tech Stack
- **Frontend:** Vue 3 with **Options API** (`export default {}`), Vite, vue-router 4 (hash history), Vuex 4 (namespaced modules), axios, vue-i18n
- **Backend:** Java 17, Spring Boot 3.5.x (Jakarta EE), Spring MVC REST controllers, SpringDoc OpenAPI
- **UI:** Bootstrap 5, SCSS, Material Design Icons (`@mdi/font`), `@cib/bootstrap-components`, `@cib/common-frontend`
- **Testing:** Vitest + @vue/test-utils (unit), Spring Boot Test + JUnit (backend), JaCoCo (Java coverage). E2E tests live in a separate repository.
- **Linting:** ESLint 9 flat config + eslint-plugin-vue (essential) + eslint-plugin-vuejs-accessibility, no Prettier

## Coding Conventions

### Vue Components
- **Always use Options API** (`export default { name, components, props, data, computed, methods, ... }`), **NOT** `<script setup>` or Composition API
- PascalCase file names: `CibSeven.vue`, `TaskList.vue`
- Every `.vue` file must start with an Apache 2.0 license header as HTML comment; every `.js` file as block comment
- Template → Script → Style ordering in SFCs
- Use **mixins** for shared logic (`permissionsMixin.js`, `assigneeMixin.js`), NOT composables
- All components must have a `name` property and explicitly declared `emits`
- Use `provide`/`inject` for dependency injection
- Accessibility: keyboard-accessible interactive elements, `alt` on images, `aria-hidden="true"` on decorative icons, labels on form inputs

### State Management
- Vuex 4 with namespaced modules (NOT Pinia); PascalCase filenames: `ProcessStore.js`, `FilterStore.js`
- Store modules are plain objects: `{ state, getters, mutations, actions }`

### Services & API
- **All API services in a single file:** `src/services.js`
- Services are plain objects with methods using `axios` and `getServicesBasePath()` for URL prefix
- Use `async/await` (not `.then()` chains); use the globally exported `axios` from `globals.js`

### Routing
- vue-router 4 with `createWebHashHistory`; routes under `/seven/`, authenticated routes under `/seven/auth/`
- Route factory `createAppRouter(routes)` exported for library consumers
- Route guards: `authGuard`, `setupGuard`, `permissionsGuard`
- Components are eagerly imported (no lazy loading)

### Styling
- Bootstrap 5 classes for layout; SCSS with themes in `src/themes/`; `mdi-*` icon classes

### Build & Distribution
- Frontend code in `frontend/`, built by Maven `frontend-maven-plugin`
- Library mode: `cross-env BUILD_MODE=library vite build` → `cibseven-components.es.js` + `cibseven-components.umd.js`

### Java Backend
- Package root: `org.cibseven.webapp.*`
- REST base path: `${cibseven.webclient.services.basePath:/services/v1}`
- All REST controllers extend `BaseService`; use `@RestController` + `@RequestMapping` + `@Operation` Swagger annotations
- Constructor injection (not field `@Autowired`); Lombok `@Data`/`@Value` for DTOs
- BPM engine abstraction via provider interfaces: `ITaskProvider`, `IProcessProvider`, etc.
- Wrap engine API calls in try-catch; throw custom exceptions from the `exception` package
- Logging: SLF4J + Log4j2

### i18n
- Translation files: JSON with **tab indentation** (`translations_*.json`)

## Testing & Coverage (required for every commit)
- **Every commit that changes production code must include new or updated tests covering that change.** Bug fixes need a regression test; new features need tests for the main paths.
- Frontend unit tests live in `frontend/src/__tests__/` (Vitest, jsdom, `@vue/test-utils`). E2E tests are maintained in a separate repository.
- Before committing, verify coverage of the code you touched:
  - Full run: `npm run test:coverage` (istanbul; reports in `target/coverage/`)
  - Scoped check: `npx vitest run <test files> --coverage --coverage.include="src/<changed files>"`
- **Aim for ≥ 80% line coverage on new/changed files, and never reduce overall coverage.** If a change is genuinely untestable (build config, generated code), say so explicitly in the PR/commit description.
- Java: run `mvn -B clean verify -pl '!.'` — **not `mvn test`**, which skips the `*IT` classes and the coverage gates. The repo-wide figure is `cibseven-coverage-aggregate/target/site/jacoco-aggregate/`, and `jacoco:check` enforces a floor per module plus a global one; never lower a floor to make a build pass. See the `java-jacoco-coverage` skill for the test recipes, the gates and the known traps.
- Tests must assert behavior — no assertion-free or snapshot-only padding to inflate numbers.

## Git Conventions
- One-line conventional commit messages: `type(scope): summary` (e.g. `fix(CIBHeaderFlow): …`); no body, no trailers.
- Never commit `.npmrc` changes (contains registry credentials).
- If a dependency was temporarily switched to a local `file:` link for testing, revert it (and the lockfile) before committing.

## Important Notes
- This project is consumed as a **library** by `cibseven-webclient-ee` — maintain backwards-compatible exports in `library.js`
- `library.js` re-exports everything including `@cib/common-frontend`
- `cibseven-modeler` is consumed as an npm dependency (may be temporarily `file:`-linked during local development — revert before commit)

## Code Style
- Prefer `const` over `let`; never use `var`
- Template literals over string concatenation; optional chaining (`?.`) and nullish coalescing (`??`)
- `async/await` over `.then()` chains; prefer early returns over deep nesting
- kebab-case event names in templates (`@my-event`)
