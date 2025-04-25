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
  <div role="progressbar" class="progress-bar" :class="classes" :style="{ width: computedWidth }">
    <slot>{{ computedLabel }}</slot>
  </div>
</template>

<script>
export default {
  props: {
    label: { type: String, default: '' },
    value: { type: Number, required: true },
    max: { type: Number, default: 100 },
    variant: { type: String, default: '' },
    animated: { type: Boolean, default: false },
    striped: { type: Boolean, default: false },
    showValue: { type: Boolean, default: false },
    showProgress: { type: Boolean, default: false },
    precision: { type: Number, default: 0 }
  },
  computed: {
    computedWidth: function() {
      return (this.value / this.max) * 100 + '%'
    },
    classes: function() {
      const res = []
      if (this.variant) {
        res.push('bg-' + this.variant)
      }
      if (this.animated) {
        res.push('progress-bar-striped', 'progress-bar-animated');
      } else if (this.striped) {
        res.push('progress-bar-striped')
      }
      return res
    },
    computedLabel: function() {
      const percentage = ((this.value / this.max) * 100).toFixed(this.precision)
      if (this.showProgress) return percentage + '%'
      if (this.showValue) return this.value
      return this.label
    }
  }
}
</script>

<style scoped>
</style>
