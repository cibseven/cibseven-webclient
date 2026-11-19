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
  <div class="col-12">
    <div class="p-3">
      <b-form-group labels-cols-lg="2" label-size="lg" label-class="fw-bold pt-0"
        :label="$t('admin.groups.editMessage', [group.name])">
      </b-form-group>
      <div class="row py-3">
        <div class="col-9">
          <h5>{{ $t('admin.groups.user.title') }}</h5>
        </div>
      </div>
      <div v-if="users" class="container-fluid overflow-auto bg-white shadow-sm border rounded g-0">
        <FlowTable :items="users" primary-key="id" striped
          :fields="[
            { label: 'admin.users.id', key: 'id', class: 'col-md-3 col-sm-3', tdClass: 'py-2' },
            { label: 'admin.users.firstName', key: 'firstName', class: 'col-md-3 col-sm-3', tdClass: 'py-2' },
            { label: 'admin.users.lastName', key: 'lastName', class: 'col-md-3 col-sm-3', tdClass: 'py-2' },
            { label: 'admin.users.email', key: 'email', class: 'col-md-3 col-sm-3', tdClass: 'py-2' },
          ]">
        </FlowTable>
      </div>
    </div>
  </div>
</template>

<script>
import { AdminService } from '@/services.js'
import { FlowTable } from '@cib/common-frontend'

export default {
  name: 'ProfileGroupUsersTab',
  components: { FlowTable },
  props: {
    group: { type: Object, required: true }
  },
  data: function() {
    return {
      users: null
    }
  },
  created: function() {
    this.loadUsers(this.group.id)
  },
  methods: {
    loadUsers: function(groupId) {
      AdminService.findUsers({ memberOfGroup: groupId }).then(response => {
        this.users = response
      })
    }
  }
}
</script>
