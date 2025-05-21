# cibseven-frontend

This template should help get you started developing with Vue 3 in Vite.

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
npm run dev
```

### Compile and Minify for Production

```sh
npm run build
```

### Run Unit Tests with [Vitest](https://vitest.dev/)

```sh
npm run test:unit
```

### Run End-to-End Tests with [Cypress](https://www.cypress.io/)

```sh
npm run test:e2e:dev
```

This runs the end-to-end tests against the Vite development server.
It is much faster than the production build.

But it's still recommended to test the production build with `test:e2e` before deploying (e.g. in CI environments):

```sh
npm run build
npm run test:e2e
```

### Lint with [ESLint](https://eslint.org/)

```sh
npm run lint
```

### Using the `ExtensionPoint` Component

The `ExtensionPoint` component allows you to create flexible extension points in your Vue application. This is particularly useful for adding new tables or other dynamic content in specific parts of your application.

#### Example Usage

To use the `ExtensionPoint` component, follow these steps:

1. Import the `ExtensionPoint` component in your Vue file:

```javascript
import ExtensionPoint from '@/components/common-components/ExtensionPoint.vue';
```

2. Add the `ExtensionPoint` component to your template where you want the extension point to be:

```html
<template>
  <div>
    <!-- Other content -->
    <ExtensionPoint>
      <!-- Dynamic content goes here -->
    </ExtensionPoint>
  </div>
</template>
```

3. Pass any necessary props to the `ExtensionPoint` component:

```html
<template>
  <div>
    <!-- Other content -->
    <ExtensionPoint :some-prop="someValue">
      <!-- Dynamic content goes here -->
    </ExtensionPoint>
  </div>
</template>
```

#### Adding New Tables Using the `ExtensionPoint`

You can add new tables dynamically using the `ExtensionPoint` component. Here's an example:

1. Create a new table component, e.g., `NewTable.vue`:

```html
<template>
  <table>
    <thead>
      <tr>
        <th>Column 1</th>
        <th>Column 2</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="item in items" :key="item.id">
        <td>{{ item.column1 }}</td>
        <td>{{ item.column2 }}</td>
      </tr>
    </tbody>
  </table>
</template>

<script>
export default {
  name: 'NewTable',
  props: {
    items: {
      type: Array,
      required: true
    }
  }
};
</script>
```

2. Use the `NewTable` component within the `ExtensionPoint`:

```html
<template>
  <div>
    <!-- Other content -->
    <ExtensionPoint>
      <NewTable :items="tableData" />
    </ExtensionPoint>
  </div>
</template>

<script>
import ExtensionPoint from '@/components/common-components/ExtensionPoint.vue';
import NewTable from '@/components/tables/NewTable.vue';

export default {
  components: {
    ExtensionPoint,
    NewTable
  },
  data() {
    return {
      tableData: [
        { id: 1, column1: 'Data 1', column2: 'Data 2' },
        { id: 2, column1: 'Data 3', column2: 'Data 4' }
      ]
    };
  }
};
</script>
```
