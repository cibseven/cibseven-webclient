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
            <p class="text-center my-4">{{ $t('login.loggedOutText') }}</p>
            <button type="button" class="btn btn-primary btn-block w-100" @click="loginAgain">{{ $t('login.loggedOutAgain') }}</button>
          </div>
        </div>
      </div>
    </div>
    <footer class="fixed-bottom text-center text-muted">CIB seven &copy; CIB, {{ new Date().getFullYear() }}</footer>
  </div>
</template>

<script>
import { LOGGED_OUT_KEY } from '@/constants.js'

// Shown when the identity provider sent the browser back after the logout. Unlike the
// plain app start it does not log in again by itself: the identity provider's session
// is gone, so the user decides when to sign in.
export default {
  name: 'LoggedOutView',
  computed: {
    productName() {
      return this.$root.config.productNamePageTitle || this.$t('login.productName')
    },
  },
  methods: {
    loginAgain: function() {
      sessionStorage.removeItem(LOGGED_OUT_KEY)
      this.$router.push({ name: 'login' })
    }
  }
}
</script>
