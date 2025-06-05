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
  <GenericTabs :tabs="tabs" @change-tab="$emit('change-tab', $event)"></GenericTabs>
</template>

<script>
import GenericTabs from '@/components/common-components/GenericTabs.vue'

export default {
  name: 'ProcessInstanceTabs',
  components: {
    GenericTabs,
  },
  emits: ['change-tab'],
  data: function () {
    return {
      tabs: [
        { id: 'variables', active: true, text: 'process.variables' },
        { id: 'incidents', active: false, text: 'process.incidents' },
        { id: 'usertasks', active: false, text: 'process.usertasks' },
        { id: 'jobs', active: false, text: 'process.jobs' },
        { id: 'calledProcessInstances', active: false, text: 'process.calledProcessInstances' }
      ]
    }
  }
}
</script>