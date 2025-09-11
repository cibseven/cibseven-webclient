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
  <b-modal ref="shortcuts" :title="$t('infoAndHelp.shortcuts.title')" :ok-only="true" size="lg">
    <div class="row">
      <div class="col-2 d-flex justify-content-center">
        <span class="mdi mdi-48px mdi-keyboard-outline pe-1 text-info"></span>
      </div>
      <div class="col-10 ps-0">
        <h5 v-if="itemsGlobal.length > 0">{{ $t('infoAndHelp.flowModalAbout.title') }}</h5>
        <ShortcutsTable :items="itemsGlobal"></ShortcutsTable>
        <h5 v-if="itemsTasks.length > 0">{{ $t('start.taskList.title') }}</h5>
        <ShortcutsTable :items="itemsTasks"></ShortcutsTable>
      </div>
    </div>
  </b-modal>
</template>

<script>
import ShortcutsTable from '@/components/modals/ShortcutsTable.vue'
import { getShortcutsForModal } from '@/utils/shortcuts.js'

export default {
  name: 'ShortcutsModal',
  components: { ShortcutsTable },
  computed: {
    itemsGlobal() {
      return getShortcutsForModal(this.$root.config, 'global')
    },
    itemsTasks() {
      return getShortcutsForModal(this.$root.config, 'tasks')
    }
  },
  methods: {
    show: function() {
      this.$refs.shortcuts.show()
    }
  }
}
</script>
