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
  <b-modal ref="editVariableModal" :title="$t('process-instance.edit')" @hidden="clearVariableInstances">
    <div v-if="loading">
      <p class="text-center p-4"><BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('admin.loading') }}</p>
    </div>
    <div v-else-if="variable && !loading">
      <b-form-group :label="$t('process-instance.variables.name')">
        <b-form-input v-model="variable.name" disabled></b-form-input>
      </b-form-group>
      <b-form-group :label="$t('process-instance.variables.type')">
        <b-form-input v-model="variable.type" disabled></b-form-input>
      </b-form-group>
      <template v-if="variable.valueInfo">
        <div v-if="variable.valueInfo.objectTypeName" class="mb-3">
          <label class="form-label">{{ $t('process-instance.variables.objectTypeName') }}</label>
          <input type="text" class="form-control" :value="variable.valueInfo.objectTypeName" disabled>
        </div>
        <div v-if="variable.valueInfo.serializationDataFormat" class="mb-3">
          <label class="form-label">{{ $t('process-instance.variables.serializationDataFormat') }}</label>
          <input type="text" class="form-control" :value="variable.valueInfo.serializationDataFormat" disabled>
        </div>
      </template>
      <b-form-group :label="$t('process-instance.variables.value')">
        <textarea
          class="form-control"
          :class="{ 'is-invalid': isNewValueInvalid }"
          rows="5"
          :placeholder="$t('process-instance.variables.enterValue')"
          v-model="formattedJsonValue"
          :disabled="disabled || saving">
        </textarea>
        <div v-if="isNewValueInvalid" class="invalid-feedback">
          {{ valueValidationError }}
        </div>
      </b-form-group>
    </div>
    <template v-slot:modal-footer>
      <div class="row w-100 me-0">
        <div class="col-2 p-0">
          <BWaitingBox v-if="saving" class="d-inline me-2" styling="width: 30px"></BWaitingBox>
        </div>
        <div class="col-10 p-0 d-flex justify-content-end pe-1">
          <b-button v-if="disabled" @click="$refs.editVariableModal.hide()">{{ $t('confirm.close') }}</b-button>
          <template v-else>
            <b-button @click="$refs.editVariableModal.hide()" variant="link">{{ $t('confirm.cancel') }}</b-button>
            <b-button @click="updateVariable" variant="primary" :disabled="!variable || isNewValueInvalid || this.newValue === null || saving">{{ $t('process-instance.save') }}</b-button>
          </template>
        </div>
      </div>
    </template>
  </b-modal>
</template>

<script>
import { BWaitingBox } from 'cib-common-components'
import { ProcessService } from '@/services.js'
import { mapGetters, mapActions } from 'vuex'

export default {
  name: 'EditVariableModal',
  components: { BWaitingBox },
  props: {
    disabled: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      loading: false,
      saving: false,
      newValue: null,
      valueValidationError: null
    }
  },
  computed: {
    ...mapGetters('variableInstance', ['currentVariableInstance']),
    ...mapGetters('historicVariableInstance', ['currentHistoricVariableInstance']),
    variable() {
      // Use regular variable instance for active processes, historic for all others
      return this.disabled
        ? this.currentHistoricVariableInstance
        : this.currentVariableInstance
    },
    formattedJsonValue: {
      get: function() {
        if (this.variable) {
          if (this.newValue !== null) {
            return this.newValue
          }

          if (this.variable.type === 'Json') {
            try {
              return JSON.stringify(JSON.parse(this.variable.value), null, 2)
            } catch {
              return this.variable.value // Fallback to original value if parsing fails
            }
          } else if (this.variable.type === 'Object') {
            return JSON.stringify(this.variable.value, null, 2)
          } else return this.variable.value
        }
        return ''
      },
      set: function(val) {
        this.validateJson(val)
        if (!this.valueValidationError) {
          this.newValue = this.variable.type === 'Object' ? JSON.parse(val) : val
        }
        else {
          this.newValue = val
        }
      }
    },
    isNewValueInvalid() {
      return this.valueValidationError !== null
    }
  },
  methods: {
    ...mapActions('variableInstance', ['clearVariableInstance', 'getVariableInstance']),
    ...mapActions('historicVariableInstance', ['clearHistoricVariableInstance', 'getHistoricVariableInstance']),
    async show(variableId) {
      this.loading = true
      this.newValue = null
      this.saving = false
      this.valueValidationError = null // Reset validation error

      this.$refs.editVariableModal.show()

      // Try active, fallback to historic if error
      // Use regular variable instance for active processes, historic for all others
      if (!this.disabled) {
        try {
          await this.getVariableInstance({ id: variableId, deserializeValue: false })
        } catch {
          await this.getHistoricVariableInstance({ id: variableId, deserializeValue: false })
        }
      } else {
        await this.getHistoricVariableInstance({ id: variableId, deserializeValue: false })
      }
      this.loading = false
    },
    updateVariable: async function() {
      this.saving = true
      if (this.variable.type === 'Object' && this.variable.valueInfo?.objectTypeName !== 'java.lang.StringBuilder') {
        const executionId = this.variable.executionId
        const variableName = this.variable.name

        var formData = new FormData()
        const jsonBlob = new Blob([this.variable.value], { type: 'application/json' })
        formData.append('data', jsonBlob, 'blob')
        formData.append('valueType', 'Bytes')

        await ProcessService.modifyVariableDataByExecutionId(executionId, variableName, formData).then(() => {
          this.$emit('variable-updated')
          this.$refs.editVariableModal.hide()
        })
      }
      else {
        const data = { modifications: {} }

        let value = this.newValue
        if (this.variable.type === 'Json') {
          // JSON validation already done in setter, no need for try-catch here
          value = JSON.stringify(JSON.parse(value))
        } else if (this.variable.type === 'Object') {
          if (typeof value !== 'string') {
            value = JSON.stringify(value)
          }
        }

        const mod = { value, type: this.variable.type }
        // Handle StringBuilder special case
        const objectTypeName = this.variable.valueInfo?.objectTypeName
        if (this.variable.type === 'Object' && objectTypeName === 'java.lang.StringBuilder') {
          mod.value = JSON.stringify(value)
          mod.valueInfo = this.variable.valueInfo
        }
        data.modifications[this.variable.name] = mod
        await ProcessService.modifyVariableByExecutionId(this.variable.executionId, data).then(() => {
          this.$emit('variable-updated')
          this.$refs.editVariableModal.hide()
        })
      }
      this.saving = false
    },
    validateJson(value) {
      if (!this.variable || (!['Json', 'Object'].includes(this.variable.type))) {
        this.valueValidationError = null
        return
      }

      if (!value || value.trim() === '') {
        this.valueValidationError = null
        return
      }

      try {
        JSON.parse(value)
        this.valueValidationError = null
      } catch (e) {
        this.valueValidationError = e.message
      }
    },
    clearVariableInstances() {
      this.clearVariableInstance()
      this.clearHistoricVariableInstance()
      this.valueValidationError = null // Reset validation error when closing modal
    }
  }
}
</script>
