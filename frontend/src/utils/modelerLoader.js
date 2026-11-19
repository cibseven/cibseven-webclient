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
import { i18n, registerTranslationLoader } from '@/i18n'
import { deepMerge } from '@/utils/config.js'

let modelerPackage = null
let translationsRegistered = false

/**
 * Loads cibseven-modeler and its styles when the modeler is first opened, and sets the store and
 * the translations up for it. The package is only ever imported dynamically: this module is part
 * of every bundle, the library included, and a static import would load the modeler at startup.
 *
 * @param {import('vuex').Store} store - The store the modeler components read
 * @param {object} [options]
 * @param {(lang: string) => object} [options.messages] - Messages layered over the modeler's own,
 *        for an edition that rewords or extends them
 * @returns {Promise<object>} The package, as cibseven-modeler exports it
 */
export async function loadModeler(store, options = {}) {
  modelerPackage ??= Promise.all([
    import('cibseven-modeler'),
    import('cibseven-modeler/dist/cibseven-modeler.css?inline')
  ]).then(([modeler, styles]) => {
    addStylesBeforeTheme('cibseven-modeler', styles.default)
    return modeler
  }).catch(error => {
    // A chunk that failed to load, after a deployment for instance, is tried again next time
    modelerPackage = null
    throw error
  })
  const modeler = await modelerPackage
  installModeler(store, modeler, options)
  return modeler
}

/**
 * Adds styles loaded on demand where they stood when they were part of the startup: before the
 * theme. Appended after it, a stylesheet that sets Bootstrap's variables, as the modeler's does,
 * would recolour the whole application. Each set of styles is added once.
 *
 * @param {string} name - Tells the sets of styles apart
 * @param {string} css - The styles, as text
 */
export function addStylesBeforeTheme(name, css) {
  if (document.head.querySelector(`style[data-styles="${name}"]`)) return
  const style = document.createElement('style')
  style.dataset.styles = name
  style.textContent = css
  document.head.insertBefore(style, document.head.querySelector('style[data-theme]'))
}

/**
 * Registers the modeler's store modules and translations, once.
 *
 * @param {import('vuex').Store} store - The store the modeler components read
 * @param {object} modeler - The stores and translations of cibseven-modeler, as it exports them
 * @param {object} [options] - As for loadModeler
 */
export function installModeler(store, modeler, { messages } = {}) {
  if (!store.hasModule('modeler')) {
    store.registerModule('modeler', {
      namespaced: true,
      modules: {
        processes: modeler.processStore,
        forms: modeler.formStore,
        elementTemplates: modeler.elementTemplateStore,
        xml: modeler.xmlStore
      }
    })
  }
  if (!translationsRegistered) {
    translationsRegistered = true
    // Merged for every language loaded so far and for each one switched to later
    registerTranslationLoader(lang => mergeBeneath(lang, modelerMessages(modeler, messages, lang)))
  }
}

function modelerMessages(modeler, messages, lang) {
  const own = modeler.modelerTranslations[lang] || modeler.modelerTranslations.en
  return messages ? deepMerge(own, messages(lang) || {}) : own
}

/**
 * The modeler's messages arrive after everything else, so they go underneath: what the application,
 * a theme, the public translation files or a plugin say for one of its keys still applies.
 */
function mergeBeneath(lang, messages) {
  i18n.global.setLocaleMessage(lang, deepMerge(messages, i18n.global.getLocaleMessage(lang)))
}
