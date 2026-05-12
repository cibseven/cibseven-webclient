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

/**
 * Check that all dependencies from project1 are present in project2 with the same version. If a dependency is missing in project2, it is ignored (we only care about version mismatches for dependencies that are present in both projects)
 * @param {*} project1 the main project, we want to ensure that all its dependencies are present in the sub project with the same version
 * @param {*} project2 the sub project, we want to check that all dependencies from the main project are present in this sub project with the same version
 * @param {*} project2Name name of the sub project, used for error messages
 * @returns empty string if all dependencies from project1 are present in project2 with the same version, otherwise a message describing the first mismatch found
 */
function checkDepVersions(project1, project2, project2Name) {
  const extractDeps = (deps) => deps ? Object.entries(deps) : []

  const deps1 = [
    ...extractDeps(project1.dependencies),
    ...extractDeps(project1.peerDependencies),
    ...extractDeps(project1.devDependencies),
  ]

  const deps2 = [
    ...extractDeps(project2.dependencies),
    ...extractDeps(project2.peerDependencies),
    ...extractDeps(project2.devDependencies),
  ]

  for (const [dep, version] of deps1) {
    const dep2 = deps2.find(([d]) => d === dep)
    if (!dep2) {
      continue // ignore missing dependencies in project2, we only care about version mismatches for dependencies that are present in both projects
    }
    const version2 = dep2[1]
    if (version !== version2) {
      if (version2.startsWith('^') && version2.substring(1) === version) {
        continue
      }
      return `Dependency ${dep} has version ${version} in ${project1.name} but ${version2} in ${project2Name}`
    }
  }
  return ''
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

    it('ensure npm install was run', () => {
      const packageJson = getPackageJson()
      const dependencies = packageJson.dependencies || {}
      expect(Object.keys(dependencies).length).toBeGreaterThan(0, 'No dependencies found in package.json')

      const lockPackageJson = getPackageJson('../package-lock.json')
      expect(lockPackageJson.packages).toBeDefined('No packages field in package-lock.json')
      expect(Object.keys(lockPackageJson.packages).length).toBeGreaterThan(0, 'No packages listed in package-lock.json')

      for (const [dep, version] of Object.entries(dependencies)) {
        const lockName = `node_modules/${dep}`
        const depInfo = lockPackageJson.packages[lockName]
        expect(depInfo).toBeDefined(`${dep} is not listed in package-lock.json`)
        expect(depInfo.version).toBe(version, `${dep} version in package-lock.json does not match package.json`)
      }
    })

    /**
     * Ensure all sub projects from the list have the same dependency as main project
     */
    it('ensure all sub projects have the same dependencies as main project', () => {
      const packageJson = getPackageJson()
      const dependencies = packageJson.dependencies || {}
      expect(Object.keys(dependencies).length).toBeGreaterThan(0, 'No dependencies found in package.json')

      const lockPackageJson = getPackageJson('../package-lock.json')
      expect(lockPackageJson.packages).toBeDefined('No packages field in package-lock.json')
      expect(Object.keys(lockPackageJson.packages).length).toBeGreaterThan(0, 'No packages listed in package-lock.json')

      const subProjects = [
        'cibseven-modeler',
        '@cib/common-frontend',
        '@cib/bootstrap-components',
      ]

      for (const subProject of subProjects) {
        const lockName = `node_modules/${subProject}`
        const subProjectInfo = lockPackageJson.packages[lockName]

        const mismatchVersion = checkDepVersions(packageJson, subProjectInfo, subProject)
        expect(mismatchVersion).toBe('', `Version mismatch found in sub project ${subProject}: ${mismatchVersion}`)
      }
    })
  })
})
