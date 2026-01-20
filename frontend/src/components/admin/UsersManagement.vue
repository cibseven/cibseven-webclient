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
        <StartViewItem v-for="item in items" :key="item.title"
          :title="$t(item.title)"
          :src="item.image"
          :to="item.link"
        ></StartViewItem>
      </div>
      <div v-else>
        <img src="@/assets/images/start/empty_start_page.svg" class="d-block mx-auto mt-5 mb-3" style="max-width: 250px" alt="">
        <div class="h5 text-secondary text-center">{{ $t('start.emptyStart') }}</div>
      </div>
    </div>
  </div>
</template>

<script>
import { permissionsMixin } from '@/permissions.js'
import StartViewItem from '@/components/start/StartViewItem.vue'

// Import the images to ensure it is bundled with the package
import adminUsersImage from '@/assets/images/admin/users_admin.svg'
import groupsAdminImage from '@/assets/images/admin/groups_admin.svg'
import tenantsAdminImage from '@/assets/images/admin/tenants_admin.svg'
import authorizationsAdminImage from '@/assets/images/admin/authorizations_admin.svg'
import systemAdminImage from '@/assets/images/admin/system_admin.svg'

export default {
  name: 'UsersManagement',
  mixins: [permissionsMixin],
  components: { StartViewItem },
  computed: {
    items: function() {
      const rawItems = [
        {
          title: 'admin.users.title',
          image: adminUsersImage,
          link: { name: 'adminUsers' },
          hasAccess: this.adminManagementPermissions(this.$root.config.permissions.usersManagement, 'user')
        },
        {
          title: 'admin.groups.title',
          image: groupsAdminImage,
          link: { name: 'adminGroups' },
          hasAccess: this.adminManagementPermissions(this.$root.config.permissions.groupsManagement, 'group')
        },
        {
          title: 'admin.tenants.title',
          image: tenantsAdminImage,
          link: { name: 'adminTenants' },
          hasAccess: this.adminManagementPermissions(this.$root.config.permissions.tenantsManagement, 'tenant')
        },
        {
          title: 'admin.authorizations.title',
          image: authorizationsAdminImage,
          link: { name: 'authorizations' },
          hasAccess: this.adminManagementPermissions(this.$root.config.permissions.authorizationsManagement, 'authorization')
        },
        {
          title: 'admin.system.title',
          image: systemAdminImage,
          link: { name: 'adminSystem' },
          hasAccess: this.adminManagementPermissions(this.$root.config.permissions.systemManagement, 'system')
        },
      ]

      return rawItems.filter(item => item.hasAccess)
    },
  }
}
</script>
