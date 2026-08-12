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

import modelerImage from '@/assets/images/start/modeler.svg'

export default {
  name: 'BuilderStartView',
  mixins: [permissionsMixin, navigationPermissionsMixin, startTileOptionsMixin],
  components: { StartHubView },
  computed: {
    builtInItems() {
      const items = []
      if (this.permissionsModeler) {
        items.push({
          to: { name: 'modeler' },
          title: this.$t('start.modeler.title'),
          src: modelerImage
        })
      }
      return items
    },
    items() {
      return this.mergeOptions(this.builtInItems, 'BuilderTileOptionsPlugin')
        .filter(item => item && item.to && item.title && item.src)
    }
  }
}
</script>
