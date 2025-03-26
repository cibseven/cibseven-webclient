import '@mdi/font/css/materialdesignicons.css'
import './assets/main.css'
import appConfig from './appConfig.js'
import { axios } from './globals.js'

import { createApp } from 'vue'

import store from './store'
import router from './router.js'
import registerOwnComponents from './register.js'
import { permissionsMixin }  from './permissions.js'

import { InfoService, AuthService } from './services.js'
import { getTheme, hasHeader, isMobile, checkExternalReturn } from './utils/init'
import { i18n, switchLanguage } from './i18n'

// check for token inside hash
// if it exists => redirect to new uri
checkExternalReturn(window.location.href, window.location.hash)

Promise.all([
  axios.get('config.json'),
  InfoService.getProperties()
]).then(responses => {
  Object.assign(responses[0].data, responses[1].data)
  var config = responses[0].data

  appConfig.servicesBasePath = config.servicesBasePath

  // (Optional) check if possible
  //axios.defaults.baseURL = appConfig.adminBasePath

  // Load personalized-css
  var logoPath = ''
  var loginImgPath = ''
  var resetPasswordImgPath = ''
  function loadTheme() {

    var css = document.createElement('Link')
    css.setAttribute('rel', 'stylesheet')
    css.setAttribute('type', 'text/css')
    css.setAttribute('href', 'themes/' + getTheme(config) + '/styles.css')

    var favicon = document.createElement('Link')
    favicon.setAttribute('rel', 'icon')
    favicon.setAttribute('type', 'image/x-icon')
    favicon.setAttribute('href', 'themes/' + getTheme(config) + '/favicon.ico')

    document.head.appendChild(css)
    document.head.appendChild(favicon)

    logoPath = 'themes/' + getTheme(config) + '/logo.svg'
    loginImgPath = 'themes/' + getTheme(config) + '/login-image.svg'
    resetPasswordImgPath = 'webjars/seven/components/password/reset-password.svg'
  }

  loadTheme()

  switchLanguage(config, i18n.global.locale).then(() => {

    const app = createApp({ /*jshint nonew:false */
      el: '#app',
      mixins: [permissionsMixin],
      provide: function() {
        return {
          currentLanguage(lang) {
            // get language
            if (!lang) return i18n.global.locale
            // set language
            return switchLanguage(config, lang).then(() => {
              return i18n.global.locale
            })
          },
          loadProcesses(extraInfo) {
              const method = extraInfo ? 'findProcessesWithInfo' : 'findProcesses'
              return this.$store.dispatch(method).then((result) => {
                  const processes = this.processesByPermissions(config.permissions.startProcess, result)
                  processes.forEach((process) => {
                      process.loading = false
                  })
                  this.$store.commit('setProcesses', { processes })
              })
          },
          loadDecisions() {
            const method = 'getDecisionList'
            return this.$store.dispatch(method).then((result) => {
                const decisions = result
                decisions.forEach(decision => decision.loading = false)
                this.$store.commit('setDecisions', { decisions })
            })
          },
          isMobile: isMobile,
          AuthService: AuthService
        }
      },
      data: function() {
        return {
          user: null,
          config: config,
          consent: localStorage.getItem('consent'),
          logoPath: logoPath,
          loginImgPath: loginImgPath,
          resetPasswordImgPath: resetPasswordImgPath,
          theme: getTheme(config),
          header: hasHeader()
        }
      },
      watch: {
        user: function(user) {
          if (user) this.handleTaskWorker()
        }
      },
      mounted: function() {
        if ('Notification' in window && this.config.notifications.tasks.enabled &&
          (Notification.permission !== 'granted' || Notification.permission !== 'denied')) {
          Notification.requestPermission()
        }
      },
      methods: {
        remember: function() { localStorage.setItem('consent', true) },
        sendReport: function(data) { axios.post('report', data) },
        handleTaskWorker: function() {
          if (window.Worker && localStorage.getItem('tasksCheckNotificationsDisabled') !== 'true' &&
            this.$root.config.notifications.tasks.enabled && Notification.permission === 'granted') {
              const taskWorker = new Worker('./task-worker.js')
            const authToken = sessionStorage.getItem('token') || localStorage.getItem('token')
              taskWorker.postMessage({ type: 'setup', interval: this.$root.config.notifications.tasks.interval,
              authToken: authToken, userId: this.$root.user.id })
            taskWorker.postMessage({ type: 'checkNewTasks' })
              taskWorker.addEventListener('message', event => {
              if (event.data && event.data.type === 'sendNotification') {
                var icon = 'themes/' + getTheme(config) + '/notification-icon.svg'
                const options = {
                  body: this.$t('notification.newTasks'),
                  tag: 'cib-flow-check-new-tasks',
                  icon: icon
                }
                const notification = new Notification(this.$t('notification.newTasksTitle'), options)
                notification.onclick = () => {
                  notification.close()
                  window.focus()
                }
              }
            })
          }
        }
      }
    })

    registerOwnComponents(app)

    app.use(router)
    app.use(store)
    app.use(i18n)
    const root = app.mount('#app')
    router.setRoot(root)

    function handler(error) {
      if (error.response) {
        var res = error.response
        if (res.status === 401) { // Unauthorized
          if (res.data && res.data.type === 'TokenExpiredException' && res.data.params && res.data.params.length > 0) {
            console && console.info('Prolonged token')
            if (sessionStorage.getItem('token')) sessionStorage.setItem('token', res.data.params[0])
            else if (localStorage.getItem('token')) localStorage.setItem('token', res.data.params[0])
            axios.defaults.headers.common.authorization = root.user.authToken = res.data.params[0]
            // Repeat last request
            error.config.headers.authorization = res.data.params[0]
            return axios.request(error.config)
          } else {
            console && console.warn('Not authenticated, redirecting ...')
            sessionStorage.getItem('token') ? sessionStorage.removeItem('token') : localStorage.removeItem('token')
            axios.defaults.headers.common.authorization = ''
            root.user = null
            if (router.currentRoute.path !== '/seven/login') {
              let url = '/seven/login'
              if (router.currentRoute.path) url += '?nextUrl=' + router.currentRoute.path
              router.push(url)
            }
            // root.$refs.error.show(res.data || res.statusText)
          }
          return Promise.reject(error)
        } else if (res.status === 500) {
          var exceptions = ['NoObjectFoundException', 'InvalidAttributeValueException', 'SubmitDeniedException',
          'UnsupportedTypeException', 'ExpressionEvaluationException', 'ExistingUserRequestException',
          'ExistingGroupRequestException', 'AccessDeniedException', 'SystemException', 'InvalidUserIdException', 'InvalidValueHistoryTimeToLive']
          if (res.data.type && exceptions.indexOf(res.data.type) !== -1)
            root.$refs.error.show(res.data)
          //root.$refs.report.show(res.data)
          return Promise.reject(error)
        } else {
          //root.$refs.error.show(res.data || res.statusText)
          return Promise.reject(error)
        }
      } else { // for example "Network Error" - doesn't work with spaces in translations.json
        console && console.error('Strange AJAX error', error)
        var message = error.message.replace(' ', '_')
        if (message !== 'Request_aborted') root.$refs.error.show({ type: message })
        return Promise.reject(error)
      }
    }
    axios.interceptors.response.use(res => { return res.data }, handler)

    return config
  })
})
