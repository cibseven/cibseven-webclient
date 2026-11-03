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
    :left-caption="$t('admin.groups.title') + ' - ' + $route.params.groupId" :left-size="[12, 6, 4, 3, 2]">
    <template v-slot:left>
      <b-list-group v-if="!notFound">
        <b-list-group-item v-for="tab in visibleTabs" :key="tab.id" class="border-0 px-3 py-2"
          :active="selectedTab === tab" exact :to="tabRoute(tab)">
          <span> {{ $t(tab.label) }}</span>
        </b-list-group-item>
      </b-list-group>
    </template>

    <transition name="slide-in" mode="out-in">
      <div v-if="group.id || notFound" class="d-flex flex-column h-100 bg-light">
        <b-button variant="light" style="min-height: 40px; line-height: 20px;" :block="true" class="rounded-0 border-bottom text-start" :to="{ name: 'adminGroups' }">
          <span class="mdi mdi-arrow-left me-2"></span>
          <span class="fw-semibold">{{ $t('admin.groups.title') }}</span>
        </b-button>
        <WarningBox v-if="notFound" :message="$t('admin.groups.notFound')"></WarningBox>
        <div v-else class="container-fluid overflow-auto">
          <!-- Each tab is a child route; a new group means a new tab, so it loads that group's data -->
          <div class="row">
            <router-view v-slot="{ Component }">
              <component :is="Component" :key="group.id" v-bind="tabProps"></component>
            </router-view>
          </div>
        </div>
      </div>
    </transition>
  </SidebarsFlow>
</template>

<script>
import { AdminService } from '@/services.js'
import { profileTabRouteName } from '@/components/admin/utils.js'
import { profileTabs } from '@/components/admin/profileTabs.js'
import { permissionsMixin } from '@/permissions.js'
import WarningBox from '@/components/common-components/WarningBox.vue'
import { SidebarsFlow } from '@cib/common-frontend'

export default {
  name: 'ProfileGroup',
  components: { SidebarsFlow, WarningBox },
  mixins: [permissionsMixin],
  data: function() {
    return {
      leftOpen: true,
      group: { id: null, name: null,  type: null },
      // The engine knows no group of the id in the route (e.g. a deleted group, or a mistyped or old link)
      notFound: false,
      // Without props(page), a tab gets the group and a handler taking the saved group (see profileTabs.js)
      tabs: profileTabs('group')
    }
  },
  watch: {
    '$route.params.groupId': function() {
      this.loadGroup(this.$route.params.groupId)
    },
    '$route.name': function() {
      if (this.group.id) this.openShownTab()
    }
  },
  computed: {
    // For the tabs' show() (see profileTabs.js)
    config() {
      return this.$root.config
    },
    visibleTabs() {
      return this.tabs.filter(tab => tab.show(this))
    },
    selectedTab() {
      return this.tabs.find(tab => this.$route.name === profileTabRouteName('adminGroup', tab.id))
    },
    tabProps() {
      return this.selectedTab?.props?.(this) ?? { group: this.group, 'onUpdate:group': group => { this.group = group } }
    }
  },
  created: function() {
    if (this.$route.params.groupId) this.loadGroup(this.$route.params.groupId)
  },
  methods: {
    loadGroup: async function(groupId) {
      const response = await AdminService.findGroups({ id: groupId })
      // The route has moved on to another group (e.g. browser back), whose response may have come first
      if (groupId !== this.$route.params.groupId) return
      this.notFound = !response[0]
      this.group = response[0] ?? { id: null, name: null, type: null }
      if (!this.notFound) this.openShownTab()
    },
    tabRoute: function(tab) {
      return { name: profileTabRouteName('adminGroup', tab.id), params: this.$route.params }
    },
    // A tab that is not shown (e.g. one the user lacks the permission for) is swapped for the first one that is
    openShownTab: function() {
      if (!this.selectedTab || this.selectedTab.show(this) || this.visibleTabs.length === 0) return
      this.$router.replace(this.tabRoute(this.visibleTabs[0]))
    }
  }
}
</script>
