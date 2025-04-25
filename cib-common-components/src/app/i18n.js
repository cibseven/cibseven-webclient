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

import en from '../assets/translations_en.json'
import de from '../assets/translations_de.json'
import es from '../assets/translations_es.json'
import it from '../assets/translations_it.json'
import ru from '../assets/translations_ru.json'

export const languages = {
  en: en,
  de: de,
  es: es,
  it: it,
  ru: ru
}

const i18n = createI18n({
  legacy: false,
  locale: localStorage.getItem('language') || navigator.language.split('-')[0] || navigator.userLanguage || 'en',
  fallbackLocale: 'en',
  messages: languages
})

export default i18n
