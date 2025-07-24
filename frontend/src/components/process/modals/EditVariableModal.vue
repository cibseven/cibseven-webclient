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
    <div v-if="variableToModify">
      <b-form-group :label="$t('process-instance.variables.name')">
        <b-form-input v-model="variableToModify.name" disabled></b-form-input>
      </b-form-group>
      <b-form-group :label="$t('process-instance.variables.type')">
        <b-form-input v-model="variableToModify.type" disabled></b-form-input>
      </b-form-group>
      <template v-if="currentInstance?.valueInfo">
        <div v-if="currentInstance.valueInfo.objectTypeName" class="mb-3">
          <label class="form-label">{{ $t('process-instance.variables.objectTypeName') }}</label>
          <input type="text" class="form-control" :value="currentInstance.valueInfo.objectTypeName" disabled>
        </div>
        <div v-if="currentInstance.valueInfo.serializationDataFormat" class="mb-3">
          <label class="form-label">{{ $t('process-instance.variables.serializationDataFormat') }}</label>
          <input type="text" class="form-control" :value="currentInstance.valueInfo.serializationDataFormat" disabled>
        </div>
      </template>
      <b-form-group :label="$t('process-instance.variables.value')">
        <textarea
          class="form-control"
          :class="{ 'is-invalid': isNewValueInvalid }"
          rows="5"
          :placeholder="$t('process-instance.variables.enterValue')"
          v-model="formattedJsonValue"
          :disabled="selectedInstance.state === 'COMPLETED'">
        </textarea>
        <div v-if="isNewValueInvalid" class="invalid-feedback">
          {{ valueValidationError }}
        </div>
      </b-form-group>
    </div>
    <template v-slot:modal-footer>
      <b-button v-if="selectedInstance.state === 'COMPLETED'" @click="$refs.editVariableModal.hide()">{{ $t('confirm.close') }}</b-button>
      <template v-else>
        <b-button @click="$refs.editVariableModal.hide()" variant="link">{{ $t('confirm.cancel') }}</b-button>
        <b-button @click="updateVariable" variant="primary" :disabled="isNewValueInvalid">{{ $t('process-instance.save') }}</b-button>
      </template>
    </template>
  </b-modal>
</template>

<script>
import { ProcessService } from '@/services.js'
import { mapGetters, mapActions } from 'vuex'

export default {
  name: 'EditVariableModal',
  props: {
    selectedInstance: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      variableToModify: null,
      selectedVariable: null,
      valueValidationError: null
    }
  },
  computed: {
    ...mapGetters('variableInstance', ['currentVariableInstance']),
    ...mapGetters('historicVariableInstance', ['currentHistoricVariableInstance']),
    currentInstance() {
      // Use regular variable instance for active processes, historic for all others
      return this.selectedInstance?.state === 'ACTIVE'
        ? this.currentVariableInstance
        : this.currentHistoricVariableInstance
    },
    formattedJsonValue: {
      get: function() {
        if (this.variableToModify) {
          if (this.variableToModify.rawValue) {
            return this.variableToModify.rawValue
          }

          if (this.variableToModify.type === 'Json') {
            return JSON.stringify(JSON.parse(this.variableToModify.value), null, 2)
          } else if (this.variableToModify.type === 'Object') {
            return JSON.stringify(this.variableToModify.value, null, 2)
          } else return this.variableToModify.value
        }
        return ''
      },
      set: function(val) {
        this.validateJson(val)
        if (!this.valueValidationError) {
          this.variableToModify.rawValue = undefined
          this.variableToModify.value = this.variableToModify.type === 'Object' ? JSON.parse(val) : val
        }
        else {
          this.variableToModify.rawValue = val
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
    async show(variable) {
      this.selectedVariable = variable
      this.variableToModify = JSON.parse(JSON.stringify(variable))
      this.valueValidationError = null // Reset validation error
      // Try active, fallback to historic if error
      // Use regular variable instance for active processes, historic for all others
      if (this.selectedInstance?.state === 'ACTIVE') {
        try {
          await this.getVariableInstance({ id: variable.id, deserializeValue: false })
        } catch {
          await this.getHistoricVariableInstance({ id: variable.id, deserializeValue: false })
          // Note: We don't modify selectedInstance.state here as it's a prop
        }
      } else {
        await this.getHistoricVariableInstance({ id: variable.id, deserializeValue: false })
      }
      this.$refs.editVariableModal.show()
    },
    updateVariable: function() {
      if (this.variableToModify.type === 'Object' && this.variableToModify.valueInfo?.objectTypeName !== 'java.lang.StringBuilder') {
        const executionId = this.variableToModify.executionId
        const variableName = this.variableToModify.name

        var formData = new FormData()
        const jsonBlob = new Blob([this.variableToModify.value], { type: 'application/json' })
        formData.append('data', jsonBlob, 'blob')
        formData.append('valueType', 'Bytes')

        ProcessService.modifyVariableDataByExecutionId(executionId, variableName, formData).then(() => {
          this.selectedVariable.value = this.variableToModify.value
          this.$emit('variable-updated', this.selectedVariable)
          this.$refs.editVariableModal.hide()
        })
      }
      else {
        const original = this.variableToModify
        const data = { modifications: {} }
        // Clone the original value
        let value = original.value
        if (original.type === 'Json') {
          // JSON validation already done in setter, no need for try-catch here
          value = JSON.stringify(JSON.parse(value))
        } else if (original.type === 'Object') {
          if (typeof value !== 'string') {
            value = JSON.stringify(value)
          }
        }
        const mod = { value, type: original.type }
        // Handle StringBuilder special case
        const objectTypeName = original.valueInfo?.objectTypeName
        if (original.type === 'Object' && objectTypeName === 'java.lang.StringBuilder') {
          mod.value = JSON.stringify(value)
          mod.valueInfo = original.valueInfo
        }
        data.modifications[original.name] = mod
        ProcessService.modifyVariableByExecutionId(original.executionId, data).then(() => {
          this.selectedVariable.value = value
          this.$emit('variable-updated', this.selectedVariable)
          this.$refs.editVariableModal.hide()
        })
      }
    },
    validateJson(value) {
      if (!this.variableToModify || (!['Json', 'Object'].includes(this.variableToModify.type))) {
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
