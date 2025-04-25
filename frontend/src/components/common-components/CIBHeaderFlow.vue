<template>
  <div style="height: 55px"> <!-- Empty container with height of navbar -->
    <b-navbar toggleable="md" fixed="top" type="light" class="border-bottom bg-white px-3">
      <slot></slot>
      <b-navbar-toggle target="nav_collapse"></b-navbar-toggle>
      <b-collapse is-nav id="nav_collapse" class="flex-grow-0">
        <b-navbar-nav>
          <b-nav-item-dropdown extra-toggle-classes="py-1" right :title="$t('cib-header.languages')">
            <template v-slot:button-content>
              <span class="visually-hidden">{{ $t('cib-header.languages') }}</span>
              <span class="mdi mdi-24px mdi-web align-middle"></span>
            </template>
            <b-dropdown-item v-for="lang in languages" :key="lang" :active="lang === currentLanguage()" @click="currentLanguage(lang)" :title="$t('cib-header.languages') + ': ' + $t('cib-header.' + lang)">
              <div class="d-flex align-items-baseline">
                <span class="lang-label text-center text-uppercase text-dark bg-body-secondary rounded me-2">
                  {{ lang }}
                </span>
                <span class="flex-grow-1">
                  {{ $t('cib-header.' + lang) }}
                </span>
              </div>
            </b-dropdown-item>
          </b-nav-item-dropdown>

          <b-nav-item-dropdown v-if="$slots.helpItems" extra-toggle-classes="py-1" right>
            <template v-slot:button-content>
              <span :title="$t('cib-header.helpItems')" class="mdi mdi-24px mdi-help-circle align-middle"></span>
              {{ $t('cib-header.helpItems') }}
            </template>
            <slot name="helpItems"></slot>
          </b-nav-item-dropdown>

          <b-nav-item-dropdown v-if="user" :title="$t('admin.users.account')" extra-toggle-classes="py-1" right>
            <template v-slot:button-content>
              <span class="visually-hidden">{{ $t('admin.users.account') }}</span>
              <span class="mdi mdi-24px mdi-account align-middle"></span> {{ user.displayName }}
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
export default {
  name: 'CIBHeaderFlow',
  inject: ['currentLanguage'],
  props: { languages: Array, user: Object },
  methods: {
    logout: function() {
      sessionStorage.getItem('token') ? sessionStorage.removeItem('token') : localStorage.removeItem('token')
      this.$emit('logout')
    }
  }
}
</script>

<style>
.lang-label {
  min-width: 36px; /* adjust as needed */
  display: inline-block;
}
</style>
