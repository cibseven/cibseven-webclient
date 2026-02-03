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
  <b-modal ref="otpDialog" v-if="credentials2" :title="$t('login.2fa')" @shown="$refs.otp.focus()">
    <CIBForm ref="form2" @submitted="onLogin2">
      <div class="mb-3">{{ $t('login.needOtp') }}</div>
<<<<<<< HEAD
      <b-form-group :invalid-feedback="$t('errors.invalid')">
=======
      <b-form-group label-cols="4" content-cols="8" :label="$t('login.otp')" :invalid-feedback="$t('errors.invalid')">
>>>>>>> ec0703e77084526dac50f064fdaebdc990501478
        <input ref="otp" type="number" max="999999" v-model="credentials2.otp" :placeholder="$t('login.otp')" :aria-label="$t('login.otp')" class="form-control" required>
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
import { CIBForm } from '@cib/common-frontend'

export default {
  name: 'OtpDialog',
  components: { CIBForm },
  props: { credentials2: Object, rememberMe: Boolean },
  emits: ['success'],
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
        const res = error.response.data
        if (res && res.type === 'LoginException') res.type = 'LoginExceptionTwoFactor'
        this.$root.$refs.error.show(res)
      }.bind(this))
    }
  }
}
</script>
