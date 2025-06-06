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
import { createI18n } from 'vue-i18n'
import { mergeLocaleMessage as loadTranslationsFromCommonComponents } from 'cib-common-components'
import { axios, moment } from './globals.js'
import { getTheme } from './utils/init'
import 'moment/dist/locale/de'
import 'moment/dist/locale/es'
import 'moment/dist/locale/uk'
import 'moment/dist/locale/ru'

function getDefaultLanguage() {
  let language = localStorage.getItem('language')
  if (!language) {
      language = navigator.language.split('-')[0] || navigator.userLanguage || 'en'
      localStorage.setItem('language', language)
  }
  return language
}

const i18n = createI18n({
  locale: getDefaultLanguage()
})

import en from '@/assets/translations_en.json'
import de from '@/assets/translations_de.json'
import es from '@/assets/translations_es.json'
import ua from '@/assets/translations_ua.json'
import ru from '@/assets/translations_ru.json'

const translations = { en, de, es, ua, ru }

const loadTranslationsFromSevenComponents = function(i18n, lang) {
  const translation = translations[lang] || translations.en
  i18n.global.mergeLocaleMessage(lang, translation)
}

const loadTranslationsFromPublic = async function(lang) {
  // Load translations from public/translations_*.json
  try {
    const res = await axios.create().get('translations_' + lang + '.json')
    i18n.global.mergeLocaleMessage(lang, res.data)
  } catch {
    console.debug('Optional translations file not found:', 'translations_' + lang + '.json')
  }
}

const loadTranslationsFromThemes = async function(config, lang) {
  // Load translations from public/themes/translations_*.json
  try {
    const res = await axios.create().get('themes/' + getTheme(config) + '/translations_' + lang + '.json')
    i18n.global.mergeLocaleMessage(lang, res.data)
  } catch {
    console.debug('Optional theme translations file not found:', 'themes/' + getTheme(config) + '/translations_' + lang + '.json')
  }
}

// Available translation sources
const translationSources = {
  commonComponents: 'commonComponents',
  sevenComponents: 'sevenComponents',
  public: 'public',
  themes: 'themes'
}

const defaultTranslationSources = [
  translationSources.commonComponents,
  translationSources.sevenComponents,
  translationSources.public,
  translationSources.themes
]

const loadTranslations = async function(config, lang, sources = defaultTranslationSources) {
  if (sources.includes(translationSources.commonComponents)) {
    // Add translations from cib-common-components/src/assets/translations_*.json
    loadTranslationsFromCommonComponents(i18n, lang)
  }

  if (sources.includes(translationSources.sevenComponents)) {
    // Add translations from src/assets/translations_*.json
    loadTranslationsFromSevenComponents(i18n, lang)
  }

  if (sources.includes(translationSources.public)) {
    // Add translations from public/translations_*.json
    await loadTranslationsFromPublic(lang)
  }

  if (sources.includes(translationSources.themes)) {
    // Add translations from public/themes/translations_*.json
    await loadTranslationsFromThemes(config, lang)
  }
}

const setLanguage = function(language) {
  i18n.global.locale = language
  axios.defaults.headers.common['Accept-Language'] = language
  localStorage.setItem('language', language)
  document.documentElement.setAttribute('lang', language)
  moment.locale(language)
}

var loadedLanguages = []
const switchLanguage = async function(config, lang) {
  var language = config.supportedLanguages.includes(lang) ? lang : config.supportedLanguages[0]

  if (loadedLanguages.includes(language)) {
    setLanguage(language)
    return Promise.resolve(language)
  }

  // Load translations before switching language
  await loadTranslations(config, language)

  loadedLanguages.push(language)
  setLanguage(language)

  return Promise.resolve(language)
}

export {
  i18n,
  switchLanguage,
  loadTranslations,
  setLanguage,
  translationSources
}
