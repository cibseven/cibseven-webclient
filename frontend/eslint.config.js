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
import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import pluginVitest from '@vitest/eslint-plugin'
import pluginCypress from 'eslint-plugin-cypress/flat'
import pluginVueA11y from "eslint-plugin-vuejs-accessibility";

export default [
  {
    name: 'app/files-to-lint',
    files: ['**/*.{js,mjs,jsx,vue}'],
    ignores: ['playwright/**', 'cypress/**'],
  },

  {
    name: 'app/files-to-ignore',
    ignores: [
      '**/dist/**',
      '**/dist-ssr/**',
      '**/coverage/**',
      '**/target/**',
      '**/playwright-report/**',
      '**/test-results/**',
    ],
  },

  js.configs.recommended,
  ...pluginVue.configs['flat/essential'],
  
  {
    ...pluginVitest.configs.recommended,
    files: ['src/**/__tests__/*'],
  },
  
  {
    ...pluginCypress.configs.recommended,
    files: [
      'cypress/e2e/**/*.{cy,spec}.{js,ts,jsx,tsx}',
      'cypress/support/**/*.{js,ts,jsx,tsx}'
    ],
  },

  {
    // Playwright test files
    files: [
      'playwright/e2e/**/*.{spec,test}.{js,ts,jsx,tsx}',
      'playwright/helpers/**/*.{js,ts,jsx,tsx}'
    ],
    rules: {
      // Allow console.log in test files for debugging
      'no-console': 'off',
    },
  },

  ...pluginVueA11y.configs["flat/recommended"],
  {
    rules: {
      "vuejs-accessibility/label-has-for": [
        "error",
        {
          "required": {
            "every": ["id"]
          },
        }
      ],
    }
  },

  {
    "rules": {
      "vue/require-name-property": "error",
      "vue/require-explicit-emits": "error",
      "no-duplicate-imports": "error",
      "no-var": "error",
      "prefer-const": "error",
    }
  },

  // CIB formatting rules
  {
    'rules': {
      //'semi': ['error', 'never'],
      // 'quotes': ['error', 'single', { 'avoidEscape': true }],
      'max-len': ['error', { 'code': 350 }],
      //'space-before-function-paren': ['error', {
      //  'anonymous': 'never',
      //  'named': 'never',
      //  'asyncArrow': 'always'
      //}],
      // 'object-shorthand': ['error', 'always']
    },
  },

  // JSON-specific rules
  {
    files: ['translations_*.json'],
    rules: {
      'indent': ['error', 'tab']
    },
  },
]
