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
  <div class="row">
    <span class="text-secondary fw-bold col-8 pe-0">{{ $t('decision.details.versionTag') }}</span>
    <span class="col-4 text-end">{{ version.versionTag }}</span>
  </div>
  <hr class="my-2">
  <div class="row">
    <span class="text-secondary fw-bold col-12">
      {{ $t('decision.details.definitionId') }}
      <button @click="copyValueToClipboard(version.id)" class="btn btn-sm mdi mdi-content-copy float-end border-0"
        :title="$t('decision.details.copyValue')"></button>
    </span>
    <span class="col-12">{{ version.id }}</span>
  </div>
  <hr class="my-2">
  <div class="row">
    <span class="text-secondary fw-bold col-12">
      {{ $t('decision.details.definitionKey') }}
      <button @click="copyValueToClipboard(version.key)" class="btn btn-sm mdi mdi-content-copy float-end border-0"
        :title="$t('decision.details.copyValue')"></button>
    </span>
    <span class="col-12">{{ version.key }}</span>
  </div>
  <hr class="my-2">
  <div class="row">
    <span class="text-secondary fw-bold col-12">
      {{ $t('decision.details.definitionName') }}
      <button @click="copyValueToClipboard(version.name)" class="btn btn-sm mdi mdi-content-copy float-end border-0"
        :title="$t('decision.details.copyValue')"></button>
    </span>
    <span class="col-12">{{ version.name }}</span>
  </div>
  <hr class="my-2">
  <div class="row">
    <span class="text-secondary fw-bold col-12">
      {{ $t('decision.details.deploymentId') }}
      <button @click="copyValueToClipboard(version.deploymentId)" class="btn btn-sm mdi mdi-content-copy float-end border-0"
        :title="$t('decision.details.copyValue')"></button>
    </span>
    <router-link class="col-12" :to="'/seven/auth/deployments/' + version.deploymentId">{{ version.deploymentId }}</router-link>
  </div>
  <hr class="my-2">
  <div class="row">
    <span class="text-secondary fw-bold col-12">
      {{ $t('decision.details.historyTimeToLive') }}
      <button @click="copyValueToClipboard(historyTimeToLive)" class="btn btn-sm mdi mdi-content-copy float-end border-0"
        :title="$t('decision.details.copyValue')"></button>
      <button @click="editHistoryTimeToLive()" class="btn btn-sm mdi mdi-pencil float-end border-0"
        :title="$t('decision-instance.edit')"></button>
    </span>
    <span class="col-12">{{ historyTimeToLiveDisplay }}</span>
  </div>
  <hr class="my-2">
  <div class="row align-items-center">
    <span class="text-secondary fw-bold col-8 pe-0">{{ $t('decision.details.totalInstances') }}</span>
    <span class="col-4 text-end">{{ version.allInstances }}</span>
  </div>

  <EditHistoryTimeToLiveModal ref="ttlModal" :description-text="ttlDescription" @ttl-updated="onTtlUpdated" />
  <SuccessAlert ref="messageCopy"> {{ $t('decision.copySuccess') }} </SuccessAlert>
</template>

<script>

import { DecisionService } from '@/services.js'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import { SuccessAlert } from '@cib/common-frontend'
import EditHistoryTimeToLiveModal from '@/components/modals/EditHistoryTimeToLiveModal.vue'

export default {
  name: 'DecisionDefinitionDetails',
  components: { SuccessAlert, EditHistoryTimeToLiveModal },
  mixins: [ copyToClipboardMixin ],
  props: {
    version: Object
  },
  emits: ['updated-history-ttl'],
  data() {
    return {
      historyTimeToLive: null
    }
  },
  computed: {
    ttlDescription() {
      return `${this.$t('decision.details.definitionName')}: ${this.version.name} (${this.$t('decision.details.definitionVersion')}: ${this.version.version})`
    },
    historyTimeToLiveDisplay() {
      if (this.historyTimeToLive === undefined || this.historyTimeToLive === null) {
        // If 'historyTimeToLive' is undefined or null, we need to check the engine configuration to determine the default TTL.
        // enforceHistoryTimeToLive is only ever a known boolean when the engine actually
        // reported its config (see EngineConfiguration.java's "atomic pair" contract) -
        // null/undefined means unknown, so check against both true and false explicitly.
        const hasEngineTTLSetup = (this.$root.config.enforceHistoryTimeToLive === true || this.$root.config.enforceHistoryTimeToLive === false)
        if (hasEngineTTLSetup) {
          if (this.$root.config.historyTimeToLive === null) {
            // null means unlimited retention (never cleaned up)
            return '∞'
          }
          // any other number represents the number of days for history retention (including 0)
          return this.$t('historyTimeToLive.choiceDefault', { value: this.$root.config.historyTimeToLive })
        }
        else {
          return this.$t('historyTimeToLive.choiceDefaultUnknown')
        }
      }
      // any other number represents the number of days for history retention (including 0)
      return `${this.historyTimeToLive} ${this.$t('decision.days')}`
    }
  },
  mounted() {
    this.historyTimeToLive = this.version.historyTimeToLive
  },
  methods: {
    editHistoryTimeToLive() {
      this.$refs.ttlModal.show({ historyTimeToLive: this.historyTimeToLive })
    },
    onTtlUpdated({ ttlValue }) {
      DecisionService.updateHistoryTTLById(this.version.id, { historyTimeToLive: ttlValue }).then(() => {
        this.historyTimeToLive = ttlValue
        this.$emit('updated-history-ttl', ttlValue)
      }).catch(error => {
        this.$root.$refs.error.show(error.response?.data)
      })
    }
  }
}
</script>
