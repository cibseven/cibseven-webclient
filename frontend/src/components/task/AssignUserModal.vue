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
  <b-modal ref="modal" class="modal-centered" :title="$t('task.assign')" hide-footer @shown="$refs.search.focus()">
    <div class="visually-hidden" ref="ariaLiveText" aria-live="polite"></div>
    <label for="assign-user-search" class="form-label">{{ $t('task.searchUserLabel') }}</label>
    <div class="input-group">
      <span class="input-group-text bg-white"><span class="mdi mdi-18px mdi-magnify" aria-hidden="true"></span></span>
      <input id="assign-user-search" ref="search" v-model.trim="query" type="search" class="form-control"
        :placeholder="$t('task.searchUserPlaceholder')" autocomplete="off" aria-describedby="assign-user-search-hint">
    </div>
    <div id="assign-user-search-hint" class="form-text">{{ $t('task.searchUserHint') }}</div>

    <div class="mt-3">
      <div v-if="loadingUsers" class="text-center py-3"><b-spinner small></b-spinner></div>
      <template v-else-if="users.length > 0">
        <h4 class="h6 text-secondary mb-2">{{ isSearching ? $t('task.searchResults') : $t('task.candidateUsers') }}</h4>
        <div class="list-group list-group-flush border-top border-bottom overflow-auto" style="max-height: 40vh">
          <button v-for="user in users" :key="user.id" type="button"
            class="list-group-item list-group-item-action d-flex align-items-center px-2" @click="select(user)">
            <b-avatar class="me-3 flex-shrink-0" :text="user.id" variant="light"></b-avatar>
            <span class="flex-grow-1 text-truncate">
              <span class="d-block text-truncate">{{ fullName(user) }}</span>
              <span class="d-block small text-secondary">{{ user.id }}</span>
            </span>
          </button>
        </div>
      </template>
      <p v-else-if="isSearching" class="d-flex align-items-center text-secondary mb-0">
        <span class="mdi mdi-18px mdi-account-off-outline me-2" aria-hidden="true"></span>{{ $t('task.noUsersFound') }}
      </p>
    </div>
  </b-modal>
</template>

<script>
import { debounce } from '@/utils/debounce.js'
import usersMixin from '@/mixins/usersMixin.js'

const MIN_SEARCH_LENGTH = 3

export default {
  name: 'AssignUserModal',
  mixins: [usersMixin],
  emits: ['select'],
  data: function() {
    return {
      query: '',
      loadingUsers: false
    }
  },
  computed: {
    isSearching: function() {
      return this.query.length >= MIN_SEARCH_LENGTH
    },
    users: function() {
      // findUsers merges several queries, so the same user can come back more than once
      const seen = new Set()
      return this.$store.state.user.searchUsers.filter(user => {
        if (seen.has(user.id)) return false
        seen.add(user.id)
        return true
      })
    }
  },
  watch: {
    query: function() {
      if (this.isSearching) {
        this.loadingUsers = true
        this.search()
      } else {
        this.loadingUsers = false
        this.resetUsers()
      }
    }
  },
  methods: {
    show: function() {
      this.query = ''
      this.resetUsers()
      this.$refs.modal.show()
    },
    hide: function() {
      this.$refs.modal.hide()
    },
    search: debounce(800, function() {
      if (this.isSearching) this.findUsers(this.query, true)
    }),
    select: function(user) {
      this.$emit('select', user.id)
      this.hide()
    },
    fullName: function(user) {
      return [user.firstName, user.lastName].filter(Boolean).join(' ') || user.id
    }
  }
}
</script>
