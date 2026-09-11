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
  <PluginBoundary v-for="contribution in contributions"
    :key="contribution.key"
    :component="contribution.component" :plugin-id="contribution.pluginId" v-bind="params">
  </PluginBoundary>
</template>

<script>
import PluginBoundary from '@/components/common/PluginBoundary.vue'
import { getPlugin } from '@/plugins/pluginsConfig.js'

export default {
  name: 'PluginSlot',
  components: { PluginBoundary },
  props: {
    // Name of the slot plugins register into, e.g. 'process-instance-tab'
    name: { type: String, required: true },
    // Props handed to every contribution of this slot
    params: { type: Object, default: () => ({}) },
    // When set, only the contribution registered under this id is rendered.
    // Used where a slot holds alternatives instead of a list, for example one
    // contribution per tab of which only the active one is shown.
    only: { type: String, default: null }
  },
  computed: {
    contributions() {
      // Renders nothing when no plugin contributed to this slot, which is the
      // default state of every slot in a webclient without plugins.
      const contributions = getPlugin(this.name).value
      if (this.only === null) return contributions
      return contributions.filter(contribution => contribution.id === this.only)
    }
  }
}
</script>
