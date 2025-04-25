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
<!-- eslint-disable vue/no-mutating-props -->
<template>
  <b-modal ref="resetDialog" v-if="credentials2" hide-footer no-close-on-backdrop :title="$t('login.2fa')" @shown="$refs.otp2.focus()">
    <div class="mb-3">{{ $t('login.needOtp') }}</div>
    <b-form-group label-cols="4" :label="$t('login.otp')" :invalid-feedback="$t('errors.invalid')">
      <input ref="otp2" type="number" max="999999" v-model="credentials2.otp" class="form-control">
    </b-form-group>
    <div class="form-row justify-content-end pr-1">
      <button :disabled="!credentials2.otp" class="btn btn-primary" @click="onForgotten2">{{ $t('login.resetPassword') }}</button>
    </div>
    <hr>
    <b-form-group label-cols="4" :label="$t('login.username')" :invalid-feedback="$t('errors.invalid')">
      <input v-model="credentials2.username" class="form-control">
    </b-form-group>
    <div class="form-group form-row">
      <label class="col-4 col-form-label">{{ $t('login.password') }}</label>
      <SecureInput v-model="credentials2.password" autocomplete="current-password" class="col-8"></SecureInput>
    </div>
    <div class="form-row justify-content-end pr-1">
      <button :disabled="!credentials2.username || !credentials2.password" class="btn btn-primary" @click="onForgotten3">{{ $t('login.reset2fa') }}</button>
    </div>
  </b-modal>
</template>

<!-- eslint-disable vue/no-mutating-props -->
<script>
import AuthService from '@/components/login/authService.js'
import SecureInput from '@/components/login/SecureInput.vue'

export default {
  name: 'ResetDialog',
  components: { SecureInput },
  props: { credentials2: Object },
  data: function() { return { busy: false } },
  methods: {
    show: function(userId) {
      this.userId = userId
      this.$refs.resetDialog.show()
      this.requestOtp()
    },
    requestOtp: function() {
      this.busy = true
      AuthService.poll4otp(this.userId).then(function(otp) {
        if (otp) { // not timeout
          this.credentials2.otp = otp
          this.onForgotten2()
        }
      }.bind(this)).finally(function() { this.busy = false }.bind(this))
    },
    onForgotten2: function() {
      delete this.credentials2.username
      delete this.credentials2.password
      AuthService.requestPasswordReset(this.credentials2).then(function() {
        this.$refs.resetDialog.hide()
        this.$emit('success')
      }.bind(this), function(error) {
        var res = error.response.data
        if (res && res.type === 'LoginException') res.type = 'LoginExceptionTwoFactor'
        this.$root.$refs.error.show(res)
      }.bind(this))
    },
    onForgotten3: function() {
      delete this.credentials2.otp
      AuthService.requestPasswordReset(this.credentials2).then(function() {
        this.$refs.resetDialog.hide()
        this.$emit('success')
      }.bind(this), function(error) {
        var res = error.response.data
        if (res && res.type === 'LoginException') res.type = 'LoginExceptionTwoFactor'
        this.$root.$refs.error.show(res)
      }.bind(this))
    }
  }
}
</script>
