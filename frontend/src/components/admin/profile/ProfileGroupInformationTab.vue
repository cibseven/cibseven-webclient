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
  <div class="col-sm-12 col-md-12 col-lg-8 col-xl-6 p-4">
    <b-card class="p-5 shadow-sm border rounded" :title="$t('admin.groups.editMessage', [group.name])">
      <b-card-text class="border-top pt-4 mt-3">
        <CIBForm @submitted="update()">
          <b-form-group :label="$t('admin.groups.name') + '*'" label-for="profile-group-name" label-cols-sm="6" label-cols-md="6" label-cols-lg="4" label-align-sm="left" label-class="pb-4"
            :invalid-feedback="$t('errors.invalid')">
            <b-form-input id="profile-group-name" v-model="form.name" @update:modelValue="dirty=true" :state="notEmpty(form.name)" required></b-form-input>
          </b-form-group>
          <b-form-group :label="$t('admin.groups.type')" label-for="profile-group-type" label-cols-sm="6" label-cols-md="6" label-cols-lg="4" label-align-sm="left" label-class="pb-4">
            <b-form-input id="profile-group-type" v-model="form.type" @update:modelValue="dirty = true"></b-form-input>
          </b-form-group>
          <div class="d-flex justify-content-between" v-if="$root.config.userProvider === 'org.cibseven.webapp.auth.SevenUserProvider'">
            <b-button variant="light" @click="$refs.deleteModal.show()">{{ $t('admin.groups.deleteGroup') }}</b-button>
            <b-button type="submit" variant="secondary" :disabled="!dirty" >{{ $t('admin.groups.update') }}</b-button>
          </div>
        </CIBForm>
      </b-card-text>
    </b-card>

    <ConfirmDialog ref="deleteModal" @ok="deleteGroup()" :ok-title="$t('confirm.delete')">
      <div>
        <p>{{ $t('admin.groups.confirmDelete') }}</p>
        <p>
          <strong>{{ $t('admin.groups.id') }}:</strong> {{ group.id }} <br>
          <strong>{{ $t('admin.groups.name') }}:</strong> {{ group.name }}<br>
          <strong>{{ $t('admin.groups.type') }}:</strong> {{ group.type }}
        </p>
      </div>
    </ConfirmDialog>

    <SuccessAlert ref="updateGroup" top="0" style="z-index: 1031">{{ $t('admin.groups.updateGroupMessage', [group.id]) }}</SuccessAlert>
    <SuccessAlert ref="deleteGroup" top="0" style="z-index: 1031">{{ $t('admin.groups.deleteGroupMessage', [group.id]) }}</SuccessAlert>
  </div>
</template>

<script>
import { AdminService } from '@/services.js'
import { notEmpty } from '@/components/admin/utils.js'
import { SuccessAlert, CIBForm, ConfirmDialog } from '@cib/common-frontend'

export default {
  name: 'ProfileGroupInformationTab',
  components: { SuccessAlert, CIBForm, ConfirmDialog },
  props: {
    group: { type: Object, required: true }
  },
  emits: ['update:group'],
  data: function() {
    return {
      // The form edits a copy, so unsaved changes stay in this tab and leave the page's group as stored
      form: { ...this.group },
      dirty: false
    }
  },
  watch: {
    group: function(group) {
      this.form = { ...group }
      this.dirty = false
    }
  },
  methods: {
    update: function() {
      const group = { ...this.form }
      AdminService.updateGroup(group.id, group).then(() => {
        this.$emit('update:group', group)
        this.$refs.updateGroup.show(2)
      })
    },
    notEmpty: function(value) {
      return notEmpty(value)
    },
    deleteGroup: function() {
      AdminService.deleteGroup(this.group.id).then(() => {
        this.$refs.deleteGroup.show(2)
        this.$router.push({ name: 'adminGroups' })
      })
    }
  }
}
</script>
