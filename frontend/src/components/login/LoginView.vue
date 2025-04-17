<template>
  <div class="container-fluid">
    <div class="row h-100 align-items-center">
      <div class="bg-light h-100 d-none d-md-block col-md-4 p-5">
        <img class="h-100 w-100" :src="$root.loginImgPath">
      </div>
      <div class="px-4 col-md-8 mb-3">
        <div class="row justify-content-center">
          <div class="col-12 col-md-8 col-lg-5">
            <h1 class="text-dark text-center">{{ $t('login.productName') }}</h1>
            <h3 class="text-secondary text-center">{{ $t('login.productSlogan') }}</h3>
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
  methods: {
    onSuccess: function(user) {
      this.AuthService.fetchAuths().then(permissions => {
      user.permissions = permissions
      this.$root.user = user
      this.$route.query.nextUrl ? this.$router.push(this.$route.query.nextUrl) :
      this.$router.push('/seven/auth/start')
      })
    },
    onForgotten: function() {
      this.$router.push('/seven/password-recover')
    }
  }
}
</script>
