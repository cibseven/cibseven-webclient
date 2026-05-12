# Copilot Instructions for cibseven-webclient

## Project Overview
This is the main CIBseven BPM web application, built as a multi-module Maven project with a Vue 3 frontend. It supports both **app mode** (multi-page SPA) and **library mode** (consumed by `cibseven-webclient-ee` as UMD/ES module).

## Tech Stack
- **Frontend:** Vue 3 with **Options API** (`export default {}`), Vite, vue-router 4 (hash history), Vuex 4 (namespaced modules), axios, vue-i18n
- **Backend:** Java 17, Spring Boot 3.5.x (Jakarta EE), Spring MVC REST controllers, SpringDoc OpenAPI
- **UI:** Bootstrap 5, SCSS, Material Design Icons (`@mdi/font`), `@cib/bootstrap-components`, `@cib/common-frontend`
- **Testing:** Vitest + @vue/test-utils (unit), Playwright (E2E), Spring Boot Test + JUnit (backend)
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
- Use Vuex 4 with namespaced modules (NOT Pinia)
- PascalCase filenames: `ProcessStore.js`, `FilterStore.js`, `TaskStore.js`
- Store modules are plain objects: `{ state, getters, mutations, actions }`

### Services & API
- **All API services in a single file:** `src/services.js`
- Services are plain objects with methods using `axios` and `getServicesBasePath()` for URL prefix
- Use `async/await` (not `.then()` chains) for asynchronous code
- Use the globally exported `axios` from `globals.js`

### Routing
- vue-router 4 with `createWebHashHistory`
- Routes under `/seven/` root, authenticated routes under `/seven/auth/`
- Route factory `createAppRouter(routes)` exported for library consumers
- Route guards: `authGuard`, `setupGuard`, `permissionsGuard`
- Components are eagerly imported (no lazy loading)

### Styling
- Bootstrap 5 classes for layout and components
- SCSS with themes in `src/themes/`
- Material Design Icons via `mdi-*` CSS classes

### Testing
- Unit tests in `src/__tests__/` with Vitest (jsdom) and `@vue/test-utils`
- E2E: Playwright in `playwright/`

### Build & Distribution
- Frontend code in `frontend/` subdirectory, built by Maven `frontend-maven-plugin`
- Library mode: `cross-env BUILD_MODE=library vite build` → `cibseven-components.es.js` + `cibseven-components.umd.js`

### Java Backend
- Package root: `org.cibseven.webapp.*`
- REST base path: `${cibseven.webclient.services.basePath:/services/v1}`
- All REST controllers extend `BaseService`; use `@RestController` + `@RequestMapping` + `@Operation` Swagger annotations
- Use constructor injection (not field `@Autowired`); Lombok `@Data`/`@Value` for DTOs
- BPM engine abstraction via provider interfaces: `ITaskProvider`, `IProcessProvider`, etc.
- Wrap engine API calls in try-catch; throw custom exceptions from the `exception` package
- Logging: SLF4J + Log4j2

### i18n
- Translation files: JSON with **tab indentation** (`translations_*.json`)

## Important Notes
- This project is consumed as a **library** by `cibseven-webclient-ee` — maintain backwards-compatible exports in `library.js`
- `library.js` re-exports everything including `@cib/common-frontend`
- `cibseven-modeler` is included as a local `file:` dependency

## Code Style
- Prefer `const` over `let`; never use `var`
- Use template literals instead of string concatenation
- Use optional chaining (`?.`) and nullish coalescing (`??`)
- Use `async/await` instead of `.then()` chains
- Prefer early returns over deeply nested if/else
- Use kebab-case for event names in templates (`@my-event`)
