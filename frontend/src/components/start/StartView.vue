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
  singleOptionTile,
  tasksOnlyRedirectTarget,
  permissionFlagsFromVm
} from '@/navigation/navGroups.js'

import taskImage from '@/assets/images/start/task.svg'
import managementImage from '@/assets/images/start/management.svg'
import adminImage from '@/assets/images/start/admin.svg'
import modelerImage from '@/assets/images/start/modeler.svg'

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
        admin: adminImage
      }
    }
  },
  computed: {
    productName() {
      return this.$root.config.productNamePageTitle || this.$t('login.productName')
    },
    startableProcesses() {
      return (this.$store.state.process.list || []).some(process => process.startableInTasklist === true && !process.revoked && process.suspended !== 'true')
    },
    navGroups() {
      let groups = buildNavGroups(permissionFlagsFromVm(this))
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
      // CE has no 'data' group of its own — EE (Ins7ght) / Flow (dataflow) inject
      // the whole group via NavGroupsExtender, so this is empty unless one did.
      return projectStartHoverOptions(this.groupById.data?.items, this.$t.bind(this))
    },
    cockpitOptions() {
      return projectStartHoverOptions(this.groupById.cockpit?.items, this.$t.bind(this))
    },
    adminOptions() {
      return projectStartHoverOptions(this.groupById.admin?.items, this.$t.bind(this))
    },
    // Builder collapses to its one remaining destination instead of linking
    // to an otherwise-empty hub page (e.g. CE/EE's Builder tile becomes
    // "Modeler"). Tasks/Cockpit/Admin keep their hub identity regardless of
    // option count.
    builderTile() {
      const single = singleOptionTile(this.builderOptions)
      return single
        ? { to: single.to, title: single.title, options: null }
        : { to: { name: 'builderHome' }, title: this.$t('start.builder.title'), options: this.builderOptions }
    },
    // Data has no CE-native content at all: it only appears, and only in its
    // single-option collapsed form (e.g. EE's Ins7ght), when EE/Flow inject a
    // 'data' group with exactly one destination. There's no CE hub page/route
    // for it, so — unlike builderTile — this has no multi-option fallback.
    // The injected group supplies its own tile illustration via `tileImage`.
    dataTile() {
      const single = singleOptionTile(this.dataOptions)
      if (!single) return null
      return { to: single.to, title: single.title, src: this.groupById.data?.tileImage, options: null }
    },
    // Single source for what the start page's tile row renders, so adding a
    // tile is one entry here (plus its image import) instead of a separate
    // computed and template block per tile. Tiles are only constructed once
    // their condition holds, so a hidden tile never pays for $t()/title
    // resolution.
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
  methods: {
    updateItems() {
      if (this.$refs.startContainer) {
        this.items = Array.from(this.$refs.startContainer.children)
      }
    },
    // If Tasks is the only tile, the start page has nothing else to show —
    // skip it and go straight into Tasks. The destination (tasklist vs. the
    // tasks hub) depends on startableProcesses, which comes from the process
    // list; decide immediately with whatever's loaded so far (defaulting to
    // "no startable process" until it arrives) rather than block the
    // redirect on that fetch — on a system with a lot of processes it can
    // take a while, and there's no reason to make the user wait for it.
    // Skip entirely when an error dialog needs to explain a prior redirect
    // (e.g. a permission/not-found bounce), so this doesn't navigate the
    // user away before they see it.
    redirectIfTasksOnly() {
      if (this.$route.query.errorType) return
      const target = tasksOnlyRedirectTarget(this.tiles, this.tasksOptions)
      if (target) this.$router.replace(target)
    }
  },
  created() {
    this.redirectIfTasksOnly()
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
