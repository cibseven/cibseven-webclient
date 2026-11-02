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
  <div class="col-lg-6 col-md-8 col-sm-12">
    <ContentBlock :title="$t('password.recover.changePassword')">
      <b-form-group v-if="$root.config.userPasswordChangeEnabled" labels-cols-lg="4" label-size="lg" label-class="fw-bold pt-0 pb-4" class="m-0">
        <b-form-group :label="$t('password.recover.currentUserPassword') + '*'" label-cols-sm="4"
          label-align-sm="left" label-class="pb-4" :invalid-feedback="$t('errors.invalid')">
          <SecureInput v-model="credentials.authenticatedUserPassword"></SecureInput>
        </b-form-group>
        <b-form-group :label="$t('password.recover.newPassword') + '*'" label-cols-sm="4"
        label-align-sm="left" label-class="pb-4" :invalid-feedback="$t('errors.invalid')">
          <SecureInput v-model="credentials.password" @blur="validatePassword" @input="resetPasswordValidation"
            ref="newPasswordInput" :class="{'is-valid': passwordValid === true,'is-invalid': passwordValid === false}"></SecureInput>
          <div v-if="passwordValid === false" class="invalid-feedback d-block">
            <h6>{{ $t('password.policy.title') }}</h6>
            <div>{{ $t('password.policy.header') }}</div>
            <ul>

              <li v-for="(item, idx) in invalidPasswordRules" :key="idx">
                {{ $t('password.policy.items.' + item.placeholder, item.parameter) }}
              </li>
            </ul>
          </div>
        </b-form-group>
        <b-form-group :label="$t('password.recover.newPasswordRepeat') + '*'" label-cols-sm="4"
          label-align-sm="left" label-class="pb-4" :invalid-feedback="$t('errors.invalid')">
          <SecureInput v-model="passwordRepeat" :class="{
            'is-invalid': passwordRepeat && !same(credentials.password, passwordRepeat),
            'is-valid': passwordValid && same(credentials.password, passwordRepeat),
          }"></SecureInput>
        </b-form-group>
        <div class="float-end d-flex align-items-center">
          <b-button type="submit" variant="secondary" @click="changePassword($event)">{{$t('password.recover.changePassword')}}</b-button>
        </div>
      </b-form-group>
      <b-form-group v-else labels-cols-lg="4" label-size="lg" label-class="fw-bold pt-0 pb-4" class="m-0">
        <b-form-group :label="$t('password.recover.id') + '*'" label-for="profile-user-id" label-cols-sm="4"
          label-align-sm="left" label-class="pb-4" :invalid-feedback="$t('errors.invalid')">
          <b-form-input id="profile-user-id" :model-value="user.id" :state="notEmpty(user.id)" required readonly></b-form-input>
        </b-form-group>
        <div class="float-end d-flex align-items-center">
          <b-spinner variant="primary" class="mx-2" v-if="sendingEmail"></b-spinner>
          <b-button type="submit" variant="secondary" :disabled="sendingEmail" @click="onSendEmail()">{{ $t('password.recover.sendEmail') }}</b-button>
        </div>
      </b-form-group>
    </ContentBlock>
    <SuccessAlert ref="emailSent"> {{ $t('password.recover.emailSent') }} </SuccessAlert>
    <SuccessAlert ref="updatePassword" top="0" style="z-index: 1031">{{ $t('admin.users.updatePasswordMessage', [user.id]) }}</SuccessAlert>
  </div>
</template>

<script>
import { AdminService, SetupService } from '@/services.js'
import { notEmpty, same } from '@/components/admin/utils.js'
import { SuccessAlert, ContentBlock } from '@cib/common-frontend'
import SecureInput from '@/components/login/SecureInput.vue'

export default {
  name: 'ProfileUserPasswordTab',
  components: { SuccessAlert, ContentBlock, SecureInput },
  inject: ['AuthService'],
  props: {
    user: { type: Object, required: true },
    editMode: { type: Boolean, default: false }
  },
  data: function() {
    return {
      credentials: { authenticatedUserPassword: null, password: null },
      passwordRepeat: null,
      passwordPolicyError: false,
      sendingEmail: false,
      passwordValid: null,
      passwordRules: []
    }
  },
  computed: {
    invalidPasswordRules() {
      return (this.passwordRules ).filter(item => {
        return item && typeof item === 'object' && item.valid === false
      })
    }
  },
  methods: {
    resetPasswordValidation: function () {
      this.passwordValid = null,
      this.passwordRules = []
    },
    validatePassword: function () {
      if (notEmpty(this.credentials.password)) {
        SetupService.validatePasswordPolicy(this.credentials.password, this.user).then((response) => {
          this.passwordValid = response.data.valid
          this.passwordRules = response.data.rules
        }, error => {
          const data = error.response.data
          if (data && data.type === 'PasswordPolicyException') {
            this.passwordPolicyError = true
          }
        })
      }
    },
    notEmpty: function(value) {
      return notEmpty(value)
    },
    same: function(value, value2) {
      return same(value, value2)
    },
    changePassword: function (evt) {
      evt.preventDefault()
      if (same(this.credentials.password, this.passwordRepeat) && notEmpty(this.credentials.authenticatedUserPassword)) {
        AdminService.updateUserCredentials(this.user.id, this.credentials.password,
          this.credentials.authenticatedUserPassword).then(() => {
            this.passwordPolicyError = false
            this.$refs.updatePassword.show(2)
          }, error => {
            const data = error.response.data
            if (data && data.type === 'PasswordPolicyException') {
              this.passwordPolicyError = true
            }
          })
      }
    },
    onSendEmail: function() {
      this.sendingEmail = true
      this.AuthService.passwordRecover({ id: this.user.id }).then(() => {
        this.sendingEmail = false
        this.$refs.emailSent.show()
      }, () => {
        this.sendingEmail = false
      })
    }
  }
}
</script>
