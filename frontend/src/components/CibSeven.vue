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
    <CIBHeaderFlow v-if="$root.header === 'true'" class="flex-shrink-0" :languages="$root.config.supportedLanguages.sort()" :user="$root.user" @logout="logout">
      <div class="me-auto d-flex flex-column flex-md-row" style="height: 38px">
        <b-navbar-brand class="py-0" :title="$t('navigation.home')" to="/seven/auth/start">
          <img height="38px" :alt="$t('cib-header.productName')" :src="$root.logoPath"/>
          <span class="d-none d-md-inline align-middle"></span>
        </b-navbar-brand>
        <div v-if="pageTitle" style="max-height: 38px;" class="d-flex align-items-center text-truncate">
          <span class="border-start border-secondary py-3 me-3"></span>
          <h3 style="line-height: normal"
          class="m-0 text-secondary text-truncate">{{ pageTitle }}</h3>
        </div>
      </div>

      <b-button v-if="$root.user && startableProcesses && $route.name === 'tasklist'" class="d-none d-sm-block py-0 me-3" variant="light" :title="$t('start.startProcess.title')" @click="openStartProcess()">
        <span class="mdi mdi-18px mdi-rocket"><span class="d-none d-lg-inline ms-2">{{ $t('start.startProcess.title') }}</span></span>
      </b-button>

      <!-- Main menu -->
      <b-collapse v-if="computedMenuItems.length > 0" is-nav id="nav_collapse" class="flex-grow-0 d-none d-md-flex">
        <b-navbar-nav>
          <b-nav-item-dropdown extra-toggle-classes="py-1" right :title="$t('navigation.menu')">
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
      </b-collapse>

      <div>
        <b-button v-if="$root.config.layout.showFeedbackButton" variant="outline-secondary" @click="$refs.report.show()" class="border-0 py-0 d-none d-md-flex" :title="$t('seven.feedback')">
          <span class="mdi mdi-24px mdi-message-alert"></span>
        </b-button>
      </div>

      <b-navbar-nav>
        <b-nav-item-dropdown extra-toggle-classes="py-1" right :title="$t('navigation.infoAndHelp')">
          <template v-slot:button-content>
            <span class="visually-hidden">{{ $t('navigation.infoAndHelp') }}</span>
            <span class="mdi mdi-24px mdi-help-circle align-middle"></span>
          </template>
          <b-dropdown-item v-if="$root.config.flowLinkHelp != ''" :href="$root.config.flowLinkHelp" :title="$t('infoAndHelp.flowLinkHelp')" target="_blank">{{ $t('infoAndHelp.flowLinkHelp') }}</b-dropdown-item>
          <b-dropdown-item v-if="$root.config.flowLinkAccessibility != ''" :href="$root.config.flowLinkAccessibility" :title="$t('infoAndHelp.flowLinkAccessibility')" target="_blank">{{ $t('infoAndHelp.flowLinkAccessibility') }}</b-dropdown-item>
          <b-dropdown-item v-if="$root.config.flowLinkTerms != ''" :href="$root.config.flowLinkTerms" :title="$t('infoAndHelp.flowLinkTerms')" target="_blank">{{ $t('infoAndHelp.flowLinkTerms') }}</b-dropdown-item>
          <b-dropdown-item v-if="$root.config.flowLinkPrivacy != ''" :href="$root.config.flowLinkPrivacy" :title="$t('infoAndHelp.flowLinkPrivacy')" target="_blank">{{ $t('infoAndHelp.flowLinkPrivacy') }}</b-dropdown-item>
          <b-dropdown-item v-if="$root.config.flowLinkImprint != ''" :href="$root.config.flowLinkImprint" :title="$t('infoAndHelp.flowLinkImprint')" target="_blank">{{ $t('infoAndHelp.flowLinkImprint') }}</b-dropdown-item>
          <b-dropdown-item-button v-if="$root.user" :title="$t('infoAndHelp.shortcuts.tooltip')" @click="$refs.shortcuts.show()">{{ $t('infoAndHelp.shortcuts.title') }}</b-dropdown-item-button>
          <b-dropdown-item-button v-if="$root.config.layout.showSupportInfo" :title="$t('infoAndHelp.flowModalSupport.modalText')" @click="$refs.support.show()">{{ $t('infoAndHelp.flowModalSupport.modalText') }}</b-dropdown-item-button>
          <b-dropdown-item-button @click="$refs.about.show()" :title="$t('infoAndHelp.flowModalAbout.modalText')">{{ $t('infoAndHelp.flowModalAbout.modalText') }}</b-dropdown-item-button>
        </b-nav-item-dropdown>
      </b-navbar-nav>

      <template v-slot:userItems>
        <b-dropdown-item v-if="$root.user && $root.config.layout.showUserSettings && !applicationPermissionsDenied($root.config.permissions.userProfile, 'userProfile')"
          :to="'/seven/auth/account/' + $root.user.id" :title="$t('seven.account')">{{ $t('seven.account') }}</b-dropdown-item>
      </template>
    </CIBHeaderFlow>

    <router-view class="flex-grow-1 overflow-hidden" ref="down"></router-view>

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
    startableProcesses: function() {
      return this.processesFiltered.find(p => { return p.startableInTasklist })
    },
    processesFiltered: function() {
      if (!this.$store.state.process.list) return []
      return this.$store.state.process.list.filter(process => {
        return ((!process.revoked))
      }).sort((objA, objB) => {
        var nameA = objA.name ? objA.name.toUpperCase() : objA.name
        var nameB = objB.name ? objB.name.toUpperCase() : objB.name
        var comp = nameA < nameB ? -1 : nameA > nameB ? 1 : 0

        if (this.$root.config.subProcessFolder) {
          if (objA.resource.indexOf(this.$root.config.subProcessFolder) > -1) comp = 1
          else if (objB.resource.indexOf(this.$root.config.subProcessFolder) > -1) comp = -1
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
            const hasInstanceIdParam = 'instanceId' in this.$route.params
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
      var isNotifiedUser = localStorage.getItem('ienotify')
      if (!isNotifiedUser) this.$refs.ieNotification.show() //must notify the user
    }
    this.refreshAppTitle(this.pageTitle)
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
