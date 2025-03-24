/* eslint-disable no-unused-vars */

import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

const backendUrl = 'http://localhost:8080'

// https://vite.dev/config/
export default defineConfig({
  base: '/webapp/',
  plugins: [
    vue(),
    vueDevTools()
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      vue: 'vue/dist/vue.esm-bundler.js',
    },
  },
  server: {
    proxy: {
		  '/webapp/services': {
        target: backendUrl,
        changeOrigin: true,
        secure: false,
        configure: (proxy, _options) => {
          proxy.on('error', (err, _req, _res) => {
            console.log('proxy error', err)
          })
          proxy.on('proxyReq', (proxyReq, req, _res) => {
            // console.log('Sending Request to the Target:', req.method, backendUrl + req.url)
          })
          proxy.on('proxyRes', (proxyRes, req, _res) => {
            // console.log('Received Response from the Target:', proxyRes.statusCode, backendUrl + req.url)
          })
        },
      },
    },
  },
})
