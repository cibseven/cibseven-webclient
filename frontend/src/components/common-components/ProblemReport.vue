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
        <Clipboard tabindex="-1" @input="clip = $event"></Clipboard>
      </b-form-group>
    </CIBForm>
    <template v-slot:modal-footer>
      <button @click="$refs.form.onSubmit()" class="btn btn-primary">{{ $t('problem-report.ok') }}</button>
      <button type="button" class="btn btn-secondary" @click="$refs.modal.hide()">{{ $t('problem-report.cancel') }}</button>
    </template>
  </b-modal>
</template>

<script>
import platform from 'platform'
import CIBForm from '@/components/common-components/CIBForm.vue'
import Clipboard from '@/components/common-components/Clipboard.vue'
import { axios } from '@/globals.js'

export default {
  name: 'ProblemReport',
  components: { CIBForm, Clipboard },
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
