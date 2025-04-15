<!-- eslint-disable vue/no-mutating-props -->
<template>
  <b-modal ref="otpDialog" v-if="credentials2" :title="$t('login.2fa')" @shown="$refs.otp.focus()">
    <CIBForm ref="form2" @submitted="onLogin2">
      <div class="mb-3">{{ $t('login.needOtp') }}</div>
      <b-form-group :invalid-feedback="$t('errors.invalid')">
        <input ref="otp" type="number" max="999999" v-model="credentials2.otp" :placeholder="$t('login.otp')" class="form-control" required>
      </b-form-group>
    </CIBForm>
    <template v-slot:modal-footer>
      <button class="btn btn-primary" @click="$refs.form2.onSubmit()">{{ $t('login.login') }}</button>
    </template>
  </b-modal>
</template>

<!-- eslint-disable vue/no-mutating-props -->
<script>
import AuthService from '@/components/login/authService.js'
import CIBForm from '@/components/common-components/CIBForm.vue'

export default {
  name: 'OtpDialog',
  components: { CIBForm },
  props: { credentials2: Object, rememberMe: Boolean },
  data: function() { return { busy: false } },
  methods: {
    show: function(userId) {
      this.userId = userId
      this.$refs.otpDialog.show()
      this.requestOtp()
    },
    requestOtp: function() {
      this.busy = true
      AuthService.poll4otp(this.userId).then(function(otp) {
        if (otp) { // not timeout
          this.credentials2.otp = otp
          this.onLogin2()
        }
      }.bind(this)).finally(function() { this.busy = false }.bind(this))
    },
    onLogin2: function() {
      AuthService.login(this.credentials2, this.rememberMe).then(function(user) { this.$emit('success', user) }.bind(this), function(error) {
        var res = error.response.data
        if (res && res.type === 'LoginException') res.type = 'LoginExceptionTwoFactor'
        this.$root.$refs.error.show(res)
      }.bind(this))
    }
  }
}
</script>
