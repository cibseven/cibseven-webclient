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
import { ElementTemplateService } from '@/services.js'

/**
 * The element templates a diagram shows the icons of. Kept apart from the modeler's own store, so
 * viewing a diagram does not load the modeler.
 */
const ElementTemplateStore = {
  namespaced: true,
  state: {
    templates: []
  },
  mutations: {
    setTemplates: function(state, templates) {
      // A disabled modeler backend answers with the application page, not with templates
      state.templates = Array.isArray(templates) ? templates : []
    }
  },
  actions: {
    fetchAllElementTemplates: async function({ commit }) {
      commit('setTemplates', await ElementTemplateService.findAll())
    }
  },
  getters: {
    // The definitions of the active templates, as the diagram renderer takes them
    allElementTemplateContents: state => state.templates
      .filter(template => template.active && template.content?.trim())
      .map(template => {
        try {
          return JSON.parse(template.content)
        } catch {
          return null
        }
      })
      .filter(Boolean)
  }
}

export default ElementTemplateStore
