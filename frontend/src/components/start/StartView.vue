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
        <StartViewItem v-if="tiles.includes('tasks')" :to="{ name: 'tasksHome' }" :title="$t('start.tasks.title')" :src="images.task"
          :options="tasksOptions"
        ></StartViewItem>
        <StartViewItem v-if="tiles.includes('cockpit')" :to="{ name: 'cockpit' }" :title="$t('start.cockpit.title')" :src="images.management"
          :options="cockpitOptions"
        ></StartViewItem>
        <StartViewItem v-if="tiles.includes('builder')" :to="{ name: 'builderHome' }" :title="$t('start.builder.title')" :src="images.modeler"
          :options="builderOptions"
        ></StartViewItem>
        <StartViewItem v-if="tiles.includes('data')" :to="{ name: 'dataHome' }" :title="$t('start.data.title')" :src="images.data"
          :options="dataOptions"
        ></StartViewItem>
        <StartViewItem v-if="tiles.includes('admin')" :to="{ name: 'usersManagement' }" :title="$t('start.admin.title')" :src="images.admin"
          :options="adminOptions"
        ></StartViewItem>
      </div>
      <div v-if="tiles.length === 0">
        <img alt="" src="@/assets/images/start/empty_start_page.svg" class="d-block mx-auto mt-5 mb-3" style="max-width: 250px">
        <div class="h5 text-secondary text-center">{{ $t('start.emptyStart', { productName }) }}</div>
      </div>
      <ErrorDialog v-if="$route.query.errorType" ref="errorPopup" variant="warning" />
    </div>
  </div>
</template>

<script>
import { permissionsMixin } from '@/permissions.js'
import navigationPermissionsMixin from '@/mixins/navigationPermissionsMixin.js'
import startTileOptionsMixin from '@/mixins/startTileOptionsMixin.js'
import { ErrorDialog } from '@cib/common-frontend'
import StartViewItem from '@/components/start/StartViewItem.vue'
import {
  buildNavGroups,
  filterVisibleNavGroups,
  projectStartHoverOptions,
  navContextFromVm
} from '@/navigation/navGroups.js'

import taskImage from '@/assets/images/start/task.svg'
import managementImage from '@/assets/images/start/management.svg'
import adminImage from '@/assets/images/start/admin.svg'
import modelerImage from '@/assets/images/start/modeler.svg'
import dataImage from '@/assets/images/start/data.svg'

export default {
  name: 'StartView',
  components: { ErrorDialog, StartViewItem },
  mixins: [permissionsMixin, navigationPermissionsMixin, startTileOptionsMixin],
  data() {
    return {
      items: [],
      mutationObserver: null,
      images: {
        task: taskImage,
        modeler: modelerImage,
        management: managementImage,
        admin: adminImage,
        data: dataImage
      }
    }
  },
  computed: {
    productName() {
      return this.$root.config.productNamePageTitle || this.$t('login.productName')
    },
    startableProcesses() {
      if (!this.$store.state.process.list) return false
      return this.$store.state.process.list.some(process => !process.revoked && process.startableInTasklist)
    },
    navGroups() {
      let groups = buildNavGroups(navContextFromVm(this))
      const extender = this.$options.components?.NavGroupsExtender
      const methods = extender?.methods || extender?.__vccOpts?.methods
      if (methods?.extend) {
        groups = methods.extend.call(this, groups)
      }
      return filterVisibleNavGroups(groups)
    },
    groupById() {
      return Object.fromEntries(this.navGroups.map(g => [g.id, g]))
    },
    builtInTasksOptions() {
      return projectStartHoverOptions(this.groupById.tasks?.items, this.$t.bind(this))
    },
    builtInBuilderOptions() {
      return projectStartHoverOptions(this.groupById.builder?.items, this.$t.bind(this))
    },
    tasksOptions() {
      return this.mergeOptions(this.builtInTasksOptions, 'TasksTileOptionsPlugin')
    },
    builderOptions() {
      return this.mergeOptions(this.builtInBuilderOptions, 'BuilderTileOptionsPlugin')
    },
    dataOptions() {
      // IA from catalog/extender only; DataTileOptionsPlugin supplies hub illustrations on DataStartView
      return projectStartHoverOptions(this.groupById.data?.items, this.$t.bind(this))
    },
    cockpitOptions() {
      return projectStartHoverOptions(this.groupById.cockpit?.items, this.$t.bind(this))
    },
    adminOptions() {
      return projectStartHoverOptions(this.groupById.admin?.items, this.$t.bind(this))
    },
    tiles() {
      const tiles = []
      if (this.tasksOptions.length > 0) tiles.push('tasks')
      if (this.groupById.cockpit) tiles.push('cockpit')
      if (this.builderOptions.length > 0) tiles.push('builder')
      if (this.dataOptions.length > 0) tiles.push('data')
      if (this.groupById.admin) tiles.push('admin')
      return tiles
    },
    countStartItems() {
      return this.items.length
    }
  },
  methods: {
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
