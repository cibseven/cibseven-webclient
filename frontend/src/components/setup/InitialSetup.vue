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
    <div class="bg-light d-flex flex-column h-100 overflow-auto w-100 py-3">
      <div class="text-center mb-4">
        <h2 class="h4">{{ $t('setup.title') }}</h2>
        <p>{{ $t('setup.description') }}</p>
      </div>

      <div class="container-fluid">
        <div class="row justify-content-center">
          <!-- Help Text - shows first on mobile -->
          <div class="col-lg-4 col-md-5 mb-4 order-1 order-md-2">
            <div class="card bg-white shadow-sm">
              <div class="card-body p-4">
                <h5 class="card-title">
                  <span class="mdi mdi-help-circle-outline me-2"></span>
                  {{ $t('setup.help.title') }}
                </h5>
                <p class="mb-0">{{ $t('setup.help.description', [engineName]) }}</p>
              </div>
            </div>
          </div>

          <!-- Form -->
          <div class="col-lg-6 col-md-7 order-2 order-md-1">
            <div class="card shadow-sm">
              <div class="card-body p-4">
                <h5 class="card-title mb-4">{{ $t('setup.createAdminUser') }}</h5>

                <form @submit.prevent="onSubmit">
                  <!-- Account Section -->
                  <div class="mb-4">
                    <h6 class="text-muted mb-3">{{ $t('admin.users.account') }}</h6>

                    <div class="mb-3">
                      <label class="form-label" for="profileId">{{ $t('admin.users.id') }} *</label>
                      <input
                        id="profileId"
                        type="text"
                        class="form-control"
                        v-model="profile.id"
                        :class="{
                          'is-invalid': submitted && !notEmpty(profile.id),
                          'is-valid': notEmpty(profile.id) && !userIdError,
                        }"
                        required
                      />
                      <div v-if="userIdError" class="invalid-feedback d-block">
                        {{ $t('errors.InvalidUserIdException') }}
                      </div>
                    </div>

                    <div class="mb-3">
                      <label class="form-label" for="password">
                        {{ $t('admin.users.password') }} *
                        <span
                          v-if="passwordPolicyEnabled"
                          ref="passwordHelper"
                          class="mdi mdi-help-circle"
                          :class="passwordPolicyError ? 'text-danger' : 'text-secondary'"
                          style="cursor: pointer"
                        ></span>
                        <b-popover v-if="passwordPolicyEnabled" :target="() => $refs.passwordHelper" triggers="hover" placement="right">
                          <h6>{{ $t('password.policy.title') }}</h6>
                          <div>{{ $t('password.policy.header') }}</div>
                          <ul class="mb-0 ps-3">
                            <li v-for="(item, index) in $tm('password.policy.items')" :key="index">
                              {{ item }}
                            </li>
                          </ul>
                        </b-popover>
                      </label>
                      <div class="input-group">
                        <input
                          id="password"
                          :type="showPassword ? 'text' : 'password'"
                          class="form-control"
                          v-model="credentials.password"
                          :class="{
                            'is-invalid': submitted && !notEmpty(credentials.password),
                            'is-valid': notEmpty(credentials.password) && !passwordPolicyError,
                          }"
                          required
                        />
                        <button
                          tabindex="-1"
                          class="btn btn-outline-secondary"
                          type="button"
                          @click="showPassword = !showPassword"
                        >
                          <span :class="showPassword ? 'mdi mdi-eye-off' : 'mdi mdi-eye'"></span>
                        </button>
                      </div>
                      <div v-if="passwordPolicyError" class="text-danger small mt-1">
                        {{ $t('errors.PasswordPolicyException') }}
                      </div>
                    </div>

                    <div class="mb-3">
                      <label class="form-label" for="passwordRepeat">{{ $t('admin.users.passwordRepeat') }} *</label>
                      <div class="input-group">
                        <input
                          id="passwordRepeat"
                          :type="showPassRepeat ? 'text' : 'password'"
                          class="form-control"
                          v-model="passwordRepeat"
                          :class="{
                            'is-invalid': submitted && !same(credentials.password, passwordRepeat),
                            'is-valid': same(credentials.password, passwordRepeat),
                          }"
                          required
                        />
                        <button
                          tabindex="-1"
                          class="btn btn-outline-secondary"
                          type="button"
                          @click="showPassRepeat = !showPassRepeat"
                        >
                          <span :class="showPassRepeat ? 'mdi mdi-eye-off' : 'mdi mdi-eye'"></span>
                        </button>
                      </div>
                      <div
                        v-if="credentials.password && !same(credentials.password, passwordRepeat)"
                        class="text-danger small mt-1"
                      >
                        {{ $t('errors.passwordDoNotMatch') }}
                      </div>
                    </div>
                  </div>

                  <!-- Profile Section -->
                  <div class="mb-4">
                    <h6 class="text-muted mb-3">{{ $t('admin.users.profile') }}</h6>

                    <div class="mb-3">
                      <label class="form-label" for="firstName">{{ $t('admin.users.firstName') }} *</label>
                      <input
                        id="firstName"
                        type="text"
                        class="form-control"
                        v-model="profile.firstName"
                        :class="{
                          'is-invalid': submitted && !notEmpty(profile.firstName),
                          'is-valid': notEmpty(profile.firstName),
                        }"
                        required
                      />
                    </div>

                    <div class="mb-3">
                      <label class="form-label" for="lastName">{{ $t('admin.users.lastName') }} *</label>
                      <input
                        id="lastName"
                        type="text"
                        class="form-control"
                        v-model="profile.lastName"
                        :class="{
                          'is-invalid': submitted && !notEmpty(profile.lastName),
                          'is-valid': notEmpty(profile.lastName),
                        }"
                        required
                      />
                    </div>

                    <div class="mb-3">
                      <label class="form-label" for="email">{{ $t('admin.users.email') }}</label>
                      <input
                        id="email"
                        type="email"
                        class="form-control"
                        v-model="profile.email"
                        :class="{ 'is-valid': isValidEmail(profile.email) }"
                      />
                    </div>
                  </div>

                  <div class="d-grid gap-2">
                    <button type="submit" class="btn btn-primary" :disabled="submitting">
                      <span v-if="submitting" class="spinner-border spinner-border-sm me-2"></span>
                      {{ $t('setup.createUser') }}
                    </button>
                  </div>
                </form>

              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Success Alert -->
    <SuccessAlert ref="successAlert" top="0" style="z-index: 1031">
      {{ $t('setup.success') }}
    </SuccessAlert>
  </div>
</template>

<script>
import { SetupService } from '@/services.js'
import { notEmpty, same, isValidEmail } from '@/components/admin/utils.js'
import { SuccessAlert } from '@cib/common-frontend'
import { ENGINE_STORAGE_KEY } from '@/constants.js'

export default {
  name: 'InitialSetup',
  components: { SuccessAlert },
  data() {
    return {
      profile: { id: null, email: null, firstName: null, lastName: null },
      credentials: { password: null },
      passwordRepeat: null,
      showPassword: false,
      showPassRepeat: false,
      passwordPolicyError: false,
      userIdError: false,
      submitted: false,
      submitting: false,
    }
  },
  computed: {
    passwordPolicyEnabled() {
      return this.$root?.config?.admin?.passwordPolicyEnabled || false
    },
    engineName() {
      return localStorage.getItem(ENGINE_STORAGE_KEY) || 'default'
    },
  },
  methods: {
    notEmpty(value) {
      return notEmpty(value)
    },
    same(value, value2) {
      return same(value, value2)
    },
    isValidEmail(value) {
      return isValidEmail(value)
    },
    async onSubmit() {
      this.submitted = true
      this.passwordPolicyError = false
      this.userIdError = false

      // Validate required fields
      if (
        !this.notEmpty(this.profile.id) ||
        !this.notEmpty(this.credentials.password) ||
        !this.notEmpty(this.profile.firstName) ||
        !this.notEmpty(this.profile.lastName)
      ) {
        return
      }

      // Validate passwords match
      if (!this.same(this.credentials.password, this.passwordRepeat)) {
        return
      }

      this.submitting = true

      try {
        await SetupService.createInitialUser({
          profile: this.profile,
          credentials: this.credentials,
        })

        // Show success alert
        this.$refs.successAlert.show()

        // Redirect to login after a short delay
        setTimeout(() => {
          this.$router.push({ name: 'login' })
        }, 1500)
      } catch (error) {
        this.submitting = false
        const data = error.response?.data

        if (data) {
          if (data.type === 'PasswordPolicyException') {
            this.passwordPolicyError = true
          } else if (data.type === 'InvalidUserIdException') {
            this.userIdError = true
          } else {
            this.$root.$refs.error.show(data)
          }
        } else {
          this.$root.$refs.error.show(data)
        }
      }
    },
  },
}
</script>