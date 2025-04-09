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
