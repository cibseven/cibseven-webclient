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
    <template v-for="(group, groupIndex) in groups" :key="group.name">
      <b-list-group-item v-if="groupIndex > 0" class="border-0 p-0 no-radius-right">
        <hr class="p-0 m-0"/>
      </b-list-group-item>
      <b-list-group-item
        v-for="resourceType in group.resourceTypes"
        :key="resourceType.id"
        class="border-0 px-3 py-2 no-radius-right"
        action
        :active="isActive(resourceType)"
        :to="editRoute(resourceType)"
        @click="$emit('middle')">
        <span>{{ $t(`admin.authorizations.resourcesTypes.${resourceType.key}`) }}</span>
      </b-list-group-item>
    </template>
    <component v-if="ExtendedAuthorizations" :is="ExtendedAuthorizations"></component>
  </b-list-group>
</template>

<script>
// Mapping of resource types to their respective groups
// Array is sorted in order how items should appear in the navigation bar
const RESOURCE_TYPE_GROUPS = [
  { name: 'application', group: 'application' },

  { name: 'user', group: 'admin' },
  { name: 'group', group: 'admin' },
  { name: 'tenant', group: 'admin' },

  { name: 'authorization', group: 'authorizations' },
  { name: 'groupMembership', group: 'authorizations' },
  { name: 'tenantMembership', group: 'authorizations' },

  { name: 'processDefinition', group: 'processes' },
  { name: 'processInstance', group: 'processes' },
  { name: 'historicProcessInstance', group: 'processes' },
  { name: 'decisionDefinition', group: 'processes' },
  { name: 'decisionRequirements', group: 'processes' },
  { name: 'deployment', group: 'processes' },
  { name: 'batch', group: 'processes' },
  { name: 'userOperationLogCategory', group: 'processes' },
  { name: 'operationLogCategory', group: 'processes' },

  { name: 'task', group: 'tasks' },
  { name: 'historicTask', group: 'tasks' },
  { name: 'filter', group: 'tasks' },

  { name: 'report', group: 'other' },
  { name: 'dashboard', group: 'other' },

  { name: 'system', group: 'system' },
]

const GROUPS = RESOURCE_TYPE_GROUPS.reduce((acc, entry) => {
  if (!acc.includes(entry.group)) {
    acc.push(entry.group)
  }
  return acc
}, [])

export default {
  name: 'AuthorizationsNavBar',
  emits: ['middle'],
  computed: {
    resourceTypes() {
      return this.$root.config.admin.resourcesTypes
    },
    groups() {
      const resourceTypes = Object.values(this.resourceTypes ?? {})
      return GROUPS
        .map(name => ({
          name,
          resourceTypes: resourceTypes
            .filter(resourceType => this.getResourceTypeGroup(resourceType.key) === name)
            .sort((a, b) => this.getResourceIndex(a.key) - this.getResourceIndex(b.key))
        }))
        .filter(group => group.resourceTypes.length > 0)
    },
    ExtendedAuthorizations() {
      return this.$options.components && this.$options.components.ExtendedAuthorizations
        ? this.$options.components.ExtendedAuthorizations
        : null
    }
  },
  methods: {
    getResourceTypeGroup(resourceTypeKey) {
      const group = RESOURCE_TYPE_GROUPS.find(entry => entry.name === resourceTypeKey)
      return group ? group.group : 'other'
    },
    getResourceIndex(resourceTypeKey) {
      const index = RESOURCE_TYPE_GROUPS.findIndex(entry => entry.name === resourceTypeKey)
      return index === -1 ? Number.MAX_SAFE_INTEGER : index
    },
    isActive: function(resourceType) {
      return this.$route.path.endsWith(`/${resourceType.id}/${resourceType.key}`)
    },
    editRoute: function(resourceType) {
      return { name: 'authorizationType', params: { resourceTypeId: resourceType.id, resourceTypeKey: resourceType.key } }
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
