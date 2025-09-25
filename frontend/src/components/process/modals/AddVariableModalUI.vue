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

    <div v-if="!error">

      <!-- Name -->
      <b-form-group>
        <template #label>{{ $t('process-instance.variables.name') }}*</template>
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
        <template #label v-if="!editMode || (type !== 'Object')">{{ $t('process-instance.variables.value') }}<span v-if="type != 'Boolean'">*</span></template>

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
        <b-form-datepicker v-else-if="type === 'Date'"
          ref="dateValue"
          v-model="value"
          :disabled="disabled || saving || loading"></b-form-datepicker>

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

    <div v-if="error" class="alert alert-danger text-danger d-flex align-items-center">
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
          <b-button v-if="disabled" @click="$refs.addVariable.hide()">{{ $t('confirm.close') }}</b-button>
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
      else if (type === 'Null') {
        this.value = null
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
      if (this.value === null) return null

      if (['Short', 'Integer', 'Long', 'Double'].includes(this.type)) {
        if (isNaN(this.value)) {
          return 'isNaN'
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
        if (this.value.trim() === '') {
          return false
        }
        // JSON validation
        try {
          JSON.parse(this.value)
          return null
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
      else if (this.type === 'Object') {
        if (this.value.trim() === '') {
          return false
        }
        // Object validation
        try {
          const parsedValue = JSON.parse(this.value)
          if (typeof parsedValue !== 'object' || parsedValue === null) {
            return 'Invalid object format. Expected a JSON object.'
          }
          return null
        } catch (e) {
          return e.message
        }
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
      if (!this.isValid) {
        return true // Disable save if form is not valid
      }
      return false // Enable save
    }
  },
  methods: {
    show: function(variable = {}) {
      this.reset()

      if (variable !== null && Object.keys(variable).length > 0) {
        const {
          name = '',
          type = 'String',
          value = '',
          valueSerialized = null,
          objectTypeName = '',
          serializationDataFormat = ''
        } = variable;

        this.name = name
        this.type = type

        this.$nextTick(() => {
          this.value = value
          this.valueSerialized = valueSerialized
          this.objectTypeName = objectTypeName
          this.serializationDataFormat = serializationDataFormat
        })
      }

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
            case 'Date':
              this.$refs.dateValue?.focus()
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
    reset: function() {
      this.name = ''
      this.nameFocused = 0
      this.isNameFocused = true
      this.type = 'String'
      this.value = ''
      this.valueSerialized = null
      this.objectTypeName = ''
      this.serializationDataFormat = ''
    }
  }
}
</script>
