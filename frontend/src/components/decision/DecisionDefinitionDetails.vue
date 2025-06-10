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
    <span class="col-12">{{ historyTimeToLive + ' ' + $t('decision.days') }}</span>
  </div>
  <hr class="my-2">
  <div class="row align-items-center">
    <span class="text-secondary fw-bold col-8 pe-0">{{ $t('decision.details.totalInstances') }}</span>
    <span class="col-4 text-end">{{ version.allInstances }}</span>
  </div>

  <b-modal ref="historyTimeToLive" :title="$t('decision.details.historyTimeToLive')">
    <div class="d-flex col-6 align-items-center ps-0">
      <input class="form-control" type="number" v-model="historyTimeToLiveChanged">
      <span class="ms-2">{{ $t('decision.days') }}</span>
    </div>
    <template v-slot:modal-footer>
      <b-button @click="$refs.historyTimeToLive.hide()" variant="link">{{ $t('confirm.cancel') }}</b-button>
      <b-button @click="updateHistoryTimeToLive()" variant="primary">{{ $t('decision-instance.save') }}</b-button>
    </template>
  </b-modal>
  <SuccessAlert ref="messageCopy"> {{ $t('decision.copySuccess') }} </SuccessAlert>
</template>

<script>

import { DecisionService } from '@/services.js'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'

export default {
  name: 'DecisionDefinitionDetails',
  components: { SuccessAlert },
  mixins: [ copyToClipboardMixin ],
  props: {
    version: Object
  },
  emits: ['updated-history-ttl'],
  data() {
    return {
      historyTimeToLive: '',
      historyTimeToLiveChanged: ''
    }
  },
  mounted() {
    this.historyTimeToLive = this.version.historyTimeToLive
  },
  methods: {
    editHistoryTimeToLive() {
      this.historyTimeToLiveChanged = this.historyTimeToLive
      this.$refs.historyTimeToLive.show()
    },
    updateHistoryTimeToLive() {
      DecisionService.updateHistoryTTLById(this.version.id,
      { historyTimeToLive: this.historyTimeToLiveChanged }).then(() => {
        // eslint-disable-next-line vue/no-mutating-props
        this.historyTimeToLive = this.version.historyTimeToLive = this.historyTimeToLiveChanged
        this.$emit('updated-history-ttl')
        this.$refs.historyTimeToLive.hide()
      })
    }
  }
}
</script>
