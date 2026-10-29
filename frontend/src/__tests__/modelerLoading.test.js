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
import fs from 'node:fs'
import path from 'node:path'
import { findComponents } from './utils.js'

/**
 * The modeler outweighs the rest of the application several times over, so it is loaded only when
 * it is opened (CIB7-2454). One static import of the package anywhere would put it back into the
 * chunk every page loads at startup, and the build does not warn about it.
 */
describe('modeler loading', () => {
  const srcDir = path.resolve('src')
  const sources = findComponents(srcDir, '.js').concat(findComponents(srcDir, '.vue'))
    .filter(file => !file.includes(`${path.sep}__tests__${path.sep}`))
  const staticImport = /^\s*import\s+(?:[^'"]+\s+from\s+)?['"]cibseven-modeler(?:\/[^'"]*)?['"]/m

  it('imports cibseven-modeler nowhere statically', () => {
    expect(sources.length).toBeGreaterThan(100)
    const offenders = sources.filter(file => staticImport.test(fs.readFileSync(file, 'utf-8')))
      .map(file => path.relative(srcDir, file))

    expect(offenders).toEqual([])
  })

  it('still finds a static import, so the check above can fail', () => {
    expect(staticImport.test("import { CibsevenModeler } from 'cibseven-modeler'")).toBe(true)
    expect(staticImport.test("import 'cibseven-modeler/dist/cibseven-modeler.css'")).toBe(true)
    expect(staticImport.test("  const m = await import('cibseven-modeler')")).toBe(false)
  })
})
