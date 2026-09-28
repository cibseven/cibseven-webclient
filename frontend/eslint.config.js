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
import { cibEslintConfig } from '@cib/frontend-preset/eslint'

export default [
  // The webclient does not use the CIB formatting rules (see max-len below).
  // Vitest rules only for files directly in a __tests__ folder, as before.
  ...cibEslintConfig({ formatting: false, testFiles: ['src/**/__tests__/*'], ignores: ['**/test-results/**'] }),

  {
    rules: {
      // form-control-has-label only recognizes native tags by default; without this,
      // our @cib/common-frontend form wrappers (which render the native control inside
      // a different component) get zero static a11y label coverage. b-form-datepicker and
      // b-form-timepicker are deliberately excluded: they already bind a (generic) internal
      // aria-label to their real input, so flagging usage sites is a false positive.
      "vuejs-accessibility/form-control-has-label": [
        "error",
        {
          "controlComponents": [
            "b-form-select",
            "b-form-input",
            "b-form-textarea",
            "b-form-file"
          ]
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

  {
    'rules': {
      'max-len': ['error', { 'code': 350 }],
    },
  },
]
