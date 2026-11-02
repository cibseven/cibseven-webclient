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
  <SidebarsFlow ref="sidebars" header-margin="55px" v-model:left-open="leftOpen"
    :left-caption="$t('admin.users.title') + ' - ' + user.id" :left-size="[12, 6, 4, 3, 2]">
    <template v-slot:left>
      <b-list-group>
        <b-list-group-item v-for="tab in visibleTabs" :key="tab.id" class="border-0 px-3 py-2"
          :active="selectedTab === tab" exact :to="tabRoute(tab)">
          <span> {{ $t(tab.label) }}</span>
        </b-list-group-item>
      </b-list-group>
    </template>

    <transition name="slide-in" mode="out-in">
      <div class="d-flex flex-column bg-light w-100 h-100" v-if="user.id">
        <b-button v-if="editMode" variant="light" style="min-height: 40px; line-height: 20px;" :block="true" class="rounded-0 border-bottom text-start" :to="{ name: 'adminUsers' }">
          <span class="mdi mdi-arrow-left me-2"></span>
          <span class="fw-bold">{{ $t('admin.users.title') }}</span>
        </b-button>
        <div class="container-fluid overflow-auto">
          <!-- Each tab is a child route; a new user means a new tab, so it loads that user's data -->
          <div class="row pt-3 ps-4 pe-4">
            <router-view v-slot="{ Component }">
              <component :is="Component" :key="user.id" v-bind="tabProps"></component>
            </router-view>
          </div>
        </div>

        <b-modal v-if="editMode" ref="deleteModal" :title="$t('confirm.title')">
          <div class="container-fluid">
            <div class="row align-items-center">
              <div class="col-2">
                <span class="mdi-36px mdi mdi-alert-outline text-warning me-3"></span>
              </div>
              <div class="col-10">
                <p>{{ $t('admin.users.confirmDelete') }}</p>
                <strong>{{ $t('admin.users.id') }}:</strong> {{ user.id }} <br>
                <strong>{{ $t('admin.users.firstName') }}:</strong> {{ user.firstName }}<br>
                <strong>{{ $t('admin.users.lastName') }}:</strong> {{ user.lastName }}<br>
                <strong>{{ $t('admin.users.email') }}:</strong> {{ user.email }}
              </div>
            </div>
          </div>
          <template v-slot:modal-footer>
            <b-button @click="$refs.deleteModal.hide()" variant="light">{{ $t('confirm.cancel') }}</b-button>
            <b-button @click="deleteUser(); $refs.deleteModal.hide()" variant="primary">{{ $t('confirm.ok') }}</b-button>
          </template>
        </b-modal>

      </div>
    </transition>
    <SuccessAlert ref="deleteUser" top="0" style="z-index: 1031">{{ $t('admin.users.userDeletedMessage', [user.id]) }}</SuccessAlert>
  </SidebarsFlow>
</template>

<script>
import { AdminService } from '@/services.js'
import { profileTabRouteName } from '@/components/admin/utils.js'
import { SidebarsFlow, SuccessAlert } from '@cib/common-frontend'

export default {
  name: 'ProfileUser',
  components: { SidebarsFlow, SuccessAlert },
  props: {
    editMode: {
      type: Boolean,
      default: false
    },
  },
  data: function () {
    return {
      leftOpen: true,
      user: { id: null, firstName: null, lastName: null, email: null, noInfo: true },
      // The tabs of the page, in menu order. Each one is a child route of the page (see profileTabRouteName), shown
      // when show(vm) holds and given props(vm), or by default the user and the edit mode. An extension of
      // the page adds its own tabs here, and their routes to the router.
      tabs: [
        { id: 'profile', label: 'admin.users.profile', show: vm => !vm.user.noInfo },
        { id: 'account', label: 'password.recover.changePassword', show: vm => vm.canChangePassword },
        { id: 'groups', label: 'admin.users.groups', show: vm => !vm.user.noInfo },
        { id: 'tenants', label: 'admin.tenants.title', show: vm => !vm.user.noInfo },
        { id: 'preferences', label: 'admin.preferences.title', show: vm => vm.$root.user?.id === vm.user.id, props: () => ({}) }
      ]
    }
  },
  watch: {
    '$route.params.userId': function() {
      if (!this.editMode && this.$route.params.userId !== this.$router.app.user.id) {
        this.$router.push({ name: 'start' })
      } else {
        this.loadUser(this.$route.params.userId)
      }
    },
    '$route.name': function() {
      if (this.user.id) this.openShownTab()
    }
  },
  computed: {
    // The route the page is shown under, which names the routes of its tabs
    page() {
      return this.editMode ? 'adminUser' : 'account'
    },
    visibleTabs() {
      return this.tabs.filter(tab => tab.show(this))
    },
    selectedTab() {
      return this.tabs.find(tab => this.$route.name === profileTabRouteName(this.page, tab.id))
    },
    tabProps() {
      return this.selectedTab?.props?.(this) ?? { user: this.user, editMode: this.editMode }
    },
    canChangePassword() {
      return !this.user.noInfo && this.$root.config.userEditable && this.$root.config.userPasswordChangeEnabled
    }
  },
  created: function () {
    if (this.$route.params.userId) this.loadUser(this.$route.params.userId)
  },
  methods: {
    loadUser: async function(userId) {
      return AdminService.findUsers({ id: userId }).then(response => {
        this.user = response[0] || { id: userId, firstName: null, lastName: null, email: null, noInfo: true }
        this.openShownTab()
      })
    },
    tabRoute: function(tab) {
      return { name: profileTabRouteName(this.page, tab.id), params: this.$route.params }
    },
    // A tab that is not shown for this user (e.g. the profile of a user the engine has no details of) is
    // swapped for the first one that is. A user without details opens their preferences, the one tab
    // that is theirs, even when an extension puts a tab of its own before it.
    openShownTab: function() {
      if (!this.selectedTab || this.selectedTab.show(this) || this.visibleTabs.length === 0) return
      const preferences = this.user.noInfo && this.visibleTabs.find(tab => tab.id === 'preferences')
      this.$router.replace(this.tabRoute(preferences || this.visibleTabs[0]))
    },
    deleteUser: function() {
      AdminService.deleteUser(this.user.id).then(() => {
        this.$refs.deleteUser.show(2)
        this.$router.push({ name: 'adminUsers' })
      })
    }
  }
}
</script>
