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
  <div class="h-100 d-flex flex-column bg-light">
    <div class="overflow-auto h-100">
      <b-button style="top: 2px; right: 60px" class="border-0 position-absolute"
        variant="btn-outline-primary" size="sm" :disabled="loading" :title="$t('decision.loadVersions')"
        @click="$emit('refresh-decision-versions', false)">
        <span class="mdi mdi-18px mdi-cog-refresh-outline"></span>
      </b-button>
      <b-button style="top: 2px; right: 30px" class="border-0 position-absolute"
        variant="btn-outline-primary" size="sm" :disabled="loading" :title="$t('decision.refreshVersions')"
        @click="$emit('refresh-decision-versions', true)">
        <span class="mdi mdi-18px mdi-refresh"></span>
      </b-button>
      <b-list-group class="mx-3 mb-3">
        <b-list-group-item @click="selectVersion(version)" v-for="version of versions" :key="version.id"
        class="rounded-0 mt-3 p-2 bg-white border-0" :class="markSelectedVersion(version.version) ? 'active shadow' : ''" style="cursor: pointer">
          <div class="d-flex align-items-center">
            <h6 style="font-size: 1rem">
              <span class="fw-bold">{{ $t('decision.details.definitionVersion') + ': ' + version.version }}</span>
            </h6>
            <div class="d-flex ms-auto" :id="version.id">
              <span class="mdi mdi-18px mdi-information-outline text-info"></span>
            </div>
          </div>
          <div class="mb-1">
            <div>{{ $t('decision.details.totalInstances') + ': ' + version.allInstances }}</div>
          </div>
          <b-popover :target="version.id" triggers="hover" placement="right" boundary="viewport" max-width="350px">
            <DecisionDefinitionDetails :version="version" @updated-history-ttl="$refs.successOperation.show()"></DecisionDefinitionDetails>
          </b-popover>
        </b-list-group-item>
      </b-list-group>
    </div>
    <SuccessAlert top="50px" style="z-index: 9999" ref="successOperation"> {{ $t('alert.successOperation') }}</SuccessAlert>
  </div>
</template>

<script>

import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import { SuccessAlert } from '@cib/common-frontend'
import DecisionDefinitionDetails from '@/components/decision/DecisionDefinitionDetails.vue'

export default {
  name: 'DecisionVersionListSidebar',
  components: { SuccessAlert, DecisionDefinitionDetails },
  mixins: [copyToClipboardMixin],
  props: {
    versions: Array
  },
  emits: ['refresh-decision-versions'],
  data() {
    return {
      loading: false
    }
  },
  computed: {
    decisionName() {
      return this.decision.name ? this.decision.name : this.decision.key
    }
  },
  methods: {
    selectVersion(version) {
      this.$router.push({ name: 'decision-version', params: { decisionKey: version.key, versionIndex: version.version } })
    },
    markSelectedVersion(versionToCheck) {
      return String(versionToCheck) === this.$route.params.versionIndex
    }
  }
}
</script>
