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
    <div class="h-100 container" :style="countStartItems === 4 ? 'max-width: 960px' : ''">
      <div ref="startContainer" class="row justify-content-center">
        <StartViewItem v-if="tiles.includes('startProcess')" :to="{ name: 'start-process' }" :title="$t('start.startProcess.title')" :src="images.process"></StartViewItem>
        <StartViewItem v-if="tiles.includes('tasklist')" :to="{ name: 'tasks' }" :title="$t('start.taskList.title')" :src="images.task"></StartViewItem>
        <StartViewItem v-if="tiles.includes('cockpit')" :to="{ name: 'cockpit' }" :title="$t('start.cockpit.title')" :src="images.management"
          :options="[
            { to: '/seven/auth/processes/list', icon: 'mdi-map-legend', title: $t('start.cockpit.processes.title'), tooltip: $t('start.cockpit.processes.tooltip') },
            { to: '/seven/auth/decisions', icon: 'mdi-wall-sconce-flat-outline', title: $t('start.cockpit.decisions.title'), tooltip: $t('start.cockpit.decisions.tooltip') },
            { to: '/seven/auth/human-tasks', icon: 'mdi-account-file-text-outline', title: $t('start.cockpit.humanTasks.title'), tooltip: $t('start.cockpit.humanTasks.tooltip') },
            { to: '/seven/auth/deployments', icon: 'mdi-upload-box-outline', title: $t('start.cockpit.deployments.title'), tooltip: $t('start.cockpit.deployments.tooltip') },
            { to: '/seven/auth/batches', icon: 'mdi-repeat', title: $t('start.cockpit.batches.title'), tooltip: $t('start.cockpit.batches.tooltip') }
          ]"
        ></StartViewItem>
        <StartViewItem v-if="tiles.includes('admin')" :to="{ name: 'usersManagement' }" :title="$t('start.admin.title')" :src="images.admin"
          :options="[
            { to: '/seven/auth/admin/users', icon: 'mdi-account-search-outline', title: $t('admin.users.title'), tooltip: $t('admin.users.title') },
            { to: '/seven/auth/admin/groups', icon: 'mdi-account-group-outline', title: $t('admin.groups.title'), tooltip: $t('admin.groups.title') },
            { to: '/seven/auth/admin/tenants', icon: 'mdi-domain', title: $t('admin.tenants.title'), tooltip: $t('admin.tenants.tooltip') },
            { to: '/seven/auth/admin/authorizations', icon: 'mdi-account-key-outline', title: $t('admin.authorizations.title'), tooltip: $t('admin.authorizations.title') },
            { to: '/seven/auth/admin/system', icon: 'mdi-cog-outline', title: $t('admin.system.title'), tooltip: $t('admin.system.tooltip') }
          ]"
        ></StartViewItem>
        <component :is="StartViewPlugin" v-if="StartViewPlugin"></component>
      </div>
      <div v-if="!applicationPermissions($root.config.permissions.tasklist, 'tasklist') &&
        !applicationPermissions($root.config.permissions.cockpit, 'cockpit') && !hasAdminManagementPermissions($root.config.permissions)">
        <img alt="" src="@/assets/images/start/empty_start_page.svg" class="d-block mx-auto mt-5 mb-3" style="max-width: 250px">
        <div class="h5 text-secondary text-center">{{ $t('start.emptyStart', { productName }) }}</div>
      </div>
      <ErrorDialog v-if="$route.query.errorType" ref="errorPopup" variant="warning" />
    </div>
  </div>
</template>

<script>
import { permissionsMixin } from '@/permissions.js'
import { ErrorDialog } from '@cib/common-frontend'
import StartViewItem from '@/components/start/StartViewItem.vue'

// images
import processImage from '@/assets/images/start/process.svg'
import taskImage from '@/assets/images/start/task.svg'
import managementImage from '@/assets/images/start/management.svg'
import adminImage from '@/assets/images/start/admin.svg'

export default {
  name: "StartView",
  components: { ErrorDialog, StartViewItem },
  mixins: [permissionsMixin],
  inject: ['loadProcesses'],
  data: function() {
    return {
      showAdminOptions: false,
      showCockpitOptions: false,
      items: [],
      mutationObserver: null,
      images: {
        process: processImage,
        task: taskImage,
        management: managementImage,
        admin: adminImage
      }
    }
  },  
  computed: {
    productName() {
      return this.$root.config.productNamePageTitle || this.$t('login.productName')
    },
    StartViewPlugin: function() {
      return this.$options.components && this.$options.components.StartViewPlugin
        ? this.$options.components.StartViewPlugin
        : null
    },
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
        const nameA = objA.name ? objA.name.toUpperCase() : objA.name
        const nameB = objB.name ? objB.name.toUpperCase() : objB.name
        const compareAMoreB = nameA > nameB ? 1 : 0
        let comp = nameA < nameB ? -1 : compareAMoreB

        if (this.$root.config.subProcessFolder) {
          if (objA.resource.includes(this.$root.config.subProcessFolder)) comp = 1
          else if (objB.resource.includes(this.$root.config.subProcessFolder)) comp = -1
        }
        return comp
      })
    },
    countStartItems: function () {
      return this.items.length
    },
  },
  methods: {
    startProcess: function () {
      this.$refs.startProcess.show()
      this.loadProcesses(false)
    },
    updateItems() {
      if (this.$refs.startContainer) {
        this.items = Array.from(this.$refs.startContainer.children)
      }
    }
  },
  mounted() {
    this.updateItems()
    if (this.$refs.startContainer) {
      this.mutationObserver = new MutationObserver(() => {
        this.updateItems()
      })      
      this.mutationObserver.observe(this.$refs.startContainer, {
        childList: true,
        subtree: false
      })
    }
    if (this.$route.query.errorType) {
      this.$nextTick(() => {
        this.$refs.errorPopup.show({
          type: this.$route.query?.errorType,
          params: this.$route.query,
        })
      })
    }
  },
  beforeUnmount() {
    if (this.mutationObserver) {
      this.mutationObserver.disconnect()
      this.mutationObserver = null
    }
  }
}
</script>
