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
  <div :style="{ 'height': 'calc(100% - 55px)' }" class="d-flex flex-column bg-light overflow-auto">
    <div class="h-100 container" :style="tiles.length === 4 ? 'max-width: 960px' : ''">
      <div ref="startContainer" class="row justify-content-center">
        <div v-if="tiles.includes('startProcess')"
          class="col-4 mt-5 mx-2 mx-md-3 bg-white rounded" style="max-width: 330px; min-width: 250px; height:250px">
          <div class="row border rounded shadow-sm h-100">
            <div class="align-top" style="flex:auto">
              <div class="text-truncate ps-1"></div>
              <router-link to="/seven/auth/start-process" class="h-100 text-decoration-none text-reset">
                <div class="container">
                  <div class="row ps-3" style="height:55px">
                    <div class="col-12 align-items-center d-flex">
                      <span class="border-start h-100 me-3 border-primary" style="border-width: 3px !important"></span>
                      <h4 class="m-0">{{ $t('start.startProcess.title') }}</h4>
                    </div>
                  </div>
                  <div class="row text-center">
                    <div class="col-12 p-0 pt-1">
                      <img :alt="$t('start.startProcess.title')" src="/src/assets/images/start/process.svg" style="height:180px; max-width:225px">
                    </div>
                  </div>
                </div>
              </router-link>
            </div>
          </div>
        </div>
        <div v-if="tiles.includes('tasklist')"
          class="col-4 mt-5 mx-2 mx-md-3 bg-white rounded" style="max-width: 330px; min-width: 250px; height:250px">
          <div class="row border rounded shadow-sm h-100">
            <div class="align-top" style="flex:auto">
              <div class="text-truncate ps-1"></div>
              <router-link to="/seven/auth/tasks" class="h-100 text-decoration-none text-reset">
                <div class="container">
                  <div class="row ps-3" style="height:55px">
                    <div class="col-12 align-items-center d-flex">
                      <span class="border-start h-100 me-3 border-primary" style="border-width: 3px !important"></span>
                      <h4 class="m-0">{{ $t('start.taskList.title') }}</h4>
                    </div>
                  </div>
                  <div class="row text-center">
                    <div class="col-12 p-0 pt-1">
                      <img :alt="$t('start.taskList.title')" src="/src/assets/images/start/task.svg" style="height:180px; max-width:225px">
                    </div>
                  </div>
                </div>
              </router-link>
            </div>
          </div>
        </div>
        <div v-if="tiles.includes('cockpit')"
          class="col-4 mt-5 mx-2 mx-md-3 bg-white rounded" style="max-width: 330px; min-width: 250px; height:250px" tabindex="0"
          @focus="showCockpitOptions = true" @blur="showCockpitOptions = true" @click="showCockpitOptions = true" @mouseover="showCockpitOptions = true" @mouseleave="showCockpitOptions = false">
          <div class="row border rounded shadow-sm h-100">
            <div class="align-top" style="flex:auto">
              <div class="text-truncate ps-1"></div>
              <router-link to="/seven/auth/processes" class="h-100 text-decoration-none text-reset">
                <div class="container">
                  <div class="row ps-3" style="height:55px">
                    <div class="col-12 align-items-center d-flex">
                      <span class="border-start h-100 me-3 border-primary" style="border-width: 3px !important"></span>
                      <h4 class="m-0">{{ $t('start.cockpit.title') }}</h4>
                    </div>
                  </div>
                  <div class="row text-center">
                    <div class="col-12 p-0 pt-1">
                      <img :alt="$t('start.cockpit.title')" src="/src/assets/images/start/management.svg" style="height:180px; max-width:225px">
                    </div>
                  </div>
                </div>
              </router-link>
            </div>
            <b-overlay :show="showCockpitOptions" :opacity="0" no-center no-wrap>
              <template #overlay>
                <b-list-group class="py-2 bg-white rounded-bottom" style="opacity: .9; position: absolute; bottom: 1px; width: calc(100% - 2px); margin-left: 1px">
                  <b-list-group-item to="/seven/auth/processes/list" class="py-1 px-3 border-start-0 border-top-0 border-end-0 h6 fw-normal mb-0" :title="$t('start.cockpit.processes.tooltip')">
                    <span class="mdi mdi-18px mdi-map-legend pe-1"></span>{{ $t('start.cockpit.processes.title') }}</b-list-group-item>
                  <b-list-group-item to="/seven/auth/decisions" class="py-1 px-3 border-start-0 border-top-0 border-end-0 h6 fw-normal mb-0" :title="$t('start.cockpit.decisions.tooltip')">
                    <span class="mdi mdi-18px mdi-wall-sconce-flat-outline pe-1"></span>{{ $t('start.cockpit.decisions.title') }}</b-list-group-item>
                  <b-list-group-item to="/seven/auth/human-tasks" class="py-1 px-3 border-start-0 border-top-0 border-end-0 h6 fw-normal mb-0" :title="$t('start.cockpit.humanTasks.tooltip')">
                    <span class="mdi mdi-18px mdi-account-file-text-outline pe-1"></span>{{ $t('start.cockpit.humanTasks.title') }}</b-list-group-item>
                  <b-list-group-item to="/seven/auth/deployments" class="py-1 px-3 border-start-0 border-top-0 border-end-0 h6 fw-normal mb-0" :title="$t('start.cockpit.deployments.tooltip')">
                    <span class="mdi mdi-18px mdi-upload-box-outline pe-1"></span>{{ $t('start.cockpit.deployments.title') }}</b-list-group-item>
                  <b-list-group-item to="/seven/auth/batches" class="py-1 px-3 border-0 h6 fw-normal mb-0" :title="$t('start.cockpit.batches.tooltip')">
                    <span class="mdi mdi-18px mdi-repeat pe-1"> </span>{{ $t('start.cockpit.batches.title') }}</b-list-group-item>
                </b-list-group>
              </template>
            </b-overlay>
          </div>
        </div>
        <div v-if="tiles.includes('admin')"
        class="col-4 mt-5 mx-2 mx-md-3 bg-white rounded" style="max-width: 330px; min-width: 250px; height:250px" tabindex="0"
        @focus="showAdminOptions = true" @blur="showAdminOptions = true" @click="showAdminOptions = true" @mouseover="showAdminOptions = true" @mouseleave="showAdminOptions = false">
          <div class="row border rounded shadow-sm h-100">
            <div class="align-top" style="flex:auto">
              <div class="text-truncate ps-1"></div>
              <router-link to="/seven/auth/admin" class="h-100 text-decoration-none text-reset" tabindex="-1">
                <div class="container">
                  <div class="row ps-3" style="height:55px">
                    <div class="col-12 align-items-center d-flex">
                      <span class="border-start h-100 me-3 border-primary" style="border-width: 3px !important"></span>
                      <h4 class="m-0">{{ $t('start.admin.title') }}</h4>
                    </div>
                  </div>
                  <div class="row text-center">
                    <div class="col-12 p-0 pt-1">
                      <img :alt="$t('start.admin.title')" src="/src/assets/images/start/admin.svg" style="height:180px; max-width:225px">
                    </div>
                  </div>
                </div>
              </router-link>
            </div>
            <b-overlay :show="showAdminOptions" :opacity="0" no-center no-wrap>
              <template #overlay>
                <b-list-group class="py-2 bg-white rounded-bottom" style="opacity: .9; position: absolute; bottom: 1px; width: calc(100% - 2px); margin-left: 1px">
                  <b-list-group-item v-if="adminManagementPermissions($root.config.permissions.usersManagement, 'user')"
                    to="/seven/auth/admin/users" class="py-1 px-3 border-start-0 border-top-0 border-end-0 h6 fw-normal mb-0" :title="$t('admin.users.title')">
                    <span class="mdi mdi-18px mdi-account-search-outline pe-1"></span>{{ $t('admin.users.title') }}</b-list-group-item>
                  <b-list-group-item v-if="adminManagementPermissions($root.config.permissions.groupsManagement, 'group')"
                    to="/seven/auth/admin/groups" class="py-1 px-3 border-start-0 border-top-0 border-end-0 h6 fw-normal mb-0" :title="$t('admin.groups.title')">
                    <span class="mdi mdi-18px mdi-account-group-outline pe-1"></span>{{ $t('admin.groups.title') }}</b-list-group-item>
                  <b-list-group-item v-if="adminManagementPermissions($root.config.permissions.groupsManagement, 'group')"
                    to="/seven/auth/admin/tenants" class="py-1 px-3 border-start-0 border-top-0 border-end-0 h6 fw-normal mb-0" :title="$t('admin.tenants.tooltip')">
                    <span class="mdi mdi-18px mdi-domain pe-1"></span>{{ $t('admin.tenants.title') }}</b-list-group-item>
                  <b-list-group-item v-if="adminManagementPermissions($root.config.permissions.authorizationsManagement, 'authorization')"
                    to="/seven/auth/admin/authorizations" class="py-1 px-3 border-start-0 border-top-0 border-end-0 h6 fw-normal mb-0" :title="$t('admin.authorizations.title')">
                    <span class="mdi mdi-18px mdi-account-key-outline pe-1"></span>{{ $t('admin.authorizations.title') }}</b-list-group-item>
                  <b-list-group-item v-if="adminManagementPermissions($root.config.permissions.systemManagement, 'system')"
                    to="/seven/auth/admin/system" class="py-1 px-3 border-0 h6 fw-normal mb-0" :title="$t('admin.system.tooltip')">
                    <span class="mdi mdi-18px mdi-cog-outline pe-1"></span>{{ $t('admin.system.title') }}</b-list-group-item>
                </b-list-group>
              </template>
            </b-overlay>
          </div>
        </div>
      </div>
      <div v-if="!applicationPermissions($root.config.permissions.tasklist, 'tasklist') &&
        !applicationPermissions($root.config.permissions.cockpit, 'cockpit') && !hasAdminManagementPermissions($root.config.permissions)">
        <img :alt="$t('start.emptyStart')" src="/src/assets/images/start/empty_start_page.svg" class="d-block mx-auto mt-5 mb-3" style="max-width: 250px">
        <div class="h5 text-secondary text-center">{{ $t('start.emptyStart') }}</div>
      </div>
    </div>
  </div>
</template>

<script>
import { permissionsMixin } from '@/permissions.js'

export default {
  name: "StartView",
  mixins: [permissionsMixin],
  inject: ['loadProcesses'],
  data: function() {
    return {
      showAdminOptions: false,
      showCockpitOptions: false
    }
  },  
  computed: {
    tiles() {
      const tiles = []
      if (this.applicationPermissions(this.$root.config.permissions.tasklist, 'tasklist') && this.startableProcesses) {
        tiles.push('startProcess')
      }
      if (this.applicationPermissions(this.$root.config.permissions.tasklist, 'tasklist')) {
        tiles.push('tasklist')
      }
      if (this.applicationPermissions(this.$root.config.permissions.cockpit, 'cockpit')) {
        tiles.push('cockpit')
      }
      if (this.hasAdminManagementPermissions(this.$root.config.permissions)) {
        tiles.push('admin')
      }
      return tiles
    },
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
    }
  },
  methods: {
    startProcess: function () {
      this.$refs.startProcess.show()
      this.loadProcesses(false)
    }
  }
}
</script>
