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
  <StartHubView :items="items"></StartHubView>
</template>

<script>
import { permissionsMixin } from '@/permissions.js'
import navigationPermissionsMixin from '@/mixins/navigationPermissionsMixin.js'
import StartHubView from '@/components/start/StartHubView.vue'
import { getVisibleGroup, accessManagementCatalogItems } from '@/navigation/navGroups.js'

export default {
  name: 'AccessManagement',
  mixins: [permissionsMixin, navigationPermissionsMixin],
  components: { StartHubView },
  emits: [],
  computed: {
    items() {
      // getVisibleGroup filters through the group-level `show` flag too, not
      // just each item's own show flag, so this can't drift from
      // UsersManagement.vue's admin-tile gate.
      const admin = getVisibleGroup(this, 'admin')
      return accessManagementCatalogItems(admin?.items).map(item => ({
        title: this.$t(item.title),
        src: item.tileImage,
        to: { name: item.routeName }
      })).filter(item => item.src && item.to?.name)
    }
  }
}
</script>
