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
  <div class="d-flex flex-column">
    <SidebarsFlow ref="sidebars" class="border-top overflow-auto" v-model:left-open="leftOpen" :left-caption="leftCaption">
      <template v-slot:left>
        <b-list-group>
          <b-list-group-item
            v-for="setting in systemSettings"
            :key="setting"
            class="border-0 px-3 py-2 no-radius-right"
            :active="$route.path.includes(`/seven/auth/admin/system/${setting}`)"
            action
            :to="`/seven/auth/admin/system/${setting}`">
            <span>{{ $t(`admin.system.${setting}.title`) }}</span>
          </b-list-group-item>
        </b-list-group>
      </template>
      <router-view/>
    </SidebarsFlow>
  </div>
</template>

<script>
import SidebarsFlow from '@/components/common-components/SidebarsFlow.vue'

export const SystemSidebarItems = ['system-diagnostics', 'execution-metrics']

export default {
  name: 'SystemView',
  components: { SidebarsFlow },
  props: {
    systemSettings: {
      type: Array,
      default: () => SystemSidebarItems
    }
  },
  data() {
    return {
      leftOpen: true
    }
  },
  computed: {
    leftCaption() {
      return this.$t('admin.system.settings')
    }
  }
}
</script>

<style lang="css" scoped>
.no-radius-right {
  border-top-right-radius: 0 !important;
  border-bottom-right-radius: 0 !important;
}
</style>
