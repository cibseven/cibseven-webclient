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
  <b-button :size="size" :variant="variant" :title="text" :disabled="disabled" class="mx-1">
    <template v-if="icon || cibIcon || loading">
      <span v-if="!loading" :class="setIconClasses" class="d-inline-block align-middle me-1" style="line-height: 0;"></span>
      <b-spinner v-else small></b-spinner>
    </template>
    <span class="d-none d-sm-inline">{{ text }}</span>
  </b-button>
</template>

<script>
export default {
  name: 'IconButton',
  props: {
    variant: { type: String, default: 'outline-secondary' },
    icon: { type: String, default: null },
    cibIcon: { type: String, default: null },
    font: { type: Number, default: 18 },
    size: { type: String, default: 'md' },
    disabled: { type: Boolean, default: false },
    text: String,
    loading: { type: Boolean, default: false }
  },
  computed: {
    setIconClasses() {
      if (this.cibIcon) {
        return `icon-cib-${this.font} icon-${this.cibIcon}`
      }
      return `mdi mdi-${this.font} mdi-${this.icon}`
    }
  }
}
</script>
