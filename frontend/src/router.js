/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import { createRouter, createWebHashHistory } from 'vue-router'

import { axios } from './globals.js'

import { AuthService, SetupService } from '@/services.js'
import { permissionsMixin } from '@/permissions.js'
import CibSeven from '@/components/CibSeven.vue'
import StartView from '@/components/start/StartView.vue'
import StartProcessView from '@/components/start-process/StartProcessView.vue'
import ProcessView from '@/components/process/ProcessView.vue'
import ProcessListView from '@/components/processes/list/ProcessListView.vue'
import ProcessesDashboardView from '@/components/processes/dashboard/ProcessesDashboardView.vue'
import DecisionView from '@/components/decision/DecisionView.vue'
import DecisionListView from '@/components/decisions/list/DecisionListView.vue'
import DecisionInstance from '@/components/decision/DecisionInstance.vue'
import DecisionDefinitionVersion from '@/components/decision/DecisionDefinitionVersion.vue'
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
import InitialSetup from '@/components/setup/InitialSetup.vue'
import { BWaitingBox } from '@cib/common-frontend'
import DeployedForm from '@/components/forms/DeployedForm.vue'
import StartDeployedForm from '@/components/forms/StartDeployedForm.vue'
import TenantsView from '@/components/tenants/TenantsView.vue'
import CreateTenant from '@/components/tenants/CreateTenant.vue'
import EditTenant from '@/components/tenants/EditTenant.vue'
import BatchesView from '@/components/batches/BatchesView.vue'
import SystemView from '@/components/system/SystemView.vue'
import SystemDiagnostics from '@/components/system/SystemDiagnostics.vue'
import ExecutionMetrics from '@/components/system/ExecutionMetrics.vue'
import { TranslationsDownload } from '@cib/common-frontend'
import { redirectToProcessInstance, redirectToTask } from '@/utils/redirects.js'

const appRoutes = [
    { path: '/',  name: 'root', redirect: '/seven/auth/start-configurable' },
    {
      path: '/api/translations',
      name: 'translations',
      component: TranslationsDownload,
    },
    { path: '/seven', name: 'seven', component: CibSeven, children: [
      { path: 'login', name: 'login', beforeEnter: function(to, from, next) {
          // Check if setup is required first
          SetupService.getStatus().then(res => {
            if (res.data) {
              next({ name: 'setup' }) // Redirect to setup if no users exist
            } else if (router.root.config.ssoActive) {
              location.href = './sso-login.html?nextUrl=' + encodeURIComponent(to.query.nextUrl ? to.query.nextUrl : '')
            } else {
              next()
            }
          }).catch(() => {
            // On error, proceed with normal login flow
            if (router.root.config.ssoActive)
              location.href = './sso-login.html?nextUrl=' + encodeURIComponent(to.query.nextUrl ? to.query.nextUrl : '')
            else next()
          })
        }, component: LoginView },
      { path: 'setup', name: 'setup', beforeEnter: setupGuard, component: InitialSetup },
      { path: 'auth', name: 'auth', beforeEnter: authGuard(true), component: {
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
        { path: 'no-permission', name: 'no-permission',
          beforeEnter: async (to, from, next) => {
            next({
              name: 'start',
              query: {
                errorType: 'NoPermission',
                permission: to.query?.permission,
                refPath: to.query?.refPath,
              }
            })
          }
        },

        // Start page with configurable redirects
        { path: 'start-configurable', name: 'start-configurable',
          beforeEnter: (to, from, next) => {
            const cockpitAvailable = router.root.applicationPermissions(router.root.config.permissions['cockpit'], 'cockpit')
            const tasklistAvailable = router.root.applicationPermissions(router.root.config.permissions['tasklist'], 'tasklist')

            const cockpitOverride = cockpitAvailable ? null : {
              name: 'no-permission',
              query: {
                permission: 'cockpit',
                refPath: from.fullPath,
              }
            }
            const tasklistOverride = tasklistAvailable ? null : {
              name: 'no-permission',
              query: {
                permission: 'tasklist',
                refPath: from.fullPath,
              }
            }

            const configuredStartPage = localStorage?.getItem('cibseven:preferences:startPage') || 'start'
            switch (configuredStartPage) {
              case 'processes-dashboard':
                next(cockpitOverride || { name: 'processesDashboard' })
                break;
              case 'decisions-list':
                next(cockpitOverride || { name: 'decision-list' })
                break;
              case 'human-tasks-dashboard':
                next(cockpitOverride || { name: 'human-tasks' })
                break;

              case 'tasks':
                next(tasklistOverride || { name: 'tasks'})
                break;
              case 'start-process':
                next(tasklistOverride || { name: 'start-process' })
                break;
              case 'start':
              default:
                next({ name: 'start' })
                break;
            }
          }
        },

        { path: 'start', name: 'start', component: StartView },
        { path: 'account/:userId', name: 'account', beforeEnter: (to, from, next) => {
            permissionsDeniedGuard('userProfile')(to, from, result => {
              if (result) next(result)
              else {
                if (to.params.userId && to.params.userId === router.root.user.id &&
                  router.root.config.layout.showUserSettings)next()
                else next({
                  name: 'no-permission',
                  query: {
                    permission: 'userProfile',
                    refPath: from.fullPath,
                  }
                })
              }
            })
          }, component: ProfileUser
        },

        // Start new process (end-user)
        { path: 'start-process', name: 'start-process', beforeEnter: permissionsGuard('tasklist'),
          component: StartProcessView
        },

        // Tasks in active processes
        { path: 'task/:taskId', name: 'task-id', beforeEnter: async (to, from, next) => redirectToTask(router, to, from, next) },
        { path: 'tasks', name: 'tasks', beforeEnter: permissionsGuard('tasklist'), component: TasksView,
          children: [
            { path: ':filterId/:taskId?', name: 'tasklist', component: TaskView }
          ]
        },

        // Batches
        { path: 'batches', name: 'batches', beforeEnter: permissionsGuard('cockpit'),
          component: BatchesView
        },

        // Process management (power-user)
        { path: 'processes', name: 'cockpit', redirect: '/seven/auth/processes/dashboard', beforeEnter: permissionsGuard('cockpit') },
        { path: 'processes/dashboard', name: 'processesDashboard', beforeEnter: permissionsGuard('cockpit'),
          component: ProcessesDashboardView
        },
        { path: 'processes/list', name: 'processManagement', beforeEnter: permissionsGuard('cockpit'),
          component: ProcessListView
        },
        // process instance by id redirect
        { path: 'processes/instance/:instanceId?', name: 'process-instance-id',
          beforeEnter: async (to, from, next) => redirectToProcessInstance(router, to, from, next),
        },
        { path: 'processes/not-found-instanceId', name: 'not-found-instanceId',
          beforeEnter: async (to, from, next) => {
            next({
              name: 'start',
              query: {
                errorType: 'notFoundInstanceId',
                instanceId: to.query?.instanceId,
                refPath: to.query?.refPath,
              }
            })
          }
        },
        { path: 'process/:processKey/:versionIndex?/:instanceId?', name: 'process', beforeEnter: permissionsGuard('cockpit'),
          component: ProcessView, props: route => ({
            processKey: route.params.processKey,
            versionIndex: route.params.versionIndex,
            instanceId: route.params.instanceId,
            tenantId: route.query.tenantId
          })
        },
        // decisions
        { path: 'decisions', name: 'decisions', redirect: '/seven/auth/decisions/list', beforeEnter: permissionsGuard('cockpit') },
        { path: 'decisions/list', name: 'decision-list', beforeEnter: permissionsGuard('cockpit'),
          component: DecisionListView
        },
        {
          path: 'decision/:decisionKey',
          name: 'decision',
          beforeEnter: permissionsGuard('cockpit'),
          component: DecisionView,
          props: true,
          children: [
            {
              path: ':versionIndex',
              name: 'decision-version',
              component: DecisionDefinitionVersion,
              props: true
            },
            {
              path: ':versionIndex/:instanceId',
              name: 'decision-instance',
              component: DecisionInstance,
              props: true
            }
          ]
        },
        { path: 'deployments/:deploymentId?', name: 'deployments', beforeEnter: permissionsGuard('cockpit'),
          component: DeploymentsView, props: route => ({
            deploymentId: route.params.deploymentId
          })
        },
        { path: 'human-tasks', name: 'human-tasks', beforeEnter: permissionsGuard('cockpit'), component: HumanTasksView },
        // users management
        { path: 'admin', name: 'admin',
          component: {
            template: '<router-view></router-view>'
          },
          children: [
            { path: '', name: 'usersManagement', component: UsersManagement },
            { path: 'users', name: 'adminUsers',
              beforeEnter: permissionsGuardUserAdmin('usersManagement', 'user'), component: AdminUsers },
            { path: 'user/:userId', name: 'adminUser',
              beforeEnter: permissionsGuardUserAdmin('usersManagement', 'user'), component: ProfileUser,
              props: () => ({ editMode: true })
            },
            { path: 'groups', name: 'adminGroups', beforeEnter: permissionsGuardUserAdmin('groupsManagement', 'group'), component: AdminGroups },
            { path: 'group/:groupId', name: 'adminGroup', beforeEnter: permissionsGuardUserAdmin('groupsManagement', 'group'), component: ProfileGroup },
            // Tenants
            { path: 'tenants', name: 'adminTenants', beforeEnter: permissionsGuardUserAdmin('tenantsManagement', 'tenant'), component: TenantsView },
            { path: 'tenant/:tenantId', name: 'adminTenant', beforeEnter: permissionsGuardUserAdmin('tenantsManagement', 'tenant'), component: EditTenant },
            // System
            { path: 'system', redirect: '/seven/auth/admin/system/system-diagnostics', name: 'adminSystem', component: SystemView,
              beforeEnter: permissionsGuardUserAdmin('systemManagement', 'system'),
              children: [
                { path: 'system-diagnostics', name: 'system-diagnostics', component: SystemDiagnostics },
                { path: 'execution-metrics', name: 'execution-metrics', component: ExecutionMetrics }
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
        { path: 'admin/create-user', name: 'createUser', beforeEnter: permissionsGuardUserAdmin('usersManagement', 'user'), component: CreateUser },
        { path: 'admin/create-group', name: 'createGroup', beforeEnter: permissionsGuardUserAdmin('groupsManagement', 'group'), component: CreateGroup },
        { path: 'admin/create-tenant', name: 'createTenant', beforeEnter: permissionsGuardUserAdmin('tenantsManagement', 'tenant'), component: CreateTenant },
      ]}
    ]},
    {
      path: '/deployed-form/:locale/:taskId/:token?/:theme?/:translation?',
      name: 'deployed-form',
      beforeEnter: combineGuards(authGuard(false), permissionsGuard('tasklist')),
      props: true,
      component: DeployedForm
    },
    {
      path: '/start-deployed-form/:locale/:processDefinitionId/:token?/:theme?/:translation?',
      name: 'start-deployed-form',
      beforeEnter: combineGuards(authGuard(false), permissionsGuard('tasklist')),
      props: true,
      component: StartDeployedForm
    },
  ];

let router = null

function authGuard(strict) {
  return function(to, from, next) {
    console && console.debug('navigation guard', from, to)

    if (router.root.user) next()
    else getSelfInfo()['catch'](error => {
      if (error.response) {
        const res = error.response
        const params = res.data.params && res.data.params.length > 0
        if (res.data && res.data.type === 'TokenExpiredException' && params) {
          console && console.info('Prolonged token')
          if (sessionStorage.getItem('token')) sessionStorage.setItem('token', res.data.params[0])
          else if (localStorage.getItem('token')) localStorage.setItem('token', res.data.params[0])
          getSelfInfo()
        } else {
          console && console.warn('Not authenticated, redirecting ...')
          sessionStorage.getItem('token') ? sessionStorage.removeItem('token') : localStorage.removeItem('token')
          // Check if setup is required before redirecting to login
          checkSetupRequired().then(requiresSetup => {
            if (requiresSetup) {
              next({ name: 'setup' })
            } else {
              next({ path: strict ? '/seven/login' : undefined, query: { nextUrl: to.fullPath } })
            }
          }).catch(() => {
            next({ path: strict ? '/seven/login' : undefined, query: { nextUrl: to.fullPath } })
          })
          if ((res.data.type !== 'AuthenticationException' && res.data.type !== 'TokenExpiredException') || params)
            router.root.$refs.error.show(res.data) // When reloading $refs.error is often undefined => init race condition ?
        }
        } else
          console && console.error('Strange AJAX error', error)
    })

    function checkSetupRequired() {
      return SetupService.getStatus()
        .then(res => res.data)
    }

    function getSelfInfo() {
      if (to.query.token) sessionStorage.setItem('token', to.query.token)
      const token = sessionStorage.getItem('token') || localStorage.getItem('token')
      const headers = { authorization: token }
      const inst = axios.create() // bypass standard error handling
      return inst.get(router.root.config.servicesBasePath + '/auth', { headers: headers }).then(res => {
        console && console.info('auth successful', res.data)
        axios.defaults.headers.common.authorization = res.data.authToken
          AuthService.fetchAuths().then(permissions => {
            router.root.user = { ...res.data, permissions }
            next()
          })
      })
    }
  }
}

function setupGuard(to, from, next) {
  SetupService.getStatus().then(res => {
    if (res.data) {
      next() // Allow access to setup page
    } else {
      next({ name: 'login' }) // Setup not required, redirect to login
    }
  }).catch(error => {
    console && console.error('Error checking setup status', error)
    next({ name: 'login' }) // On error, redirect to login
  })
}

function permissionsGuard(permission) {
  return function(to, from, next) {
    if (router.root.applicationPermissions(router.root.config.permissions[permission], permission)) next()
    else {
      next({
        name: 'no-permission',
        query: {
          permission: permission,
          refPath: from.fullPath,
        }
      })
    }
  }
}
function permissionsDeniedGuard(permission) {
  return function(to, from, next) {
    if (!router.root.applicationPermissionsDenied(router.root.config.permissions[permission], permission)) next()
    else {
      next({
        name: 'no-permission',
        query: {
          permission: permission,
          refPath: from.fullPath,
        }
      })
    }
  }
}
function permissionsGuardUserAdmin(permission, condition) {
  return function(to, from, next) {
    if (router.root.adminManagementPermissions(router.root.config.permissions[permission], condition)) next()
    else {
      next({
        name: 'no-permission',
        query: {
          permission: permission,
          refPath: from.fullPath,
        }
      })
    }
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

/**
 * Creates and configures the main Vue Router instance for the application.
 * Sets up hash-based navigation, active link class, and assigns the provided routes.
 * Also adds a `setRoot` method to the router instance for storing a reference to the root Vue component.
 *
 * @param {Array} routes - The array of route objects to use for the router.
 * @returns {Router} The configured Vue Router instance.
 */
function createAppRouter(routes) {

  // this method is required to set the root component
  // in order to access the config and user object
  // in the router guards

  router = createRouter({
    history: createWebHashHistory(),
    linkActiveClass: 'active',
    routes: routes
  })

  router.setRoot = function(value) {
    this.root = value
  }

  return router
}

export {
  appRoutes,

  createAppRouter,

  authGuard,
  setupGuard,
  permissionsGuard,
  permissionsDeniedGuard,
  permissionsGuardUserAdmin
}
