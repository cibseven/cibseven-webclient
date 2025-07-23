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
  <li
    class="nav-item m-0 flex-shrink-0 border-0"
    v-for="(tab, index) in tabs"
    :key="index"
  >
    <a
      :href="getTabUrl(tab)"
      @click.prevent="handleTabClick(tab)"
      class="nav-link py-2 border-0 rounded-0"
      :class="{
        active: tab.id === modelValue,
        'bg-light border border-bottom-0': tab.id != modelValue
      }"
    >
      {{ $t(tab.text) }}
    </a>
  </li>
</template>

<script>
export default {
  name: 'GenericTabs',
  emits: ['update:modelValue'],
  props: { tabs: Array, modelValue: String },
  methods: {
    getTabUrl(tab) {
      const resolved = this.$router.resolve({
        ...this.$route,
        query: { ...this.$route.query, tab: tab.id }
      })
      return resolved.href
    },
    handleTabClick(tab) {
      this.$emit('update:modelValue', tab.id)
    }
  }
}
</script>