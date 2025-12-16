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
  <AddVariableModalUI ref="addVariableModalUI"
    :edit-mode="true"
    :disabled="historic"
    :loading="loading"
    :saving="saving"
    :error="error"
    :show-only-error="showOnlyError"

    @add-variable="saveVariable"
  ></AddVariableModalUI>
</template>

<script>
import AddVariableModalUI from '@/components/process/modals/AddVariableModalUI.vue'
import { ProcessService, VariableInstanceService, HistoricVariableInstanceService } from '@/services.js'

export default {
name: 'EditVariableModal',
emits: ['variable-updated'],
components: { AddVariableModalUI },
props: {
    historic: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      executionId: null,
      variableName: null,
      loading: true,
      saving: false,
      error: null,
      /**
       * show only error when load failed, but show error with controls when save failed
       */
      showOnlyError: false,
    }
  },
  computed: {
    isHistoricFetch() {
      return this.historic && this.$root.config.camundaHistoryLevel !== 'none'
    }
  },
  methods: {
    async show(variableId, variableName) {
      this.executionId = null
      this.variableName = variableName
      this.loading = true
      this.saving = false
      this.error = null
      this.showOnlyError = false

      this.$refs.addVariableModalUI.show(null, true)

      let variable = null
      await this.loadVariableInstance(variableId).then(res => {
        variable = res
      }).catch((error) => {
        variable = null
        this.showOnlyError = true
        this.error = this.$t(
          this.isHistoricFetch ? 'process-instance.variables.loadStatus.historicError' : 'process-instance.variables.loadStatus.runtimeError',
          { name: variableName || '?', id: variableId }) + ' ' + error.message
      })

      this.executionId = variable?.executionId
      this.$refs.addVariableModalUI.show({
        name: variable?.name || variableName,
        type: variable?.type || 'String',
        value: this.calcEditableValue(variable),
        valueSerialized: variable?.type === 'Object' ? variable?.valueSerialized : null,
        valueInfo: variable?.valueInfo || null,
      }, true)
      this.loading = false
    },

    async loadVariableInstance(variableId) {
      return this.isHistoricFetch ? HistoricVariableInstanceService.getHistoricVariableInstance(variableId, false) : VariableInstanceService.getVariableInstance(variableId, false)
    },

    async saveVariable(variable) {
      const isEditDeserializedValue = (variable.type === 'Object' &&
        // only for non-json serialized objects
        (variable.valueInfo?.serializationDataFormat !== 'application/json'))
      return isEditDeserializedValue ? this.updateVariableDeserialized(variable) : this.updateVariableSerialized(variable)
    },

    async updateVariableDeserialized(variable) {
      this.saving = true

      const formData = new FormData()
      const jsonBlob = new Blob([variable.value.toString()], { type: 'application/json' })
      formData.append('data', jsonBlob, 'blob')
      formData.append('valueType', variable.valueInfo?.objectTypeName || 'java.lang.Object')

      await ProcessService.modifyVariableDataByExecutionId(this.executionId, this.variableName, formData).then(() => {
        this.$refs.addVariableModalUI.hide()
        this.$emit('variable-updated')
      }).catch((error) => {
        this.error = error.message
        this.saving = false
      })
    },

    async updateVariableSerialized(variable) {
      this.saving = true

      const value = (variable.type === 'Null') ? null : (variable.value?.toString() || null)
      const data = { modifications: {} }
      data.modifications[this.variableName] = {
        value,
        type: variable.type,
        valueInfo: variable.valueInfo,
      }

      await ProcessService.modifyVariableByExecutionId(this.executionId, data).then(() => {
        this.$refs.addVariableModalUI.hide()
        this.$emit('variable-updated')
      }).catch((error) => {
        this.error = error.message
        this.saving = false
      })
    },

    calcEditableValue: function(variable) {
      if (!variable) {
        return null
      }

      if (variable.value === null || variable.value === undefined) {
        return null
      }

      // Format the original value based on its type
      switch (variable.type) {
        case 'String':
        case 'Integer':
        case 'Long':
        case 'Double':
          return variable.value.toString() // Convert primitive types to string
        case 'Boolean':
          return !!variable.value
        case 'Null':
          return null
        case 'Json': {
          try {
            return JSON.stringify(JSON.parse(variable.value), null, 2)
          } catch {
            return variable.value.toString() // Fallback to original value if parsing fails
          }
        }
        case 'Object': {

          // based on serialization format
          switch (variable.valueInfo?.serializationDataFormat) {
            case 'application/json': {
              const value = variable.valueSerialized
              if (typeof value === 'string') {
                try {
                  return JSON.stringify(JSON.parse(value), null, 2)
                } catch {
                  return value.toString() // Fallback to original value if parsing fails
                }
              }
              break
            }
          }

          // based on object type
          const value = variable.valueDeserialized
          const objectTypeName = variable.valueInfo?.objectTypeName
          switch (objectTypeName) {
            case 'java.lang.String':
            case 'java.lang.Integer':
            case 'java.lang.Long':
            case 'java.lang.Double':
            case 'java.lang.Boolean':
              return value.toString() // Convert primitive object types to string
            case 'java.util.Date':
              return value.toString()
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
              return JSON.stringify(value, null, 2) // Format collections as JSON
            // Handle StringBuilder specifically
            case 'java.lang.StringBuffer':
            case 'java.lang.StringBuilder':
              // If the value is a StringBuilder or StringBuffer, convert it to string
              return value.toString()
            default: {
              // For other object types, try to parse as JSON
              if (typeof value === 'string' && value.trim() === '') {
                return '' // Return empty string for empty values
              }

              const treatAsJson = (
                (typeof value === 'string' && value.startsWith('{') && value.endsWith('}')) ||
                (typeof value === 'string' && value.startsWith('[') && value.endsWith(']'))
              )

              if (treatAsJson) {
                try {
                  return JSON.stringify(JSON.parse(value), null, 2)
                } catch {
                  return value.toString() // Fallback to original value if parsing fails
                }
              }

              if (typeof value === 'object') {
                try {
                  return JSON.stringify(value, null, 2) // Format objects as JSON
                } catch {
                  return value.toString() // Fallback to original value if serialization fails
                }
              }

              return value.toString() // For any other object type, return the value directly
            }
          }
        }
        default:
          return variable.value.toString() // For any other type, return the value directly
      }
    },
  }
}
</script>
