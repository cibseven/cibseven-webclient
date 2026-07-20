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
  <div class="html-form-preview">
    <div v-if="error" class="html-form-preview-error d-flex align-items-center">
      <span class="mdi mdi-48px mdi-file-cancel-outline pe-1 text-warning"></span>
      <span>{{ $t('deployment.formPreviewError') }}</span>
    </div>
    <div v-show="!error">
      <iframe ref="frame" class="html-form-preview-frame" sandbox="allow-same-origin"
        :title="$t('deployment.formPreviewHeading')"></iframe>
      <h5 class="html-form-preview-heading mt-3">{{ $t('deployment.formRawDataHeading') }}</h5>
      <pre class="html-form-preview-raw">{{ rawData }}</pre>
    </div>
  </div>
</template>

<script>
export default {
  name: 'HtmlFormPreview',
  emits: [],
  data: function () {
    return {
      error: false,
      rawData: ''
    }
  },
  methods: {
    // Imperative entry point, mirrors FormPreview.showForm(content).
    // Renders the raw HTML form in a sandboxed iframe and shows the source below.
    showForm(content) {
      this.error = false
      const html = typeof content === 'string' ? content : String(content ?? '')
      if (!html.trim()) {
        this.error = true
        return
      }
      this.rawData = html
      const frame = this.$refs.frame
      if (!frame) return
      try {
        const doc = frame.contentDocument || frame.contentWindow?.document
        doc.open()
        doc.write(html)
        doc.close()
      } catch (err) {
        console.error('HtmlFormPreview: failed to render html form', err)
        this.error = true
      }
    }
  }
}
</script>

<style scoped>
.html-form-preview {
  height: 100%;
  overflow: auto;
  background: #fff;
  padding: 12px 14px;
}
.html-form-preview-error {
  padding: 8px 0;
}
.html-form-preview-frame {
  width: 100%;
  min-height: 260px;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  background: #fff;
}
.html-form-preview-heading {
  font-weight: 600;
  padding-bottom: 6px;
  margin-bottom: 12px;
  border-bottom: 1px solid #dee2e6;
}
.html-form-preview-raw {
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
