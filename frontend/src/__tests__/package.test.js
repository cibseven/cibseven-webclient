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
import { findComponents } from './utils.js'
import fs from 'node:fs'
import path from 'node:path'

// eslint-disable-next-line no-undef
const srcDir = path.resolve(__dirname, '../')

function getPackageJson(relativePath = '../package.json') {
  const packageJsonPath = path.resolve(srcDir, relativePath)
  const packageJsonContent = fs.readFileSync(packageJsonPath, 'utf-8')
  return JSON.parse(packageJsonContent)
}

function checkLicenseHeader(files) {
  for (const f of files) {
    const content = fs.readFileSync(f, 'utf-8')
    const hasLicenseHeader = content.includes('Copyright CIB software GmbH') && content.includes('apache.org/licenses/LICENSE-2.0')
    const message = hasLicenseHeader || `File "${f}" is missing license header`
    expect(message).toBe(true)
  }
}

describe('package', () => {

  describe('license header', () => {
    it.each([
      ['.vue'],
      ['.js'],
      ['.scss'],
    ])('all %s files have license headers', (extension) => {
      const files = findComponents(srcDir, extension)
      expect(files.length).toBeGreaterThan(0)
      checkLicenseHeader(files)
    })
  })

  describe('package.json', () => {
    it('has correct name and version', () => {
      const packageJson = getPackageJson()
      expect(packageJson.name).toBe('cibseven-components', 'Package name is incorrect')
      expect(packageJson.version).toMatch(/^\d+\.\d+\.\d+(-.+)?$/, 'Package version is not a valid semver')
    })

    describe('dependencies', () => {
      it('fixed versions', () => {
        const packageJson = getPackageJson()
        const dependencies = packageJson.dependencies || {}
        for (const [dep, version] of Object.entries(dependencies)) {
          expect(version).not.toMatch(/^[\^~]/, `Dependency ${dep} has a version (${version}) that is not fixed`)
        }
      })

      describe('friezed dependencies', () => {
        const lockPackageJson = getPackageJson('../package-lock.json')

        it.each([
          ['apexcharts', '4.7.0'],
          ['vue3-apexcharts', '1.8.0'],
        ])('%s version %s', (packageName, packageVersion) => {
          const packageJson = getPackageJson()
          const dependencies = packageJson.dependencies || {}
          expect(dependencies[packageName]).toBeDefined(`${packageName} is not listed as a dependency`)
          expect(dependencies[packageName]).toBe(packageVersion, `${packageName} version is incorrect`)

          const lockName = `node_modules/${packageName}`
          const dep = lockPackageJson.packages[lockName]
          expect(dep).toBeDefined(`${packageName} is not listed in package-lock.json`)
          expect(dep.version).toBe(packageVersion, `${packageName} version in package-lock.json is incorrect`)
          expect(dep.license).toBe('MIT', `${packageName} license in package-lock.json is incorrect`)
        })
      })
    })
  })
})
