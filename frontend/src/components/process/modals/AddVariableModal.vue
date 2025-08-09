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
  <b-modal ref="addVariable" :title="$t('process-instance.addVariable')" @hide="reset()" @shown="$refs.variableName.focus()">
    <div>
      <b-form-group>
        <template #label>{{ $t('process-instance.variables.name') }}*</template>
        <b-form-input ref="variableName" v-model="name" autofocus
          @focus="isNameFocused = true, nameFocused++"
          @blur="isNameFocused = false"
          :class="{ 'is-invalid': !name && (!isNameFocused || nameFocused > 2) }"
        ></b-form-input>
      </b-form-group>
      <b-form-group :label="$t('process-instance.variables.type')">
        <b-form-select v-model="type" :options="types"  class="mb-0"></b-form-select>
      </b-form-group>
      <div v-if="type === 'Object'">
        <b-form-group>
          <template #label>{{ $t('process-instance.variables.objectTypeName') }}*</template>
          <b-form-input v-model="objectTypeName"></b-form-input>
        </b-form-group>
        <b-form-group>
          <template #label>{{ $t('process-instance.variables.serializationDataFormat') }}*</template>
          <b-form-input v-model="serializationDataFormat"></b-form-input>
        </b-form-group>
      </div>
      <b-form-group class="p-0 mb-0">
        <template #label>{{ $t('process-instance.variables.value') }}<span v-if="type != 'Boolean'">*</span></template>
        <div v-if="type === 'Boolean'" class="d-flex justify-content-end">
          <span class="me-2">{{ value ? $t('process.true') : $t('process.false') }}</span>
          <b-form-checkbox v-model="value" switch :title="value ? $t('process.true') : $t('process.false')"></b-form-checkbox>
        </div>
        <b-form-input v-else-if="['Short', 'Integer', 'Long', 'Double'].includes(type)"
          v-model="value" type="number" :class="{ 'is-invalid': valueValidationError !== null }"></b-form-input>
        <b-form-datepicker v-else-if="type === 'Date'" v-model="value"></b-form-datepicker>
        <div v-else-if="type === 'Null'"></div>
        <textarea v-else
          class="form-control"
          :class="{ 'is-invalid': valueValidationError !== null }"
          rows="5"
          :placeholder="$t('process-instance.variables.enterValue')"
          v-model="value">
        </textarea>
        <div v-if="valueValidationError" class="invalid-feedback">
          {{ valueValidationError }}
        </div>
      </b-form-group>
    </div>
    <template v-slot:modal-footer>
      <b-button @click="$refs.addVariable.hide()" variant="link">{{ $t('confirm.cancel') }}</b-button>
      <b-button :disabled="!isValid" @click="addVariable()" variant="primary">{{ $t('process-instance.addVariable') }}</b-button>
    </template>
  </b-modal>
</template>

<script>
import { moment } from '@/globals.js'
import { ProcessService } from '@/services.js'

export default {
  name: 'AddVariableModal',
  props: { selectedInstance: Object },
  data: function() {
    return {
      name: '',
      nameFocused: 0,
      isNameFocused: true,
      type: 'String',
      value: '',
      objectTypeName: '',
      serializationDataFormat: ''
    }
  },
  watch: {
    'type': function(type) {
      if (type === 'Boolean') {
        this.value = true
      }
      else if (['Short', 'Integer', 'Long', 'Double'].includes(type)) {
        if (this.value == null || this.value === '' || isNaN(this.value) || isNaN(Number(this.value))) {
          this.value = 0
        }
        else {
          this.value = Number(this.value)
        }
      }
      else if (type === 'Date') {
        this.value = moment(new Date()).startOf('day').format('YYYY-MM-DDTHH:mm:ss.SSSZZ')
      }
      else if (type === 'Json') {
        this.value = '{}'
      }
      else if (type === 'String') {
        if (this.value == null) {
          this.value = ''
        }
        else {
          this.value = '' + this.value
        }
      }
      else this.value = ''
    }
  },
  computed: {
    types: function() {
      return [
        {
          label: 'Basic',
          options: [
            { text: 'Boolean', value: 'Boolean' },
            { text: 'Date', value: 'Date' },
            { text: 'String', value: 'String' },
          ]
        },
        {
          label: 'Numbers',
          options: [
            { text: 'Short', value: 'Short' },
            { text: 'Integer', value: 'Integer' },
            { text: 'Long', value: 'Long' },
            { text: 'Double', value: 'Double' },
          ]
        },
        {
          label: 'Objects',
          options: [
            { text: 'Null', value: 'Null' },
            { text: 'Object', value: 'Object' },
            { text: 'Json', value: 'Json' },
            { text: 'Xml', value: 'Xml' }
          ]
        },
      ]
    },
    isValid: function() {
      if (this.type === 'Null') return true
      if (this.type === 'Boolean') return !!this.name
      if (!this.name || this.value == null) return false
      if (this.type === 'Object') {
        return this.objectTypeName.length > 0 && this.serializationDataFormat.length > 0
      }
      return this.valueValidationError == null
    },
    valueValidationError: function() {
      if (['Short', 'Integer', 'Long', 'Double'].includes(this.type)) {
        if (isNaN(this.value)) {
          return 'isNan'
        }

        if (this.type === 'Short') {
          if (this.value < -32768 || this.value > 32767) {
            return 'Out of range: -32768 ... 32767'
          }
        }
        else if (this.type === 'Integer') {
          if (this.value < -2147483648 || this.value > 2147483647) {
            return 'Out of range: -2147483648 ... 2147483647'
          }
        }
        else if (this.type === 'Long') {
          if (this.value < -Number.MAX_SAFE_INTEGER || this.value > Number.MAX_SAFE_INTEGER) {
            return 'Out of range: -' + Number.MAX_SAFE_INTEGER + ' ... ' + Number.MAX_SAFE_INTEGER
          }
        }
      }
      else if (this.type === 'Json') {
        try {
          JSON.parse(this.value)
        } catch (e) {
          return e.message
        }
      }
      else if (this.type === 'Xml') {
        const parser = new DOMParser()
        const xmlDoc = parser.parseFromString(this.value, 'text/xml')
        const error = xmlDoc.getElementsByTagName('parsererror')
        if (error?.length > 0) {
          return error[0].textContent || error[0].innerText
        }
      }

      return null
    }
  },
  methods: {
    show: function() {
      this.reset()
      this.$refs.addVariable.show()
    },
    addVariable: function() {
      var variable = {
        name: this.name,
        type: this.type,
        value: this.value,
        valueInfo: {
          objectTypeName: this.objectTypeName,
          serializationDataFormat: this.serializationDataFormat,
        }
      }
      if (variable.type === 'Date') variable.value = moment(variable.value).format('YYYY-MM-DDTHH:mm:ss.SSSZZ')
      if (variable.type !== 'Object') delete variable.valueInfo
      ProcessService.putLocalExecutionVariable(this.selectedInstance.id, variable.name, variable).then(() => {
        this.$emit('variable-added')
        this.$refs.addVariable.hide()
      })
    },
    reset: function() {
      this.name = ''
      this.nameFocused = 0
      this.isNameFocused = true
      this.type = 'String'
      this.value = ''
      this.objectTypeName = ''
      this.serializationDataFormat = ''
    }
  }
}
</script>
