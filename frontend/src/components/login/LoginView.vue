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
            <h1 class="text-dark text-center">{{ $t('login.productName') }}</h1>
            <h2 class="text-secondary text-center h3">{{ $t('login.productSlogan') }}</h2>
            <LoginForm :credentials="credentials" hide-forgotten
              @success="onSuccess" @forgotten="onForgotten"></LoginForm>
          </div>
        </div>
      </div>
    </div>
    <footer class="fixed-bottom text-center text-muted">CIB seven &copy; CIB, {{ new Date().getFullYear() }}</footer>
  </div>
</template>

<script>
import LoginForm from '@/components/login/LoginForm.vue'

export default {
  name: 'LoginView',
  components: { LoginForm },
  inject: ['AuthService'],
  data: function() {
    return {
      credentials : {
        username: null,
        password: null,
        type: 'org.cibseven.webapp.auth.rest.StandardLogin'
      }
    }
  },
  mounted: function() {
    // if already logged in, redirect to start page
    if (this.$root.user) {
      this.$router.replace({ name: 'start-configurable' })
    }
  },
  methods: {
    onSuccess: function(user) {
      this.AuthService.fetchAuths().then(permissions => {
        user.permissions = permissions
        this.$root.user = user
        if (this.$route.query.nextUrl) {
          this.$router.replace(this.$route.query.nextUrl)
        }
        else {
          this.$router.replace({ name: 'start-configurable' })
        }
      })
    },
    onForgotten: function() {
      this.$router.push('/seven/password-recover')
    }
  }
}
</script>
