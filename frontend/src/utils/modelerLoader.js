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

let modelerPackage = null
let translationsRegistered = false

/**
 * Loads cibseven-modeler and its styles when the modeler is first opened, and sets the store and
 * the translations up for it. The package is only ever imported dynamically: this module is part
 * of every bundle, the library included, and a static import would load the modeler at startup.
 *
 * @param {import('vuex').Store} store - The store the modeler components read
 * @returns {Promise<object>} The package, as cibseven-modeler exports it
 */
export async function loadModeler(store) {
  modelerPackage ??= Promise.all([
    import('cibseven-modeler'),
    import('cibseven-modeler/dist/cibseven-modeler.css')
  ]).then(([modeler]) => modeler).catch(error => {
    // A chunk that failed to load, after a deployment for instance, is tried again next time
    modelerPackage = null
    throw error
  })
  const modeler = await modelerPackage
  installModeler(store, modeler)
  return modeler
}

/**
 * Registers the modeler's store modules and translations, once. Takes the package as an argument,
 * for an edition that loads it together with its own.
 *
 * @param {import('vuex').Store} store - The store the modeler components read
 * @param {object} modeler - The stores and translations of cibseven-modeler, as it exports them
 */
export function installModeler(store, { processStore, formStore, elementTemplateStore, xmlStore, mergeModelerTranslations }) {
  if (!store.hasModule('modeler')) {
    store.registerModule('modeler', {
      namespaced: true,
      modules: {
        processes: processStore,
        forms: formStore,
        elementTemplates: elementTemplateStore,
        xml: xmlStore
      }
    })
  }
  if (!translationsRegistered) {
    translationsRegistered = true
    // Merged for every language loaded so far and for each one switched to later
    registerTranslationLoader(lang => mergeModelerTranslations(i18n, lang))
  }
}
