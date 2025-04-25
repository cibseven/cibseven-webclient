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
          <b-card class="border-0 p-5" :title="$t('admin.groups.createLabel')">
            <b-card-text class="border-top pt-4 mt-3">
              <CIBForm @submitted="onSubmit()">
                <b-form-group labels-cols-lg="2" label-size="lg" label-class="font-weight-bold pt-0 pb-4">
                  <b-form-group :label="$t('admin.groups.id') + '*'" label-cols-sm="2"
                    label-align-sm="left" label-class="pb-4" :invalid-feedback="$t('errors.invalid')">
                    <b-form-input v-model="group.id" :state="isValidId(group.id)" required></b-form-input>
                  </b-form-group>
                  <b-form-group :label="$t('admin.groups.name') + '*'" label-cols-sm="2" label-align-sm="left" label-class="pb-4"
                    :invalid-feedback="$t('errors.invalid')">
                    <b-form-input v-model="group.name" :state="notEmpty(group.name)" required></b-form-input>
                  </b-form-group>
                  <b-form-group :label="$t('admin.groups.type')" label-cols-sm="2" label-align-sm="left" label-class="pb-4">
                    <b-form-input v-model="group.type"></b-form-input>
                  </b-form-group>
                  <div class="float-end">
                    <b-button type="reset" @click="onReset()" variant="link">{{ $t('admin.groups.cancel') }}</b-button>
                    <b-button type="submit" variant="secondary">{{ $t('admin.groups.createLabel') }}</b-button>
                  </div>
                </b-form-group>
              </CIBForm>
            </b-card-text>
          </b-card>
        </div>
      </div>
    </div>
    <SuccessAlert v-if="group.id" top="0" style="z-index: 1031" ref="groupCreated">{{ $t('admin.groups.groupCreatedMessage', [group.id]) }}</SuccessAlert>
    <ErrorDialog top="0" style="z-index: 1031" ref="systemError">{{ $t('errors.SystemException') }}</ErrorDialog>
  </div>
</template>

<script>
import { AdminService } from '@/services.js'
import { notEmpty, isValidId } from '@/components/admin/utils.js'
import ErrorDialog from '@/components/common-components/ErrorDialog.vue'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'
import CIBForm from '@/components/common-components/CIBForm.vue'

export default {
  name: 'CreateGroup',
  components: { ErrorDialog, SuccessAlert, CIBForm },
  data: function() {
    return {
      group: { id: null, name: null, type: null }
    }
  },
  methods: {
    onSubmit: function() {
      AdminService.createGroup(this.group).then(() => {
        this.$refs.groupCreated.show(1)
        setTimeout(() => {
          this.$router.push('/seven/auth/admin/groups')
        }, 1000)
      })
    },
    onReset: function() {
      this.$router.push('/seven/auth/admin/groups')
    },
    notEmpty: function(value) {
      return notEmpty(value)
    },
    isValidId: function(value) {
      return isValidId(value)
    }
  }
}
</script>
