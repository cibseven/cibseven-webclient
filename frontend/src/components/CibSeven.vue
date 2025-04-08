<template>
  <div class="h-100 d-flex flex-column">
    <CIBHeaderFlow v-if="$root.header === 'true'" class="flex-shrink-0" :languages="$root.config.supportedLanguages" :user="$root.user" @logout="logout">
      <div class="me-auto d-flex flex-column flex-md-row" style="height: 38px">
        <b-navbar-brand class="py-0" :title="$t('navigation.home')" to="/">
          <img height="38px" :alt="$t('cib-header.productName')" :src="$root.logoPath"/>
          <span class="d-none d-md-inline align-middle"></span>
        </b-navbar-brand>
        <div v-if="pageTitle" style="max-height: 38px;" class="d-flex align-items-center text-truncate">
          <span class="border-start border-secondary py-3 me-3"></span>
          <h3 style="line-height: normal"
          class="m-0 text-secondary text-truncate">{{ pageTitle }}</h3>
        </div>
      </div>

      <b-button v-if="$root.user && startableProcesses && $route.name === 'tasklist'" class="d-none d-sm-block py-0 me-3" variant="outline-secondary" :title="$t('start.startProcesses')" @click="openStartProcess()">
        <span class="mdi mdi-18px mdi-rocket"><span class="d-none d-lg-inline">{{ $t('start.startProcesses') }}</span></span>
      </b-button>

      <b-collapse v-if="(permissionsTaskList && startableProcesses) || permissionsCockpit" is-nav id="nav_collapse" class="flex-grow-0 d-none d-md-flex">
        <b-navbar-nav>
          <b-nav-item-dropdown extra-toggle-classes="py-1" right :title="$t('navigation.menu')">
            <template v-slot:button-content>
              <span class="visually-hidden">{{ $t('navigation.menu') }}</span>
              <span class="mdi mdi-24px mdi-menu align-middle"></span>
            </template>
            <b-dropdown-item v-if="permissionsTaskList && startableProcesses" to="/seven/auth/start-process" :active="$route.path.includes('seven/auth/start-process')" :title="$t('start.startProcesses')">{{ $t('start.startProcesses') }}</b-dropdown-item>
            <b-dropdown-item v-if="permissionsTaskList" to="/seven/auth/tasks" :active="$route.path.includes('seven/auth/tasks')" :title="$t('start.taskList')">{{ $t('start.taskList') }}</b-dropdown-item>

            <b-dropdown-divider v-if="permissionsTaskList && permissionsCockpit"></b-dropdown-divider>
            <b-dropdown-group v-if="permissionsCockpit" header="{{ $t('start.groupOperations') }}">
              <b-dropdown-item to="/seven/auth/processes" :active="$route.path.includes('seven/auth/process')" :title="$t('start.admin')">{{ $t('process.title') }}</b-dropdown-item>
              <b-dropdown-item to="/seven/auth/decisions" :active="$route.path.includes('seven/auth/decisions')" :title="$t('start.adminDecisions')">{{ $t('start.adminDecisions') }}</b-dropdown-item>
              <b-dropdown-item to="/seven/auth/human-tasks" :active="$route.path.includes('seven/auth/human-tasks')" :title="$t('start.adminHumanTasks')">{{ $t('start.adminHumanTasks') }}</b-dropdown-item>
              <b-dropdown-item to="/seven/auth/deployments" :active="$route.path.includes('seven/auth/deployments')" :title="$t('deployment.title')">{{ $t('deployment.title') }}</b-dropdown-item>
              <b-dropdown-item to="/seven/auth/batches" :active="$route.path.includes('seven/auth/batches')" :title="$t('batches.tooltip')">{{ $t('batches.title') }}</b-dropdown-item>
            </b-dropdown-group>

            <b-dropdown-divider v-if="permissionsUsers && (permissionsTaskList || permissionsCockpit)"></b-dropdown-divider>
            <b-dropdown-group v-if="permissionsUsers" header="{{ $t('start.groupAdministration') }}">
              <b-dropdown-item to="/seven/auth/admin/users" :active="$route.path.includes('seven/auth/admin/users')" :title="$t('admin.users.title')">{{ $t('admin.users.title') }}</b-dropdown-item>
              <b-dropdown-item to="/seven/auth/admin/groups" :active="$route.path.includes('seven/auth/admin/groups')" :title="$t('admin.groups.title')">{{ $t('admin.groups.title') }}</b-dropdown-item>
              <b-dropdown-item to="/seven/auth/admin/tenants" :active="$route.path.includes('seven/auth/admin/tenants')" :title="$t('admin.tenants.tooltip')">{{ $t('admin.tenants.title') }}</b-dropdown-item>
              <b-dropdown-item to="/seven/auth/admin/authorizations" :active="$route.path.includes('seven/auth/admin/authorizations')" :title="$t('admin.authorizations.title')">{{ $t('admin.authorizations.title') }}</b-dropdown-item>
              <b-dropdown-item to="/seven/auth/admin/system" :active="$route.path.includes('seven/auth/admin/system')" :title="$t('admin.system.tooltip')">{{ $t('admin.system.title') }}</b-dropdown-item>
            </b-dropdown-group>

            <b-dropdown-divider v-if="permissionsCockpit"></b-dropdown-divider>
            <b-dropdown-item v-if="permissionsCockpit" :href="$root.config.cockpitUrl" :title="$t('start.cockpit')" target="_blank">{{ $t('start.cockpit') }}</b-dropdown-item>
          </b-nav-item-dropdown>
        </b-navbar-nav>
      </b-collapse>

      <div>
        <b-button v-if="$root.config.layout.showFeedbackButton" variant="outline-secondary" @click="$refs.report.show()" class="border-0 py-0 d-none d-md-flex" :title="$t('seven.feedback')">
          <span class="mdi mdi-24px mdi-message-alert"></span>
        </b-button>
      </div>

      <b-navbar-nav>
        <b-nav-item-dropdown extra-toggle-classes="py-1" right :title="$t('navigation.infoAndHelp')">
          <template v-slot:button-content>
            <span class="visually-hidden">{{ $t('navigation.infoAndHelp') }}</span>
            <span class="mdi mdi-24px mdi-help-circle align-middle"></span>
          </template>
          <b-dropdown-item v-if="$root.config.flowLinkHelp != ''" :href="$root.config.flowLinkHelp" :title="$t('infoAndHelp.flowLinkHelp')" target="_blank">{{ $t('infoAndHelp.flowLinkHelp') }}</b-dropdown-item>
          <b-dropdown-item v-if="$root.config.flowLinkAccessibility != ''" :href="$root.config.flowLinkAccessibility" :title="$t('infoAndHelp.flowLinkAccessibility')" target="_blank">{{ $t('infoAndHelp.flowLinkAccessibility') }}</b-dropdown-item>
          <b-dropdown-item v-if="$root.config.flowLinkTerms != ''" :href="$root.config.flowLinkTerms" :title="$t('infoAndHelp.flowLinkTerms')" target="_blank">{{ $t('infoAndHelp.flowLinkTerms') }}</b-dropdown-item>
          <b-dropdown-item v-if="$root.config.flowLinkPrivacy != ''" :href="$root.config.flowLinkPrivacy" :title="$t('infoAndHelp.flowLinkPrivacy')" target="_blank">{{ $t('infoAndHelp.flowLinkPrivacy') }}</b-dropdown-item>
          <b-dropdown-item v-if="$root.config.flowLinkImprint != ''" :href="$root.config.flowLinkImprint" :title="$t('infoAndHelp.flowLinkImprint')" target="_blank">{{ $t('infoAndHelp.flowLinkImprint') }}</b-dropdown-item>
          <b-dropdown-item-button v-if="$root.config.layout.showSupportInfo" :title="$t('infoAndHelp.flowModalSupport.modalText')" @click="$refs.support.show()">{{ $t('infoAndHelp.flowModalSupport.modalText') }}</b-dropdown-item-button>
          <b-dropdown-item-button @click="$refs.about.show()" :title="$t('infoAndHelp.flowModalAbout.modalText')">{{ $t('infoAndHelp.flowModalAbout.modalText') }}</b-dropdown-item-button>
        </b-nav-item-dropdown>
      </b-navbar-nav>

      <template v-slot:userItems>
        <b-dropdown-item v-if="$root.user && $root.config.layout.showUserSettings && !applicationPermissionsDenied($root.config.permissions.userProfile, 'userProfile')"
          :to="'/seven/auth/account/' + $root.user.id" :title="$t('seven.account')">{{ $t('seven.account') }}</b-dropdown-item>
      </template>
    </CIBHeaderFlow>

    <router-view class="flex-grow-1 overflow-hidden" ref="down"></router-view>

    <b-modal ref="ieNotification" :title="$t('seven.titleInfo')">
      <div class="container-fluid">
        <div class="row">
          {{ $t('ienotify.text') }}
        </div>
      </div>
      <template v-slot:modal-footer>
        <div class="row w-100 me-0">
          <div class="col col-8 p-0 pt-1"><b-form-checkbox v-model="rememberNotShow">{{$t('ienotify.remember')}}</b-form-checkbox></div>
          <div class="col col-4"><b-button variant="primary" @click="doNotShowIeNotification();$refs.ieNotification.hide()" class="float-right">{{ $t('start.close') }}</b-button></div>
        </div>
      </template>
    </b-modal>

    <SupportModal ref="support" v-if="$root.config.layout.showSupportInfo"></SupportModal>
    <AboutModal ref="about"></AboutModal>
    <FeedbackModal ref="report" url="feedback" :email="$root.user && $root.user.email" @report="$refs.down.$emit('report', $event)"></FeedbackModal>

    <GlobalEvents v-if="permissionsTaskList" @keydown.ctrl.left.prevent="$router.push('/seven/auth/start-process')"></GlobalEvents>
    <GlobalEvents v-if="permissionsCockpit" @keydown.ctrl.right.prevent="$router.push('/seven/auth/processes/list')"></GlobalEvents>
    <GlobalEvents v-if="permissionsTaskList" @keydown.ctrl.down.prevent="$router.push('/seven/auth/tasks')"></GlobalEvents>

  </div>
</template>

<script>
import platform from 'platform'
import { permissionsMixin } from '@/permissions.js'
import AboutModal from '@/components/modals/AboutModal.vue'
import SupportModal from '@/components/modals/SupportModal.vue'
import CIBHeaderFlow from '@/components/common-components/CIBHeaderFlow.vue'
import FeedbackModal from '@/components/modals/FeedbackModal.vue'
import { updateAppTitle } from '@/utils/init'

export default {
  name: 'CibSeven',
  components: { AboutModal, SupportModal, CIBHeaderFlow, FeedbackModal },
  mixins: [permissionsMixin],
  inject: ['isMobile'],
  data: function() {
    return {
       rememberNotShow: false
    }
  },
  watch: {
    // when the title of the view inside top toolbar is changed
    // => let's change title of the whole web-page in browser
    pageTitle: function(title) {
      this.refreshAppTitle(title)
    }
  },
  computed: {
    startableProcesses: function() {
      return this.processesFiltered.find(p => { return p.startableInTasklist })
    },
    processesFiltered: function() {
      if (!this.$store.state.process.list) return []
      return this.$store.state.process.list.filter(process => {
        return ((!process.revoked))
      }).sort((objA, objB) => {
        var nameA = objA.name ? objA.name.toUpperCase() : objA.name
        var nameB = objB.name ? objB.name.toUpperCase() : objB.name
        var comp = nameA < nameB ? -1 : nameA > nameB ? 1 : 0

        if (this.$root.config.subProcessFolder) {
          if (objA.resource.indexOf(this.$root.config.subProcessFolder) > -1) comp = 1
          else if (objB.resource.indexOf(this.$root.config.subProcessFolder) > -1) comp = -1
        }
        return comp
      })
    },
    // when route is changed => let's change title of the view inside top toolbar
    pageTitle: function() {
      switch (this.$route.name) {
        case 'login': return this.$t('login.login')
        case 'tasklist': return this.$t('start.taskList')
        case 'deployments': return this.$t('deployment.title')
        case 'start-process': return this.$t('start.startProcesses')
        case 'processManagement':
        case 'process': return this.$t('start.admin')
        case 'batches': return this.$t('batches.title')
        case 'decision-version':
        case 'decision-instance':
        case 'decision-list': return this.$t('start.adminDecisions')
        case 'human-tasks': return this.$t('start.adminHumanTasks')
        case 'usersManagement': return this.$t('start.groupAdministration')
        case 'adminUser':
        case 'adminUsers':
        case 'createUser':
          return this.$t('admin.users.title')
        case 'adminGroup':
        case 'adminGroups':
        case 'createGroup':
          return this.$t('admin.groups.title')
        case 'authorizations':
        case 'authorizationType':
          return this.$t('admin.authorizations.title')
        case 'adminTenants': return this.$t('admin.tenants.title')
        case 'adminSystem': return this.$t('admin.system.title')
        default: return ''
      }
    },
    permissionsTaskList: function() {
      return this.$root.user && this.applicationPermissions(this.$root.config.permissions.tasklist, 'tasklist')
    },
    permissionsCockpit: function() {
      return this.$root.user && this.applicationPermissions(this.$root.config.permissions.tasklist, 'cockpit')
    },
    permissionsUsers: function() {
      return this.$root.user && this.hasAdminManagementPermissions(this.$root.config.permissions)
    },
    isUsersManagementActive: function() {
      return this.$route.path.includes('seven/auth/admin/user') ||
        this.$route.path.includes('seven/auth/admin/group') ||
        this.$route.path.includes('seven/auth/admin/authorizations')
    }
  },
  mounted: function () {
    if (platform.name === 'IE') {
      var isNotifiedUser = localStorage.getItem('ienotify')
      if (!isNotifiedUser) this.$refs.ieNotification.show() //must notify the user
    }
    this.refreshAppTitle(this.pageTitle)
  },
  methods: {
    logout: function() {
      this.$router.push('/')
      location.reload() //refresh to empty vuex and axios defaults
      //Remove some storage variables when logout
      //https://helpdesk.cib.de/browse/BPM4CIB-3691
      localStorage.removeItem('accessToken')
      localStorage.removeItem('tokenModeler')
      sessionStorage.removeItem('accessToken')
      sessionStorage.removeItem('tokenModeler')
    },
    openStartProcess: function() {
      this.$eventBus.emit('openStartProcess')
    },
    doNotShowIeNotification: function() { if (this.rememberNotShow) localStorage.setItem('ienotify', true) },
    // change title of the whole web-page in browser
    refreshAppTitle: function (title) {
      switch (this.$route.name) {
        case 'adminUser':
        case 'adminUsers':
        case 'adminGroup':
        case 'adminGroups':
        case 'authorizations':
        case 'authorizationType':
          // "CIB seven | Users Management | <view>"
          updateAppTitle(
            this.$root.config.productNamePageTitle,
            this.$t('start.adminPanel'),
            title
          )
          break
        default:
          // "CIB seven | <view>"
          updateAppTitle(
            this.$root.config.productNamePageTitle,
            title
          )
          break
      }
    }
  }
}
</script>

<style lang="css" scoped>
/* Customizing the separator to reduce gap */
.dropdown-divider {
  margin-top: 0.15rem; /* Reduce top gap */
  margin-bottom: 0.15rem; /* Reduce bottom gap */
}
</style>
