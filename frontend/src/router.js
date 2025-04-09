
import { createRouter, createWebHashHistory } from 'vue-router'

import { axios } from './globals.js'

import { AuthService, ProcessService } from '@/services.js'
import { permissionsMixin } from '@/permissions.js'
import CibSeven from '@/components/CibSeven.vue'
import StartView from '@/components/start/StartView.vue'
import StartProcessView from '@/components/start-process/StartProcessView.vue'
import ProcessView from '@/components/process/ProcessView.vue'
import ProcessListView from '@/components/processes/list/ProcessListView.vue'
import DecisionView from '@/components/decision/DecisionView.vue'
import DecisionListView from '@/components/decisions/list/DecisionListView.vue'
import UsersManagement from '@/components/admin/UsersManagement.vue'
import AdminUsers from '@/components/admin/AdminUsers.vue'
import CreateUser from '@/components/admin/CreateUser.vue'
import ProfileUser from '@/components/admin/ProfileUser.vue'
import AdminGroups from '@/components/admin/AdminGroups.vue'
import ProfileGroup from '@/components/admin/ProfileGroup.vue'
import CreateGroup from '@/components/admin/CreateGroup.vue'
import AdminAuthorizations from '@/components/admin/AdminAuthorizations.vue'
import AdminAuthorizationsTable from '@/components/admin/AdminAuthorizationsTable.vue'
import DeploymentsView from '@/components/deployment/DeploymentsView.vue'
import HumanTasksView from '@/components/task/HumanTasksView.vue'
import TasksView from '@/components/task/TasksView.vue'
import TaskView from '@/components/task/TaskView.vue'
import LoginView from '@/components/login/LoginView.vue'
import { BWaitingBox } from 'cib-common-components'
import DeployedForm from '@/components/forms/DeployedForm.vue'
import StartDeployedForm from '@/components/forms/StartDeployedForm.vue'
import TenantsView from '@/components/tenants/TenantsView.vue'
import BatchesView from '@/components/batches/BatchesView.vue'
import SystemView from './components/system/SystemView.vue'
import SystemSetting from './components/system/SystemSetting.vue'

const router = createRouter({
  history: createWebHashHistory(),
  linkActiveClass: 'active',
  routes: [
    { path: '/', redirect: '/seven/auth/start' },
    { path: '/start-process', name: 'start-process', component: () => {
      return axios.get('webjars/seven/components/process/external-start-process.html').then(function(html) {
        return {
          template: html,
          data: function() {
            return {
              startParamUrl: '',
              selectedProcess: {},
              started: false
            }
          },
          mounted: function() {
            AuthService.createAnonUserToken().then(user => {
              this.$root.user = user
              axios.defaults.headers.common.authorization = user.authToken
              if (this.$route.query.processKey) {
                  ProcessService.findProcessByDefinitionKey(this.$route.query.processKey).then(processLatest => {
                  this.selectedProcess = processLatest
                  ProcessService.startForm(processLatest.id).then(url => {
                    if (url.key) {
                      this.startParamUrl = this.$root.config.uiElementTemplateUrl + '/startform/' +
                        url.key.split('?template=')[1] + '?processDefinitionId=' + processLatest.id +
                        '&processDefinitionKey=' + processLatest.key
                    }
                  })
                })
              }
            })
          },
          methods: {
            taskCompleted: function() {
              this.started = true
            },
            navigateBack: function() {
              history.back()
            }
          }
        }
      })
    }},
    { path: '/seven', component: CibSeven, children: [
      { path: 'login', name: 'login', beforeEnter: function(to, from, next) {
          if (router.root.config.ssoActive) //If SSO go to other login
            location.href = './sso-login.html?nextUrl=' + encodeURIComponent(to.query.nextUrl ? to.query.nextUrl : '')
          else next()
        }, component: LoginView },
      { path: 'auth', beforeEnter: authGuard(true), component: {
        components: { BWaitingBox }, template: '<BWaitingBox ref="loader" class="d-flex justify-content-center" styling="width:20%">\
          <router-view ref="down" class="w-100 h-100"></router-view></BWaitingBox>',
        mixins: [permissionsMixin],
        inject: ['loadProcesses','loadDecisions'],
        mounted: function() {
          this.$refs.loader.done = true
          this.$refs.loader.wait(this.loadProcesses(false))
          this.$refs.loader.wait(this.loadDecisions())
          // Preload the filters to have them in the admin view.
          this.$store.dispatch('findFilters').then(response => {
            this.$store.commit('setFilters',
              { filters: this.filtersByPermissions(this.$root.config.permissions.displayFilter, response) })
          })
        },
      }, children: [
        { path: 'start', name: 'start', component: StartView },
        { path: 'account/:userId', name: 'account', beforeEnter: (to, from, next) => {
            permissionsDeniedGuard('userProfile')(to, from, result => {
                if (result) next(result)
              else {
                    if (to.params.userId && to.params.userId === router.root.user.id &&
                      router.root.config.layout.showUserSettings)next()
                    else next('/seven/auth/start')
                }
              })
          }, component: ProfileUser
        },

        // Start new process (end-user)
        { path: 'start-process', name: 'start-process', beforeEnter: permissionsGuard('tasklist'),
          component: StartProcessView
        },

        // Tasks in active processes
        { path: 'tasks', beforeEnter: permissionsGuard('tasklist'), component: TasksView,
          children: [
            { path: ':filterId/:taskId?', name: 'tasklist', component: TaskView }
          ]
        },

        // Batches
        { path: 'batches', name: 'batches', beforeEnter: permissionsGuard('cockpit'),
          component: BatchesView
        },

        // Process management (power-user)
        { path: 'processes', redirect: '/seven/auth/processes/list', beforeEnter: permissionsGuard('cockpit') },
        { path: 'processes/list', name: 'processManagement', beforeEnter: permissionsGuard('cockpit'),
          component: ProcessListView
        },
        { path: 'process/:processKey/:versionIndex?/:instanceId?', name: 'process', beforeEnter: permissionsGuard('cockpit'),
          component: ProcessView, props: route => ({
            processKey: route.params.processKey,
            versionIndex: route.params.versionIndex,
            instanceId: route.params.instanceId,
          })
        },
        // decisions
        { path: 'decisions', redirect: '/seven/auth/decisions/list', beforeEnter: permissionsGuard('cockpit') },
        { path: 'decisions/list', name: 'decision-list', beforeEnter: permissionsGuard('cockpit'),
          component: DecisionListView
        },
        {
          path: 'decision/:decisionKey',
          component: DecisionView,
          props: true,
          children: [
            {
              path: ':versionIndex',
              name: 'decision-version',
              component: () => import('@/components/decision/DecisionDefinitionVersion.vue'),
              props: true
            },
            {
              path: ':versionIndex/:instanceId',
              name: 'decision-instance',
              component: () => import('@/components/decision/DecisionInstance.vue'),
              props: true
            }
          ]
        },
        { path: 'deployments/:deploymentId?', name: 'deployments', beforeEnter: permissionsGuard('cockpit'),
          component: DeploymentsView
        },
        { path: 'human-tasks', name: 'human-tasks', beforeEnter: permissionsGuard('cockpit'), component: HumanTasksView },
        // users management
        { path: 'admin',
          component: {
            template: '<router-view></router-view>'
          },
          children: [
            { path: '', name: 'usersManagement', component: UsersManagement },
            { path: 'users', name:'adminUsers',
              beforeEnter: permissionsGuardUserAdmin('usersManagement', 'user'), component: AdminUsers },
            { path: 'user/:userId', name: 'adminUser',
              beforeEnter: permissionsGuardUserAdmin('usersManagement', 'user'), component: ProfileUser,
              props: () => ({ editMode: true })
            },
            { path: 'groups', name: 'adminGroups', beforeEnter: permissionsGuardUserAdmin('groupsManagement', 'group'), component: AdminGroups },
            { path: 'group/:groupId', name: 'adminGroup', beforeEnter: permissionsGuardUserAdmin('groupsManagement', 'group'), component: ProfileGroup },
            // Tenants
            { path: 'tenants', name:'adminTenants', component: TenantsView },
            // System
            { path: 'system', redirect: '/seven/auth/admin/system/diagnostics', name: 'adminSystem', component: SystemView,
              children: [
                { path: ':setting', name: 'systemSetting', component: SystemSetting, props: true }
              ]
            },
            // Authorizations
            { path: 'authorizations', name: 'authorizations',
              beforeEnter: permissionsGuardUserAdmin('authorizationsManagement', 'authorization'), component: AdminAuthorizations,
              children: [
                { path: ':resourceTypeId/:resourceTypeKey', name: 'authorizationType', component: AdminAuthorizationsTable }
              ]
            }
          ]
        },
        { path: 'admin/create-user', name: 'createUser', component: CreateUser },
        { path: 'admin/create-group', name: 'createGroup', beforeEnter: permissionsGuard('cockpit'), component: CreateGroup }
      ]}
    ]},
    {
      path: '/deployed-form/:locale/:taskId/:token?/:theme?/:translation?',
      beforeEnter: combineGuards(authGuard(false), permissionsGuard('tasklist')),
      props: true,
      component: DeployedForm
    },
    {
      path: '/start-deployed-form/:locale/:processDefinitionId/:token?/:theme?/:translation?',
      beforeEnter: combineGuards(authGuard(false), permissionsGuard('tasklist')),
      props: true,
      component: StartDeployedForm
    },
  ]
})

function authGuard(strict) {
  return function(to, from, next) {
    console && console.debug('navigation guard', from, to)

    if (router.root.user) next()
    else getSelfInfo()['catch'](error => {
      if (error.response) {
        var res = error.response
        var params = res.data.params && res.data.params.length > 0
        if (res.data && res.data.type === 'TokenExpiredException' && params) {
          console && console.info('Prolonged token')
          if (sessionStorage.getItem('token')) sessionStorage.setItem('token', res.data.params[0])
          else if (localStorage.getItem('token')) localStorage.setItem('token', res.data.params[0])
          getSelfInfo()
        } else {
          console && console.warn('Not authenticated, redirecting ...')
          sessionStorage.getItem('token') ? sessionStorage.removeItem('token') : localStorage.removeItem('token')
          next({ path: strict ? '/seven/login' : undefined, query: { nextUrl: to.fullPath } })
          if ((res.data.type !== 'AuthenticationException' && res.data.type !== 'TokenExpiredException') || params)
            router.root.$refs.error.show(res.data) // When reloading $refs.error is often undefined => init race condition ?
        }
        } else
          console && console.error('Strange AJAX error', error)
    })

    function getSelfInfo() {
      if (to.query.token) sessionStorage.setItem('token', to.query.token)
      var token = sessionStorage.getItem('token') || localStorage.getItem('token')
      var inst = axios.create() // bypass standard error handling
      return inst.get(router.root.config.servicesBasePath + '/auth', { headers: { authorization: token } }).then(res => {
        console && console.info('auth successful', res.data)
        axios.defaults.headers.common.authorization = res.data.authToken
          AuthService.fetchAuths().then(permissions => {
            res.data.permissions = permissions
            router.root.user = res.data
            next()
          })
      })
    }
  }
}
function permissionsGuard(permission) {
  return function(to, from, next) {
    if (router.root.applicationPermissions(router.root.config.permissions[permission], permission)) next()
    else next('/seven/auth/start')
  }
}
function permissionsDeniedGuard(permission) {
  return function(to, from, next) {
    if (!router.root.applicationPermissionsDenied(router.root.config.permissions[permission], permission)) next()
    else next('/seven/auth/start')
  }
}
function permissionsGuardUserAdmin(permission, condition) {
  return function(to, from, next) {
    if (router.root.adminManagementPermissions(router.root.config.permissions[permission], condition)) next()
    else next('/seven/auth/start')
  }
}

function combineGuards(...guards) {
  return function(to, from, next) {
    let index = 0;

    const runGuard = () => {
      if (index < guards.length) {
        const guard = guards[index];
        index++;
        guard(to, from, (result) => {
          if (result === false || result instanceof Error || typeof result === 'string' || result?.path) {
            next(result); // Stop if a guard blocks navigation
          } else {
            runGuard(); // Proceed to the next guard
          }
        });
      } else {
        next(); // All guards passed
      }
    };

    runGuard();
  };
}

router.setRoot = function(value) {
  this.root = value
}

export default router
