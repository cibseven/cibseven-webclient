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
  <form @submit.prevent="onSubmit" :class="{ 'was-validated': showValidation }" ref="form" novalidate>
    <slot :showValidation="showValidation"></slot>
  </form>
</template>

<script>
export default {
  name: 'CIBForm',
  data: function() {
    return { showValidation: false }
  },
  methods: {
    onSubmit: function() {
      if (this.$refs.form.checkValidity()) {
        this.showValidation = false
        this.$emit('submitted')
        return true
      } else {
        this.showValidation = true
        this.$emit('fail')
        return false
      }
    }
  }
}
</script>
