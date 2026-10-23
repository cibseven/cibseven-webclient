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

  <EditHistoryTimeToLiveModal ref="ttlModal" :description-items="ttlDescriptionItems" @ttl-updated="onTtlUpdated" />
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
    ttlDescriptionItems() {
      return [
        { title: this.$t('decision.details.definitionName'), value: this.version.name },
        { title: this.$t('decision.details.definitionVersion'), value: this.version.version }
      ]
    },
    historyTimeToLiveDisplay() {
      if (this.historyTimeToLive === undefined || this.historyTimeToLive === null) {
        // If decision 'historyTimeToLive' is undefined or null, we need to check the engine configuration to determine the default TTL.
        // /info passes both engine values through as reported (see EngineConfiguration.java):
        // engines since 3.0.0 always report a known boolean for enforceHistoryTimeToLive and a
        // number for historyTimeToLive (-1 if no default is configured), while older engines do
        // not report them at all, which arrives here as null ("unknown"). Unknown must not be
        // mistaken for a real value, so check against both true and false explicitly.
        const hasEngineTTLSetup = (this.$root.config.enforceHistoryTimeToLive === true || this.$root.config.enforceHistoryTimeToLive === false)
        if (hasEngineTTLSetup) {
          const engineDefault = this.$root.config.historyTimeToLive
          if (engineDefault === null || engineDefault === undefined || engineDefault < 0) {
            // the engine reports -1 for unlimited retention (never cleaned up)
            return '∞'
          }
          if (engineDefault === 0) {
            // 0 means history is cleaned up immediately
            return this.$t('historyTimeToLive.choiceDefaultZero')
          }
          // any other number represents the number of days for history retention
          return this.$t('historyTimeToLive.choiceDefault', { value: engineDefault })
        }
        else {
          return this.$t('historyTimeToLive.choiceDefaultUnknown')
        }
      }
      // any other number represents the number of days for history retention (including 0)
      if (this.historyTimeToLive === 0) {
        return this.$t('historyTimeToLive.choiceZero')
      }
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
