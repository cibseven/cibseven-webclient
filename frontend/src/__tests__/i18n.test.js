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
import { findComponents } from './utils.js'
import { mergeLocaleMessage as commonFrontendMergeLocaleMessage } from '@cib/common-frontend'

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
    keysBase.sort((a, b) => a.localeCompare(b))
    keysTest.sort((a, b) => a.localeCompare(b))

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

function extractKeys(obj, path) {
  const stringKeys = []
  if (typeof obj === 'string') {
    stringKeys.push(path)
  } else if (typeof obj === 'object' && obj !== null) {
    for (const key of Object.keys(obj)) {
      const keyPath = path.length > 0 ? `${path}.${key}` : key
      stringKeys.push(
        ...extractKeys(obj[key], keyPath),
      )
    }
  }
  return stringKeys
}

describe('i18n', () => {

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

  describe('usage', () => {
    it.skip('all en keys should be used', () => {
      const translationEn = getTranslation('en')

      // convert transaltion object to flat list of keys
      let stringLongKeys = extractKeys(translationEn, '')
      expect(stringLongKeys.length).toBeGreaterThan(0)

      const langKeys = languages.map(l => `cib-header.${l}`)
      stringLongKeys = stringLongKeys.filter(keyPath => 
        !keyPath.startsWith('admin.authorizations.resourcesTypes.') &&
        !keyPath.startsWith('errors.') &&
        !keyPath.startsWith('nav-bar.filters.keys.') &&
        !langKeys.includes(keyPath)
      )

      // Find all .vue files in /src/
      const vueFiles = findComponents('src', '.vue')
      expect(vueFiles.length).toBeGreaterThan(0)

      const jsFiles = findComponents('src', '.js')
      expect(jsFiles.length).toBeGreaterThan(0)

      const patterns = [
        key => `$t('${key}'`,
        key => ` label: '${key}'`,
        key => ` title: '${key}'`,
        key => ` tooltip: '${key}'`,
        key => ` text: '${key}'`, // TODO: remove after `GenericTabs` refactoring
        key => ` message: '${key}'`, // TODO: remove after ('filter-alert', { message: ...}) refactoring
        key => `return '${key}'`,
        key => `? '${key}' :`,
        key => `: '${key}'`,
        key => `// - '${key}'`,
        key => `keypath="${key}"`,
        key => `i18n.global.t('${key}'`,
      ]

      // Check usage of each key in .vue files
      // When found in any file, we consider it used => remove from list
      for (const file of [...vueFiles, ...jsFiles]) {
        const content = readFileSync(file, 'utf-8')
        stringLongKeys = stringLongKeys.filter(keyPath => {
          return !patterns.some(pattern =>
            content.includes(pattern(keyPath))
          )
        })
      }

      // Report unused keys
      if (stringLongKeys.length > 0) {
        stringLongKeys = stringLongKeys.sort((a, b) => a.localeCompare(b))
        const message = `Unused translation keys in en (checked ${vueFiles.length} .vue files):\n` + stringLongKeys.map(k => `- ${k}`).join('\n')
        expect(message).toBe('')
      }
      expect(stringLongKeys.length).toBe(0)
    })

    it('all used keys should be declared in en', () => {
      const translationEn = getTranslation('en')

      // convert transaltion object to flat list of keys
      const stringLongKeys = extractKeys(translationEn, '')
      expect(stringLongKeys.length).toBeGreaterThan(0)
      let notDeclaredKeys = []

      // get keys from @cib/common-frontend package using commonFrontendMergeLocaleMessage()
      const commonFrontendTranslationEn = {}
      commonFrontendMergeLocaleMessage({ global: { mergeLocaleMessage: (lang, messages) => {
        Object.assign(commonFrontendTranslationEn, messages)
      } } }, 'en')
      const commonFrontendStringLongKeys = extractKeys(commonFrontendTranslationEn, '')
      expect(commonFrontendStringLongKeys.length).toBeGreaterThan(0)
      // add them to declared keys list
      stringLongKeys.push(
        ...commonFrontendStringLongKeys,
      )

      // Find all .vue files in /src/
      const vueFiles = findComponents('src', '.vue')
      expect(vueFiles.length).toBeGreaterThan(0)

      // Check usage of each used key in .vue files
      // When not found in localization file, we consider it is not declared => report error
      for (const file of vueFiles) {
        const content = readFileSync(file, 'utf-8')

        // get all $t('...') usages in the file
        const usagePattern = /\$t\(\s*['"`]([^'"`]+)['"`]/g
        let match
        while ((match = usagePattern.exec(content)) !== null) {
          const keyPath = match[1]

          // Remove ignored paths
          if (skipPath(keyPath)) {
            continue
          }

          // Check if key is declared in en translation
          const isDeclared = stringLongKeys.includes(keyPath)
          if (!isDeclared) {

            if (keyPath.endsWith('.') || keyPath.includes('${')) {
              continue
            }

            // Not declared, add to report list
            notDeclaredKeys.push(keyPath)
          }
        }
      }

      // Report unused keys
      if (notDeclaredKeys.length > 0) {
        notDeclaredKeys = notDeclaredKeys.sort((a, b) => a.localeCompare(b))
        const message = `Next translation keys are missing in en, but used in checked ${vueFiles.length} .vue files:\n` + notDeclaredKeys.map(k => `- ${k}`).join('\n')
        expect(message).toBe('')
      }
      expect(notDeclaredKeys.length).toBe(0)
    })
  })

  it('all own keys should not redeclare keys from @cib/common-frontend', () => {
    const translationEn = getTranslation('en')

    // convert transaltion object to flat list of keys
    const ownLongKeys = extractKeys(translationEn, '')
    expect(ownLongKeys.length).toBeGreaterThan(0)

    // get keys from @cib/common-frontend package using commonFrontendMergeLocaleMessage()
    const parentEn = {}
    commonFrontendMergeLocaleMessage({ global: { mergeLocaleMessage: (lang, messages) => {
      Object.assign(parentEn, messages)
    } } }, 'en')
    const parentLongKeys = extractKeys(parentEn, '')
    expect(parentLongKeys.length).toBeGreaterThan(0)

    // Check that none of own keys is in common-frontend keys
    expect(ownLongKeys.length).not.toBe(parentLongKeys.length)
    const redeclaredKeys = ownLongKeys.filter(k => parentLongKeys.includes(k))
    if (redeclaredKeys.length > 0) {
      const message = 'Next translation keys are redeclaring keys from @cib/common-frontend:\n' + redeclaredKeys.map(k => `- ${k}`).join('\n')
      expect(message).toBe('')
    }
    expect(redeclaredKeys.length).toBe(0)
  })
})
