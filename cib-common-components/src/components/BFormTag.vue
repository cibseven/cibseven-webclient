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
  <span
    :class="['badge', badgeVariant, 'me-1']"
    role="button"
    :aria-disabled="disabled.toString()"
    @click="handleClick">
    <slot></slot>
    <button
      v-if="!disabled"
      type="button"
      class="btn-close ms-2"
      :aria-label="$t('bcomponents.formTag.remove')"
      @click="handleRemove">
    </button>
  </span>
</template>

<script>
export default {
  props: {
    variant: {
      type: String,
      default: 'primary'
    },
    disabled: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    badgeVariant: function () {
      return `bg-${this.variant}`
    }
  },
  methods: {
    handleRemove: function (event) {
      event.stopPropagation()
      this.$emit('remove')
    },
    handleClick: function (event) {
      if (!this.disabled) {
        this.$emit('click', event)
      }
    }
  }
}
</script>

<style scoped>
</style>
