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
  <transition name="fade">
    <div ref="alert" v-if="show_" role="alert" class="alert alert-dismissible" :class="'alert-' + variant" aria-live="assertive" aria-atomic="true">
      <slot></slot>
      <BButtonClose @click="dismiss"></BButtonClose>
    </div>
  </transition>
</template>

<script>
import BButtonClose from './BButtonClose.vue'

export default {
  props: {
    variant: { type: String, default: 'primary' },
    show: { type: [Number, Boolean], default: false }
  },
  components: {
    BButtonClose
  },
  data: function () {
    return {
      show_: false,
      timeoutId: null
    }
  },
  watch: {
    show: function (val) {
      this.show_ = val
      if (typeof val === 'number' && val > 0) {
        this.clearTimeout()
        this.timeoutId = setTimeout(() => {
            this.dismiss()
        }, val * 1000)
      }
    }
  },
  methods: {
    dismiss: function () {
      this.clearTimeout()
      this.show_ = false
      this.$emit('dismissed')
    },
    clearTimeout: function () {
      if (this.timeoutId) {
        clearTimeout(this.timeoutId)
        this.timeoutId = null
      }
    }
  },
  beforeUnmount: function () {
      this.clearTimeout()
  }
}
</script>

<style scoped>
</style>
