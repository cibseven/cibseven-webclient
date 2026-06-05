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
          <b-button class="me-1" size="sm" variant="secondary" v-if="$root.config.userProvider === 'org.cibseven.webapp.auth.SevenUserProvider'" @click="add()">
            <span class="mdi mdi-plus"> {{ $t('admin.groups.add') }} </span>
          </b-button>
          <b-button v-if="$root.config.userProvider === 'org.cibseven.webapp.auth.SevenUserProvider'" size="sm" variant="light"
            :disabled="exporting" @click="exportCSV()">
            <span v-if="exporting"><b-spinner small></b-spinner> {{ $t('admin.exportCsv') }}</span>
            <span v-else class="mdi mdi-download"> {{ $t('admin.exportCsv') }} </span>
          </b-button>
        </div>
      </div>
    </div>
    <div class="container overflow-auto bg-white shadow-sm border rounded g-0" @scroll="showMore">
      <FlowTable thead-class="sticky-header" striped :items="groups" primary-key="id"
        :fields="[
          { label: 'admin.groups.id', key: 'id', class: 'col-md-4 col-sm-4', tdClass: 'py-1' },
          { label: 'admin.groups.name', key: 'name', class: 'col-md-4 col-sm-4', tdClass: 'py-1' },
          { label: 'admin.groups.type', key: 'type', class: 'col-md-2 col-sm-2', tdClass: 'py-1' },
          { label: 'admin.groups.actions', key: 'actions', class: 'col-md-2 col-sm-2 text-center', sortable: false, thClass: 'justify-content-center', tdClass: 'justify-content-center py-0' },
        ]"
      >
        <template v-slot:cell(actions)="row">
          <template v-if="$root.config.userProvider === 'org.cibseven.webapp.auth.SevenUserProvider'">
            <CellActionButton @click="edit(row.item)" :title="$t('admin.groups.editGroup')" icon="mdi-pencil-outline"></CellActionButton>
            <CellActionButton @click="prepareRemove(row.item)" :title="$t('admin.groups.deleteGroup')" icon="mdi-delete-outline"></CellActionButton>
          </template>
          <template v-else>
            <CellActionButton @click="edit(row.item)" :title="$t('admin.groups.viewGroup')" icon="mdi-eye-outline"></CellActionButton>
          </template>
        </template>
      </FlowTable>
      <div class="mb-3 text-center w-100" v-if="loading">
        <BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('admin.loading') }}
      </div>
      <div class="mb-3 text-center w-100" v-if="!loading && groups.length === 0">
        {{ $t('admin.noResults') }}
      </div>
    </div>

    <ConfirmDialog ref="deleteModal" @ok="remove(groupSelected)" :ok-title="$t('confirm.delete')">
      <div v-if="groupSelected">
        <p>{{ $t('admin.groups.confirmDelete') }}</p>
        <p>
          <strong>{{ $t('admin.groups.id') }}:</strong> {{ groupSelected.id }} <br>
          <strong>{{ $t('admin.groups.name') }}:</strong> {{ groupSelected.name }}<br>
          <strong>{{ $t('admin.groups.type') }}:</strong> {{ groupSelected.type }}
        </p>
      </div>
    </ConfirmDialog>

    <TaskPopper ref="importPopper"></TaskPopper>
  </div>
</template>

<script>
import { AdminService } from '@/services.js'
import { moment } from '@/globals.js'
import { debounce } from '@/utils/debounce.js'
import { getStringObjByKeys } from '@/components/admin/utils.js'
import { FlowTable , TaskPopper, ConfirmDialog, BWaitingBox } from '@cib/common-frontend'
import CellActionButton from '@/components/common-components/CellActionButton.vue'

export default {
  name: 'AdminGroups',
  components: { FlowTable, TaskPopper, BWaitingBox, ConfirmDialog, CellActionButton },
  data: function () {
    return {
      selected: null,
      filter: '',
      groups: [],
      groupSelected: null,
      firstResult: 0,
      maxResults: 40,
      loading: false,
      exporting: false
    }
  },
  watch: {
    filter: function() {
      this.searchGroups()
    }
  },
  created: function () {
    this.loading = true
    this.loadGroups()
  },
  methods: {
    loadGroups: debounce(800, function (showMore = false) {
      AdminService.findGroups({ firstResult: this.firstResult, maxResults: this.maxResults }).then(response => {
        if (!showMore) this.groups = response
        else this.groups = this.groups.concat(response)
        this.loading = false
      })
    }),
    add: function() {
      this.$router.push({ name: 'createGroup' })
    },
    prepareRemove: function(group) {
      this.groupSelected = group
      this.$refs.deleteModal.show()
    },
    remove: function (group) {
      AdminService.deleteGroup(group.id).then(() => {
        this.firstResult = 0
        this.groupSelected = null
        this.loadGroups()
      })
    },
    edit: function(group) {
      this.$router.push({ name: 'adminGroup', params: { groupId: group.id }, query: { tab: 'information' } })
    },
    showMore: function(el) {
      if (this.firstResult <= this.groups.length && !this.loading) {
        if ((el.target.offsetHeight + el.target.scrollTop + 1) >= el.target.scrollHeight && this.filter==='') {
          this.firstResult += this.maxResults
          this.loading = true
          this.loadGroups(true)
        }
      }
    },
    searchGroups: function() {
      if (this.filter.length > 2) {
        this.groups = []
        this.loading = true
        this.findGroups(this.filter)
      } else if (!this.filter || this.filter.length === 0) {
        this.groups = []
        this.userSelected = null
        this.firstResult = 0
        this.loading = true
        this.loadGroups()
      }
    },
    findGroups: debounce(800, function(filter) {
      const nameLike = ({ nameLike: '*' + filter + '*' })
      const id = ({ id: filter })

      Promise.all([AdminService.findGroups(nameLike), AdminService.findGroups(id)])
      .then(groups => {
        groups = groups[0].concat(groups[1])
        // Remove duplicates
        groups = groups.filter((value, index, self) =>
          index === self.findIndex((t) => (
          t.id === value.id
          ))
        )
        this.groups = groups
        this.loading = false
      })
    }),
    exportCSV: function() {
      this.exporting = true
      const keys = ['id', 'name', 'type']
      let csvContent = keys.map(k => this.$t('admin.groups.' + k)).join(';') + '\n'
      AdminService.findGroups().then(groups => {
        if (groups.length > 0) {
          groups.forEach(r => {
            csvContent += getStringObjByKeys(keys, r) + '\n'
          })
          const csvBlob = new Blob([csvContent], { type: 'text/csv' })
          const filename = 'groups_' + moment().format('YYYYMMDD_HHmm') + '.csv'
          this.$refs.importPopper.triggerDownload(csvBlob, filename)
        }
        this.exporting = false
      })
    }
  }
}
</script>
