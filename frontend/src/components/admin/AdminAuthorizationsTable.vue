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
    <div class="container-fluid pb-2 pt-4">
      <div class="row align-items-center px-4">
        <div class="col-4">
          <b-input-group size="sm">
            <template #prepend>
              <b-button class="rounded-left" variant="secondary"><span class="mdi mdi-magnify" style="line-height: initial"></span></b-button>
            </template>
            <b-form-input :placeholder="$t('searches.search')" v-model.trim="filter"></b-form-input>
          </b-input-group>
        </div>
        <div class="col-8 text-end">
          <b-button class="me-1" size="sm" variant="secondary" @click="add()">
            <span class="mdi mdi-plus"> {{ $t('admin.authorizations.add') }} </span>
          </b-button>
          <b-button v-if="$root.config.userProvider === 'org.cibseven.webapp.auth.SevenUserProvider' && exportableAuth" size="sm" variant="light"
            :disabled="exporting" @click="exportCSV()">
            <span v-if="exporting"><b-spinner small></b-spinner> {{ $t('admin.exportCsv') }}</span>
            <span v-else class="mdi mdi-download"> {{ $t('admin.exportCsv') }} </span>
          </b-button>
        </div>
      </div>
    </div>
    <div class="container-fluid overflow-auto h-100 g-0" @scroll="showMore">
      <div class="px-4 mb-5">
        <FlowTable striped thead-class="sticky-header light" :items="authorizations" primary-key="id"
          prefix="admin.authorizations." :fields="authorizationFields"
          class="shadow-sm border rounded"
          @contextmenu="focused = $event" @mouseenter="focused = $event" @mouseleave="focused = null">
          <template v-slot:cell(type)="row">
            <div class="w-100 d-flex align-items-center" v-if="edit === row.item.id && edit === '0'">
              <span> {{ $t('admin.authorizations.types.' + types[row.item.type].key) }} </span>
              <b-dropdown class="float-right" size="lg" variant="link" toggle-class="text-decoration-none" no-caret>
                <template #button-content>
                  <b-button class="border-0" variant="outline-secondary">
                    <span class="mdi mdi-18px mdi-pencil"></span>
                  </b-button>
                </template>
                <b-dropdown-item class="ms-2" v-for="type in types" :key="type.key"
                  @click="row.item.type = type.id" :active="row.item.type === type.id">
                  {{ $t('admin.authorizations.types.' + type.key) }}
                </b-dropdown-item>
              </b-dropdown>
            </div>
            <div v-else>
              <span> {{ $t('admin.authorizations.types.' + types[row.item.type].key) }} </span>
            </div>
          </template>
          <template v-slot:cell(userIdGroupId)="row">
            <div v-if="edit === row.item.id">
              <b-input-group>
                <b-input-group-prepend>
                  <b-button variant="outline-secondary" @click="isUserToEdit = !isUserToEdit">
                    <span class="mdi" :class="isUserToEdit ? 'mdi-account' : 'mdi-account-group'"></span>
                  </b-button>
                </b-input-group-prepend>
                <b-form-input v-if="row.item.userId" v-model="row.item.userId" autofocus></b-form-input>
                <b-form-input v-else v-model="row.item.groupId" autofocus></b-form-input>
              </b-input-group>
            </div>
            <div v-else>
              <span class="mdi" :class="row.item.userId ? 'mdi-account' : 'mdi-account-group'"></span>
              {{ row.item.userId != null ? row.item.userId : row.item.groupId }}
            </div>
          </template>
          <template v-slot:cell(permissions)="row">
            <div class="w-100 d-flex align-items-center" v-if="edit === row.item.id">
              <span v-if="selected.length === resourcesTypes[$route.params.resourceTypeId].permissions.length">
                ALL
              </span>
              <span v-else-if="selected.length === 0">
                NONE
              </span>
              <span v-else>{{ selected.join(", ") }}</span>
              <b-dropdown class="float-right" size="lg" variant="link" toggle-class="text-decoration-none" no-caret>
                <template #button-content>
                  <b-button class="border-0" variant="outline-secondary"><span class="mdi mdi-18px mdi-pencil"></span></b-button>
                </template>
                <b-form-checkbox class="ms-2" @input="selectAll(row.item)">
                  <span v-if="selected.length === resourcesTypes[$route.params.resourceTypeId].permissions.length">
                    {{ $t('admin.authorizations.unselectAll') }}
                  </span>
                  <span v-else>
                    {{ $t('admin.authorizations.selectAll') }}
                  </span>
                </b-form-checkbox>
                <b-dropdown-divider></b-dropdown-divider>
                <b-form-checkbox class="ms-2" v-for="permission in resourcesTypes[$route.params.resourceTypeId].permissions"
                  :value="permission" :key="permission" v-model="selected">
                  {{ permission }}
                </b-form-checkbox>
              </b-dropdown>
            </div>
            <div v-else>
              <span v-if="row.item.permissions.length > 0">{{ row.item.permissions.join(", ") }}</span>
              <span v-if="row.item.permissions.length === 0">NONE</span>
            </div>
          </template>
          <template v-slot:cell(name)="row">
            <div v-if="edit === row.item.id">
              <b-form-select v-model="row.item.name" :options="filterNameOptions" @change="onFilterNameChange(row.item)">
                <template v-slot:first>
                  <b-form-select-option :value="null"></b-form-select-option>
                </template>
              </b-form-select>
            </div>
            <div v-else> {{ row.item.name }} </div>
          </template>
          <template v-slot:cell(resourceId)="row">
            <div v-if="edit === row.item.id">
              <b-form-input v-model="row.item.resourceId" :readonly="!!row.item.name"></b-form-input>
            </div>
            <div v-else> {{ row.item.resourceId }} </div>
          </template>
          <template v-slot:cell(actions)="row">
            <div class="d-flex">
              <div v-if="edit === row.item.id">
                <b-button style="opacity: 1" class="px-2 border-0 shadow-none" variant="link" @click="save(row.item)"
                  :disabled="focused !== row.item || (row.item.userId == null && row.item.groupId == null)"><span class="mdi mdi-18px mdi-content-save-outline"></span></b-button>
                <span class="border-start h-50" :class="focused === row.item ? 'border-secondary' : ''"></span>
                <b-button :disabled="focused !== row.item" style="opacity: 1" class="px-2 border-0 shadow-none" variant="link" @click="cancelEdit(row.item)"><span class="mdi mdi-18px mdi-block-helper"></span></b-button>
              </div>
              <div v-else>
                <b-button :disabled="focused !== row.item" style="opacity: 1" variant="link" class="px-2 border-0 shadow-none" @click="prepareEdit(row.item)"><span class="mdi mdi-18px mdi-pencil-outline"></span></b-button>
                <span class="border-start h-50" :class="focused === row.item ? 'border-secondary' : ''"></span>
                <b-button :disabled="focused !== row.item" style="opacity: 1" variant="link" class="px-2 border-0 shadow-none" @click="prepareRemove(row.item)"><span class="mdi mdi-18px mdi-delete-outline"></span></b-button>
              </div>
            </div>
          </template>
        </FlowTable>
        <div class="text-center w-100" v-if="loading">
          <BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('admin.loading') }}
        </div>
        <div class="text-center w-100" v-if="!loading && authorizations.length === 0">
          {{ $t('admin.noResults') }}
        </div>
      </div>
    </div>

    <ConfirmDialog ref="deleteModal" @ok="remove(authorizationSelected)" :ok-title="$t('confirm.delete')">
      <span v-if="authorizationSelected">
        <i18n-t keypath="admin.authorizations.confirmDelete" tag="span" scope="global"/>
        <br><br>
        <template v-for="(value, key) in authTranslationMap" :key="key">
          <strong>
            <i18n-t :keypath="`admin.authorizations.confirmParams.${key}`" tag="span" scope="global"></i18n-t>
          </strong>
          <span>{{ value }}</span>
          <br>
        </template>
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
import FlowTable from '@/components/common-components/FlowTable.vue'
import TaskPopper from '@/components/common-components/TaskPopper.vue'
import ConfirmDialog from '@/components/common-components/ConfirmDialog.vue'
import { BWaitingBox } from 'cib-common-components'

export default {
  name: 'AdminAuthorizationsTable',
  components: { FlowTable, TaskPopper, BWaitingBox, ConfirmDialog },
  data: function () {
    return {
      selected: [],
      focused: null,
      filter: '',
      authorizations: [],
      resourcesTypes: this.$root.config.admin.resourcesTypes,
      types: this.$root.config.admin.types,
      edit: null,
      isUserToEdit: true,
      authorizationSelected: null,
      firstResult: 0,
      maxResults: 40,
      loading: false,
      exporting: false
    }
  },
  watch: {
    '$route.params.resourceTypeId': function(resourceTypeId) {
      this.authorizations = []
      this.firstResult = 0
      this.loading = true
      this.loadAuthorizations(resourceTypeId)
    },
    filter: function() {
      this.searchAuthorizations()
    }
  },
  mounted: function () {
    if (this.$route.params.resourceTypeId) {
      this.loading = true
      this.loadAuthorizations(this.$route.params.resourceTypeId)
    }
  },
  computed: {
    authorizationFields: function() {
      var baseFields = [
        { label: 'type', key: 'type', class: 'col' },
        { label: 'userIdGroupId', key: 'userIdGroupId', class: 'col' },
        { label: 'permissions', key: 'permissions', class: 'col' },
        { label: 'resourceId', key: 'resourceId', class: 'col' },
        { label: 'actions', key: 'actions', class: 'col text-center', sortable: false,
          thClass: 'justify-content-center', tdClass: 'justify-content-center py-0' }
      ]
      if (this.$route.params.resourceTypeId === '5')
        baseFields.splice(3, 0, { label: 'name', key: 'name', class: 'col' })

      return baseFields
    },
    exportableAuth: function() {
      return ['0', '4', '5', '6', '9'].includes(this.$route.params.resourceTypeId)
    },
    filterNameOptions: function() {
      return this.$store.state.filter.list.map(item => item.name)
    },
    authTranslationMap() {
      return {
        type: this.$t('admin.authorizations.types.' + this.types[this.authorizationSelected.type].key),
        user: this.authorizationSelected.userId,
        group: this.authorizationSelected.groupId,
        permissions: this.authorizationSelected.permissions.join(', '),
        resource: this.$t('admin.authorizations.resourcesTypes.' + this.resourcesTypes[this.$route.params.resourceTypeId].key),
        resourceId: this.authorizationSelected.resourceId
      }
    }
  },
  methods: {
    loadAuthorizations: debounce(800, function (resourceTypeId, showMore = false) {
      AdminService.findAuthorizations({
          resourceType: resourceTypeId,
          firstResult: this.firstResult,
          maxResults: this.maxResults
        }).then(response => {
          if (!showMore) this.authorizations = response
          else this.authorizations = this.authorizations.concat(response)

          this.authorizations.forEach(authorization => {
            authorization.userIdGroupId = authorization.userId != null ? authorization.userId : authorization.groupId
          })

          if (resourceTypeId === '5') {
            this.authorizations.forEach(authorization => {
              var filter = this.$store.state.filter.list.find(obj => obj.id === authorization.resourceId)
              if (filter) authorization.name = filter.name
            })
          }
          this.loading = false
        })
    }),
    getClasses: function(authorization) {
      var classes = []
      if (authorization !== this.focused) classes.push('invisible')
      return classes
    },
    prepareEdit: function (authorization) {
      // In case we press edit and we are in the middle to add a new authorization, that extra "unfinished"
      // auth needs to be removed from list.
      if (this.authorizations[0].id === "0") this.authorizations.shift()
      this.edit = authorization.id
      this.isUserToEdit = (authorization.userId != null) ? true : false
      if (authorization.permissions.length === 0) {
        this.selected = []
      }
      else if (authorization.permissions.length === 1 && authorization.permissions[0] === 'ALL') {
        this.selected = [...this.resourcesTypes[this.$route.params.resourceTypeId].permissions]
      }
      else if (authorization.permissions.length === 1 && authorization.permissions[0] === 'NONE') {
        this.selected = []
      }
      else {
        this.selected = [...authorization.permissions]
      }
      this.authorizationSelected = authorization
    },
    prepareRemove: function (authorization) {
      this.authorizationSelected = authorization
      this.$refs.deleteModal.show()
    },
    cancelEdit: function(authorization) {
      this.selected = []
      this.edit = null
      authorization = this.authorizationSelected
      this.authorizationSelected = null
      // If id == 0 then means that we are creating a new authorization.
      if (authorization.id === "0") {
        this.authorizations.shift()
      }
    },
    remove: function(authorization) {
      AdminService.deleteAuthorization(authorization.id).then(() => {
        this.authorizationSelected = null
        this.firstResult = 0
        this.loadAuthorizations(this.$route.params.resourceTypeId)
      })
    },
    selectAll: function() {
      if (this.selected.length === this.resourcesTypes[this.$route.params.resourceTypeId].permissions.length) {
        this.selected = []
      } else {
        this.selected = this.resourcesTypes[this.$route.params.resourceTypeId].permissions
      }
    },
    save: function (authorization) {
      if ((this.isUserToEdit) && (authorization.userId == null)) {
        authorization.userId = authorization.groupId
        authorization.groupId = null
      } else if ((!this.isUserToEdit) && (authorization.groupId == null)) {
        authorization.groupId = authorization.userId
        authorization.userId = null
      }
      authorization.userIdGroupId = authorization.userId != null ? authorization.userId : authorization.groupId
      
      if (this.selected.length === this.resourcesTypes[this.$route.params.resourceTypeId].permissions.length) {
        authorization.permissions = ['ALL']
      } else if (this.selected.length === 0) {
        authorization.permissions = ['NONE']
      } else {
        authorization.permissions = this.selected
      }
      // If id == 0 then means that we are creating a new authorization. and new auth is going to be always in first place.
      if (authorization.id === "0") {
        authorization.id = null
        AdminService.createAuthorization(authorization).then((res) => {
          authorization.id = res.id
        })
      } else {
        AdminService.updateAuthorization(authorization.id, authorization).then(() => {
          this.cancelEdit(authorization)
        })
      }
    },
    add: function () {
      // If we are already adding a new element, no more should be allowed.
      if (this.authorizations.length === 0 || this.authorizations[0].id !== "0") {
        this.authorizations.unshift({
          id: "0",
          type: "1",
          permissions: ["ALL"],
          userId: null,
          groupId: null,
          resourceType: this.$route.params.resourceTypeId,
          resourceId: null
        })
        this.selected = this.authorizations[0].permissions
        this.isUserToEdit = true
        this.authorizationSelected = this.authorizations[0]
        this.edit = this.authorizationSelected.id
      }
    },
    showMore: function(el) {
      if (this.firstResult <= this.authorizations.length && !this.loading) {
        if ((el.target.offsetHeight + el.target.scrollTop + 1) >= el.target.scrollHeight && this.filter==='') {
          this.firstResult += this.maxResults
          this.loading = true
          this.loadAuthorizations(this.$route.params.resourceTypeId, true)
        }
      }
    },
    searchAuthorizations: function() {
      if (this.filter.length > 2) {
        this.authorizations = []
        this.loading = true
        this.findAuthorizations(this.filter)
      } else if (!this.filter || this.filter.length === 0) {
        this.authorizations = []
        this.authorizationSelected = null
        this.firstResult = 0
        this.loading = true
        this.loadAuthorizations(this.$route.params.resourceTypeId)
      }
    },
    findAuthorizations: debounce(800, function(filter) {
      Promise.all([
        AdminService.findAuthorizations({ resourceType: this.$route.params.resourceTypeId, resourceId: filter }),
        AdminService.findAuthorizations({ resourceType: this.$route.params.resourceTypeId, userIdIn: filter }),
        AdminService.findAuthorizations({ resourceType: this.$route.params.resourceTypeId, groupIdIn: filter })
      ])
      .then(authorizations => {
        authorizations = authorizations[0].concat(authorizations[1]).concat(authorizations[2])
        // Remove duplicates
        authorizations = authorizations.filter((value, index, self) =>
          index === self.findIndex((t) => (
          t.id === value.id
          ))
        )
        this.authorizations = authorizations
        this.loading = false
      })
    }),
    onFilterNameChange: function(item) {
      var filter = this.$store.state.filter.list.find(obj => obj.name === item.name)
      if (filter) item.resourceId = filter.id
      else item.resourceId = '*'
    },
    exportCSV: function() {
      this.exporting = true
      var params = this.$route.params
      var keys = ['type', 'userIdGroupId', 'permissions', 'resourceId']
      var csvContent = keys.map(k => this.$t('admin.authorizations.' + k)).join(';') + '\n'
      AdminService.findAuthorizations({ resourceType: params.resourceTypeId }).then(auths => {
        if (auths.length > 0) {
          auths.forEach(r => {
            csvContent += getStringObjByKeys(keys, r) + '\n'
          })
          var csvBlob = new Blob([csvContent], { type: 'text/csv' })
          var filename = 'authorizations_' + this.$t('admin.authorizations.resourcesTypes.' + params.resourceTypeKey) +
            '_' + moment().format('YYYYMMDD_HHmm') + '.csv'
          this.$refs.importPopper.triggerDownload(csvBlob, filename)
        }
        this.exporting = false
      })
    }
  }
}
</script>
