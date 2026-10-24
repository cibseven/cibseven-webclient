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

/**
 * Inside the application a language is a short code - `de`, `zh`, `zh-tw` - because
 * that is the key its messages are stored under. Browsers, URLs, stored preferences
 * and host applications, however, hand out full BCP 47 tags such as `zh-TW` or
 * `de-DE`, and the libraries this application composes merge their messages by the
 * same short code. Resolving every incoming tag to that code keeps all the sources
 * under one key: a tag that only matches by prefix would otherwise create a second,
 * empty locale and fall back to English.
 */

// Chinese needs its region to be told apart, and `hant` names traditional script
// on its own, without one.
const traditionalChineseSubtags = ['tw', 'hk', 'mo', 'hant']

const normalizeLanguage = function(language) {
  if (!language || typeof language !== 'string') return 'en'

  // Underscore tags (`zh_TW`) are what Java style locales use
  const tag = language.trim().replace(/_/g, '-').toLowerCase()
  if (!tag) return 'en'

  if (tag === 'zh' || tag.startsWith('zh-')) {
    const subtags = tag.split('-')
    return subtags.some(subtag => traditionalChineseSubtags.includes(subtag)) ? 'zh-tw' : 'zh'
  }

  return tag.split('-')[0]
}

/**
 * Picks the language to switch to. The configured list has the last word: a tag the
 * application has no translations for falls back to its first language, which is
 * what the header and the deep links already relied on.
 *
 * The comparison is made on normalized codes, so a configuration may spell a
 * language either way (`zh-TW` or `zh-tw`); the returned value is always the code
 * the messages are stored under.
 *
 * @param {string[]} supportedLanguages
 * @param {string} language
 * @returns {string}
 */
const resolveLanguage = function(supportedLanguages, language) {
  const supported = (supportedLanguages || []).map(normalizeLanguage)
  const requested = normalizeLanguage(language)

  const index = supported.indexOf(requested)
  if (index !== -1) return supported[index]

  return supported[0] || requested
}

export {
  normalizeLanguage,
  resolveLanguage
}
