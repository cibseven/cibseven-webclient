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
import { readFileSync } from 'fs'
import { resolve } from 'path'

function getTranslation(lang) {
  // eslint-disable-next-line no-undef
  const filePath = resolve(__dirname, `../../public/translations_${lang}.json`)
  const translation = JSON.parse(readFileSync(filePath, 'utf-8'))
  return translation
}

function haveSameProperties(objBase, objTest, path) {
  // Check if both are objects and not null
  expect(objBase).not.toBeNull()
  expect(objTest).not.toBeNull()

  if (typeof objBase === 'string' && typeof objTest === 'string') {
    // nothing to check
  }
  else {
    expect(objBase).toBeTypeOf('object')
    expect(objTest).toBeTypeOf('object')

    const keysBase = Object.keys(objBase)
    const keysTest = Object.keys(objTest)

    // Compare key sets
    const keysBaseSorted = keysBase.sort().join(',')
    const keysTestSorted = keysTest.sort().join(',')
    expect(keysBaseSorted, `Missing/extra key for "${path}" path`).toBe(keysTestSorted)

    // Recurse into nested objects
    for (const key of keysBase) {
      if (!haveSameProperties(objBase[key], objTest[key], path + '.' + key)) {
        return false
      }
    }
  }

  return true
}

describe('translations', () => {
  const languages = ['de', 'en', 'ru', 'es']

  describe('loadable', () => {
    languages.forEach(lang => {
      it(`${lang}`, () => {
        const translations = getTranslation(lang)
        expect(translations).toBeDefined()
      })
    })
  })

  describe('compare en with', () => {
    const translationEn = getTranslation('en')
    languages.filter(lang => lang !== 'en').forEach(lang => {
      it(`${lang}`, () => {
        const translationLang = getTranslation(lang)
        expect(haveSameProperties(translationEn, translationLang, lang)).toBeTruthy()
      })
    })
  })
})
