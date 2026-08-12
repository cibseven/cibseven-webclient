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
      <div v-if="hasTiles" class="row justify-content-center">
        <StartViewItem v-if="showAccessManagement"
          :title="$t('start.accessManagement.title')"
          :src="accessManagementImage"
          :to="{ name: 'accessManagement' }"
        ></StartViewItem>
        <StartViewItem v-if="showSystem"
          :title="$t('admin.system.title')"
          :src="systemImage"
          :to="{ name: 'adminSystem' }"
        ></StartViewItem>
      </div>
      <div v-else>
        <img src="@/assets/images/start/empty_start_page.svg" class="d-block mx-auto mt-5 mb-3" style="max-width: 250px" alt="">
        <div class="h5 text-secondary text-center">{{ $t('start.emptyStart', { productName }) }}</div>
      </div>
    </div>
  </div>
</template>

<script>
import { permissionsMixin } from '@/permissions.js'
import navigationPermissionsMixin from '@/mixins/navigationPermissionsMixin.js'
import StartViewItem from '@/components/start/StartViewItem.vue'
import {
  buildNavGroups,
  filterVisibleNavGroups,
  accessManagementCatalogItems,
  navContextFromVm
} from '@/navigation/navGroups.js'

import accessManagementImage from '@/assets/images/start/admin.svg'
import systemAdminImage from '@/assets/images/admin/system_admin.svg'

export default {
  name: 'UsersManagement',
  mixins: [permissionsMixin, navigationPermissionsMixin],
  components: { StartViewItem },
  data() {
    return {
      accessManagementImage,
      systemImage: systemAdminImage
    }
  },
  computed: {
    productName() {
      return this.$root.config.productNamePageTitle || this.$t('login.productName')
    },
    // Reuses the shared nav catalog so admin-tile visibility can't drift from
    // the start page / toolbar (see StartView.vue, AccessManagement.vue).
    adminGroup() {
      return filterVisibleNavGroups(buildNavGroups(navContextFromVm(this))).find(g => g.id === 'admin')
    },
    showAccessManagement() {
      return accessManagementCatalogItems(this.adminGroup?.items).length > 0
    },
    showSystem() {
      return !!this.adminGroup?.items.some(item => item.collapseGroup === 'system')
    },
    hasTiles() {
      return this.showAccessManagement || this.showSystem
    }
  }
}
</script>
