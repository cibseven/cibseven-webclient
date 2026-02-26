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
  <div>
    <b-modal ref="modal" :title="$t('process-instance.stacktrace')" size="xl" :ok-only="true">
      <div class="container-fluid p-0">
        <div class="position-relative">
          <label class="visually-hidden" for="stackTraceTextarea">{{ $t('process-instance.stacktrace') }}</label>
          <textarea
            ref="textarea"
            v-model="stackTraceMessage"
            rows="20"
            readonly
            :aria-label="$t('process-instance.stacktrace')"
            class="form-control w-100"
            id="stackTraceTextarea"
          ></textarea>
          <b-button
            type="button"
            variant="link"
            class="position-absolute end-0 top-0 mt-2 me-2"
            style="z-index: 2"
            @mousedown="selectTextarea"
            @click="copyValueToClipboard"
            :title="$t('commons.copyValue')"
          >
            <span class="mdi mdi-content-copy mdi-18px" aria-label="Copy"></span>
          </b-button>
        </div>
      </div>
    </b-modal>
    <SuccessAlert ref="messageCopy" style="z-index: 9999">{{ $t('process.copySuccess') }}</SuccessAlert>
  </div>
</template>

<script>
import { SuccessAlert } from '@cib/common-frontend'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'

export default {
  name: 'StackTraceModal',
  components: {
    SuccessAlert
  },
  mixins: [copyToClipboardMixin],
  data: function() {
    return {
      stackTraceMessage: ''
    }
  },
  methods: {
    show(stackTraceMessage) {
      this.stackTraceMessage = stackTraceMessage || ''
      this.$refs.modal.show()
    },
    hide() {
      this.$refs.modal.hide()
    },
    selectTextarea() {
      if (this.$refs.textarea) {
        this.$refs.textarea.focus()
        this.$refs.textarea.select && this.$refs.textarea.select()
      }
    }
  }
}
</script>
