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
    <CIBHeaderFlow v-if="$root.header === 'true'" ref="headerFlow" class="flex-shrink-0" :languages="$root.config.supportedLanguages.sort()" :user="$root.user"
      :nav-clusters="navClusters" :utility-clusters="utilityClusters" :user-items="userMenuItems"
      @logout="logout" @select="onHeaderSelect">
      <div class="me-auto d-flex flex-row overflow-hidden" style="min-height: 38px;">
        <!-- Product name on the home page and the user's own account page; elsewhere the icon and the page title -->
        <b-navbar-brand ref="brandHome" class="py-0 flex-shrink-0" :aria-label="brandLabel" :title="brandLabel" to="/seven/auth/start">
          <img v-if="showFullLogo" height="38px" alt="" :src="$root.logoPath"/>
          <!-- The mark at the size and place it has in the full logo, so it does not change when leaving
               the start page: the icon file draws it about 1/0.62 times larger (matched by eye at 23.75px;
               whole pixels look visibly too small or too big). Set on the element: EE renders this template
               from its own component, where this component's scoped styles do not apply. -->
          <img v-else style="height: 23.75px; margin: 7px 0 7.25px 9px" alt="" :src="$root.logoIconPath"/>
        </b-navbar-brand>
        <div v-if="pageTitle" style="max-height: 38px; min-width: 0;" class="d-flex align-items-center overflow-hidden flex-shrink-1">
          <span class="border-start border-secondary py-3 me-2 me-md-3"></span>
          <h1 style="line-height: normal"
          class="h3 m-0 text-secondary text-truncate">{{ pageTitle }}</h1>
        </div>
        <h1 v-else-if="isStartPage" class="visually-hidden">{{ $t('navigation.home') + ' - ' + $t('navigation.menu') }}</h1>
      </div>

      <template v-if="$root.user && startableProcesses && $route.name === 'tasklist'" #actions>
        <button type="button" class="cib-nav-action" :title="$t('start.startProcess.title')"
          :aria-label="$t('start.startProcess.title')" aria-haspopup="dialog" @click="openStartProcess()">
          <span class="mdi mdi-18px mdi-rocket" aria-hidden="true"></span>
          <span class="d-none d-md-inline">{{ $t('start.startProcess.title') }}</span>
        </button>
      </template>
    </CIBHeaderFlow>

    <!-- Messages for every logged-in user, e.g. system notifications of the enterprise edition -->
    <div v-if="$root.user" class="flex-shrink-0">
      <PluginSlot name="app-banner" :params="{ user: $root.user }"></PluginSlot>
    </div>

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
import PluginSlot from '@/components/common/PluginSlot.vue'
import { updateAppTitle } from '@/utils/init'
import { buildNavGroups, extendNavGroups, projectGroupsForNavbar, permissionFlagsFromVm, itemsToSections, singleDestination } from '@/navigation/navGroups.js'

export default {
  name: 'CibSeven',
  components: { ShortcutsModal, AboutModal, SupportModal, CIBHeaderFlow, FeedbackModal, PluginSlot },
  mixins: [permissionsMixin, navigationPermissionsMixin],
  inject: ['isMobile'],
  emits: [],
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
    // Label and tooltip of the logo, which leads to the start page
    brandLabel() {
      return `${this.productName} - ${this.$t('navigation.home')}`
    },
    // Same catalog and extensions as the start page and hub pages (see buildVisibleNavGroups)
    menuItems: function() {
      return extendNavGroups(buildNavGroups(permissionFlagsFromVm(this)), this)
    },
    computedMenuItems: function() {
      return this.getVisibleMenuItems(this.menuItems)
    },
    isStartPage: function() {
      return this.$route.name === 'start'
    },
    // The account page has no page title next to the logo, so it keeps the full one like the start page
    showFullLogo: function() {
      return this.isStartPage || this.$route.name === 'account'
    },
    // The catalog as header clusters: the icon leads to the section, the menu title to its overview page
    navClusters: function() {
      return this.computedMenuItems.map(tool => {
        const single = singleDestination(tool)?.to
        return {
          id: tool.id,
          label: this.$t(tool.title),
          icon: tool.icon,
          to: single ?? this.getToolDefaultTo(tool),
          // The menu title always opens the overview page, even when it has a single tile
          titleTo: tool.startTo ?? single ?? null,
          active: this.isToolActive(tool),
          sections: itemsToSections(tool.items).map(section => section.map(item => ({
            key: item.href ?? JSON.stringify(item.to) ?? item.title,
            label: this.$t(item.title),
            tooltip: this.$t(item.tooltip || item.title),
            to: item.to,
            href: item.href,
            external: item.external,
            active: this.isMenuItemActive(item)
          })))
        }
      })
    },
    utilityClusters: function() {
      const clusters = []
      if (this.$root.config.layout.showFeedbackButton) {
        clusters.push({ id: 'feedback', label: this.$t('seven.feedback'), icon: 'mdi-message-alert-outline', modal: 'report' })
      }
      if (this.$root.config.layout.showInfoAndHelp && this.helpMenuItems.length > 0) {
        clusters.push({
          id: 'help',
          label: this.$t('navigation.infoAndHelp'),
          icon: 'mdi-help-circle-outline',
          sections: [this.helpMenuItems.map(item => ({
            key: item.href ?? item.ref,
            label: this.$t(item.title),
            tooltip: this.$t(item.tooltip),
            href: item.href,
            external: item.type === 'link',
            modal: item.ref
          }))]
        })
      }
      return clusters
    },
    userMenuItems: function() {
      if (!this.$root.user || !this.$root.config.layout.showUserSettings || !this.permissionsUserProfile) return []
      const config = this.$root.config
      // One entry per tab of the account page (ProfileUser), which reads the tab from ?tab=
      const tabs = [
        { tab: 'profile', label: 'start.account.profile.title', tooltip: 'start.account.profile.tooltip' },
        { tab: 'account', label: 'password.recover.changePassword', show: !!(config.userEditable && config.userPasswordChangeEnabled) },
        { tab: 'groups', label: 'admin.users.groups' },
        { tab: 'tenants', label: 'admin.tenants.title' },
        { tab: 'authorizations', label: 'admin.authorizations.title', tooltip: 'admin.authorizations.tooltip', show: this.applicationPermissions(this.$root.config.permissions.authorizationsManagement, 'authorization') },
        { tab: 'preferences', label: 'admin.preferences.title' }
      ]
      const onAccountPage = this.isMenuItemActive({ active: ['seven/auth/account'] })
      const currentTab = this.$route.query?.tab ?? 'profile'
      return tabs.filter(entry => entry.show !== false).map(({ tab, label, tooltip }) => ({
        key: tab,
        label: this.$t(label),
        tooltip: this.$t(tooltip ?? label),
        to: { name: 'account', params: { userId: this.$root.user.id }, query: { tab } },
        active: onAccountPage && currentTab === tab
      }))
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
      this.$nextTick(() => this.focusBrandLink())
    }
  },
  methods: {
    // Browsers ring a focus set on load as if it came from the keyboard. The ring stays
    // hidden until the first key press, so keyboard users still see where they are.
    focusBrandLink: function() {
      const brandLink = this.$refs.headerFlow && this.$refs.brandHome?.$refs.brandLink
      if (!brandLink) return
      brandLink.classList.add('initial-focus')
      brandLink.focus()
      document.addEventListener('keydown', () => brandLink.classList.remove('initial-focus'), { once: true, capture: true })
    },
    // override this method to add/remove menu items
    getVisibleMenuItems: function(items) {
      return projectGroupsForNavbar(items)
    },
    getToolDefaultTo: function(tool) {
      return tool.defaultTo || tool.items.find(item => item.to)?.to || '/seven/auth/start'
    },
    onHeaderSelect: function(item) {
      if (item.modal) this.$refs[item.modal]?.show()
    },
    isToolActive: function(tool) {
      if (tool.startTo?.name && this.$route.name === tool.startTo.name) return true
      if (tool.hubRouteNames?.includes(this.$route.name)) return true
      return tool.items.some(item => this.isMenuItemActive(item))
    },
    isMenuItemActive: function(item) {
      if (item.routeName || item.activeRouteNames) {
        // A route without a name matches none of them (undefined === undefined must not count)
        const name = this.$route.name
        return !!name && (name === item.routeName || !!item.activeRouteNames?.includes(name))
      }
      if (!item.active) {
        return false
      }
      // Entries match whole path segments: 'seven/auth/tasks' does not match '/seven/auth/tasks-home'.
      // A leading '/' is optional; a trailing '/' matches anything below that path.
      const path = this.$route.path
      return item.active.some(entry => {
        const a = entry.replace(/^\//, '')
        if (item.activeExact) return path.endsWith(`/${a}`)
        if (a.endsWith('/')) return path.includes(`/${a}`)
        return path.endsWith(`/${a}`) || path.includes(`/${a}/`)
      })
    },
    logout: function() {
      // The logged-out page signs the user out, while it shows what is going on
      this.$router.push({ name: 'loggedOut' })
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
/* See focusBrandLink */
:deep(.navbar-brand.initial-focus:focus) {
  outline: none !important;
  box-shadow: none;
}</style>
