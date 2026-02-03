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
  <div style="height: 55px"> <!-- Empty container with height of navbar -->
    <b-navbar toggleable="md" fixed="top" type="light" class="border-bottom bg-white px-3">
      <slot></slot>
      <button 
        class="navbar-toggler" 
        type="button" 
        @click.stop="isCollapsed = !isCollapsed"
        :aria-expanded="isCollapsed"
        aria-label="Toggle navigation">
        <span v-if="isCollapsed" class="mdi mdi-24px mdi-close"></span>
        <span v-else class="navbar-toggler-icon"></span>
      </button>
      <b-collapse is-nav id="nav_collapse" class="flex-grow-0" v-model="isCollapsed">
        <b-navbar-nav>
          <!-- Custom navigation items slot (for mobile menu items from parent) -->
          <slot name="customNavItems"></slot>
          
          <!-- Engine Selector - only show if more than one engine -->
          <b-nav-item-dropdown 
            v-if="normalizedEngines.length > 1" 
            extra-toggle-classes="py-1" 
            right 
            :title="$t('cib-header.engineMenu')"
            :label="$t('cib-header.engineMenu')">
            <template v-slot:button-content>
              <span class="mdi mdi-24px mdi-engine align-middle me-2" aria-hidden="true"></span><span class="d-md-none">{{ $t('cib-header.engine') }}</span>
            </template>
            <b-dropdown-item 
              v-for="engine in normalizedEngines" 
              :key="engine.id" 
              :active="engine.id === selectedEngine" 
              @click="selectEngine(engine.id)" 
              :title="engine.tooltip || $t('cib-header.engine') + ': ' + engine.label">
              <div class="d-flex align-items-baseline">
                <span class="flex-grow-1">
                  {{ engine.label }}
                </span>
              </div>
            </b-dropdown-item>
          </b-nav-item-dropdown>

          <b-nav-item-dropdown 
            extra-toggle-classes="py-1" 
            right 
            :title="$t('cib-header.languages')"
            :label="$t('cib-header.languagesMenu')">
            <template v-slot:button-content>
              <span class="mdi mdi-24px mdi-web align-middle me-2" aria-hidden="true"></span><span class="d-md-none">{{ $t('cib-header.languages') }}</span>
            </template>
            <b-dropdown-item v-for="lang in languages" :key="lang" :active="lang === currentLanguage()" @click="setCurrentLanguage(lang)" :title="$t('cib-header.languages') + ': ' + $t('cib-header.' + lang)">
              <div class="d-flex align-items-baseline">
                <span class="lang-label text-center text-uppercase text-dark rounded me-2" :class="{ 'lang-label-active': lang === currentLanguage() }">
                  {{ lang }}
                </span>
                <span class="flex-grow-1">
                  {{ $t('cib-header.' + lang) }}
                </span>
              </div>
            </b-dropdown-item>
          </b-nav-item-dropdown>

          <b-nav-item-dropdown 
            v-if="$slots.helpItems" 
            extra-toggle-classes="py-1" 
            right
            :label="$t('cib-header.helpMenu')">
            <template v-slot:button-content>
              <span class="mdi mdi-24px mdi-help-circle align-middle" aria-hidden="true"></span>
              <span class="d-md-none">{{ $t('cib-header.helpMenu') }}</span>
            </template>
            <slot name="helpItems"></slot>
          </b-nav-item-dropdown>

          <b-nav-item-dropdown 
            v-if="user" 
            :title="$t('admin.users.account')" 
            extra-toggle-classes="py-1" 
            right
            :label="$t('cib-header.userMenu')">
            <template v-slot:button-content>
              <span class="mdi mdi-24px mdi-account align-middle" aria-hidden="true"></span> <span>{{ user.displayName }}</span>
            </template>
            <slot name="userItems"></slot>
            <b-dropdown-item @click="logout" :title="$t('cib-header.logout')">{{ $t('cib-header.logout') }}</b-dropdown-item>
          </b-nav-item-dropdown>
        </b-navbar-nav>
      </b-collapse>
    </b-navbar>
  </div>
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

export default {
  name: 'CIBHeaderFlow',
  inject: ['currentLanguage', 'setCurrentLanguage'],
  props: { languages: Array, user: Object },
  emits: ['logout'],
  data() {
    return {
      engines: [],
      selectedEngine: null,
      isCollapsed: false
    }
  },
  computed: {
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
    }
  },
  mounted() {
    this.loadEngines()
    // Close menu when clicking outside on mobile
    document.addEventListener('click', this.handleClickOutside)
  },
  beforeUnmount() {
    document.removeEventListener('click', this.handleClickOutside)
  },
  methods: {
    handleClickOutside(event) {
      // Only handle on mobile when menu is open
      if (!this.isCollapsed) return
      
      // Check if click is on the toggle button or any of its children (like the span icons)
      const toggleButton = this.$el.querySelector('.navbar-toggler')
      if (toggleButton && toggleButton.contains(event.target)) {
        return // Don't close if clicking the toggle button or its children
      }
      
      const navbar = this.$el.querySelector('.navbar')
      if (navbar && !navbar.contains(event.target)) {
        this.closeMenu()
      }
    },
    closeMenu() {
      this.isCollapsed = false
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
        // No cached token, need to logout and re-authenticate
        this.logout()
      }
    }
  }
}
</script>

<style scoped>
/* Ensure hamburger toggle is always on top */
:deep(.navbar-toggler) {
  position: relative;
  z-index: 1050;
}

/* Ensure dropdowns in collapsed menu don't overlap the toggle button */
:deep(.navbar-collapse .dropdown-menu) {
  z-index: 1040;
}

.lang-label {
  min-width: 36px;
  display: inline-block;
  background-color: var(--bs-gray-200) !important;
}
.dropdown-item:hover .lang-label {
  background-color: var(--bs-gray-400) !important;
}
.lang-label-active,
.dropdown-item:hover .lang-label-active {
  background-color: var(--bs-gray-500) !important;
}
</style>
