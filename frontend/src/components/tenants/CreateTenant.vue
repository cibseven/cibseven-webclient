<template>
  <div class="d-flex flex-column bg-light" :style="{ height: 'calc(100% - 55px)' }">
    <div class="container overflow-auto bg-white shadow" style="margin-top: 24px">
      <div class="row">
        <div class="col-12 p-0">
          <b-card class="border-0 p-5" :title="$t('admin.tenants.create')">
            <b-card-text class="border-top pt-4 mt-3">
              <form @submit.prevent="onSubmit($event)">
                  <b-form-group :label="$t('admin.tenants.id') + '*'" label-cols-sm="2" label-align-sm="left" label-class="pb-4" :invalid-feedback="$t('errors.invalid')">
                    <b-form-input v-model="tenant.id" :state="notEmpty(tenant.id) && !tenantIdError" required></b-form-input>
                  </b-form-group>
                  <b-form-group :label="$t('admin.tenants.name') + '*'" label-cols-sm="2" label-align-sm="left" label-class="pb-4" :invalid-feedback="$t('errors.invalid')">
                    <b-form-input v-model="tenant.name" :state="notEmpty(tenant.name)" required></b-form-input>
                  </b-form-group>
                  <div class="float-end mt-4">
                    <b-button type="reset" @click="onReset()" variant="link">{{ $t('admin.tenants.cancel') }}</b-button>
                    <b-button type="submit" variant="secondary">{{ $t('admin.tenants.create') }}</b-button>
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
  import SuccessAlert from '@/components/common-components/SuccessAlert.vue'
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
      async onSubmit(evt) {
        evt.preventDefault()
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
      onReset: function() {
        this.$router.push('/seven/auth/admin/tenants')
      },
      notEmpty: function(value) {
        return notEmpty(value)
      }
    }
  }
</script>
