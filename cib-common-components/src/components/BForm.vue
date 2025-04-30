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
  <form
    :class="formClasses"
    @submit="handleSubmit"
    :novalidate="novalidate"
    role="form"
    :aria-describedby="ariaDescribedby">
    <slot></slot>
  </form>
</template>

<script>
export default {
  props: {
    novalidate: { type: Boolean, default: false },
    formClass: { type: String, default: '' },
    ariaDescribedby: { type: String, default: null }
  },
  computed: {
    formClasses: function () {
      return ['needs-validation', this.formClass]
    }
  },
  methods: {
    handleSubmit: function (event) {
      if (!this.novalidate && !event.target.checkValidity()) {
        event.preventDefault()
        event.stopPropagation()
        this.$emit('validation-failed', event)
      } else {
        this.$emit('submit', event)
      }
      event.target.classList.add('was-validated')
    }
  }
}
</script>

<style scoped>
</style>
