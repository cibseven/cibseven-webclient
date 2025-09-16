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
  <b-modal ref='confirm' v-if="variable" :title="$t('confirm.title')">
    <div class="row">
      <div class="col-2 d-flex justify-content-center">
        <span class="mdi-36px mdi mdi-alert-outline text-warning"></span>
      </div>
      <div class="col-10 d-flex align-items-center ps-0">
        <div>
          <p>{{ $t('process-instance.variables.confirmDelete') }}</p>

          <p>
            {{ $t('process-instance.variables.name') }}: <strong>{{ variable?.name }}</strong>
            <br>
            {{ $t('process-instance.variables.type') }}: <strong>{{ variable?.type }}</strong>
          </p>

          <p v-if="errorMessage" class="alert alert-danger d-flex align-items-center">
            <span class="mdi mdi-alert-octagon-outline text-danger"></span>
            <span class="ms-2">{{ errorMessage }}</span>
          </p>
        </div>
      </div>
    </div>
    <template v-slot:modal-footer>
      <div class="row w-100 me-0">
        <div class="col-4 p-0">
          <BWaitingBox v-if="deleting" class="d-inline me-2" styling="width: 25px"></BWaitingBox>
        </div>
        <div class="col-8 p-0 text-end">
          <b-button :disabled="deleting" @click="$refs.confirm.hide('cancel')" variant="link">{{ $t('confirm.cancel') }}</b-button>
          <b-button :disabled="deleting" @click="onDelete" variant="primary">{{ $t('confirm.delete') }}</b-button>
        </div>
      </div>
    </template>
  </b-modal>
</template>

<script>
import { BWaitingBox } from 'cib-common-components'
import { ProcessService, HistoryService } from '@/services.js'

export default {
  name: 'DeleteVariableModal',
  emits: ['variable-deleted'],
  components: { BWaitingBox },
  data: function() {
    return {
      isInstanceActive: false,
      variable: Object,
      deleting: false,
      errorMessage: '',
    }
  },
  methods: {
    show: function(isInstanceActive, variable) {
      this.isInstanceActive = isInstanceActive
      this.variable = variable
      this.errorMessage = ''
      this.deleting = false

      this.$refs.confirm.show()
    },
    async onDelete() {
      this.deleting = true
      this.errorMessage = ''

      let success = false
      try {
        if (this.isInstanceActive) {
          await ProcessService.deleteVariableByExecutionId(this.variable.executionId, this.variable.name)
        } else {
          await HistoryService.deleteVariableHistoryInstance(this.variable.id)
        }
        success = true
      } catch {
        success = false
      }

      if (success) {
        this.$refs.confirm.hide('ok')
        this.$emit('variable-deleted', this.variable)
      }
      else {
        const messageTemplate = this.isInstanceActive ? 'process-instance.variables.deleteStatus.runtimeError' : 'process-instance.variables.deleteStatus.historicError'
        this.errorMessage = this.$t(messageTemplate, { name: this.variable.name })
        this.deleting = false
      }
    }
  }
}
</script>
