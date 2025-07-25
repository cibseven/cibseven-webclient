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
          :class="{ 'is-invalid': valueValidationError !== null }"
          rows="5"
          :placeholder="$t('process-instance.variables.enterValue')"
          v-model="formattedValue"
          :disabled="disabled || saving">
        </textarea>
        <div v-if="valueValidationError" class="invalid-feedback">
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
            <b-button @click="updateVariable" variant="primary" :disabled="isSaveDisabled">{{ $t('process-instance.save') }}</b-button>
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
    valueValidationError() {
      if (!this.variable) {
        return null
      }

      if (!this.formattedValue || this.formattedValue.trim() === '') {
        return null
      }

      switch (this.variable.type) {
        case 'String':
        case 'Integer':
        case 'Long':
        case 'Double':
        case 'Boolean':
          // No specific validation needed for these types
          return null
        case 'Json': {
          // JSON validation
          try {
            JSON.parse(this.formattedValue)
            return null
          } catch (e) {
            return e.message
          }
        }
        case 'Object': {
          // Object validation
          try {
            const parsedValue = JSON.parse(this.formattedValue)
            if (typeof parsedValue !== 'object' || parsedValue === null) {
              return 'Invalid object format. Expected a JSON object.'
            }
            return null
          } catch (e) {
            return e.message
          }
        }
        default:
          // For any other type, we assume the value is valid
          return null
      }
    },
    formattedValue: {
      get: function() {
        if (!this.variable) {
          return ''
        }

        if (this.newValue !== null) {
          // If newValue is set, return it directly
          return this.newValue
        }

        // Format the value based on its type
        switch (this.variable.type) {
          case 'String':
          case 'Integer':
          case 'Long':
          case 'Double':
          case 'Boolean':
            return this.variable.value.toString() // Convert primitive types to string
          case 'Json': {
            try {
              return JSON.stringify(JSON.parse(this.variable.value), null, 2)
            } catch {
              return this.variable.value.toString() // Fallback to original value if parsing fails
            }
          }
          case 'Object': {
            const objectTypeName = this.variable.valueInfo?.objectTypeName
            switch (objectTypeName) {
              case 'java.lang.String':
              case 'java.lang.Integer':
              case 'java.lang.Long':
              case 'java.lang.Double':
              case 'java.lang.Boolean':
                return this.variable.value.toString() // Convert primitive object types to string
              case 'java.util.Date':
                return this.variable.value.toString()
              case 'java.util.List':
              case 'java.util.Map':
              case 'java.util.ArrayList':
              case 'java.util.HashMap':
              case 'java.util.Set':
              case 'java.util.Collection':
              case 'java.util.LinkedList':
              case 'java.util.HashSet':
              case 'java.util.TreeSet':
              case 'java.util.Vector':
              case 'java.util.Stack':
                return JSON.stringify(this.variable.value, null, 2) // Format collections as JSON
              // Handle StringBuilder specifically
              case 'java.lang.StringBuffer':
              case 'java.lang.StringBuilder':
                // If the value is a StringBuilder or StringBuffer, convert it to string
                return this.variable.value.toString()
              default: {
                // For other object types, try to parse as JSON
                if (typeof this.variable.value === 'string' && this.variable.value.trim() === '') {
                  return '' // Return empty string for empty values
                }
                if (typeof this.variable.value === 'string' && this.variable.value.startsWith('{') && this.variable.value.endsWith('}')) {
                  // If the value is a JSON object, parse and format it
                  try {
                    return JSON.stringify(JSON.parse(this.variable.value), null, 2)
                  } catch {
                    return this.variable.value.toString() // Fallback to original value if parsing fails
                  }
                }
                if (typeof this.variable.value === 'string' && this.variable.value.startsWith('[') && this.variable.value.endsWith(']')) {
                  // If the value is a JSON array
                  try {
                    return JSON.stringify(JSON.parse(this.variable.value), null, 2)
                  } catch {
                    return this.variable.value.toString() // Fallback to original value if parsing fails
                  }
                }
              }
            }
            return this.variable.value.toString() // For any other object type, return the value directly
          }
          default:
            return this.variable.value.toString() // For any other type, return the value directly
        }
      },
      set: function(val) {
        this.newValue = val
      }
    },
    isSaveDisabled() {
      if (!this.variable) {
        return true // Disable save if variable is not loaded
      }
      if (this.disabled) {
        return true // Disable save if modal is in disabled state
      }
      if (this.saving) {
        return true // Disable save if already saving
      }
      if (this.newValue === null) {
        return true // Disable save if newValue is not set
      }
      if (this.valueValidationError !== null) {
        return true // Disable save if there is a validation error
      }
      return false
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
      const deserializeValue = true
      if (!this.disabled) {
        try {
          await this.getVariableInstance({ id: variableId, deserializeValue })
        } catch {
          await this.getHistoricVariableInstance({ id: variableId, deserializeValue })
        }
      } else {
        await this.getHistoricVariableInstance({ id: variableId, deserializeValue })
      }
      this.loading = false
    },
    updateVariable: async function() {
      this.saving = true

      const value = this.formattedValue.toString()
      const data = { modifications: {} }
      data.modifications[this.variable.name] = {
        value,
        type: this.variable.type,
        valueInfo: this.variable.valueInfo
      }

      try {
        await ProcessService.modifyVariableByExecutionId(this.variable.executionId, data).then(() => {
          this.$emit('variable-updated')
          this.$refs.editVariableModal.hide()
        })
      }
      finally {
        this.saving = false
      }
    },
    clearVariableInstances() {
      this.clearVariableInstance()
      this.clearHistoricVariableInstance()
    }
  }
}
</script>
