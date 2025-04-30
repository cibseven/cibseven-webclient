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
  <div
    :class="colClasses"
    :role="role"
    :aria-label="ariaLabel">
    <slot></slot>
  </div>
</template>

<script>
export default {
  props: {
    cols: {
      type: [Number, String],
      default: null
    },
    offset: {
      type: [Number, String],
      default: null
    },
    order: {
      type: [Number, String],
      default: null
    },
    alignSelf: {
      type: String,
      default: null
    },
    role: {
      type: String,
      default: null
    },
    ariaLabel: {
      type: String,
      default: null
    }
  },
  computed: {
    colClasses: function () {
      const classes = []

      if (this.cols) classes.push(`col-${this.cols}`)
      else classes.push('col')

      if (this.offset) classes.push(`offset-${this.offset}`)
      if (this.order) classes.push(`order-${this.order}`)
      if (this.alignSelf) classes.push(`align-self-${this.alignSelf}`)

      return classes.join(' ')
    }
  }
}
</script>

<style scoped>
</style>
