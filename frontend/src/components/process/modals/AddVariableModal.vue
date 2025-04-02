<template>
  <b-modal ref="addVariable" :title="$t('process-instance.addVariable')" @hide="reset()">
    <div>
      <b-form-group>
        <template #label>{{ $t('process-instance.variables.name') }}*</template>
        <b-form-input v-model="variable.name"></b-form-input>
      </b-form-group>
      <b-form-group :label="$t('process-instance.variables.type')">
        <b-form-select v-model="variable.type" :options="types"></b-form-select>
      </b-form-group>
      <div v-if="variable.type === 'Object'">
        <b-form-group>
          <template #label>{{ $t('process-instance.variables.objectTypeName') }}*</template>
          <b-form-input v-model="variable.valueInfo.objectTypeName"></b-form-input>
        </b-form-group>
        <b-form-group>
          <template #label>{{ $t('process-instance.variables.serializationDataFormat') }}*</template>
          <b-form-input v-model="variable.valueInfo.serializationDataFormat"></b-form-input>
        </b-form-group>
      </div>
      <b-form-group>
        <template #label>{{ $t('process-instance.variables.value') }}*</template>
        <b-form-select v-if="variable.type === 'Boolean'" v-model="variable.value">
          <b-form-select-option :value="true">{{ $t('process.true') }}</b-form-select-option>
          <b-form-select-option :value="false">{{ $t('process.false') }}</b-form-select-option>
        </b-form-select>
        <b-form-input v-else-if="['Short', 'Integer', 'Long', 'Double'].includes(variable.type)"
          v-model="variable.value" type="number"></b-form-input>
        <b-form-datepicker v-else-if="variable.type === 'Date'" v-model="variable.value"></b-form-datepicker>
        <div v-else-if="variable.type === 'Null'"></div>
        <b-form-textarea v-else v-model="variable.value"></b-form-textarea>
      </b-form-group>
    </div>
    <template v-slot:modal-footer>
      <b-button @click="$refs.addVariable.hide()" variant="link">{{ $t('confirm.cancel') }}</b-button>
      <b-button :disabled="!isValid" @click="addVariable()" variant="primary">{{ $t('confirm.ok') }}</b-button>
    </template>
  </b-modal>
</template>

<script>
import moment from 'moment'
import { ProcessService } from '@/services.js'

export default {
  name: 'AddVariableModal',
  props: { selectedInstance: Object },
  data: function() {
    return {
      variable: { name: '', type: 'String', value: null, valueInfo: {} }
    }
  },
  watch: {
    'variable.type': function(type) {
      if (type === 'Boolean') {
        this.variable.value = true
      } else this.variable.value = null
    }
  },
  computed: {
    types: function() {
      return [
        { text: 'String', value: 'String' },
        { text: 'Boolean', value: 'Boolean' },
        { text: 'Short', value: 'Short' },
        { text: 'Integer', value: 'Integer' },
        { text: 'Long', value: 'Long' },
        { text: 'Double', value: 'Double' },
        { text: 'Date', value: 'Date' },
        { text: 'Null', value: 'Null' },
        { text: 'Object', value: 'Object' },
        { text: 'Json', value: 'Json' },
        { text: 'Xml', value: 'Xml' }
      ]
    },
    isValid: function() {
      if (this.variable.type === 'Null') return true
      if (!this.variable.name || !this.variable.value) return false
      const value = this.variable.value
      if (this.variable.type === 'Object') {
        return this.variable.valueInfo.objectTypeName && this.variable.valueInfo.serializationDataFormat
      } else if (this.variable.type === 'Short') {
        return value >= -32768 && value <= 32767
      } else if (this.variable.type === 'Integer') {
        return value >= -2147483648 && value <= 2147483647
      } else if (this.variable.type === 'Long') {
        return value >= -Number.MAX_SAFE_INTEGER && value <= Number.MAX_SAFE_INTEGER
      } else if (this.variable.type === 'Double') {
        return !isNaN(value)
      } else if (this.variable.type === 'Json') {
        try {
          JSON.parse(value)
          return true
          // eslint-disable-next-line no-unused-vars
        } catch(error) {
          return false
        }
      } else if (this.variable.type === 'Xml') {
        const parser = new DOMParser()
        const xmlDoc = parser.parseFromString(value, 'text/xml')
        return !xmlDoc.getElementsByTagName('parsererror').length
      }
      return true
    }
  },
  methods: {
    show: function() {
      this.$refs.addVariable.show()
    },
    addVariable: function() {
      var variable = JSON.parse(JSON.stringify(this.variable))
      if (variable.type === 'Date') variable.value = moment(variable.value).format('YYYY-MM-DDTHH:mm:ss.SSSZZ')
      if (variable.type !== 'Object') delete variable.valueInfo
      ProcessService.putLocalExecutionVariable(this.selectedInstance.id, variable.name, variable).then(() => {
        this.$emit('variable-added')
        this.$refs.addVariable.hide()
      })
    },
    reset: function() {
      this.variable = { name: '', type: 'String', value: null, valueInfo: {} }
    }
  }
}
</script>
