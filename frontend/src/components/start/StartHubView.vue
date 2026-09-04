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
  <div :style="{ 'height': 'calc(100% - 55px)' }" class="d-flex flex-column">
    <div class="h-100 container-fluid overflow-auto bg-light">
      <div v-if="items.length > 0" class="row justify-content-center">
        <StartViewItem v-for="item in items" :key="item.title"
          :title="item.title"
          :src="item.src"
          :to="item.to"
        ></StartViewItem>
      </div>
      <div v-else>
        <img src="@/assets/images/start/empty_start_page.svg" class="d-block mx-auto mt-5 mb-3" style="max-width: 250px" alt="">
        <div class="h5 text-secondary text-center">{{ $t('start.emptyStart', { productName }) }}</div>
      </div>
    </div>
  </div>
</template>

<script>
import StartViewItem from '@/components/start/StartViewItem.vue'

/** Shared shell for the Tasks/Builder/Data/Access-management hub pages: a tile grid or empty state. */
export default {
  name: 'StartHubView',
  components: { StartViewItem },
  props: {
    items: {
      type: Array,
      default: () => []
    }
  },
  computed: {
    productName() {
      return this.$root.config.productNamePageTitle || this.$t('login.productName')
    }
  }
}
</script>
