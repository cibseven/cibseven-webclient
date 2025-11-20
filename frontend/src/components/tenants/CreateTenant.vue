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
    <div class="container overflow-auto bg-white shadow-sm border rounded mt-3">
      <div class="row">
        <div class="col-12 p-0">
          <b-card class="border-0 p-5" :title="$t('admin.tenants.create')">
            <b-card-text class="border-top pt-4 mt-3">
              <form @submit.prevent="onSubmit">
                  <b-form-group :label="$t('admin.tenants.id') + '*'" label-cols-sm="2" label-align-sm="left" label-class="pb-4" :invalid-feedback="$t('errors.invalid')">
                    <b-form-input v-model="tenant.id" :state="notEmpty(tenant.id) && !tenantIdError" required></b-form-input>
                  </b-form-group>
                  <b-form-group :label="$t('admin.tenants.name') + '*'" label-cols-sm="2" label-align-sm="left" label-class="pb-4" :invalid-feedback="$t('errors.invalid')">
                    <b-form-input v-model="tenant.name" :state="notEmpty(tenant.name)" required></b-form-input>
                  </b-form-group>
                  <div class="d-flex justify-content-end gap-2 mt-4">
                    <b-button type="button" @click="cancel()" variant="light">{{ $t('admin.tenants.cancel') }}</b-button>
                    <b-button type="submit" variant="primary">{{ $t('admin.tenants.create') }}</b-button>
                  </div>
              </form>
            </b-card-text>
          </b-card>
        </div>
      </div>
    </div>
    <SuccessAlert v-if="tenant.id" top="0" style="z-index: 1031" ref="tenantCreated">{{ $t('admin.tenants.tenantCreatedMessage', [tenant.id]) }}</SuccessAlert>
  </div>
</template>

<script>
  import { notEmpty } from '@/components/admin/utils.js'
  import { SuccessAlert } from '@cib/common-frontend'
  import { mapActions } from 'vuex'

  export default {
    name: 'CreateTenant',
    components: { SuccessAlert },
    data: function () {
      return {
        tenant: { id: null, name: null },
        tenantIdError: false
      }
    },
    methods: {
      ...mapActions(['createTenant']),
      async onSubmit() {
        try {
          await this.createTenant(this.tenant)
          this.tenantIdError = false
          this.$refs.tenantCreated.show(1)
          setTimeout(() => {
            this.$router.push('/seven/auth/admin/tenants')
          }, 1000)
        } catch (error) {
          const data = error.response?.data
          if (data?.type === 'InvalidTenantIdException') {
            this.tenantIdError = true
          }
        }
      },
      cancel: function() {
        this.$router.push('/seven/auth/admin/tenants')
      },
      notEmpty: function(value) {
        return notEmpty(value)
      }
    }
  }
</script>
