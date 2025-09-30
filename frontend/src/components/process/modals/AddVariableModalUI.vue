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
  <b-modal ref="addVariable"
    :title="computedTitle"
    @hide="reset()"
    @shown="onShown">

    <div v-if="loading && !error">
      <p class="text-center p-4"><BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('admin.loading') }}</p>
    </div>

    <div v-else-if="!showOnlyError">

      <!-- Name -->
      <b-form-group>
        <template #label>{{ $t('process-instance.variables.name') }}<span v-if="!editMode">*</span></template>
        <b-form-input ref="variableName" v-model="name" autofocus
          @focus="isNameFocused = true, nameFocused++"
          @blur="isNameFocused = false"
          :disabled="editMode || disabled || saving || loading"
          :class="{ 'is-invalid': !name && (!isNameFocused || nameFocused > 2) }"
        ></b-form-input>
      </b-form-group>

      <!-- Type -->
      <b-form-group :label="$t('process-instance.variables.type')">
        <b-form-select v-model="type" :options="types"  class="mb-0" :disabled="disabled || saving || loading"></b-form-select>
      </b-form-group>

      <!-- Object Type Name -->
      <div v-if="type === 'Object'">
        <b-form-group>
          <template #label>{{ $t('process-instance.variables.objectTypeName') }}*</template>
          <b-form-input v-model="objectTypeName" :disabled="disabled || saving || loading"></b-form-input>
        </b-form-group>
        <b-form-group>
          <template #label>{{ $t('process-instance.variables.serializationDataFormat') }}*</template>
          <b-form-input v-model="serializationDataFormat" :disabled="disabled || saving || loading"></b-form-input>
        </b-form-group>
      </div>

      <!-- Value -->
      <b-form-group class="p-0 mb-0" v-if="type !== 'Null'">

        <!-- Label -->
        <template #label v-if="!editMode || (type !== 'Object')">{{ $t('process-instance.variables.value') }}<span v-if="type != 'Boolean' && !disabled">*</span></template>
        <p v-if="value === null"><small class="form-text text-muted">Null value is specified.</small></p>

        <!-- Input: Boolean -->
        <div v-if="type === 'Boolean'" class="d-flex justify-content-end">
          <span class="me-2">{{ value ? $t('process.true') : $t('process.false') }}</span>
          <b-form-checkbox
            v-model="value" switch
            :title="value ? $t('process.true') : $t('process.false')"
            :disabled="disabled || saving || loading"></b-form-checkbox>
        </div>

        <!-- Input: Number Types -->
        <b-form-input v-else-if="['Short', 'Integer', 'Long', 'Double'].includes(type)"
          ref="numberValue"
          v-model="value" type="number" :class="{ 'is-invalid': valueValidationError !== null }"
          :disabled="disabled || saving || loading"></b-form-input>

        <!-- Input: Date -->
        <template v-else-if="type === 'Date'">
          <b-form-input v-if="disabled"
            v-model="value" type="text" :class="{ 'is-invalid': valueValidationError !== null }"
            :disabled="true"></b-form-input>
          <b-form-datepicker v-else
            v-model="value"
            :disabled="disabled || saving || loading"></b-form-datepicker>
        </template>

        <!-- Input: Null -->
        <div v-else-if="type === 'Null'"></div>

        <!-- Input: Object (only in edit mode) -->
        <b-tabs v-else-if="editMode && type === 'Object'" :activeTab="1">
          <b-tab id="1" :title="$t('process-instance.variables.value')">
            <textarea
              ref="textValue"
              class="form-control mt-2"
              :class="{ 'is-invalid': valueValidationError !== null }"
              rows="5"
              :placeholder="$t('process-instance.variables.enterValue')"
              v-model="value"
              :disabled="disabled || saving || loading">
            </textarea>
          </b-tab>
          <b-tab id="2" :title="$t('process-instance.variables.valueSerialized')">
            <textarea
              class="form-control mt-2"
              rows="5"
              v-model="valueSerialized"
              :disabled="true"></textarea>
          </b-tab>
        </b-tabs>

        <!-- Input: String, Json, Xml, Object -->
        <textarea v-else
          ref="textValue"
          class="form-control"
          :class="{ 'is-invalid': valueValidationError !== null }"
          rows="5"
          :placeholder="$t('process-instance.variables.enterValue')"
          :disabled="disabled || saving || loading"
          v-model="value">
        </textarea>

        <!-- Validation Error -->
        <div v-if="valueValidationError" class="invalid-feedback">
          {{ valueValidationError }}
        </div>
      </b-form-group>
    </div>

    <div v-if="error" class="text-danger d-flex align-items-center" :class="showOnlyError ? '' : 'alert alert-danger mt-4 mb-2'">
      <div class="me-4">
          <span class="mdi-36px mdi mdi-alert-octagon-outline text-danger"></span>
      </div>
      <div>
        <p class="ms-0">{{ error }}</p>
      </div>
    </div>

    <template v-slot:modal-footer>
      <div class="row w-100 me-0">
        <div class="col-2 p-0">
          <BWaitingBox v-if="saving" class="d-inline me-2" styling="width: 30px"></BWaitingBox>
        </div>
        <div class="col-10 p-0 d-flex justify-content-end pe-1">
          <b-button v-if="disabled || showOnlyError" @click="$refs.addVariable.hide()">{{ $t('confirm.close') }}</b-button>
          <template v-else>
            <b-button @click="$refs.addVariable.hide()" variant="link">{{ $t('confirm.cancel') }}</b-button>
            <b-button :disabled="isSubmitDisabled" @click="onSubmit" variant="primary">{{ computedSubmitButtonText }}</b-button>
          </template>
        </div>
      </div>
    </template>
  </b-modal>
</template>

<script>
import { moment } from '@/globals.js'
import { BWaitingBox } from 'cib-common-components'

export default {
  name: 'AddVariableModalUI',
  components: { BWaitingBox },
  props: {
    editMode: {
      type: Boolean,
      default: false
    },
    disabled: {
      type: Boolean,
      default: false
    },
    loading: {
      type: Boolean,
      default: false
    },
    saving: {
      type: Boolean,
      default: false
    },
    error: {
      type: String,
      default: ''
    },
    showOnlyError: {
      type: Boolean,
      default: false
    }
  },
  emits: [ 'add-variable' ],
  data: function() {
    return {
      name: '',
      nameFocused: 0,
      isNameFocused: true,
      type: 'String',
      value: '',
      valueSerialized: null,
      objectTypeName: '',
      serializationDataFormat: '',
    }
  },
  watch: {
    'type': function(type) {
      if (type === 'Boolean') {
        this.value = true
      }
      else if (type === 'Date') {
        this.value = this.currentDate()
      }
      else if (type === 'String') {
        if (this.value == null) {
          this.value = ''
        }
        else if (this.value === 0 || this.value === 0.0 || this.value === false || this.value === true) {
          this.value = ''
        }
        else {
          this.value = '' + this.value
        }
      }
      else if (['Short', 'Integer', 'Long'].includes(type)) {
        if (this.value == null || this.value === '' || isNaN(this.value) || isNaN(Number(this.value))) {
          this.value = 0
        }
        else {
          this.value = Math.trunc(Number(this.value))
        }
      }
      else if ('Double' === type) {
        if (this.value == null || this.value === '' || isNaN(this.value) || isNaN(Number(this.value))) {
          this.value = 0
        }
        else {
          this.value = Number(this.value)
        }
      }
      else if (type === 'Null') {
        this.value = null
      }
      else if (type === 'Json') {
        if (typeof this.value === 'string' && this.verifyJson(this.value) === null) {
          try {
            const obj = JSON.parse(this.value)
            this.value = JSON.stringify(obj, null, 2)
          } catch {
            this.value = '{}'
          }
        }
        else {
          this.value = '{}'
        }
      }
      else if (type === 'Object') {
        if (this.value === null || this.value === 0 || this.value === 0.0 || this.value === false || this.value === true) {
          this.value = ''
        }
        else if (typeof this.value === 'string' && this.verifyJson(this.value) === null) {
          try {
            const obj = JSON.parse(this.value)
            this.value = JSON.stringify(obj, null, 2)

            if (!this.objectTypeName) {
              if (obj instanceof Array) this.objectTypeName = 'java.util.List'
              if (obj instanceof Object) this.objectTypeName = 'java.util.Map'
              else this.objectTypeName = ''
            }

            if (!this.serializationDataFormat && this.objectTypeName) {
              this.serializationDataFormat = 'application/json'
            }
          } catch {
            this.value = '' + this.value
          }
        }
        else {
          this.value = '' + this.value
        }
      }
      else if (type === 'Xml') {
        this.value = ''
      }
      else this.value = ''
    }
  },
  computed: {
    computedTitle: function() {
      return this.editMode ? this.$t('process-instance.edit') : this.$t('process-instance.addVariable')
    },
    computedSubmitButtonText: function() {
      return this.editMode ? this.$t('process-instance.save') : this.$t('process-instance.addVariable')
    },
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
    valueValidationError: function() {
      if (this.value === undefined) return 'Value is required'

      switch (this.type) {
        case 'Boolean': {
          if (this.value === null) return null
          if (typeof this.value !== 'boolean') {
            return 'Invalid boolean value'
          }
          return null
        }
        case 'Date': {
          if (this.value === null) return null
          if (typeof this.value !== 'string') {
            return 'Invalid string value'
          }
          if (this.value.trim() === '') {
            return null
          }
          if (!moment(this.value, moment.ISO_8601, true).isValid()) {
            return 'Invalid date format'
          }
          return null
        }
        case 'String': {
          if (this.value === null) return null
          if (typeof this.value !== 'string') {
            return 'Invalid string value'
          }
          return null
        }
        case 'Short':
        case 'Integer':
        case 'Long':
        case 'Double': {
          if (this.value === null) return null
          if (this.value === '' || isNaN(this.value) || isNaN(Number(this.value))) {
            return 'Invalid number'
          }

          if (this.type === 'Short') {
            if (this.value < -32768 || this.value > 32767) {
              return 'Out of range: -32768 ... 32767'
            }
            // if floating point number
            if (!Number.isInteger(Number(this.value))) {
              return 'Invalid Short value: must be an short integer'
            }
          }
          else if (this.type === 'Integer') {
            if (this.value < -2147483648 || this.value > 2147483647) {
              return 'Out of range: -2147483648 ... 2147483647'
            }
            // if floating point number
            if (!Number.isInteger(Number(this.value))) {
              return 'Invalid Integer value: must be an integer'
            }
          }
          else if (this.type === 'Long') {
            if (this.value < -Number.MAX_SAFE_INTEGER || this.value > Number.MAX_SAFE_INTEGER) {
              return 'Out of range: -' + Number.MAX_SAFE_INTEGER + ' ... ' + Number.MAX_SAFE_INTEGER
            }
            // if floating point number
            if (!Number.isInteger(Number(this.value))) {
              return 'Invalid Long value: must be an Long integer'
            }
          }

          return null
        }
        case 'Null': {
          if (this.value === null) return null
          return this.value === null ? null : 'Value must be null'
        }
        case 'Object': {
          if (!this.objectTypeName ||
              this.objectTypeName.toString().trim().length === 0 ||
              !this.serializationDataFormat ||
              this.serializationDataFormat.toString().trim().length === 0) {
            return 'Object Type Name and Serialization Data Format are required'
          }

          if (this.value === null) return null

          // based on serialization format
          switch (this.serializationDataFormat) {
            case 'application/json': {
              return this.verifyJson(this.value)
            }
          }

          if (this.value.trim() === '') {
            return null
          }

          return null
        }
        case 'Json': {
          return this.verifyJson(this.value)
        }
        case 'Xml': {
          if (this.value === null) return null
          if (this.value.trim() === '') {
            return null
          }

          const parser = new DOMParser()
          const xmlDoc = parser.parseFromString(this.value, 'text/xml')
          const error = xmlDoc.getElementsByTagName('parsererror')
          if (error?.length > 0) {
            return error[0].textContent || error[0].innerText
          }
          break
        }
        default:
          return 'Unknown type: ' + this.type
      }

      return null
    },
    isSubmitDisabled: function() {
      if (this.disabled) {
        return true // Disable save if modal is in disabled state
      }
      if (this.saving || this.loading) {
        return true // Disable save if already saving or loading
      }
      if (!this.name) {
        return true // Disable save if name is empty
      }
      if (this.valueValidationError !== null) {
        return true // Disable save if form is not valid
      }
      return false // Enable save
    }
  },
  methods: {
    currentDate: function() {
      return moment(new Date()).startOf('day').format('YYYY-MM-DDTHH:mm:ss.SSSZZ')
    },

    verifyJson: function(value) {
      if (value === null) return null
      if (typeof value !== 'string') {
        return 'Invalid JSON value'
      }
      if (value.trim() === '') {
        return null
      }      
      try {
        JSON.parse(value)
        return null
      } catch (e) {
        return e.message
      }
    },

    reset: function() {
      this.name = ''
      this.nameFocused = 0
      this.isNameFocused = true
      this.type = 'String'
      this.value = ''
      this.valueSerialized = null
      this.objectTypeName = ''
      this.serializationDataFormat = ''
    },

    setData: function(variable = {}) {
      if (variable !== null && Object.keys(variable).length > 0) {
        const {
          name = '',
          type = 'String',
          valueSerialized = null,
          objectTypeName = '',
          serializationDataFormat = ''
        } = variable;

        this.name = name
        this.type = type

        this.$nextTick(() => {
          // wait until type watcher sets correct default value
          // and then override with provided value

          if (variable?.value === null || variable?.value === undefined || !Object.prototype.hasOwnProperty.call(variable, 'value')) {
            this.value = null
          }
          else {
            this.value = variable?.value
          }

          this.valueSerialized = valueSerialized
          this.objectTypeName = objectTypeName
          this.serializationDataFormat = serializationDataFormat
        })
      }
    },

    show: function(variable = {}) {
      this.reset()
      this.setData(variable)
      this.$refs.addVariable.show()
    },

    hide: function() {
      this.$refs.addVariable.hide()
    },

    onShown: function() {
      this.$nextTick(() => {
        if (this.error) return
        if (this.editMode) {
          switch (this.type) {
            case 'Short':
            case 'Integer':
            case 'Long':
            case 'Double':
              this.$refs.numberValue?.focus()
              break
            case 'String':
            case 'Json':
            case 'Xml':
            case 'Object':
              this.$refs.textValue?.focus()
              break
          }
        }
        else {
          this.$refs.variableName?.focus()
        }
      })
    },

    onSubmit: function() {
      var variable = {
        name: this.name,
        type: this.type,
        value: this.value,
        valueInfo: {
          objectTypeName: this.objectTypeName,
          serializationDataFormat: this.serializationDataFormat,
        }
      }
      if (variable.type === 'Null') variable.value = null
      if (variable.type === 'Date') variable.value = moment(variable.value).format('YYYY-MM-DDTHH:mm:ss.SSSZZ')
      if (variable.type !== 'Object') delete variable.valueInfo

      // minimize value
      if (variable.type === 'Json') {
        variable.value = JSON.stringify(JSON.parse(variable.value))
      }

      this.$emit('add-variable', variable)
    },
  }
}
</script>
