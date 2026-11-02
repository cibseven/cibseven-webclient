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
  <div class="container-fluid">
    <div class="row h-100 align-items-center">
      <div class="bg-light h-100 d-none d-md-block col-md-4 p-5">
        <img class="h-100 w-100" :src="$root.loginImgPath" alt="">
      </div>
      <div class="px-4 col-md-8 mb-3">
        <div class="row justify-content-center">
          <div class="col-12 col-md-8 col-lg-5">
            <h1 class="text-dark text-center">{{ productName }}</h1>
            <h2 class="text-secondary text-center h3">{{ $t('login.productSlogan') }}</h2>
            <div v-if="signingOut" class="text-center my-4" role="status">
              <BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('login.loggingOut') }}
            </div>
            <template v-else>
              <p class="text-center my-4">{{ $t('login.loggedOutText') }}</p>
              <button type="button" class="btn btn-primary btn-block w-100" @click="loginAgain">{{ $t('login.loggedOutAgain') }}</button>
            </template>
          </div>
        </div>
      </div>
    </div>
    <AppFooter />
  </div>
</template>

<script>
import { BWaitingBox } from '@cib/common-frontend'
import { AuthService } from '@/services.js'
import { axios } from '@/globals.js'
import { LOGGED_OUT_KEY } from '@/constants.js'
import AppFooter from '../common-components/AppFooter.vue';

// The page of the logout. Reached from inside the application, with the session still loaded, it signs
// the user out while it shows what is going on. Reached any other way (the identity provider sent the
// browser back, a reload, going back in the history) nobody is signed in, and it only says so. Unlike
// the plain app start it does not log in again by itself: the identity provider's session is gone, so
// the user decides when to sign in.
export default {
  name: 'LoggedOutView',
  components: { BWaitingBox, AppFooter },
  data: function() {
    return { signingOut: !!this.$root.user }
  },
  computed: {
    productName() {
      return this.$root.config.productNamePageTitle || this.$t('login.productName')
    },
  },
  mounted: function() {
    if (this.signingOut) this.signOut()
  },
  methods: {
    signOut: async function() {
      // The backend ends the session on its side (e.g. revokes tokens) and may name an external
      // URL to finish the logout at, such as an identity provider. Must run before the cleanup
      // below, which drops the token the request is sent with.
      let logoutRedirectUrl
      try {
        // The external system returns the browser to the app, without hash or query
        const appUrl = new URL('./', window.location.href).href
        logoutRedirectUrl = (await AuthService.logout(appUrl))?.logoutRedirectUrl
      } catch (error) {
        // Never block the local logout on the backend or an external system failing
        console.error('Logout at the backend failed', error)
      }
      // Nothing on this page needs the user, it is like the login page; and a page the browser
      // restores from its cache later is not left looking logged in
      this.$root.user = null
      delete axios.defaults.headers.common.authorization
      //Remove some storage variables when logout
      //https://helpdesk.cib.de/browse/BPM4CIB-3691
      localStorage.removeItem('accessToken')
      localStorage.removeItem('tokenModeler')
      sessionStorage.removeItem('accessToken')
      sessionStorage.removeItem('tokenModeler')
      // Note: engine token cleanup is handled by CIBHeaderFlow.logout()
      if (logoutRedirectUrl) {
        // The external system (e.g. OIDC RP-Initiated Logout) ends its session and sends the browser
        // back. The marker makes the app show this page rather than log in again on its own. The
        // spinner stays until the browser has left.
        sessionStorage.setItem(LOGGED_OUT_KEY, '1')
        window.location.href = logoutRedirectUrl
        return
      }
      // Set the hash before reload: router.push is async and loses the race, so the
      // reload would otherwise land on the current page instead of the start page.
      window.location.hash = '#/'
      window.location.reload() //refresh to empty vuex and axios defaults
    },
    loginAgain: function() {
      sessionStorage.removeItem(LOGGED_OUT_KEY)
      this.$router.push({ name: 'login' })
    }
  }
}
</script>
