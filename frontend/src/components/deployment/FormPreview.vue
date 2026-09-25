<!--

    Copyright CIB software GmbH and/or licensed to CIB software GmbH
    under one or more contributor license agreements. See the NOTICE file
    distributed with this work for additional information regarding copyright
    ownership. CIB software licenses this file to you under the Apache License,
    Version 2.0; you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.

-->
<template>
  <div class="form-preview">
    <div v-if="error" class="form-preview-error d-flex align-items-center">
      <span class="mdi mdi-48px mdi-file-cancel-outline pe-1 text-warning"></span>
      <span>{{ $t('deployment.formPreviewError') }}</span>
    </div>
    <div v-show="!error">
      <h5 class="form-preview-heading">{{ $t('deployment.formPreviewHeading') }}</h5>
      <div ref="container"></div>
      <h5 class="form-preview-heading mt-3">{{ $t('deployment.formRawDataHeading') }}</h5>
      <pre class="form-preview-raw">{{ rawData }}</pre>
    </div>
  </div>
</template>

<script>
import { Form } from '@bpmn-io/form-js'
import '@bpmn-io/form-js/dist/assets/form-js-base.css'
import '@bpmn-io/form-js/dist/assets/form-js.css'
import '@bpmn-io/form-js/dist/assets/flatpickr/light.css'

export default {
  name: 'FormPreview',
  emits: [],
  data: function () {
    return {
      error: false,
      form: null,
      rawData: ''
    }
  },
  beforeUnmount: function () {
    this.destroyForm()
  },
  methods: {
    destroyForm: function () {
      if (this.form) {
        try {
          this.form.destroy()
        } catch (err) {
          // already torn down
          console.warn('FormPreview: destroy failed', err)
        }
        this.form = null
      }
    },
    // Imperative entry point, mirrors BpmnViewer/DmnViewer.showDiagram(content).
    // Accepts the form-js schema either as an already-parsed object or as a JSON string.
    async showForm(content) {
      this.destroyForm()
      this.error = false
      const container = this.$refs.container
      if (!container) return
      let schema = null
      try {
        schema = typeof content === 'string' ? JSON.parse(content) : content
      } catch (err) {
        console.error('FormPreview: failed to parse form schema', err)
        this.error = true
        return
      }
      if (!schema || typeof schema !== 'object') {
        console.error('FormPreview: form schema is not an object', schema)
        this.error = true
        return
      }
      try {
        this.form = new Form({ container })
        await this.form.importSchema(schema)
        this.rawData = JSON.stringify(schema, null, 2)
      } catch (err) {
        console.error('FormPreview: failed to import form schema', err)
        this.error = true
      }
    }
  }
}
</script>

<style scoped>
.form-preview {
  height: 100%;
  overflow: auto;
  background: #fff;
  padding: 12px 14px;
}
.form-preview-error {
  padding: 8px 0;
}
.form-preview-heading {
  font-weight: 600;
  padding-bottom: 6px;
  margin-bottom: 12px;
  border-bottom: 1px solid #dee2e6;
}
.form-preview-raw {
  background: #f8f9fa;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  padding: 10px 12px;
  margin: 0;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
