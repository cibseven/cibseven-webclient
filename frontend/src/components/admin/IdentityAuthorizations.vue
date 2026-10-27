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
  <ContentBlock :title="title" class="mb-3">
    <template v-slot:actions>
      <b-form-checkbox v-model="includeWildcard" switch>
        {{ $t('admin.authorizations.identity.includeWildcard') }}
      </b-form-checkbox>
    </template>

    <div v-if="rows.length > 0" class="p-0">
      <FlowTable striped :items="rows" primary-key="id" :fields="fields">
        <template v-slot:cell(resourceType)="row">
          <router-link v-if="resourceTypeRoute(row.item.resourceType)" :to="resourceTypeRoute(row.item.resourceType)">
            {{ resourceTypeName(row.item.resourceType) }}
          </router-link>
          <span v-else>{{ resourceTypeName(row.item.resourceType) }}</span>
        </template>
        <template v-slot:cell(type)="row">
          {{ typeName(row.item.type) }}
        </template>
        <template v-slot:cell(userIdGroupId)="row">
          <span class="mdi" :class="row.item.userId != null ? 'mdi-account' : 'mdi-account-group'" aria-hidden="true"></span>
          {{ row.item.userId ?? row.item.groupId }}
        </template>
        <template v-slot:cell(permissions)="row">
          {{ permissionsText(row.item.permissions) }}
        </template>
      </FlowTable>
    </div>
    <div v-else-if="loading" class="text-center p-4">
      <BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('admin.loading') }}
    </div>
    <span v-else class="d-block text-center p-4">
      {{ $t('admin.noResults') }}
    </span>
  </ContentBlock>
</template>

<script>
import { AdminService } from '@/services.js'
import { FlowTable, ContentBlock, BWaitingBox } from '@cib/common-frontend'

// The engine's wildcard identity, meaning "every user" (or "every group").
const WILDCARD = '*'

export default {
  name: 'IdentityAuthorizations',
  components: { FlowTable, ContentBlock, BWaitingBox },
  props: {
    userId: { type: String, default: null },
    groupId: { type: String, default: null },
    title: { type: String, default: '' }
  },
  emits: [],
  data() {
    return {
      authorizations: [],
      includeWildcard: false,
      loading: false,
      // Identifies the latest request, so a slower earlier response cannot overwrite a newer one.
      requestId: 0
    }
  },
  watch: {
    userId() { this.loadAuthorizations() },
    groupId() { this.loadAuthorizations() },
    includeWildcard() { this.loadAuthorizations() }
  },
  computed: {
    fields() {
      return [
        { label: 'admin.authorizations.resourceType', key: 'resourceType', class: 'col-3' },
        { label: 'admin.authorizations.type', key: 'type', class: 'col-1' },
        { label: 'admin.authorizations.userIdGroupId', key: 'userIdGroupId', class: 'col-2' },
        { label: 'admin.authorizations.permissions', key: 'permissions', class: 'col-4' },
        { label: 'admin.authorizations.resourceId', key: 'resourceId', class: 'col-2' }
      ]
    },
    rows() {
      return [...this.authorizations].sort((a, b) =>
        this.resourceTypeName(a.resourceType).localeCompare(this.resourceTypeName(b.resourceType)) ||
        String(a.resourceId ?? '').localeCompare(String(b.resourceId ?? '')))
    }
  },
  created() {
    this.loadAuthorizations()
  },
  methods: {
    queries() {
      const queries = []
      if (this.userId) queries.push({ userIdIn: this.userId })
      else if (this.groupId) queries.push({ groupIdIn: this.groupId })
      // Asked one identity at a time: the direct provider passes userIdIn/groupIdIn on as a single id.
      if (queries.length > 0 && this.includeWildcard) queries.push({ userIdIn: WILDCARD }, { groupIdIn: WILDCARD })
      return queries
    },
    async loadAuthorizations() {
      const queries = this.queries()
      const requestId = ++this.requestId
      if (queries.length === 0) {
        this.authorizations = []
        return
      }
      this.loading = true
      try {
        const results = await Promise.all(queries.map(query => AdminService.findAuthorizations(query)))
        if (requestId !== this.requestId) return
        const byId = new Map()
        results.flat().forEach(authorization => byId.set(authorization.id, authorization))
        this.authorizations = [...byId.values()]
      } finally {
        if (requestId === this.requestId) this.loading = false
      }
    },
    resourceType(resourceType) {
      return this.$root.config.admin?.resourcesTypes?.[String(resourceType)] ?? null
    },
    resourceTypeName(resourceType) {
      const type = this.resourceType(resourceType)
      return type ? this.$t(`admin.authorizations.resourcesTypes.${type.key}`) : String(resourceType)
    },
    resourceTypeRoute(resourceType) {
      const type = this.resourceType(resourceType)
      if (!type) return null
      return { name: 'authorizationType', params: { resourceTypeId: type.id, resourceTypeKey: type.key } }
    },
    typeName(type) {
      const entry = Object.values(this.$root.config.admin?.types ?? {}).find(t => Number(t.id) === type)
      return entry ? this.$t(`admin.authorizations.types.${entry.key}`) : String(type)
    },
    permissionsText(permissions) {
      return permissions?.length > 0 ? permissions.join(', ') : 'NONE'
    }
  }
}
</script>
