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
import { describe, it, expect } from 'vitest'
import * as library from '@/library.js'
import { findComponents } from './utils.js'
import fs from 'node:fs'
import path from 'node:path'

describe('library.js', () => {
  // eslint-disable-next-line no-undef
  const srcDir = path.resolve(__dirname, '../../src')

  describe('*.vue', () => {
    // Find all .vue files in /src/
    const vueFiles = findComponents(srcDir, '.vue')

    it('exports all .vue components from /src/', () => {

      // Get the base name (without extension) for each .vue file
      const vueComponentNames = vueFiles.map(f =>
        path.basename(f, '.vue')
      )

      // Get all exported keys from library.js
      const exportedKeys = Object.keys(library)

      // Check that each component is exported
      const missing = vueComponentNames.filter(
        name => !exportedKeys.includes(name)
      )

      expect(missing.map((name) => {
        const pathName = vueFiles.find(f => path.basename(f, '.vue') === name).substring(srcDir.length)
        return `import ${name} from '@${pathName}'`
      }).join('\n')).toEqual('')
    })

    it('valid name of .vue components', () => {
      vueFiles.forEach(file => {
        const fileName = path.basename(file, '.vue')

        // Check that the file name is a valid JavaScript identifier
        expect(fileName).toMatch(/^[a-zA-Z_$][0-9a-zA-Z_$]*$/)

        // At least two words in the name
        expect(fileName.split(/(?=[A-Z])/).length).toBeGreaterThanOrEqual(2)

        // File should contain name in format: "'name: <componentName>'"
        const content = fs.readFileSync(file, 'utf-8')
        const isSetupScript = content.includes('<script setup>')
        if (!isSetupScript) {
          const namePattern = new RegExp(String.raw`name:\s*['"]${fileName}['"],`)
          expect(content).toMatch(namePattern, `File ${file} does not contain name: '${fileName}'`)
        }
      })
    })
  })

  describe('*Mixin.js', () => {
    // Find all '*Mixin.js' files in /src/
    const mixinFiles = findComponents(srcDir, 'Mixin.js')

    it('exports all *Mixin.js from /src/', () => {

      // Get the base name (without extension) for each '*Mixin.js' file
      // 'abcMixin.js' -> 'abcMixin'
      const mixinComponentNames = mixinFiles.map(f =>
        path.basename(f, '.js')
      )

      // Get all exported keys from library.js
      const exportedKeys = Object.keys(library)

      // Check that each mixin is exported
      const missing = mixinComponentNames.filter(
        name => !exportedKeys.includes(name)
      )

      expect(missing.map((name) => {
        const pathName = mixinFiles.find(f => path.basename(f, '.js') === name).substring(srcDir.length)
        return `import ${name} from '@${pathName}'`
      }).join('\n')).toEqual('')
    })
  })
})
