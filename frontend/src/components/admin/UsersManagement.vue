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
  <div :style="{ 'height': 'calc(100% - 55px)' }" class="d-flex flex-column">
    <div class="h-100 container-fluid overflow-auto bg-light">
      <div v-if="items.length > 0" class="row justify-content-center">
        <div v-for="item in items" :key="item.title"
          class="col-4 mt-5 mx-3 bg-white" style="max-width:330px; height:250px">
          <div class="row border rounded shadow-sm h-100">
            <div class="align-top" style="flex:auto">
              <div class="text-truncate ps-1"></div>
              <router-link :to="item.link" class="h-100 text-decoration-none text-reset">
                <div class="container">
                  <div class="row ps-3" style="height:55px">
                    <div class="col-12 align-items-center d-flex">
                      <span class="border-start h-100 me-3 border-primary" style="border-width: 3px !important"></span>
                      <h4 class="m-0">{{ $t(item.title) }}</h4>
                    </div>
                  </div>
                  <div class="row text-center">
                    <div class="col-12 p-0 pt-1">
                      <img v-if="item.image" :alt="$t(item.title)" :src="item.image" style="height:180px">
                    </div>
                  </div>
                </div>
              </router-link>
            </div>
          </div>
        </div>
      </div>
      <div v-else>
        <img src="@/assets/images/start/empty_start_page.svg" class="d-block mx-auto mt-5 mb-3" style="max-width: 250px">
        <div class="h5 text-secondary text-center">{{ $t('start.emptyStart') }}</div>
      </div>
    </div>
  </div>
</template>

<script>
import { permissionsMixin } from '@/permissions.js'
export default {
  name: 'UsersManagement',
  mixins: [permissionsMixin],
  computed: {
    items: function() {
      const rawItems = [
        {
          title: 'admin.users.title',
          image: '@/assets/images/admin/users_admin.svg',
          link: '/seven/auth/admin/users',
          hasAccess: this.adminManagementPermissions(this.$root.config.permissions.usersManagement, 'user')
        },
        {
          title: 'admin.groups.title',
          image: '@/assets/images/admin/groups_admin.svg',
          link: '/seven/auth/admin/groups',
          hasAccess: this.adminManagementPermissions(this.$root.config.permissions.groupsManagement, 'group')
        },
        {
          title: 'admin.tenants.title',
          image: '@/assets/images/admin/tenants_admin.svg',
          link: '/seven/auth/admin/tenants',
          hasAccess: this.adminManagementPermissions(this.$root.config.permissions.tenantsManagement, 'tenant')
        },
        {
          title: 'admin.authorizations.title',
          image: '@/assets/images/admin/authorizations_admin.svg',
          link: '/seven/auth/admin/authorizations',
          hasAccess: this.adminManagementPermissions(this.$root.config.permissions.authorizationsManagement, 'authorization')
        },
        {
          title: 'admin.system.title',
          image: '@/assets/images/admin/system_admin.svg',
          link: '/seven/auth/admin/system',
          hasAccess: this.adminManagementPermissions(this.$root.config.permissions.systemManagement, 'system')
        },
      ]

      return rawItems.filter(item => item.hasAccess)
    },
  }
}
</script>
