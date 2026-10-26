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
        <StartViewItem v-for="tile in visibleTiles" :key="tile.key" :to="tile.to" :title="tile.title" :src="tile.src"
          :options="tile.options"
        ></StartViewItem>
      </div>
      <div v-if="tiles.length === 0">
        <img alt="" src="@/assets/images/start/empty_start_page.svg" class="d-block mx-auto mt-5 mb-3" style="max-width: 250px">
        <div class="h5 text-secondary text-center">{{ $t('start.emptyStart', { productName }) }}</div>
      </div>
      <ErrorDialog v-if="$route.query.errorType" ref="errorPopup" variant="warning" />
    </div>
    <AppFooter />
  </div>
</template>

<script>
import { permissionsMixin } from '@/permissions.js'
import navigationPermissionsMixin from '@/mixins/navigationPermissionsMixin.js'
import { ErrorDialog } from '@cib/common-frontend'
import StartViewItem from '@/components/start/StartViewItem.vue'
import {
  buildVisibleNavGroups,
  projectStartHoverOptions,
  singleDestination,
  singleOptionTile,
  tasksOnlyRedirectTarget
} from '@/navigation/navGroups.js'
import { getPluginsSettled } from '@/plugins/pluginsConfig.js'

import taskImage from '@/assets/images/start/task.svg'
import managementImage from '@/assets/images/start/management.svg'
import adminImage from '@/assets/images/start/admin.svg'
import modelerImage from '@/assets/images/start/modeler.svg'
import AppFooter from '@/components/common-components/AppFooter.vue'

export default {
  name: 'StartView',
  components: { ErrorDialog, StartViewItem, AppFooter },
  mixins: [permissionsMixin, navigationPermissionsMixin],
  emits: [],
  data() {
    return {
      items: [],
      mutationObserver: null,
      images: {
        task: taskImage,
        modeler: modelerImage,
        management: managementImage,
        admin: adminImage
      }
    }
  },
  computed: {
    productName() {
      return this.$root.config.productNamePageTitle || this.$t('login.productName')
    },
    pluginsSettled() {
      return getPluginsSettled().value
    },
    navGroups() {
      return buildVisibleNavGroups(this)
    },
    groupById() {
      return Object.fromEntries(this.navGroups.map(g => [g.id, g]))
    },
    // Tiles come from the catalog only, like the navbar: a tile always leads somewhere the user may open
    tasksOptions() {
      return projectStartHoverOptions(this.groupById.tasks?.items, this.$t.bind(this))
    },
    builderOptions() {
      return projectStartHoverOptions(this.groupById.builder?.items, this.$t.bind(this))
    },
    dataOptions() {
      // CE has no 'data' group; EE/Flow add it through registerNavGroupsExtender.
      return projectStartHoverOptions(this.groupById.data?.items, this.$t.bind(this))
    },
    cockpitOptions() {
      return projectStartHoverOptions(this.groupById.cockpit?.items, this.$t.bind(this))
    },
    adminOptions() {
      return projectStartHoverOptions(this.groupById.admin?.items, this.$t.bind(this))
    },
    // Collapses to its one destination (e.g. "Modeler") instead of an empty hub tile, as the navbar does.
    builderTile() {
      const single = singleDestination(this.groupById.builder)
      return single
        ? { to: single.to, title: this.$t(single.title), options: null }
        : { to: { name: 'builderHome' }, title: this.$t('start.builder.title'), options: this.builderOptions }
    },
    // Only when EE/Flow inject a 'data' group. A single tool is the tile itself; with several the
    // tile leads to the group's overview page (startTo, else its default destination) and lists the tools on hover.
    dataTile() {
      const data = this.groupById.data
      if (!data || this.dataOptions.length === 0) return null
      const single = singleOptionTile(this.dataOptions)
      if (single) return { to: single.to, title: single.title, src: data.tileImage, options: null }
      return { to: data.startTo ?? data.defaultTo ?? this.dataOptions[0].to, title: this.$t(data.title), src: data.tileImage, options: this.dataOptions }
    },
    // Single source for the tile row; adding a tile is one entry here.
    visibleTiles() {
      const tiles = []
      if (this.tasksOptions.length > 0) {
        tiles.push({ key: 'tasks', to: { name: 'tasksHome' }, title: this.$t('start.tasks.title'), src: this.images.task, options: this.tasksOptions })
      }
      if (this.groupById.cockpit) {
        tiles.push({ key: 'cockpit', to: { name: 'cockpit' }, title: this.$t('start.cockpit.title'), src: this.images.management, options: this.cockpitOptions })
      }
      if (this.builderOptions.length > 0) {
        tiles.push({ key: 'builder', to: this.builderTile.to, title: this.builderTile.title, src: this.images.modeler, options: this.builderTile.options })
      }
      if (this.dataTile) {
        tiles.push({ key: 'data', to: this.dataTile.to, title: this.dataTile.title, src: this.dataTile.src, options: this.dataTile.options })
      }
      if (this.groupById.admin) {
        tiles.push({ key: 'admin', to: { name: 'usersManagement' }, title: this.$t('start.admin.title'), src: this.images.admin, options: this.adminOptions })
      }
      return tiles
    },
    tiles() {
      return this.visibleTiles.map(tile => tile.key)
    },
    countStartItems() {
      return this.items.length
    }
  },
  watch: {
    // Decided once plugins have loaded: an admin area contributed by a plugin adds a tile of its own
    pluginsSettled: {
      handler(settled) {
        if (settled) this.redirectIfTasksOnly()
      },
      immediate: true
    }
  },
  methods: {
    updateItems() {
      if (this.$refs.startContainer) {
        this.items = Array.from(this.$refs.startContainer.children)
      }
    },
    // If Tasks is the only tile, skip the start page and go straight there.
    // Skip when an error dialog still needs to explain a prior redirect.
    redirectIfTasksOnly() {
      if (this.$route.query.errorType) return
      const target = tasksOnlyRedirectTarget(this.tiles, this.groupById.tasks?.defaultTo)
      if (target) this.$router.replace(target)
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
