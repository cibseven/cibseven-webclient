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
  <div>
    <CIBForm @submitted="onLogin">
      <b-form-group label-cols="4" :label="$t('login.username')" :invalid-feedback="$t('errors.invalid')">
        <input ref="username" v-model="credentials.username" class="form-control" required autofocus autocomplete="username">
      </b-form-group>
      <b-form-group label-cols="4" :label="$t('login.password')">
        <SecureInput ref="password" v-model="credentials.password" class="col-8" required></SecureInput>
      </b-form-group>

      <slot></slot>

      <div class="d-flex justify-content-between">
        <div class="form-group float-start">
          <b-form-checkbox v-model="rememberMe">{{ $t('login.rememberMe') }}</b-form-checkbox>
        </div>
        <div v-if="!hideForgotten" class="form-group float-end">
          <a class="text-primary" style="cursor:pointer" @click="$refs.emailDialog.show()">{{ $t('login.forgotten') }}</a>
        </div>
      </div>

      <div class="form-group">
        <button type="submit" class="btn btn-primary btn-block w-100">{{ $t('login.login') }}</button>
      </div>
      <div v-if="onRegister" class="form-group text-center">
        {{ $t('login.register') }}
        <a @click="onRegister" class="text-primary" style="cursor:pointer">{{ $t('login.registerLink') }}</a>
      </div>
    </CIBForm>

    <b-modal ref="emailDialog" :title="$t('login.forgotten')" @shown="$refs.email.focus()">
      <CIBForm ref="form" @submitted="onForgotten">
        <b-form-group :invalid-feedback="$t('errors.invalid')">
          <input ref="email" :type="forgottenType" :placeholder="$t('login.email')" class="form-control" required autocomplete="email">
        </b-form-group>
      </CIBForm>
      <template v-slot:modal-footer>
        <b-button @click="$refs.form.onSubmit()" variant="primary">{{ $t('confirm.ok') }}</b-button>
      </template>
    </b-modal>

    <OptDialog ref="otpDialog" :credentials2="credentials2" :remember-me="rememberMe" @success="$emit('success', $event)"></OptDialog>
    <ResetDialog ref="resetDialog" :credentials2="credentials2" @success="$refs.emailSent.show(true)"></ResetDialog>
    <SuccessAlert ref="emailSent">{{ $t('login.emailSent', [email]) }}</SuccessAlert>
  </div>
</template>

<!-- eslint-disable vue/no-mutating-props -->
 <script>
import AuthService from '@/components/login/authService.js'
import OptDialog from '@/components/login/OtpDialog.vue'
import ResetDialog from '@/components/login/ResetDialog.vue'
import { SuccessAlert, CIBForm } from '@cib/common-frontend'
import SecureInput from '@/components/login/SecureInput.vue'

export default {
  name: 'LoginForm',
  components: { OptDialog, ResetDialog, SuccessAlert, SecureInput, CIBForm },
  props: {
    credentials: Object,
    credentials2: Object,
    hideForgotten: Boolean,
    onRegister: Function,
    forgottenType: { type: String, default: 'email' }
  },
  emits: ['success'],
  data: function() {
    return {
      rememberMe: true,
      show: false,
      email: null
    }
  },
  methods: {
    onLogin: function() {
      var self = this
      this.credentials.username = this.$refs.username.value // https://helpdesk.cib.de/browse/DOXISAFES-456
      this.credentials.password = this.$refs.password.$refs.input.value
      AuthService.login(this.credentials, this.rememberMe).then(function(user) { self.$emit('success', user) }, function(error) {
        var res = error.response.data
        if (res && res.type === 'LoginException' && res.params && res.params.length >= 1 && res.params[0] === 'StandardLogin') {
          self.credentials2.username = self.credentials.username
          self.credentials2.password = self.credentials.password
          self.$refs.otpDialog.show(res.params[1])
        } else if (error.response.status === 429) { // Too many requests
          res.params[1] = new Date(res.params[1]).toLocaleString('de-DE')
          self.$root.$refs.error.show(res)
        } else self.$root.$refs.error.show(res)
      })
    }, // https://vuejs.org/v2/guide/components-custom-events.html

    onForgotten: function() {
      this.email = this.$refs.email.value
      if (this.credentials2) this.credentials2.email = this.email
      AuthService.requestPasswordReset({ email: this.email }).then(function() {
        this.$refs.emailDialog.hide()
        this.$refs.emailSent.show(true)
      }.bind(this),
      function(error) {
        this.$refs.emailDialog.hide()
        var res = error.response.data
        if (res && res.type === 'LoginException' && res.params && res.params.length >= 1 && res.params[0] === 'StandardLogin')
          this.$refs.resetDialog.show(res.params[1])
        else this.$root.$refs.error.show(res)
      }.bind(this))
    }
  }
}
</script>
