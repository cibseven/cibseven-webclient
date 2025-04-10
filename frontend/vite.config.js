/* eslint-disable no-unused-vars */

import { fileURLToPath, URL } from 'node:url'
import path from 'node:path'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

const backendUrl = 'http://localhost:8080'

// Detect build mode
/* eslint-disable no-undef */
const isLibrary = process.env.BUILD_MODE === 'library'
/* eslint-enable no-undef */

// https://flaviocopes.com/fix-dirname-not-defined-es-module-scope/
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

console.log('isLibrary', isLibrary)

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
            //console.log('Sending Request to the Target:', req.method, backendUrl + req.url)
          })
          proxy.on('proxyRes', (proxyRes, req, _res) => {
            //console.log('Received Response from the Target:', proxyRes.statusCode, backendUrl + req.url)
          })
        },
      },
    },
  },
  build: isLibrary
    ? {
        lib: {
          entry: path.resolve(__dirname, 'src/library.js'),
          name: 'cibseven-components',
          formats: ['es', 'umd'],
          fileName: (format) => `cibseven-components.${format}.js`,
        },
        rollupOptions: {
          external: ['vue', /^\/assets\/images\//],
          output: {
            globals: {
              vue: 'Vue',
            },
            // Ensure CSS is extracted and placed in the dist folder
            assetFileNames: 'cibseven-components.[ext]',
          },
        },
        cssCodeSplit: true, // Ensure CSS is extracted into a separate file
        outDir: 'dist', // The output directory
      }
    : {
      rollupOptions: {
        input: {
          main: path.resolve(__dirname, 'index.html'),
          ssoLogin: path.resolve(__dirname, 'sso-login.html')
        }
      }
    }
})
