import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import path from 'path'

// Detect build mode
/* eslint-disable no-undef */
const isLibrary = process.env.BUILD_MODE === 'library'
/* eslint-enable no-undef */

console.log('isLibrary', isLibrary)

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      vue: 'vue/dist/vue.esm-bundler.js'
    },
  },
  build: isLibrary
    ? {
        lib: {
          /* eslint-disable no-undef */
          entry: path.resolve(__dirname, 'src/library.js'),
          /* eslint-enable no-undef */
          name: 'cib-common-components',
          formats: ['es', 'umd'],
          fileName: (format) => `cib-common-components.${format}.js`,
        },
        rollupOptions: {
          external: ['vue'],
          output: {
            globals: {
              vue: 'Vue',
            },
          },
        },
      }
    : {}
})
