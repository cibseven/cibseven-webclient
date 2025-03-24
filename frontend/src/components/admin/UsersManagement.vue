<template>
  <div :style="{ 'height': 'calc(100% - 55px)' }" class="d-flex flex-column">
    <div class="h-100 container-fluid overflow-auto bg-light">
      <div class="row justify-content-center my-5">
        <div v-if="adminManagementPermissions($root.config.permissions.usersManagement, 'user')"
          class="col-4 mt-5 mx-3 bg-white" style="max-width:330px; height:250px">
          <div class="row border rounded shadow-sm h-100">
            <div class="align-top" style="flex:auto">
              <div class="text-truncate ps-1"></div>
              <router-link to="/seven/auth/admin/users" class="h-100 text-decoration-none text-reset">
                <div class="container">
                  <div class="row ps-3" style="height:55px">
                    <div class="col-12 align-items-center d-flex">
                      <span class="border-start h-100 me-3 border-primary" style="border-width: 3px !important"></span>
                      <h4 class="m-0">{{ $t('admin.users.title') }}</h4>
                    </div>
                  </div>
                  <div class="row text-center">
                    <div class="col-12 p-0 pt-1">
                      <img :alt="$t('admin.users.title')" src="/assets/images/admin/users_admin.svg" style="height:180px">
                    </div>
                  </div>
                </div>
              </router-link>
            </div>
          </div>
        </div>
        <div v-if="adminManagementPermissions($root.config.permissions.groupsManagement, 'group')"
          class="col-4 mt-5 mx-3 bg-white" style="max-width:330px; height:250px">
          <div class="row border rounded shadow-sm h-100">
            <div class="align-top" style="flex:auto">
              <div class="text-truncate ps-1"></div>
              <router-link to="/seven/auth/admin/groups" class="h-100 text-decoration-none text-reset">
                <div class="container">
                  <div class="row ps-3" style="height:55px">
                    <div class="col-12 align-items-center d-flex">
                      <span class="border-start h-100 me-3 border-primary" style="border-width: 3px !important"></span>
                      <h4 class="m-0">{{ $t('admin.groups.title') }}</h4>
                    </div>
                  </div>
                  <div class="row text-center">
                    <div class="col-12 p-0 pt-1">
                      <img :alt="$t('admin.groups.title')" src="/assets/images/admin/groups_admin.svg" style="height:180px">
                    </div>
                  </div>
                </div>
              </router-link>
            </div>
          </div>
        </div>
        <div v-if="adminManagementPermissions($root.config.permissions.authorizationsManagement, 'authorization')" class="col-4 mt-5 mx-3 bg-white" style="max-width:330px; height:250px">
          <div class="row border rounded shadow-sm h-100">
            <div class="align-top" style="flex:auto">
              <div class="text-truncate ps-1"></div>
              <router-link to="/seven/auth/admin/authorizations" class="h-100 text-decoration-none text-reset">
                <div class="container">
                  <div class="row ps-3" style="height:55px">
                    <div class="col-12 align-items-center d-flex">
                      <span class="border-start h-100 me-3 border-primary" style="border-width: 3px !important"></span>
                      <h4 class="m-0">{{ $t('admin.authorizations.title') }}</h4>
                    </div>
                  </div>
                  <div class="row text-center">
                    <div class="col-12 p-0 pt-1">
                      <img :alt="$t('admin.authorizations.title')" src="/assets/images/admin/authorizations_admin.svg" style="height:180px">
                    </div>
                  </div>
                </div>
              </router-link>
            </div>
          </div>
        </div>
      </div>
      <div v-if="!hasAdminManagementPermissions($root.config.permissions)">
        <img src="/assets/images/start/empty_start_page.svg" class="d-block mx-auto mt-5 mb-3" style="max-width: 250px">
        <div class="h5 text-secondary text-center">{{ $t('start.emptyStart') }}</div>
      </div>
    </div>
  </div>
</template>

<script>
import { permissionsMixin } from '@/permissions.js'
export default {
  name: 'UsersManagement',
  mixins: [permissionsMixin]
}
</script>
