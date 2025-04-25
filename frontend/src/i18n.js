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
import { mergeLocaleMessage } from 'cib-common-components'
import moment from 'moment'
import { axios } from './globals.js'
import { getTheme } from './utils/init'

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

var loadedLanguages = []
function fetchTranslation(config, lang) {
  // http://kazupon.github.io/vue-i18n/guide/lazy-loading.html
  return loadedLanguages.includes(lang) ? Promise.resolve() : axios.create().get('translations_' + lang + '.json').then(res => {
    i18n.global.setLocaleMessage(lang, res.data)
    mergeLocaleMessage(i18n, lang)
    // Load custom translations files
    axios.create().get('themes/' + getTheme(config) + '/translations_' + lang + '.json').then(res => {
      i18n.global.mergeLocaleMessage(lang, res.data)
      loadedLanguages.push(lang)
    }).catch(() => {
      loadedLanguages.push(lang)
    })
  })
}

const switchLanguage = function(config, lang) {
  var language = config.supportedLanguages.includes(lang) ? lang : config.supportedLanguages[0]

  // load localzation before switching language
  return fetchTranslation(config, language).then(() => {
    i18n.global.locale = language
    axios.defaults.headers.common['Accept-Language'] = language
    localStorage.setItem('language', language)
    document.documentElement.setAttribute('lang', language)
    moment.locale(language)

    return language
  })
}

export {
  i18n,
  switchLanguage
}
