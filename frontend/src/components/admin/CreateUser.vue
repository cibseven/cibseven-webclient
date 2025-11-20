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
  <div class="d-flex flex-column bg-light" :style="{ height: 'calc(100% - 55px)' }">
    <div class="container overflow-auto bg-white shadow-sm border rounded" style="margin-top: 24px">
      <div class="row">
        <div class="col-12 p-0">
          <b-card class="border-0 p-5" :title="$t('admin.users.create')">
            <b-card-text class="border-top pt-4 mt-3">
              <form @submit.prevent="onSubmit">
                <b-form-group labels-cols-lg="2" :label="$t('admin.users.account')" label-size="lg" label-class="h6 pt-0 mb-4" class="m-0">
                  <b-form-group :label="$t('admin.users.id') + '*'" label-cols-sm="2"
                    label-align-sm="left" label-class="pb-4" :invalid-feedback="$t('errors.invalid')">
                    <b-form-input v-model="profile.id" :state="notEmpty(profile.id) && !userIdError" required></b-form-input>
                  </b-form-group>
                  <b-form-group label-cols-sm="2" label-align-sm="left">
                    <template v-slot:label>
                      {{ $t('admin.users.password') + '*' }}
                      <span v-if="$root.config.admin.passwordPolicyEnabled" ref="passwordHelper" style="cursor: pointer" class="mdi mdi-help-circle" :class="passwordPolicyError ? 'text-danger' : 'text-secondary'"></span>
                    </template>
                    <b-form-input :type="fieldType(showPassword)" ref="pass" v-model="credentials.password" :state="notEmpty(credentials.password) && !passwordPolicyError" required>
                      <template v-slot:append>
                        <button class="btn btn-outline-secondary rounded-start-0" type="button" @click="showPassword = !showPassword">
                          <span :class="showPassword ? 'mdi mdi-eye-off' : 'mdi mdi-eye'"></span>
                        </button>
                      </template>
                    </b-form-input>
                    <div v-if="passwordPolicyError" class="text-danger">{{ $t('errors.PasswordPolicyException') }}</div>
                  </b-form-group>
                  <b-form-group :label="$t('admin.users.passwordRepeat') + '*'" label-cols-sm="2" label-align-sm="left">
                    <b-form-input :type="fieldType(showPassRepeat)" ref="passRepeat" v-model="passwordRepeat" :state="same(credentials.password, passwordRepeat)" required>
                      <template v-slot:append>
                        <button class="btn btn-outline-secondary rounded-start-0" type="button" @click="showPassRepeat = !showPassRepeat">
                          <span :class="showPassRepeat ? 'mdi mdi-eye-off' : 'mdi mdi-eye'"></span>
                        </button>
                      </template>
                    </b-form-input>
                    <div v-if="credentials.password && !same(credentials.password, passwordRepeat)" class="text-danger">
                      {{ $t('errors.passwordDoNotMatch') }}
                    </div>
                  </b-form-group>

                  <b-form-group :label="$t('admin.users.profile')" label-size="lg" label-class="h6 mt-4"></b-form-group>

                  <b-form-group :label="$t('admin.users.firstName') + '*'" label-cols-sm="2" label-align-sm="left" label-class="pb-4" :invalid-feedback="$t('errors.invalid')">
                    <b-form-input v-model="profile.firstName" :state="notEmpty(profile.firstName)" required></b-form-input>
                  </b-form-group>
                  <b-form-group :label="$t('admin.users.lastName') + '*'" label-cols-sm="2" label-align-sm="left" label-class="pb-4" :invalid-feedback="$t('errors.invalid')">
                    <b-form-input v-model="profile.lastName" :state="notEmpty(profile.lastName)" required></b-form-input>
                  </b-form-group>
                  <b-form-group :label="$t('admin.users.email')" label-cols-sm="2" label-align-sm="left" :invalid-feedback="$t('errors.invalid')">
                    <b-form-input v-model="profile.email" type="email" autocomplete="email" :state="isValidEmail(profile.email)"></b-form-input>
                  </b-form-group>
                  <div class="d-flex justify-content-end gap-2 mt-4">
                    <b-button type="button" @click="onReset()" variant="light">{{ $t('admin.users.cancel') }}</b-button>
                    <b-button type="submit" variant="primary">{{ $t('admin.users.create') }}</b-button>
                  </div>
                </b-form-group>
              </form>
            </b-card-text>
          </b-card>
        </div>
      </div>
    </div>
    <b-popover :target="() => $refs.passwordHelper" triggers="hover">
      <h6>{{ $t('password.policy.title') }}</h6>
      <div>{{ $t('password.policy.header') }}</div>
      <ul>
        <li v-for="(item, index) in $tm('password.policy.items')" :key="index">{{ item }}</li>
      </ul>
    </b-popover>
    <SuccessAlert v-if="profile.id" top="0" style="z-index: 1031" ref="userCreated">{{ $t('admin.users.userCreatedMessage', [profile.id]) }}</SuccessAlert>
  </div>
</template>

<script>
import { AdminService } from '@/services.js'
import { notEmpty, same, isValidEmail } from '@/components/admin/utils.js'
import { SuccessAlert } from '@cib/common-frontend'

export default {
  name: 'CreateUser',
  components: { SuccessAlert },
  data: function () {
    return {
      profile: { id: null, email: null, firstName: null, lastName: null },
      credentials: { password: null },
      passwordRepeat: null,
      showPassword: false,
      showPassRepeat: false,
      passwordPolicyError: false,
      userIdError: false
    }
  },
  methods: {
    fieldType: function(showPass) {
      if (showPass)
        return 'text'
      return 'password'
    },
    onSubmit: function() {
      if (!same(this.credentials.password, this.passwordRepeat)) return
      AdminService.createUser({ 'profile': this.profile, 'credentials': this.credentials }).then(() => {
        this.passwordPolicyError = false
        this.userIdError = false
        this.$refs.userCreated.show(1)
        setTimeout(() => {
          this.$router.push('/seven/auth/admin/users')
        }, 1000)
      }, error => {
        var data = error.response.data
        if (data) {
          if (data.type === 'PasswordPolicyException') this.passwordPolicyError = true
          else if (data.type === 'InvalidUserIdException') this.userIdError = true
        }
      })
    },
    onReset: function() {
      this.$router.push('/seven/auth/admin/users')
    },
    notEmpty: function(value) {
      return notEmpty(value)
    },
    same: function(value, value2) {
      return same(value, value2)
    },
    isValidEmail: function(value) {
      return isValidEmail(value)
    }
  }
}
</script>
