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
  <div class="progress" :class="$attrs.class" :style="progressStyle">
    <slot v-if="$slots.default"></slot>
    <BProgressBar v-else-if="value !== null"
      :value="value"
      :max="max"
      :label="label"
      :variant="variant"
      :animated="animated"
      :striped="striped"
      :showValue="showValue"
      :showProgress="showProgress"
      :precision="precision">
    </BProgressBar>
  </div>
</template>

<script>
import BProgressBar from './BProgressBar.vue'
export default {
  components: { BProgressBar },
    props: {
      variant: { type: String, default: '' },
      animated: { type: Boolean, default: false },
      striped: { type: Boolean, default: false },
      value: { type: Number, default: null },
      max: { type: Number, default: 100 },
      label: { type: String, default: '' },
      height: { type: [String, Number], default: null },
    showValue: { type: Boolean, default: false },
      showProgress: { type: Boolean, default: false },
      precision: { type: Number, default: 0 }
    },
    computed: {
      progressStyle: function() {
          return this.height ? { height: typeof this.height === 'number' ? this.height + 'px' : this.height } : {};
      }
    }
}
</script>

<style scoped>
</style>
