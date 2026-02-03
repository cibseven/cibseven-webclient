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
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const languages = ['de', 'en', 'es', 'ru', 'ua']

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

    // Sort keys for comparison
    keysBase.sort()
    keysTest.sort()

    // Compare key sets
    const keysBaseSorted = keysBase.join(',')
    const keysTestSorted = keysTest.join(',')
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
      'n/a',
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
      'format',
      'name: {name}version: {version}',
      'element',
      'navigation',
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
  let status = true

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

let hasHeader = false
function reportSameValuesTable(objBase, objTest, languages, path) {
  // Check if both are objects and not null
  expect(objBase).not.toBeNull()
  expect(objTest).not.toBeNull()

  if (skipPath(path)) {
    return true
  }

  if (typeof objBase === 'string') {
    const hasSameValues = objTest.map(
      (v, index) => objBase === v && !skipValue(objBase, languages[index])
    ).find(Boolean)
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
    const additionalLanguages = languages.filter(lang => lang !== 'en')

    it.each(additionalLanguages)(`en.keys === %s.keys`, (lang) => {
      const translationLang = getTranslation(lang)
      expect(haveSameProperties(translationEn, translationLang, lang)).toBeTruthy()
    })

    it.each(additionalLanguages)(`en !== %s, report same values`, (lang) => {
      const translationLang = getTranslation(lang)
      expect(reportSameValues(translationEn, translationLang, lang, lang)).toBeTruthy()
    })

    it(`same values as table`, () => {
      const translations = additionalLanguages.map(lang => getTranslation(lang))
      expect(reportSameValuesTable(translationEn, translations, additionalLanguages, '')).toBeTruthy()
    })
  })
})
