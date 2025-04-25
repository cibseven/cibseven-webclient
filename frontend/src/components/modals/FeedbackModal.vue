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
  <b-modal ref="modal" :title="$t('problem-report.title')" @shown="$refs.textArea.focus()">
    <CIBForm ref="form" @submitted="report(); $refs.modal.hide(); problem = null">
      <b-form-group :invalid-feedback="$t('errors.invalid')">
        <input v-model="email2" type="email" :placeholder="$t('problem-report.email')" class="form-control">
      </b-form-group>
      <b-form-group>
        <b-form-textarea ref="textArea" v-model="problem" :rows="10" :max-rows="10" required></b-form-textarea>
      </b-form-group>
      <b-form-group>
        <FeedbackScreenshot tabindex="-1" @input="clip = $event"></FeedbackScreenshot>
      </b-form-group>
    </CIBForm>
    <template v-slot:modal-footer>
      <b-button @click="$refs.modal.hide()" variant="link">{{ $t('problem-report.cancel') }}</b-button>
      <b-button @click="$refs.form.onSubmit()" variant="primary">{{ $t('problem-report.ok') }}</b-button>
    </template>
  </b-modal>
</template>

<script>
import platform from 'platform'
import CIBForm from '@/components/common-components/CIBForm.vue'
import FeedbackScreenshot from '@/components/modals/FeedbackScreenshot.vue'
import { axios } from '@/globals.js'

export default {
  name: 'FeedbackModal',
  components: { CIBForm, FeedbackScreenshot },
  props: { url: String, email: String },
  data: function() { return { problem: '', email2: null, clip: null } },
  methods: {
    show: function() {
      this.email2 = this.email
      this.$refs.modal.show()
    },
    report: function() {
      var params = {
        email : this.email2,
        platform: platform,
        screen: {
          height: screen.height,
          width: screen.width
        }
      }
      this.$emit('report', params)
      console && console.warn('Reporting problem', params)
      var formData = new FormData()
      var text = window.location.origin + window.location.pathname + '\n' + this.problem
      formData.append('description', new Blob([text], { type : 'text/plain' }), 'description.txt')
      formData.append('logs', new Blob([JSON.stringify(params)], { type : 'application/json' }), 'params.json')
      if (this.clip) formData.append('original', this.clip)
      axios.post(this.url, formData, { headers: { 'Content-Type': 'multipart/form-data' } })
        .then(function(res) { this.$emit('sent', res) }.bind(this))
    }
  }
}
</script>
