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
import startTileOptionsMixin from '@/mixins/startTileOptionsMixin.js'
import StartHubView from '@/components/start/StartHubView.vue'
import { buildNavGroups, filterVisibleNavGroups, projectStartHoverOptions, permissionContextFromVm } from '@/navigation/navGroups.js'
import { hasStartableProcess } from '@/utils/processes.js'

import processImage from '@/assets/images/start/process.svg'
import taskImage from '@/assets/images/start/task.svg'

const TASK_IMAGES = {
  '/seven/auth/tasks': taskImage,
  '/seven/auth/start-process': processImage
}

export default {
  name: 'TasksStartView',
  mixins: [permissionsMixin, navigationPermissionsMixin, startTileOptionsMixin],
  components: { StartHubView },
  emits: [],
  computed: {
    startableProcesses() {
      return hasStartableProcess(this.$store.state.process.list)
    },
    // Reuses navGroups.js's own 'tasks' group instead of re-encoding the same
    // permission/startableProcesses-gated item list here, so this hub page
    // can't silently diverge from the toolbar's Tasks dropdown.
    builtInItems() {
      const groups = filterVisibleNavGroups(buildNavGroups(permissionContextFromVm(this)))
      const tasks = groups.find(g => g.id === 'tasks')
      return projectStartHoverOptions(tasks?.items, this.$t.bind(this))
        .map(item => ({ to: item.to, title: item.title, src: TASK_IMAGES[item.to] }))
    },
    items() {
      return this.mergeOptions(this.builtInItems, 'TasksTileOptionsPlugin')
        .filter(item => item && item.to && item.title && item.src)
    }
  }
}
</script>
