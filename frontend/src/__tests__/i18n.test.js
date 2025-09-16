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
  const filePath = resolve(__dirname, `../assets/translations_${lang}.json`)
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

function skipPath(path) {
  return path.includes('.operators.') || path.includes('.cib-header.')
    || path.includes('.flowModalSupport.phoneNumber')
    || path.includes('.flowModalSupport.email')
}

function skipValue(value, lang) {
  const ignoreWords = {
    '': [
      '',
      'cib seven', 'ok', 'id',
      'email',
      '{activityid}',

      'ctrl', // en = ru
      'chat', // en = de = es

      // module names
      'tasklist',
      'cockpit',
      'admin',

      // authorizations.types:
      'allow',
      'deny',
      'global',
    ],
    'de': [
      'system',
      'version',
      'name',
      'deployed',
      'information',
      'support',
      'batches',
      'deployments',
      'jobs',
      'stacktrace',
      'hostname',
      'filter',
      'dashboard',
      'filter',
      'status',
      'name: {name}version: {version}',
      'element'
    ],
    'es': [
      'tenant',
      'tenants',
      'error',
      'timestamp',
      'business key',
      'variables',
      'total',
    ],
    'ua': [
    ],
    'ru': [
    ]
  }

  const lower = value.toLowerCase()
  return ignoreWords[''].includes(lower) || ignoreWords[lang].includes(lower) || value.startsWith('@')
}

function reportSameValues(objBase, objTest, path, lang) {
  var status = true

  // Check if both are objects and not null
  expect(objBase).not.toBeNull()
  expect(objTest).not.toBeNull()

  if (typeof objBase === 'string' && typeof objTest === 'string') {
    if (!skipPath(path)) {
      if (objBase === objTest && ! skipValue(objBase, lang)) {
        console.log(`Error: Not translated: "${path}" = "${objBase}"`)
        status = false
      }
    }
  }
  else {
    expect(objBase).toBeTypeOf('object')
    const keysBase = Object.keys(objBase)

    // Recurse into nested objects
    for (const key of keysBase) {
      if (!reportSameValues(objBase[key], objTest[key], path + '.' + key, lang)) {
        status = false
      }
    }
  }

  return status
}

var hasHeader = false
function reportSameValuesTable(objBase, objTest, languages, path) {
  // Check if both are objects and not null
  expect(objBase).not.toBeNull()
  expect(objTest).not.toBeNull()

  if (typeof objBase === 'string') {
    if (!skipPath(path)) {

      const hasSameValues = objTest.map(
        (v, index) => objBase === v && !skipValue(objBase, languages[index])
      ).find(v => v)
      if (hasSameValues) {

        if (!hasHeader) {
          console.log(`Error: Next strings have the same values comparing to EN`)
          hasHeader = true
        }

        const v = objTest.map(
          (v, index) => (objBase === v && !skipValue(objBase, languages[index])) ? languages[index] : '  '
        ).join(' | ')
        console.log(`| en | ${v} | ${path} |`)
      }
    }
  }
  else {
    expect(objBase).toBeTypeOf('object')
    const keysBase = Object.keys(objBase)

    // Recurse into nested objects
    for (const key of keysBase) {
      if (!reportSameValuesTable(objBase[key], objTest.map(k => k[key]), languages, path + '.' + key)) {
        return false
      }
    }
  }

  return true
}

describe('translations', () => {
  const languages = ['de', 'en', 'es', 'ru', 'ua']

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

    languages.filter(lang => lang !== 'en').forEach(lang => {
      it(`${lang}, report same values`, () => {
        const translationLang = getTranslation(lang)
        expect(reportSameValues(translationEn, translationLang, lang, lang)).toBeTruthy()
      })
    })

    it(`same values as table`, () => {
      const filteredLanguages = languages.filter(lang => lang !== 'en')
      const translations = filteredLanguages.map(lang => getTranslation(lang))
      expect(reportSameValuesTable(translationEn, translations, filteredLanguages, '')).toBeTruthy()
    })
  })
})
