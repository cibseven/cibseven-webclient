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
  <div class="h-100 d-flex flex-column">
    <CIBHeaderFlow v-if="$root.header === 'true'" ref="headerFlow" class="flex-shrink-0" :languages="$root.config.supportedLanguages.sort()" :user="$root.user" @logout="logout">
      <div class="me-auto d-flex flex-row overflow-hidden" style="min-height: 38px;">
        <b-navbar-brand ref="brandHome" class="py-0 flex-shrink-0" :aria-label="$t('cib-header.productName') + ' - ' + $t('navigation.home')" to="/seven/auth/start">
          <img height="38px" alt="" :src="$root.logoPath" class="d-none d-md-inline"/>
          <img height="38px" alt="" :src="$root.logoIconPath" class="d-md-none"/>
          <span class="d-none d-md-inline align-middle"></span>
        </b-navbar-brand>
        <div v-if="pageTitle" style="max-height: 38px; min-width: 0;" class="d-flex align-items-center overflow-hidden flex-shrink-1">
          <span class="border-start border-secondary py-3 me-2 me-md-3 d-none d-md-inline"></span>
          <h3 style="line-height: normal"
          class="m-0 text-secondary text-truncate">{{ pageTitle }}</h3>
        </div>
      </div>

      <b-button v-if="$root.user && startableProcesses && $route.name === 'tasklist'" class="d-none d-sm-block py-0 me-3" variant="light" :title="$t('start.startProcess.title')" :aria-label="$t('start.startProcess.title')" aria-haspopup="dialog" @click="openStartProcess()">
        <span class="mdi mdi-18px mdi-rocket" aria-hidden="true"><span class="d-none d-lg-inline ms-2">{{ $t('start.startProcess.title') }}</span></span>
      </b-button>

      <!-- Desktop: Show menus as icons outside collapse -->
      <div class="d-none d-md-flex">
        <b-button v-if="$root.config.layout.showFeedbackButton" variant="outline-secondary" @click="$refs.report.show()" class="border-0 py-0 me-2" :title="$t('seven.feedback')" :label="$t('seven.feedback')">
          <span class="mdi mdi-24px mdi-message-alert"></span>
        </b-button>

        <b-navbar-nav v-if="computedMenuItems.length > 0">
          <b-nav-item-dropdown extra-toggle-classes="py-1" right :title="$t('navigation.menu')" :label="$t('navigation.navigation')">
            <template v-slot:button-content>
              <span class="visually-hidden">{{ $t('navigation.menu') }}</span>
              <span class="mdi mdi-24px mdi-menu align-middle"></span>
            </template>
            <template v-for="(group, gIdx) in computedMenuItems" :key="gIdx">
              <b-dropdown-divider v-if="group.divider"></b-dropdown-divider>
              <b-dropdown-group v-else-if="group.items && group.items.length > 0" :header="$t(group.groupTitle)">
                <b-dropdown-item
                  v-for="(item, idx) in group.items"
                  :key="'ext-' + idx"
                  :to="item.to"
                  :href="item.href"
                  :title="$t(item.tooltip)"
                  :active="isMenuItemActive(item)"
                  :target="item.external ? '_blank' : undefined"
                ><span :class="item.group ? 'fw-semibold' : ''">{{ $t(item.title) }}</span></b-dropdown-item>
              </b-dropdown-group>
            </template>
          </b-nav-item-dropdown>
        </b-navbar-nav>
        
        <b-navbar-nav v-if="$root.config.layout.showInfoAndHelp">
          <b-nav-item-dropdown extra-toggle-classes="py-1" right :title="$t('navigation.infoAndHelp')" :label="$t('navigation.infoAndHelp')">
            <template v-slot:button-content>
              <span class="visually-hidden">{{ $t('navigation.infoAndHelp') }}</span>
              <span class="mdi mdi-24px mdi-help-circle align-middle"></span>
            </template>
            <template v-for="(item, idx) in helpMenuItems" :key="idx">
              <b-dropdown-item v-if="item.type === 'link'" :href="item.href" :title="$t(item.tooltip)" target="_blank">{{ $t(item.title) }}</b-dropdown-item>
              <b-dropdown-item-button v-else :title="$t(item.tooltip)" @click="$refs[item.ref].show()">{{ $t(item.title) }}</b-dropdown-item-button>
            </template>
          </b-nav-item-dropdown>
        </b-navbar-nav>
      </div>

      <!-- Mobile: Show menus as list items inside CIBHeaderFlow's collapse (via customNavItems slot) -->
      <template v-if="$root.user || ($root.config.layout.showInfoAndHelp && helpMenuItems.length > 0)" #customNavItems>
        <b-nav-item-dropdown v-if="$root.config.layout.showFeedbackButton" class="d-md-none" no-caret :title="$t('seven.feedback')" :label="$t('seven.feedback')">
          <template v-slot:button-content>
            <span class="mdi mdi-24px mdi-message-alert align-middle me-2"></span>{{ $t('seven.feedback') }}
          </template>
          <b-dropdown-item @click="closeMenuAndShow('report')">{{ $t('seven.feedback') }}</b-dropdown-item>
        </b-nav-item-dropdown>
        <b-nav-item-dropdown v-if="computedMenuItems.length > 0" class="d-md-none" extra-toggle-classes="py-1" right :title="$t('navigation.menu')" :label="$t('navigation.navigation')">
          <template v-slot:button-content>
            <span class="mdi mdi-24px mdi-menu align-middle me-2"></span>{{ $t('navigation.menu') }}
          </template>
          <template v-for="(group, gIdx) in computedMenuItems" :key="gIdx">
            <b-dropdown-divider v-if="group.divider"></b-dropdown-divider>
            <b-dropdown-group v-else-if="group.items && group.items.length > 0" :header="$t(group.groupTitle)">
              <b-dropdown-item
                v-for="(item, idx) in group.items"
                :key="'mob-' + idx"
                :to="item.to"
                :href="item.href"
                :title="$t(item.tooltip)"
                :active="isMenuItemActive(item)"
                :target="item.external ? '_blank' : undefined"
              ><span :class="item.group ? 'fw-semibold' : ''">{{ $t(item.title) }}</span></b-dropdown-item>
            </b-dropdown-group>
          </template>
        </b-nav-item-dropdown>
        <b-nav-item-dropdown class="d-md-none" extra-toggle-classes="py-1" right :title="$t('navigation.infoAndHelp')" :label="$t('navigation.infoAndHelp')">
          <template v-slot:button-content>
            <span class="mdi mdi-24px mdi-help-circle align-middle me-2"></span>{{ $t('navigation.infoAndHelp') }}
          </template>
          <template v-for="(item, idx) in helpMenuItems" :key="idx">
            <b-dropdown-item v-if="item.type === 'link'" :href="item.href" :title="$t(item.tooltip)" target="_blank">{{ $t(item.title) }}</b-dropdown-item>
            <b-dropdown-item-button v-else :title="$t(item.tooltip)" @click="closeMenuAndShow(item.ref)">{{ $t(item.title) }}</b-dropdown-item-button>
          </template>
        </b-nav-item-dropdown>
      </template>

      <template v-slot:userItems>
        <b-dropdown-item v-if="$root.user && $root.config.layout.showUserSettings && !applicationPermissionsDenied($root.config.permissions.userProfile, 'userProfile')"
          :to="'/seven/auth/account/' + $root.user.id"
          :active="isMenuItemActive({active: ['seven/auth/account']})"
          :title="$t('admin.users.profile')">{{ $t('admin.users.profile') }}</b-dropdown-item>
      </template>
    </CIBHeaderFlow>

    <main class="flex-grow-1 overflow-hidden d-flex flex-column">
      <router-view class="flex-grow-1 overflow-hidden" ref="down"></router-view>
    </main>

    <b-modal ref="ieNotification" :title="$t('seven.titleInfo')">
      <div class="container-fluid">
        <div class="row">
          {{ $t('ienotify.text') }}
        </div>
      </div>
      <template v-slot:modal-footer>
        <div class="row w-100 me-0">
          <div class="col col-8 p-0 pt-1"><b-form-checkbox v-model="rememberNotShow">{{$t('ienotify.remember')}}</b-form-checkbox></div>
          <div class="col col-4"><b-button variant="primary" @click="doNotShowIeNotification();$refs.ieNotification.hide()" class="float-right">{{ $t('start.close') }}</b-button></div>
        </div>
      </template>
    </b-modal>

    <ShortcutsModal ref="shortcuts"></ShortcutsModal>
    <SupportModal ref="support" v-if="$root.config.layout.showSupportInfo"></SupportModal>
    <AboutModal ref="about"></AboutModal>
    <FeedbackModal ref="report" url="feedback" :email="$root.user && $root.user.email" @report="$refs.down.$emit('report', $event)"></FeedbackModal>

    <GlobalEvents 
      v-for="shortcut in globalShortcuts" 
      :key="shortcut.id" 
      @keydown="handleShortcut($event, shortcut)">
    </GlobalEvents>

  </div>
</template>

<script>
import platform from 'platform'
import { permissionsMixin } from '@/permissions.js'
import { getGlobalNavigationShortcuts, checkKeyMatch } from '@/utils/shortcuts.js'
import ShortcutsModal from '@/components/modals/ShortcutsModal.vue'
import AboutModal from '@/components/modals/AboutModal.vue'
import SupportModal from '@/components/modals/SupportModal.vue'
import CIBHeaderFlow from '@/components/common-components/CIBHeaderFlow.vue'
import FeedbackModal from '@/components/modals/FeedbackModal.vue'
import { updateAppTitle } from '@/utils/init'

export default {
  name: 'CibSeven',
  components: { ShortcutsModal, AboutModal, SupportModal, CIBHeaderFlow, FeedbackModal },
  mixins: [permissionsMixin],
  inject: ['isMobile'],
  data: function() {
    return {
       rememberNotShow: false
    }
  },
  watch: {
    // when the title of the view inside top toolbar is changed
    // => let's change title of the whole web-page in browser
    pageTitle: function(title) {
      this.refreshAppTitle(title)
    }
  },
  computed: {
    menuItems: function() {
      return [{
          show: this.permissionsTaskList && this.startableProcesses,
          groupTitle: 'start.taskList.title',
          items: [{
              to: '/seven/auth/start-process',
              active: ['seven/auth/start-process'],
              tooltip: 'start.startProcess.tooltip',
              title: 'start.startProcess.title'
            }, {
              to: '/seven/auth/tasks',
              active: ['seven/auth/tasks'],
              tooltip: 'start.taskList.tooltip',
              title: 'start.taskList.title'
            }
          ]
        }, {
          show: this.permissionsTaskList && this.permissionsCockpit && this.startableProcesses,
          divider: true,
        }, {
          show: this.permissionsCockpit,
          groupTitle: 'start.cockpit.title',
          items: [{
              group: true,
              to: '/seven/auth/processes',
              active: ['seven/auth/processes/dashboard'],
              tooltip: 'start.cockpit.tooltip',
              title: 'start.cockpit.title'
            }, {
              to: '/seven/auth/processes/list',
              active: ['seven/auth/process/', 'seven/auth/processes/list'],
              tooltip: 'start.cockpit.processes.tooltip',
              title: 'start.cockpit.processes.title'
            }, {
              to: '/seven/auth/decisions/list',
              active: ['seven/auth/decision/', 'seven/auth/decisions/list'],
              tooltip: 'start.cockpit.decisions.tooltip',
              title: 'start.cockpit.decisions.title',
            }, {
              to: '/seven/auth/human-tasks',
              active: ['seven/auth/human-tasks'],
              tooltip: 'start.cockpit.humanTasks.tooltip',
              title: 'start.cockpit.humanTasks.title'
            }, {
              to: '/seven/auth/deployments',
              active: ['seven/auth/deployments'],
              tooltip: 'start.cockpit.deployments.tooltip',
              title: 'start.cockpit.deployments.title'
            }, {
              to: '/seven/auth/batches',
              active: ['seven/auth/batches'],
              tooltip: 'start.cockpit.batches.tooltip',
              title: 'start.cockpit.batches.title',
              activeExact: true,
            }
          ]
        }, {
          show: this.permissionsUsers && (this.permissionsTaskList || this.permissionsCockpit),
          divider: true,
        }, {
          show: this.permissionsUsers,
          groupTitle: 'start.admin.title',
          items: [{
              group: true,
              to: '/seven/auth/admin',
              active: ['seven/auth/admin'],
              activeExact: true,
              tooltip: 'start.admin.tooltip',
              title: 'start.admin.title'
            }, {
              to: '/seven/auth/admin/users',
              active: ['seven/auth/admin/user', 'seven/auth/admin/create-user'],
              tooltip: 'admin.users.title',
              title: 'admin.users.title'
            }, {
              to: '/seven/auth/admin/groups',
              active: ['seven/auth/admin/group', 'seven/auth/admin/create-group'],
              tooltip: 'admin.groups.title',
              title: 'admin.groups.title'
            }, {
              to: '/seven/auth/admin/tenants',
              active: ['seven/auth/admin/tenant', 'seven/auth/admin/create-tenant'],
              tooltip: 'admin.tenants.tooltip',
              title: 'admin.tenants.title'
            }, {
              to: '/seven/auth/admin/authorizations',
              active: ['seven/auth/admin/authorizations'],
              tooltip: 'admin.authorizations.title',
              title: 'admin.authorizations.title'
            }, {
              to: '/seven/auth/admin/system',
              active: ['seven/auth/admin/system'],
              tooltip: 'admin.system.tooltip',
              title: 'admin.system.title'
            }
          ]
        }, {
          show: this.permissionsCockpit && this.$root.config.cockpitUrl,
          divider: true,
        }, {
          show: this.permissionsCockpit && this.$root.config.cockpitUrl,
          groupTitle: 'start.oldCockpit.title',
          items: [{
              href: this.$root.config.cockpitUrl,
              tooltip: 'start.oldCockpit.tooltip',
              title: 'start.oldCockpit.title',
              external: true
            }
          ]
        }
      ]
    },
    computedMenuItems: function() {
      return this.getVisibleMenuItems(this.menuItems)
    },
    helpMenuItems: function() {
      const items = []
      if (this.$root.config.flowLinkHelp) items.push({ type: 'link', href: this.$root.config.flowLinkHelp, title: 'infoAndHelp.flowLinkHelp', tooltip: 'infoAndHelp.flowLinkHelp' })
      if (this.$root.config.flowLinkAccessibility) items.push({ type: 'link', href: this.$root.config.flowLinkAccessibility, title: 'infoAndHelp.flowLinkAccessibility', tooltip: 'infoAndHelp.flowLinkAccessibility' })
      if (this.$root.config.flowLinkTerms) items.push({ type: 'link', href: this.$root.config.flowLinkTerms, title: 'infoAndHelp.flowLinkTerms', tooltip: 'infoAndHelp.flowLinkTerms' })
      if (this.$root.config.flowLinkPrivacy) items.push({ type: 'link', href: this.$root.config.flowLinkPrivacy, title: 'infoAndHelp.flowLinkPrivacy', tooltip: 'infoAndHelp.flowLinkPrivacy' })
      if (this.$root.config.flowLinkImprint) items.push({ type: 'link', href: this.$root.config.flowLinkImprint, title: 'infoAndHelp.flowLinkImprint', tooltip: 'infoAndHelp.flowLinkImprint' })
      if (this.$root.user) items.push({ type: 'button', ref: 'shortcuts', title: 'infoAndHelp.shortcuts.title', tooltip: 'infoAndHelp.shortcuts.tooltip' })
      if (this.$root.config.layout.showSupportInfo) items.push({ type: 'button', ref: 'support', title: 'infoAndHelp.flowModalSupport.modalText', tooltip: 'infoAndHelp.flowModalSupport.modalText' })
      items.push({ type: 'button', ref: 'about', title: 'infoAndHelp.flowModalAbout.modalText', tooltip: 'infoAndHelp.flowModalAbout.modalText' })
      return items
    },
    startableProcesses: function() {
      return this.processesFiltered.find(p => { return p.startableInTasklist })
    },
    processesFiltered: function() {
      if (!this.$store.state.process.list) return []
      return this.$store.state.process.list.filter(process => {
        return ((!process.revoked))
      }).sort((objA, objB) => {
        const nameA = objA.name ? objA.name.toUpperCase() : objA.name
        const nameB = objB.name ? objB.name.toUpperCase() : objB.name
        const compareAMoreB = nameA > nameB ? 1 : 0
        let comp = nameA < nameB ? -1 : compareAMoreB

        if (this.$root.config.subProcessFolder) {
          if (objA.resource.includes(this.$root.config.subProcessFolder)) comp = 1
          else if (objB.resource.includes(this.$root.config.subProcessFolder)) comp = -1
        }
        return comp
      })
    },
    // when route is changed => let's change title of the view inside top toolbar
    pageTitle: function() {
      let title = ''
      this.computedMenuItems.some(group => {
        if (!group.items) {
           return false
        }
        const item = group.items.find(i => this.isMenuItemActive(i))
        if (item) {
          // exceptional case with 'Processes' menu item
          if (this.$route.name === 'process') {
            const hasInstanceIdParam = this.$route.params?.instanceId?.length > 0
            if (hasInstanceIdParam) {
              title = this.$t('start.cockpit.process-instance.title')
            }
            else {
              title = this.$t('start.cockpit.process-definition.title')
            }
          }
          else if (this.$route.name === 'decision-list') {
            title = this.$t('start.cockpit.decisions.title')
          }
          // default
          if (!title) {
            title = this.$t(item.title)
          }
          return true
        }
        else {
          if (this.$route.name === 'decision-version') {
            title = this.$t('start.cockpit.decision-definition.title')
          }
          else if (this.$route.name === 'decision-instance') {
            title = this.$t('start.cockpit.decision-instance.title')
          }
        }
        return false
      })
      return title
    },
    globalShortcuts() {
      const shortcuts = getGlobalNavigationShortcuts(this.$root.config)
      return shortcuts.filter(shortcut => {
        // Apply permission checks based on the route
        if (shortcut.route.includes('/seven/auth/start-process') || shortcut.route.includes('/seven/auth/tasks')) {
          return this.permissionsTaskList
        }
        if (shortcut.route.includes('/seven/auth/processes')) {
          return this.permissionsCockpit
        }
        return true
      })
    },
    permissionsTaskList: function() {
      return this.$root.user && this.applicationPermissions(this.$root.config.permissions.tasklist, 'tasklist')
    },
    permissionsCockpit: function() {
      return this.$root.user && this.applicationPermissions(this.$root.config.permissions.cockpit, 'cockpit')
    },
    permissionsUsers: function() {
      return this.$root.user && this.hasAdminManagementPermissions(this.$root.config.permissions)
    },
    isUsersManagementActive: function() {
      return this.$route.path.includes('seven/auth/admin/user') ||
        this.$route.path.includes('seven/auth/admin/group') ||
        this.$route.path.includes('seven/auth/admin/authorizations')
    }
  },
  mounted: function () {
    if (platform.name === 'IE') {
      const isNotifiedUser = localStorage.getItem('ienotify')
      if (!isNotifiedUser) this.$refs.ieNotification.show() //must notify the user
    }
    this.refreshAppTitle(this.pageTitle)
    // Focus the brand-home link for screen reader accessibility when user is logged in
    if (this.$root.user) {
      this.$nextTick(() => {
        if (this.$refs.headerFlow && this.$refs.brandHome) {
          const brandLink = this.$refs.brandHome.$refs.brandLink
          brandLink?.focus()
        }
      })
    }
  },
  methods: {
    // override this method to add/remove menu items
    getVisibleMenuItems: function(items) {
      return items.filter(group => group.show)
    },
    isMenuItemActive: function(item) {
      if (!item.active) {
        return false
      }
      if (item.activeExact) {
        return item.active.some(a => this.$route.path.endsWith(a))
      } else {
        return item.active.some(a => this.$route.path.includes(a))
      }
    },
    logout: function() {
      this.$router.push('/')
      location.reload() //refresh to empty vuex and axios defaults
      //Remove some storage variables when logout
      //https://helpdesk.cib.de/browse/BPM4CIB-3691
      localStorage.removeItem('accessToken')
      localStorage.removeItem('tokenModeler')
      sessionStorage.removeItem('accessToken')
      sessionStorage.removeItem('tokenModeler')
      // Note: engine token cleanup is handled by CIBHeaderFlow.logout()
    },
    openStartProcess: function() {
      this.$eventBus.emit('openStartProcess')
    },
    handleShortcut: function(event, shortcut) {
      // Check if the current key combination matches the shortcut
      const isMatch = checkKeyMatch(event, shortcut.keys)
      if (isMatch) {
        event.preventDefault()
        this.$router.push(shortcut.route)
      }
    },
    doNotShowIeNotification: function() { if (this.rememberNotShow) localStorage.setItem('ienotify', true) },
    closeMenuAndShow: function(modalRef) {
      // Close the burger menu on mobile before showing modal
      if (this.$refs.headerFlow) {
        this.$refs.headerFlow.closeMenu()
      }
      // Show the modal
      this.$refs[modalRef].show()
    },
    // change title of the whole web-page in browser
    refreshAppTitle: function (title) {
      switch (this.$route.name) {
        case 'adminUser':
        case 'adminUsers':
        case 'createUser':
        case 'adminGroup':
        case 'adminGroups':
        case 'createGroup':
        case 'authorizations':
        case 'authorizationType':
        case 'createTenant':
        case 'adminTenant':
        case 'adminTenants':
          // "CIB seven | Admin | <view>"
          updateAppTitle(
            this.$root.config.productNamePageTitle || this.$t('cib-header.productName'),
            this.$t('start.admin.title'),
            title
          )
          break
        default:
          // "CIB seven | <view>"
          updateAppTitle(
            this.$root.config.productNamePageTitle || this.$t('cib-header.productName'),
            title
          )
          break
      }
    }
  }
}
</script>

<style lang="css" scoped>
/* Customizing the separator to reduce gap */
.dropdown-divider {
  margin-top: 0.15rem; /* Reduce top gap */
  margin-bottom: 0.15rem; /* Reduce bottom gap */
}
</style>
