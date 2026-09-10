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
  <GenericTabs :tabs="tabs" :modelValue="modelValue" @update:modelValue="$emit('update:modelValue', $event)" @tab-click="$emit('tab-click', $event)"></GenericTabs>
</template>

<script>
import { GenericTabs } from '@cib/common-frontend'
import { getPlugin, reserveSlotIds } from '@/plugins/pluginsConfig.js'

const BUILTIN_TABS = [
  { id: 'variables', text: 'process.variables' },
  { id: 'incidents', text: 'process.incidents' },
  { id: 'usertasks', text: 'process.usertasks' },
  { id: 'jobs', text: 'process.jobs' },
  { id: 'calledProcessInstances', text: 'process.calledProcessInstances' },
  { id: 'externalTasks', text: 'process.externalTasks' }
]

// At import time, because a plugin can register before this tab bar is ever rendered
reserveSlotIds('process-instance-tab', BUILTIN_TABS.map(tab => tab.id))

export default {
  name: 'ProcessInstanceTabs',
  components: {
    GenericTabs,
  },
  props: { modelValue: String },
  emits: ['update:modelValue', 'tab-click'],
  data: function () {
    return {
      builtinTabs: BUILTIN_TABS
    }
  },
  computed: {
    tabs: function() {
      // Tabs contributed by plugins are appended, so the order of the built-in
      // tabs never depends on what is deployed. Their content is rendered by the
      // PluginSlot in ProcessInstanceView.
      const contributed = getPlugin('process-instance-tab').value
        .filter(contribution => contribution.id && contribution.text)
        .map(({ id, text }) => ({ id, text }))
      return [...this.builtinTabs, ...contributed]
    }
  }
}
</script>
