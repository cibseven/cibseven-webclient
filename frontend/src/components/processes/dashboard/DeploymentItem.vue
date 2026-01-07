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
  <div class="col-3 my-2">
    <router-link :to="link" :title="tooltip" class="text-decoration-none">
      <div class="py-3 text-center rounded hovered"
        :class="{ clicked: isClicked }"
        @mousedown="isClicked = true"
        @mouseup="isClicked = false"
        @mouseleave="isClicked = false"
      >
        <h5 class="link-dark">{{ title }}</h5>
        <h2 class="link-dark">
          <span v-if="count !== null" :class="computedValueClass">{{ count }}</span>
          <span v-else><BWaitingBox class="d-inline" styling="width: 24px" :title="$t('admin.loading')"></BWaitingBox></span>
        </h2>
      </div>
    </router-link>
  </div>
</template>

<script>
import { BWaitingBox } from '@cib/common-frontend'

export default {
  name: 'DeploymentItem',
  components: { BWaitingBox },
  props: {
    title: String,
    tooltip: String,
    count: [ Number, String ],
    link: String
  },
  data() {
    return {
      isClicked: false
    }
  },
  computed: {
    computedValueClass: function() {
      return this.count === 'x' ? 'text-warning' : ''
    },
  }
}
</script>

<style lang="css" scoped>
.hovered {
  transition: background-color 0.3s ease;
  background-color: var(--bs-light);
}
.hovered:hover {
 background-color: color-mix(in srgb, var(--bs-light) 95%, #000 5%) !important;
}
.hovered.clicked {
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.1) !important;
}
</style>
