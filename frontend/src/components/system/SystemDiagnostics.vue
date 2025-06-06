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
  <div class="container-fluid pt-4 d-flex flex-column h-100">
    <h4>{{ $t('admin.system.system-diagnostics.title') }}</h4>
    <div class="flex-fill d-flex flex-column overflow-hidden">
      <div class="alert alert-info">{{ $t('admin.system.system-diagnostics.diagnosticHelp') }}</div>
      <div class="diagnostic-container position-relative flex-fill d-flex flex-column overflow-hidden">
        <div class="px-3 py-2">
          {{ $t('admin.system.system-diagnostics.diagnosticData') + ':' }}
          <button :title="$t('commons.copyValue')"
            @click.stop="copyValueToClipboard(diagnosticsFormatted)"
            class="mdi mdi-18px mdi-content-copy px-2 btn btn-sm btn-link position-absolute top-0 d-none diagnostic-copy-btn"
          ></button>
        </div>
        <div class="card flex-fill overflow-hidden mb-3">
          <div class="card-body p-0 h-100 d-flex flex-column">
            <pre class="mb-0 flex-fill overflow-auto p-3">{{ diagnosticsFormatted }}</pre>
          </div>
        </div>
      </div>
    </div>
    <SuccessAlert ref="messageCopy"> {{ $t('decision.copySuccess') }} </SuccessAlert>
  </div>
</template>

<script>
import { SystemService } from '@/services.js'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'

export default {
  name: 'SystemDiagnostics',
  components: { SuccessAlert },
  mixins: [copyToClipboardMixin],
  data() {
    return {
      diagnostics: ''
    }
  },
  computed: {
    diagnosticsFormatted() {
      if (this.diagnostics) return JSON.stringify(this.diagnostics, null, 2)
      return ''
    }
  },
  async mounted() {
    this.diagnostics = await SystemService.getTelemetryData()
  }
}
</script>

<style lang="css" scoped>
.diagnostic-container:hover .diagnostic-copy-btn {
  display: inline-block !important;
}
</style>
