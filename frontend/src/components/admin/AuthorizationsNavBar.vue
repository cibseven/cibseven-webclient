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
  <b-list-group>
    <b-list-group-item
      v-for="resourceType in $root.config.admin.resourcesTypes"
      :key="resourceType.id"
      class="border-0 px-3 py-2 no-radius-right"
      action
      :active="$route.path.includes(calcLink(resourceType))"
      :to="calcLink(resourceType)"
      @click="$emit('middle')">
      <span>{{ $t(`admin.authorizations.resourcesTypes.${resourceType.key}`) }}</span>
    </b-list-group-item>
    <component v-if="ExtendedAuthorizations" :is="ExtendedAuthorizations"></component>
  </b-list-group>
</template>

<script>
export default {
  name: 'AuthorizationsNavBar',
  computed: {
    ExtendedAuthorizations: function() {
      return this.$options.components && this.$options.components.ExtendedAuthorizations
        ? this.$options.components.ExtendedAuthorizations
        : null
    }
  },
  methods: {
    calcLink: function(resourceType) {
      return `/seven/auth/admin/authorizations/${resourceType.id}/${resourceType.key}`
    }
  }
}
</script>

<style lang="css" scoped>
.no-radius-right {
  border-top-right-radius: 0 !important;
  border-bottom-right-radius: 0 !important;
}
</style>
