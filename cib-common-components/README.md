# cib-common-components

## Recommended IDE Setup

[VSCode](https://code.visualstudio.com/) + [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (and disable Vetur).

## Customize configuration

See [Vite Configuration Reference](https://vite.dev/config/).

## Project Setup

```sh
npm install
```

### Compile and Hot-Reload for Development

```sh
npm dev
```

### Compile and Minify for Production

```sh
npm build
```

## Publish as NPM library

```sh
npm publish
```

### Run Unit Tests with [Vitest](https://vitest.dev/)

```sh
npm test:unit
```

### Run End-to-End Tests with [Cypress](https://www.cypress.io/)

```sh
npm test:e2e:dev
```

This runs the end-to-end tests against the Vite development server.
It is much faster than the production build.

But it's still recommended to test the production build with `test:e2e` before deploying (e.g. in CI environments):

```sh
npm build
npm test:e2e
```

### Lint with [ESLint](https://eslint.org/)

```sh
npm lint
```
