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
  <option
    :value="modelValue"
    :disabled="disabled"
    :aria-disabled="disabled.toString()">
    <slot></slot>
  </option>
</template>

<script>
export default {
  props: {
    modelValue: {
      type: [String, Number, Object, Boolean],
      default: null
    },
    disabled: {
      type: Boolean,
      default: false
    }
  }
}
</script>

<style scoped>
</style>
