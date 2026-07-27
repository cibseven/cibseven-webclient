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
// Persists the BpmnViewer pan/zoom viewport across navigation/reload, scoped to
// sessionStorage (cleared when the tab/browser closes). Expects the host component
// to expose `this.process.id` (process definition id) and a `ref="diagram"` pointing
// to a BpmnViewer/BpmnViewerPlugin instance.
export default {
  methods: {
    viewboxStorageKey: function() {
      return `bpmn-viewbox:${this.process.id}`
    },
    onViewboxChanged: function(viewbox) {
      try {
        sessionStorage.setItem(this.viewboxStorageKey(), JSON.stringify(viewbox))
      } catch {
        // sessionStorage unavailable or quota exceeded - ignore
      }
    },
    restoreViewboxIfSaved: function() {
      try {
        const raw = sessionStorage.getItem(this.viewboxStorageKey())
        if (raw) this.$refs.diagram.setViewbox(JSON.parse(raw))
      } catch {
        // ignore malformed/unavailable storage
      }
    }
  }
}
