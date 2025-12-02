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
    <div class="container pt-4">
      <div class="row align-items-center pb-2">
        <div class="col-4">
          <b-input-group size="sm">
            <template #prepend>
              <b-button :title="$t('searches.search')" aria-hidden="true" class="rounded-left" variant="secondary"><span class="mdi mdi-magnify" style="line-height: initial"></span></b-button>
            </template>
            <b-form-input :title="$t('searches.search')" :placeholder="$t('searches.search')" v-model.trim="filter"></b-form-input>
          </b-input-group>
        </div>
        <div class="col-8 text-end">
          <b-button class="me-1" size="sm" variant="secondary" v-if="$root.config.userEditable" @click="add()">
            <span class="mdi mdi-plus"> {{ $t('admin.users.add') }} </span>
          </b-button>
          <b-button v-if="$root.config.userEditable" size="sm" variant="light"
            :disabled="exporting" @click="exportCSV()">
            <span v-if="exporting"><b-spinner small></b-spinner> {{ $t('admin.exportCsv') }}</span>
            <span v-else class="mdi mdi-download"> {{ $t('admin.exportCsv') }} </span>
          </b-button>
        </div>
      </div>
    </div>
    <div class="container overflow-auto bg-white shadow-sm border rounded g-0" @scroll="showMore">
      <FlowTable striped thead-class="sticky-header" :items="users" primary-key="id"
        prefix="admin.users." :fields="tableFields"
        @contextmenu="focused = $event" @mouseenter="focused = $event" @mouseleave="focused = null">
        <template v-slot:cell(actions)="row">
          <div v-if="$root.config.userEditable">
            <b-button :disabled="focused !== row.item" style="opacity: 1" @click="openUser(row.item)" class="px-2 border-0 shadow-none" :title="$t('admin.users.editUser')" variant="link">
              <span class="mdi mdi-18px mdi-pencil-outline"></span>
            </b-button>
            <span class="border-start h-50" :class="focused === row.item ? 'border-secondary' : ''"></span>
            <b-button :disabled="focused !== row.item" style="opacity: 1" @click="prepareRemove(row.item)" class="px-2 border-0 shadow-none" :title="$t('admin.users.deleteUser')" variant="link">
              <span class="mdi mdi-18px mdi-delete-outline"></span>
            </b-button>
          </div>
          <div v-else>
            <b-button :disabled="focused !== row.item" style="opacity: 1" @click="openUser(row.item)" class="px-2 border-0 shadow-none" :title="$t('admin.users.viewUser')" variant="link">
              <span class="mdi mdi-18px mdi-eye-outline"></span>
            </b-button>
          </div>
        </template>
      </FlowTable>
      <div class="mb-3 text-center w-100" v-if="loading">
        <BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('admin.loading') }}
      </div>
      <div class="mb-3 text-center w-100" v-if="!loading && users.length === 0">
        {{ $t('admin.noResults') }}
      </div>
    </div>

    <ConfirmDialog ref="deleteModal" @ok="remove(userSelected)" :ok-title="$t('confirm.delete')">
      <span v-if="userSelected">
        <p>{{ $t('admin.users.confirmDelete') }}</p>
        <p>
          <strong>{{ $t('admin.users.id') }}:</strong> {{ userSelected.id }} <br>
          <strong>{{ $t('admin.users.firstName') }}:</strong> {{ userSelected.firstName }}<br>
          <strong>{{ $t('admin.users.lastName') }}:</strong> {{ userSelected.lastName }}<br>
          <strong>{{ $t('admin.users.email') }}:</strong> {{ userSelected.email }}
        </p>
      </span>
    </ConfirmDialog>

    <TaskPopper ref="importPopper"></TaskPopper>
  </div>
</template>

<script>
import { AdminService } from '@/services.js'
import { moment } from '@/globals.js'
import { debounce } from '@/utils/debounce.js'
import { getStringObjByKeys } from '@/components/admin/utils.js'
import { FlowTable } from '@cib/common-frontend'
import { TaskPopper, ConfirmDialog } from '@cib/common-frontend'
import { BWaitingBox } from '@cib/bootstrap-components'

export default {
  name: 'AdminUsers',
  components: { FlowTable, TaskPopper, BWaitingBox, ConfirmDialog },
  data: function () {
    return {
      selected: null,
      focused: null,
      filter: '',
      users: [],
      userSelected: null,
      firstResult: 0,
      maxResults: 40,
      loading: false,
      exporting: false
    }
  },
  computed: {
    tableFields() {
      return [
        { label: 'id', key: 'id', class: 'col-md-2 col-sm-2', tdClass: 'py-1' },
        { label: 'firstName', key: 'firstName', class: 'col-md-3 col-sm-3', tdClass: 'py-1' },
        { label: 'lastName', key: 'lastName', class: 'col-md-2 col-sm-2', tdClass: 'py-1' },
        { label: 'email', key: 'email', class: 'col-md-3 col-sm-3', tdClass: 'py-1' },
        { label: 'actions', key: 'actions', class: 'col-md-2 col-sm-2 text-center', sortable: false, thClass: 'justify-content-center', tdClass: 'justify-content-center py-0' }
      ]
    }
  },
  watch: {
    filter: function() {
      this.searchUsers()
    }
  },
  created: function() {
    this.loading = true
    this.loadUsers()
  },
  methods: {
    loadUsers: debounce(800, function (showMore = false) {
      AdminService.findUsers({ firstResult: this.firstResult, maxResults: this.maxResults }).then(response => {
        if (!showMore) this.users = response
        else this.users = this.users.concat(response)
        this.loading = false
      })
    }),
    add: function () {
      this.$router.push({ name: 'createUser' })
    },
    prepareRemove: function (user) {
      this.userSelected = user
      this.$refs.deleteModal.show()
    },
    remove: function (user) {
      AdminService.deleteUser(user.id).then(() => {
        this.userSelected = null
        this.firstResult = 0
        this.loadUsers()
      })
    },
    openUser: function (user) {
      this.$router.push({ name: 'adminUser', params: { userId: user.id }, query: { tab: 'profile' } })
    },
    showMore: function(el) {
      if (this.firstResult <= this.users.length && !this.loading) {
        if ((el.target.offsetHeight + el.target.scrollTop + 1) >= el.target.scrollHeight && this.filter==='') {
          this.firstResult += this.maxResults
          this.loading = true
          this.loadUsers(true)
        }
      }
    },
    searchUsers: function() {
      if (this.filter.length > 2) {
        this.users = []
        this.loading = true
        this.findUsers(this.filter)
      } else if (!this.filter || this.filter.length === 0) {
        this.users = []
        this.userSelected = null
        this.firstResult = 0
        this.loading = true
        this.loadUsers()
      }
    },
    findUsers: debounce(800, function(filter) {
      var firstNameLike = null
      var lastNameLike = null
      var id = null
      firstNameLike = ({ firstNameLike: '*' + filter + '*' })
      lastNameLike = ({ lastNameLike: '*' + filter + '*' })
      id = ({ id: filter })
      Promise.all([AdminService.findUsers(firstNameLike),
        AdminService.findUsers(lastNameLike), AdminService.findUsers(id)])
      .then(users => {
        users = users[0].concat(users[1]).concat(users[2])
        // Remove duplicates
        users = users.filter((value, index, self) =>
          index === self.findIndex((t) => (
          t.id === value.id
          ))
        )
        this.users = users
        this.loading = false
      })
    }),
    exportCSV: function() {
      this.exporting = true
      var keys = ['id', 'firstName', 'lastName', 'email']
      var csvContent = keys.map(k => this.$t('admin.users.' + k)).join(';') + '\n'
      AdminService.findUsers().then(users => {
        if (users.length > 0) {
          users.forEach(r => {
            csvContent += getStringObjByKeys(keys, r) + '\n'
          })
          var csvBlob = new Blob([csvContent], { type: 'text/csv' })
          var filename = 'users_' + moment().format('YYYYMMDD_HHmm') + '.csv'
          this.$refs.importPopper.triggerDownload(csvBlob, filename)
        }
        this.exporting = false
      })
    }
  }
}
</script>
