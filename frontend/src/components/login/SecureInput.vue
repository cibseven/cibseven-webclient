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
  <div class="input-group">
    <input ref="input" :value="modelValue" :placeholder="placeholder" :type="show ? 'text' : 'password'" class="form-control rounded-right pr-5"
      :required="required" :disabled="disabled" :autocomplete="autocomplete" @input="$emit('update:modelValue', $event.target.value)">
    <span @click="show = !show" class="mdi mdi-18px mdi-eye text-secondary"
    style="position: absolute; right: 11px; top: 4px; z-index: 3; cursor: pointer"></span>
    <div class="invalid-feedback">{{ $t('errors.invalid') }}</div>
  </div>
</template>

<script>
export default {
  name: 'SecureInput',
  props: { modelValue: String, placeholder: String, required: Boolean, disabled: Boolean, autocomplete: String },
  emits: ['update:modelValue'],
  data: function() {
    return { show: false }
  }
}
</script>
