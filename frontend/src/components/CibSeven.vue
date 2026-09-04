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
        <b-navbar-brand ref="brandHome" class="py-0 flex-shrink-0" :aria-label="productName + ' - ' + $t('navigation.home')" to="/seven/auth/start">
          <img height="38px" alt="" :src="$root.logoPath" class="d-none d-md-inline"/>
          <img height="38px" alt="" :src="$root.logoIconPath" class="d-md-none"/>
          <span class="d-none d-md-inline align-middle"></span>
        </b-navbar-brand>
        <div v-if="pageTitle" style="max-height: 38px; min-width: 0;" class="d-flex align-items-center overflow-hidden flex-shrink-1">
          <span class="border-start border-secondary py-3 me-2 me-md-3 d-none d-md-inline"></span>
          <h1 style="line-height: normal"
          class="h3 m-0 text-secondary text-truncate">{{ pageTitle }}</h1>
        </div>
        <h1 v-else-if="$route.name === 'start'" class="visually-hidden">{{ $t('navigation.home') + ' - ' + $t('navigation.menu') }}</h1>
      </div>

      <b-button v-if="$root.user && startableProcesses && $route.name === 'tasklist'" class="d-none d-sm-block py-0 me-3" variant="light"
        :title="$t('start.startProcess.title')"
        :aria-label="$t('start.startProcess.title')" aria-haspopup="dialog" @click="openStartProcess()">
        <span class="mdi mdi-18px mdi-rocket" aria-hidden="true"></span>
        <span class="d-none d-lg-inline ms-2">{{ $t('start.startProcess.title') }}</span>
      </b-button>

      <!-- Desktop: navbar tools | utilities -->
      <div class="d-none d-md-flex align-items-center cib-navbar-tools">
        <b-navbar-nav v-if="computedMenuItems.length > 0" class="flex-row align-items-center">
          <li
            v-for="tool in computedMenuItems"
            :key="tool.id"
            class="nav-item d-flex align-items-center gap-0 cib-navbar-tool"
          >
            <router-link
              class="cib-navbar-icon-btn d-inline-flex align-items-center justify-content-center p-0 text-decoration-none text-body-secondary"
              :class="{ 'cib-navbar-control-active': isToolActive(tool) }"
              :to="getToolDefaultTo(tool)"
              :title="$t(tool.title)"
              :aria-label="$t(tool.title)"
            >
              <span :class="['mdi', 'mdi-24px', tool.icon]" aria-hidden="true"></span>
            </router-link>
            <b-nav-item-dropdown
              class="cib-navbar-chevron-dropdown"
              :class="{ 'cib-navbar-control-active': isToolActive(tool) }"
              no-caret
              right
              :title="$t(tool.title)"
              :label="$t(tool.title)"
            >
              <template v-slot:button-content>
                <span class="visually-hidden">{{ $t(tool.title) }}</span>
                <span class="mdi mdi-18px mdi-chevron-down align-middle" aria-hidden="true"></span>
              </template>
              <b-dropdown-item
                class="cib-dropdown-title"
                :to="getToolDefaultTo(tool)"
                :title="$t(tool.title)"
              ><span class="fw-semibold">{{ $t(tool.title) }}</span></b-dropdown-item>
              <b-dropdown-divider></b-dropdown-divider>
              <template v-for="(item, idx) in tool.items" :key="tool.id + '-' + idx">
                <b-dropdown-divider v-if="item.divider"></b-dropdown-divider>
                <b-dropdown-item
                  v-else
                  :to="item.to"
                  :href="item.href"
                  :title="item.tooltip ? $t(item.tooltip) : $t(item.title)"
                  :active="isMenuItemActive(item)"
                  :target="item.external ? '_blank' : undefined"
                >{{ $t(item.title) }}</b-dropdown-item>
              </template>
            </b-nav-item-dropdown>
          </li>
        </b-navbar-nav>

        <span
          v-if="computedMenuItems.length > 0 || $root.config.layout.showFeedbackButton || $root.config.layout.showInfoAndHelp"
          class="cib-navbar-divider d-inline-block flex-shrink-0 mx-2"
          aria-hidden="true"
        ></span>

        <b-button
          v-if="$root.config.layout.showFeedbackButton"
          variant="outline-secondary"
          @click="$refs.report.show()"
          class="border-0 cib-navbar-icon-btn cib-navbar-icon-btn-standalone d-inline-flex align-items-center justify-content-center p-0 text-decoration-none text-body-secondary me-1"
          :title="$t('seven.feedback')"
          :aria-label="$t('seven.feedback')"
        >
          <span class="mdi mdi-24px mdi-message-alert" aria-hidden="true"></span>
        </b-button>

        <b-navbar-nav v-if="$root.config.layout.showInfoAndHelp">
          <b-nav-item-dropdown
            class="cib-navbar-utility-dropdown"
            extra-toggle-classes="cib-navbar-icon-btn"
            no-caret
            right
            :title="$t('navigation.infoAndHelp')"
            :label="$t('navigation.infoAndHelp')"
          >
            <template v-slot:button-content>
              <span class="visually-hidden">{{ $t('navigation.infoAndHelp') }}</span>
              <span class="mdi mdi-24px mdi-help-circle align-middle" aria-hidden="true"></span>
              <span class="mdi mdi-18px mdi-chevron-down align-middle" aria-hidden="true"></span>
            </template>
            <li class="cib-dropdown-title px-3 pt-2 pb-1" role="presentation">{{ $t('navigation.infoAndHelp') }}</li>
            <b-dropdown-divider></b-dropdown-divider>
            <template v-for="(item, idx) in helpMenuItems" :key="'help-' + idx">
              <b-dropdown-item v-if="item.type === 'link'" :href="item.href" :title="$t(item.tooltip)" target="_blank">{{ $t(item.title) }}</b-dropdown-item>
              <b-dropdown-item-button v-else :title="$t(item.tooltip)" @click="$refs[item.ref].show()">{{ $t(item.title) }}</b-dropdown-item-button>
            </template>
          </b-nav-item-dropdown>
        </b-navbar-nav>
      </div>

      <!-- Mobile: tools + feedback + help inside collapse -->
      <template v-if="$root.user || ($root.config.layout.showInfoAndHelp && helpMenuItems.length > 0)" #customNavItems>
        <template v-for="tool in computedMenuItems" :key="'mob-' + tool.id">
          <b-nav-item-dropdown
            class="d-md-none cib-navbar-utility-dropdown"
            extra-toggle-classes="py-1"
            right
            :title="$t(tool.title)"
            :label="$t(tool.title)"
          >
            <template v-slot:button-content>
              <span class="d-flex align-items-center">
                <span :class="['mdi', 'mdi-24px', tool.icon, 'align-middle', 'me-2']" aria-hidden="true"></span>
                <span class="cib-mobile-row-title">{{ $t(tool.title) }}</span>
              </span>
              <span class="mdi mdi-18px mdi-chevron-down align-middle ms-1" aria-hidden="true"></span>
            </template>
            <b-dropdown-item
              class="cib-dropdown-title"
              :to="getToolDefaultTo(tool)"
              :title="$t(tool.title)"
            ><span class="fw-semibold">{{ $t(tool.title) }}</span></b-dropdown-item>
            <b-dropdown-divider></b-dropdown-divider>
            <template v-for="(item, idx) in tool.items" :key="'mob-' + tool.id + '-' + idx">
              <b-dropdown-divider v-if="item.divider"></b-dropdown-divider>
              <b-dropdown-item
                v-else
                :to="item.to"
                :href="item.href"
                :title="item.tooltip ? $t(item.tooltip) : $t(item.title)"
                :active="isMenuItemActive(item)"
                :target="item.external ? '_blank' : undefined"
              >{{ $t(item.title) }}</b-dropdown-item>
            </template>
          </b-nav-item-dropdown>
        </template>
        <b-nav-item-dropdown v-if="$root.config.layout.showFeedbackButton" class="d-md-none" no-caret :title="$t('seven.feedback')" :label="$t('seven.feedback')">
          <template v-slot:button-content>
            <span class="mdi mdi-24px mdi-message-alert align-middle me-2"></span>{{ $t('seven.feedback') }}
          </template>
          <b-dropdown-item-button @click="closeMenuAndShow('report')">{{ $t('seven.feedback') }}</b-dropdown-item-button>
        </b-nav-item-dropdown>
        <b-nav-item-dropdown
          v-if="$root.config.layout.showInfoAndHelp"
          class="d-md-none cib-navbar-utility-dropdown"
          extra-toggle-classes="py-1"
          right
          :title="$t('navigation.infoAndHelp')"
          :label="$t('navigation.infoAndHelp')"
        >
          <template v-slot:button-content>
            <span class="d-flex align-items-center">
              <span class="mdi mdi-24px mdi-help-circle align-middle me-2"></span>{{ $t('navigation.infoAndHelp') }}
            </span>
            <span class="mdi mdi-18px mdi-chevron-down align-middle ms-1" aria-hidden="true"></span>
          </template>
          <li class="cib-dropdown-title px-3 pt-2 pb-1" role="presentation">{{ $t('navigation.infoAndHelp') }}</li>
          <b-dropdown-divider></b-dropdown-divider>
          <template v-for="(item, idx) in helpMenuItems" :key="'mob-help-' + idx">
            <b-dropdown-item v-if="item.type === 'link'" :href="item.href" :title="$t(item.tooltip)" target="_blank">{{ $t(item.title) }}</b-dropdown-item>
            <b-dropdown-item-button v-else :title="$t(item.tooltip)" @click="closeMenuAndShow(item.ref)">{{ $t(item.title) }}</b-dropdown-item-button>
          </template>
        </b-nav-item-dropdown>
      </template>

      <template v-slot:userItems>
        <b-dropdown-item v-if="$root.user && $root.config.layout.showUserSettings && !applicationPermissionsDenied($root.config.permissions.userProfile, 'userProfile')"
          :to="'/seven/auth/account/' + $root.user.id"
          :active="isMenuItemActive({active: ['seven/auth/account']})"
          :title="$t('start.account.profile.tooltip')">{{ $t('start.account.profile.title') }}</b-dropdown-item>
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
import navigationPermissionsMixin from '@/mixins/navigationPermissionsMixin.js'
import { getGlobalNavigationShortcuts, checkKeyMatch } from '@/utils/shortcuts.js'
import ShortcutsModal from '@/components/modals/ShortcutsModal.vue'
import AboutModal from '@/components/modals/AboutModal.vue'
import SupportModal from '@/components/modals/SupportModal.vue'
import CIBHeaderFlow from '@/components/common-components/CIBHeaderFlow.vue'
import FeedbackModal from '@/components/modals/FeedbackModal.vue'
import { updateAppTitle } from '@/utils/init'
import { buildNavGroups, projectGroupsForNavbar, permissionFlagsFromVm } from '@/navigation/navGroups.js'

export default {
  name: 'CibSeven',
  components: { ShortcutsModal, AboutModal, SupportModal, CIBHeaderFlow, FeedbackModal },
  mixins: [permissionsMixin, navigationPermissionsMixin],
  inject: ['isMobile'],
  data: function() {
    return {
       rememberNotShow: false
    }
  },
  watch: {
    // when the title of the view inside top navbar is changed
    // => let's change title of the whole web-page in browser
    pageTitle: function(title) {
      this.refreshAppTitle(title)
    }
  },
  computed: {
    productName() {
      return this.$root.config.productNamePageTitle || this.$t('login.productName')
    },
    menuItems: function() {
      return buildNavGroups(permissionFlagsFromVm(this))
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
      items.push({ type: 'button', ref: 'about', title: 'infoAndHelp.about.title', tooltip: 'infoAndHelp.about.tooltip' })
      return items
    },
    startableProcesses: function() {
      return (this.$store.state.process.list || []).some(process => process.startableInTasklist === true && !process.revoked && process.suspended !== 'true')
    },
    // when route is changed => let's change title of the view inside top navbar
    pageTitle: function() {
      if (this.$route.meta?.title) {
        return this.$t(this.$route.meta.title)
      }
      let title = ''
      this.computedMenuItems.some(tool => {
        if (!tool.items) {
           return false
        }
        const item = tool.items.find(i => !i.divider && this.isMenuItemActive(i))
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
      return projectGroupsForNavbar(items)
    },
    getToolDefaultTo: function(tool) {
      return tool.defaultTo || tool.items.find(item => item.to)?.to || '/seven/auth/start'
    },
    isToolActive: function(tool) {
      if (tool.startTo?.name && this.$route.name === tool.startTo.name) return true
      if (tool.hubRouteNames?.includes(this.$route.name)) return true
      return tool.items.some(item => this.isMenuItemActive(item))
    },
    isMenuItemActive: function(item) {
      if (item.routeName || item.activeRouteNames) {
        return this.$route.name === item.routeName || !!item.activeRouteNames?.includes(this.$route.name)
      }
      if (!item.active) {
        return false
      }
      const path = this.$route.path
      if (item.activeExact) {
        return item.active.some(a => path.endsWith(a))
      }
      return item.active.some(a => a.endsWith('/')
        ? path.includes('/' + a)
        : path.endsWith('/' + a) || path.includes('/' + a + '/'))
    },
    logout: function() {
      //Remove some storage variables when logout
      //https://helpdesk.cib.de/browse/BPM4CIB-3691
      localStorage.removeItem('accessToken')
      localStorage.removeItem('tokenModeler')
      sessionStorage.removeItem('accessToken')
      sessionStorage.removeItem('tokenModeler')
      // Note: engine token cleanup is handled by CIBHeaderFlow.logout()
      // Set the hash before reload: router.push is async and loses the race, so the
      // reload would otherwise land on the current page instead of the start page.
      window.location.hash = '#/'
      window.location.reload() //refresh to empty vuex and axios defaults
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
            this.productName,
            this.$t('start.admin.title'),
            title
          )
          break
        default:
          // "CIB seven | <view>"
          updateAppTitle(
            this.productName,
            title
          )
          break
      }
    }
  }
}
</script>

<style lang="css" scoped>
.dropdown-divider {
  margin-top: 0.15rem;
  margin-bottom: 0.15rem;
}
</style>

<!--
  .cib-navbar-divider / .cib-navbar-* / .cib-dropdown-title / .cib-navbar-utility
  chrome styling now lives in @cib/bootstrap-theme (src/scss/_utilities.scss) —
  CibSevenEE extends this component, and BNavItemDropdown hardcodes Bootstrap's
  dropdown-toggle caret, so neither scoped nor :deep() selectors here can reach
  it reliably. See that file's comment for the full explanation.
-->

