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
  <b-form-group label-cols="4" content-cols="8" :label="$t('login.password')" :invalid-feedback="$t('errors.invalid')">
    <input ref="input" id="input" :value="modelValue" :placeholder="placeholder" :type="show ? 'text' : 'password'" class="form-control rounded-right"
      style="padding-right: 2.5rem; position: relative; z-index: 1"
      :required="required" :disabled="disabled" :autocomplete="autocomplete" @input="handleInput">
    <b-button type="button" variant="link" size="sm" tabindex="0" @click="show = !show" @keydown.enter="show = !show" @keydown.space.prevent="show = !show" class="mdi mdi-18px mdi-eye text-secondary pt-0"
      :class="show ? 'mdi-eye-off' : 'mdi-eye'"
      :aria-label="show ? $t('login.hidePassword') : $t('login.showPassword')"
      style="position: absolute; right: 5px; top: 4px; z-index: 3; cursor: pointer"></b-button>
    <div class="invalid-feedback">{{ $t('errors.invalid') }}</div>
  </b-form-group>
</template>

<script>
export default {
  name: 'SecureInput',
  props: { modelValue: String, placeholder: String, required: Boolean, disabled: Boolean, autocomplete: String },
  emits: ['update:modelValue'],
  data: function() {
    return { show: false }
  },
  methods: {
    handleInput(event) {
      this.$emit('update:modelValue', event.target.value)
    }
  }
}
</script>
