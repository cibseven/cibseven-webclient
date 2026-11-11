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
    <ContentBlock :title="$t('admin.users.editMessage', [user.firstName + ' ' + user.lastName])">
      <CIBForm @submitted="update()">
        <b-form-group :label="$t('admin.users.firstName') + '*'" label-for="profile-user-first-name" label-cols-sm="6" label-cols-md="6" label-cols-lg="4" label-align-sm="left" label-class="pb-4"
          :invalid-feedback="$t('errors.invalid')">
          <b-form-input id="profile-user-first-name" v-model="form.firstName"  @update:modelValue="dirty=true"
            :state="notEmpty(form.firstName)" :readonly="!$root.config.userEditable || !editMode" required></b-form-input>
        </b-form-group>
        <b-form-group :label="$t('admin.users.lastName') + '*'" label-for="profile-user-last-name" label-cols-sm="6" label-cols-md="6" label-cols-lg="4" label-align-sm="left" label-class="pb-4"
          :invalid-feedback="$t('errors.invalid')">
          <b-form-input id="profile-user-last-name" v-model="form.lastName"  @update:modelValue="dirty=true"
            :state="notEmpty(form.lastName)" :readonly="!$root.config.userEditable || !editMode" required></b-form-input>
        </b-form-group>
        <b-form-group :label="$t('admin.users.email')" label-for="profile-user-email" label-cols-sm="6" label-cols-md="6" label-cols-lg="4" label-align-sm="left" label-class="pb-4"
          :invalid-feedback="$t('errors.invalid')">
          <b-form-input id="profile-user-email" v-model="form.email" type="email" autocomplete="email"  @update:modelValue="dirty = true" :readonly="!$root.config.userEditable || !editMode"></b-form-input>
        </b-form-group>
        <div class="float-end" v-if="$root.config.userEditable">
          <b-button type="submit" variant="secondary" :disabled="!dirty" >{{ $t('admin.users.update') }}</b-button>
        </div>
      </CIBForm>
    </ContentBlock>
    <SuccessAlert ref="updateProfile" top="0" style="z-index: 1031">{{ $t('admin.users.updateProfileMessage', [user.id]) }}</SuccessAlert>
  </div>
</template>

<script>
import { AdminService } from '@/services.js'
import { notEmpty } from '@/components/admin/utils.js'
import { SuccessAlert, CIBForm, ContentBlock } from '@cib/common-frontend'

export default {
  name: 'ProfileUserInformationTab',
  components: { SuccessAlert, CIBForm, ContentBlock },
  props: {
    user: { type: Object, required: true },
    editMode: { type: Boolean, default: false }
  },
  emits: ['update:user'],
  data: function() {
    return {
      // The form edits a copy, so unsaved changes stay in this tab and leave the page's user as stored
      form: { ...this.user },
      dirty: false
    }
  },
  watch: {
    user: function(user) {
      this.form = { ...user }
      this.dirty = false
    }
  },
  methods: {
    update: function() {
      const user = { ...this.form }
      AdminService.updateUserProfile(user.id, user).then(() => {
        this.$emit('update:user', user)
        this.$refs.updateProfile.show(2)
      })
    },
    notEmpty: function(value) {
      return notEmpty(value)
    }
  }
}
</script>
