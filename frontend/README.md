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

## Extending ProcessInstanceView.vue

### Using Slots

To extend the `ProcessInstanceView.vue` component using slots, follow these steps:

1. Modify `ProcessInstanceView.vue` to include a named slot:

```vue
<template>
  <div v-if="selectedInstance" class="h-100">
    <!-- Existing code -->
    <slot name="additional-content"></slot>
  </div>
</template>
```

2. In the parent component, use the slot to pass the additional components:

```vue
<template>
  <ProcessInstanceView>
    <template v-slot:additional-content>
      <MyCustomComponent />
    </template>
  </ProcessInstanceView>
</template>
```

### Using Dynamic Component Loading

To extend the `ProcessInstanceView.vue` component using dynamic component loading, follow these steps:

1. Maintain a list of components to be added and render them dynamically within `ProcessInstanceView.vue`:

```vue
<template>
  <div v-if="selectedInstance" class="h-100">
    <!-- Existing code -->
    <component v-for="(component, index) in additionalComponents" :is="component" :key="index"></component>
  </div>
</template>

<script>
export default {
  data() {
    return {
      additionalComponents: []
    };
  },
  methods: {
    addComponent(component) {
      this.additionalComponents.push(component);
    }
  }
};
</script>
```

2. In the parent component, add the components dynamically:

```vue
<template>
  <ProcessInstanceView ref="processInstanceView" />
</template>

<script>
import MyCustomComponent from './MyCustomComponent.vue';

export default {
  mounted() {
    this.$refs.processInstanceView.addComponent(MyCustomComponent);
  }
};
</script>
```

### Using a Plugin System

To extend the `ProcessInstanceView.vue` component using a plugin system, follow these steps:

1. Create a plugin system where external applications can register components to be added to `ProcessInstanceView.vue`:

```javascript
// plugin.js
import Vue from 'vue';

const Plugin = {
  install(Vue) {
    Vue.prototype.$registerComponent = function(component) {
      this.$root.$emit('register-component', component);
    };
  }
};

Vue.use(Plugin);
```

2. Use a global event bus or a Vuex store to manage the registered components and render them dynamically within `ProcessInstanceView.vue`:

```vue
<template>
  <div v-if="selectedInstance" class="h-100">
    <!-- Existing code -->
    <component v-for="(component, index) in registeredComponents" :is="component" :key="index"></component>
  </div>
</template>

<script>
export default {
  data() {
    return {
      registeredComponents: []
    };
  },
  created() {
    this.$root.$on('register-component', (component) => {
      this.registeredComponents.push(component);
    });
  }
};
</script>
```

3. In the external application, register the components using the plugin system:

```javascript
import MyCustomComponent from './MyCustomComponent.vue';

Vue.prototype.$registerComponent(MyCustomComponent);
```
