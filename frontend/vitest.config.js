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
import { fileURLToPath } from 'node:url'
import { mergeConfig, defineConfig, configDefaults } from 'vitest/config'
import viteConfig from './vite.config'

export default mergeConfig(
  viteConfig,
  defineConfig({
    test: {
      environment: 'jsdom',
      exclude: [...configDefaults.exclude, 'e2e/**'],
      root: fileURLToPath(new URL('./', import.meta.url)),
      coverage: {
        provider: 'istanbul',
        reporter: ['text', 'lcov', 'cobertura'], // 'text', 'html', 'lcov', 'cobertura'
        reportsDirectory: './coverage',
        exclude: [
          // Build artifacts and minified files
          'dist/**',
          'target/**',
          'node_modules/**',
          'coverage/**',
          
          // Library files
          '**/cib-common-components*.js', // Exclude common components bundle
          
          // Test and config files
          'cypress/e2e/**', // Exclude Cypress tests
          'cypress.config.js', // Exclude Cypress config
          'vite.config.js', // Exclude Vite config
          'vitest.config.js', // Exclude this config file itself
          '**/*.config.js', // Exclude all config files
          '**/*.config.ts', // Exclude all TypeScript config files
          
          // Other common exclusions
          '**/*.d.ts', // Type definition files
          '**/index.js', // Entry point files (often just re-exports)
          '**/main.js', // Main entry files
        ],
      },
    },
  }),
)
