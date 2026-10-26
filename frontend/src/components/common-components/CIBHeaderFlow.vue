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
  <header class="cib-navbar" style="height: 55px"> <!-- Empty container with height of navbar -->
    <b-navbar fixed="top" type="light" class="border-bottom bg-white px-3 flex-nowrap">
      <slot></slot>

      <!-- Page actions (e.g. start process), kept apart from the navigation by a divider -->
      <div v-if="$slots.actions" class="cib-nav-actions d-flex align-items-center flex-shrink-0">
        <slot name="actions"></slot>
        <span class="cib-nav-divider d-none d-md-inline-block" aria-hidden="true"></span>
      </div>

      <!-- Desktop: main clusters (incl. engine) | standard menus (help, language, user) -->
      <div class="d-none d-md-flex align-items-center flex-shrink-0 cib-nav-clusters">
        <NavCluster v-for="cluster in mainClusters" :key="cluster.id" :cluster="cluster"
          :open="openCluster === cluster.id" @update:open="setOpenCluster(cluster.id, $event)" @select="onSelect">
        </NavCluster>
        <span v-if="mainClusters.length && utilityList.length" class="cib-nav-divider" aria-hidden="true"></span>
        <NavCluster v-for="cluster in utilityList" :key="cluster.id" :cluster="cluster" align="end"
          :open="openCluster === cluster.id" @update:open="setOpenCluster(cluster.id, $event)" @select="onSelect">
          <template v-if="$slots.userItems" #menu-prepend="{ cluster: menu }">
            <ul v-if="menu.id === 'user'" class="cib-nav-legacy-items list-unstyled m-0 py-1"><slot name="userItems"></slot></ul>
          </template>
        </NavCluster>
      </div>

      <button type="button" class="cib-nav-action cib-nav-burger d-md-none flex-shrink-0"
        @click.stop="isCollapsed = !isCollapsed"
        :aria-expanded="isCollapsed ? 'true' : 'false'"
        :aria-label="$t('navigation.menu')">
        <span :class="['mdi', 'mdi-24px', isCollapsed ? 'mdi-close' : 'mdi-menu']" aria-hidden="true"></span>
      </button>
    </b-navbar>

    <NavMobileMenu v-if="isCollapsed" class="d-md-none" :main="mainClusters" :utility="utilityList"
      @select="onSelect" @navigate="closeMenu">
      <!-- Legacy slots (pre-navClusters parents such as CIB flow): rendered inside the new menus -->
      <template v-if="$slots.customNavItems" #prepend>
        <ul class="navbar-nav cib-nav-legacy-items px-3"><slot name="customNavItems"></slot></ul>
      </template>
      <template v-if="$slots.userItems" #cluster-prepend="{ cluster }">
        <ul v-if="cluster.id === 'user'" class="cib-nav-legacy-items list-unstyled m-0 py-1"><slot name="userItems"></slot></ul>
      </template>
    </NavMobileMenu>
  </header>
</template>

<script>
import { EngineService } from '@/services.js'
import { ENGINE_STORAGE_KEY } from '@/constants.js'
import { 
  storeTokenForEngine, 
  removeTokenForEngine, 
  hasTokenForEngine, 
  restoreTokenForEngine 
} from '@/utils/engineTokens.js'
import { getUserInitials } from '@/utils/user.js'
import NavCluster from '@/components/common-components/navigation/NavCluster.vue'
import NavMobileMenu from '@/components/common-components/navigation/NavMobileMenu.vue'

/**
 * Application header. The parent fills the left side (default slot: brand, page title, actions)
 * and hands in its navigation as cluster models (see NavCluster); the header adds the engine
 * selector to the main clusters and language + user to the standard ones, and renders all of
 * them as the desktop bar and as the mobile menu. Items the header does not handle itself are
 * passed on through `select`.
 */
export default {
  name: 'CIBHeaderFlow',
  components: { NavCluster, NavMobileMenu },
  inject: ['currentLanguage', 'setCurrentLanguage'],
  props: {
    languages: Array,
    user: Object,
    // Main navigation clusters (tasks, cockpit, ...)
    navClusters: { type: Array, default: () => [] },
    // Standard clusters placed before language and user (feedback, help)
    utilityClusters: { type: Array, default: () => [] },
    // Entries of the user menu above "Logout"; the first one with `to` is where the avatar leads.
    // Parents built for the old header may still use the `userItems` and `customNavItems` slots instead.
    userItems: { type: Array, default: () => [] }
  },
  emits: ['logout', 'select'],
  data() {
    return {
      engines: [],
      selectedEngine: null,
      isCollapsed: false,
      openCluster: null
    }
  },
  computed: {
    productName() {
      return this.$root.config.productNamePageTitle || this.$t('login.productName')
    },
    userInitials() {
      return getUserInitials(this.user)
    },
    engineCluster() {
      if (this.normalizedEngines.length <= 1) return null
      return {
        id: 'engine',
        label: this.$t('cib-header.engine'),
        icon: 'mdi-dns-outline',
        sections: [this.normalizedEngines.map(engine => ({
          key: `engine:${engine.id}`,
          label: engine.label,
          tooltip: engine.tooltip || `${this.$t('cib-header.engine')}: ${engine.label}`,
          active: engine.id === this.selectedEngine,
          checkable: true,
          engineId: engine.id
        }))]
      }
    },
    languageCluster() {
      if (!this.languages?.length) return null
      const current = this.currentLanguage()
      return {
        id: 'language',
        label: this.$t('cib-header.languages'),
        icon: 'mdi-web',
        sections: [this.languages.map(lang => ({
          key: `lang:${lang}`,
          tag: lang.toUpperCase(),
          label: this.$t(`cib-header.${lang}`),
          active: lang === current,
          checkable: true,
          language: lang
        }))]
      }
    },
    userCluster() {
      if (!this.user) return null
      return {
        id: 'user',
        label: this.user.displayName || this.user.id,
        avatar: this.userInitials,
        to: this.userItems.find(item => item.to)?.to ?? null,
        active: this.userItems.some(item => item.active),
        // Logout in a section of its own, so a line sets it apart from the account entries
        sections: [this.userItems, [{
          key: 'logout',
          label: this.$t('start.account.logout.title'),
          tooltip: this.$t('start.account.logout.tooltip', { productName: this.productName }),
          logout: true
        }]].filter(section => section.length)
      }
    },
    mainClusters() {
      return [...this.navClusters, this.engineCluster].filter(Boolean)
    },
    utilityList() {
      return [...this.utilityClusters, this.languageCluster, this.userCluster].filter(Boolean)
    },
    normalizedEngines() {
      return this.engines.map(engine => ({
        ...engine,
        // id is set by the backend in format "url|path|engineName"
        // Use it directly, or fall back to name for legacy support
        id: engine.id || engine.name,
        label: engine.displayName || engine.name
      }))
    }
  },
  watch: {
    // When user is set (after login), store the token for the current engine
    user(newUser) {
      if (newUser && this.selectedEngine) {
        storeTokenForEngine(this.selectedEngine)
      }
    },
    // Navigating closes whatever menu led there
    '$route.fullPath'() {
      this.openCluster = null
      this.closeMenu()
    }
  },
  mounted() {
    this.loadEngines()
    // Close menu when clicking outside on mobile
    document.addEventListener('click', this.handleClickOutside)
    // From md up the mobile menu is only hidden by CSS: close it there, which also ends its scroll lock
    this.desktopQuery = window.matchMedia?.('(min-width: 768px)')
    this.desktopQuery?.addEventListener('change', this.onDesktopChange)
  },
  beforeUnmount() {
    document.removeEventListener('click', this.handleClickOutside)
    this.desktopQuery?.removeEventListener('change', this.onDesktopChange)
  },
  methods: {
    onDesktopChange(event) {
      if (event.matches) this.closeMenu()
    },
    handleClickOutside(event) {
      // Only handle on mobile when menu is open
      if (!this.isCollapsed) return
      // The header holds the bar and the mobile menu; clicks inside either keep the menu open
      if (!this.$el.contains(event.target)) {
        this.closeMenu()
      }
    },
    closeMenu() {
      this.isCollapsed = false
    },
    setOpenCluster(id, open) {
      if (open) this.openCluster = id
      else if (this.openCluster === id) this.openCluster = null
    },
    // Engine, language and logout are the header's own; anything else goes to the parent
    onSelect(item) {
      this.closeMenu()
      if (item.engineId) this.selectEngine(item.engineId)
      else if (item.language) this.setCurrentLanguage(item.language)
      else if (item.logout) this.logout()
      else this.$emit('select', item)
    },
    logout: function() {
      // Remove the token for the current engine from the cache
      if (this.selectedEngine) {
        removeTokenForEngine(this.selectedEngine)
      }
      sessionStorage.getItem('token') ? sessionStorage.removeItem('token') : localStorage.removeItem('token')
      this.$emit('logout')
    },
    loadEngines() {
      EngineService.getEngines()
        .then(response => {
          this.engines = response
          this.initializeSelectedEngine()
        })
    },
    initializeSelectedEngine() {
      // Check if an engine is already selected in localStorage
      const storedEngine = localStorage.getItem(ENGINE_STORAGE_KEY)
      
      if (storedEngine && this.normalizedEngines.some(e => e.id === storedEngine)) {
        this.selectedEngine = storedEngine
      } else {
        // No stored engine or stored engine not found in list
        // Try to find 'default' engine
        const defaultEngine = this.normalizedEngines.find(e => e.name === 'default')
        if (defaultEngine) {
          this.selectedEngine = defaultEngine.id
        } else if (this.normalizedEngines.length > 0) {
          // Take the first engine
          this.selectedEngine = this.normalizedEngines[0].id
        }
        
        // Store the selected engine
        if (this.selectedEngine) {
          localStorage.setItem(ENGINE_STORAGE_KEY, this.selectedEngine)
        }
      }
    },
    selectEngine(engineIdentifier) {
      const previousEngine = this.selectedEngine
      
      // Store current token for the old engine before switching
      if (previousEngine) {
        storeTokenForEngine(previousEngine)
      }
      
      this.selectedEngine = engineIdentifier
      localStorage.setItem(ENGINE_STORAGE_KEY, engineIdentifier)
      
      // Check if we have a cached token for the new engine
      if (hasTokenForEngine(engineIdentifier)) {
        // Restore the cached token for the new engine
        restoreTokenForEngine(engineIdentifier)
        // Change the URL to the start page first, then force a full reload
        // This ensures the app reinitializes with the correct engine context and URL
        window.location.hash = '#/seven/auth/start-configurable'
        window.location.reload()
      } else {
        // No cached token, need to logout and re-authenticate. logout() navigates to
        // the start page before reloading, so login won't return to the old engine's page.
        this.logout()
      }
    }
  }
}
</script>

<style scoped>
/* One keyboard focus ring for every link and button in the header and its menus: the theme's dark
   outline rather than each browser's own colour, never an underline. Inset, so the menus' clipping
   does not cut it off. */
.cib-navbar :deep(a:focus-visible),
.cib-navbar :deep(button:focus-visible) {
  outline: 2px solid var(--bs-dark) !important;
  outline-offset: -2px;
  box-shadow: none !important;
  text-decoration: none;
}
.cib-nav-clusters {
  gap: 2px;
}
.cib-nav-divider {
  width: 1px;
  height: 24px;
  margin: 0 10px;
  /* The theme's $gray-650 (input borders), which has no CSS variable */
  background: #8092A6;
}
/* Page actions from the parent and the burger: one button style, reacting like the nav buttons */
.cib-nav-action,
:slotted(.cib-nav-action) {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 5px 10px;
  border: 1px solid var(--bs-gray-600);
  border-radius: 4px;
  background: var(--bs-body-bg);
  color: var(--bs-gray-800);
  font-size: 0.875rem;
  font-weight: 600;
}
.cib-nav-action:hover,
:slotted(.cib-nav-action:hover) {
  background: var(--bs-gray-300);
}
.cib-nav-action:active,
:slotted(.cib-nav-action:active) {
  background: var(--bs-gray-400);
}
/* Square touch targets on mobile */
.cib-nav-burger {
  width: 44px;
  height: 44px;
  margin-left: 0.5rem;
  padding: 0;
}
@media (max-width: 767.98px) {
  :slotted(.cib-nav-action) {
    width: 44px;
    height: 44px;
    padding: 0;
  }
}
</style>
