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
  <div class="badge bg-info rounded-pill ps-0 pe-3 pt-0 pb-1 d-inline-flex align-items-center" :title="tooltip">

    <button @click="onRemove" class="btn btn-link p-0 m-0 ms-2 text-white px-1" :title="tooltipRemove">
      <small class="mdi mdi-close-thick" style="font-weight: 500; font-size: 0.75rem"></small>
    </button>

    <span style="font-weight: 500; font-size: 0.75rem" class="pt-1">
      {{ label }}
    </span>

  </div>
</template>

<script>
export default {
  name: 'RemovableBadge',
  emits: ['on-remove'],
  props: { 
    tooltipRemove: { type: String, required: true },
    label: { type: String, required: true },
    tooltip: { type: String, required: false }
  },
  methods: {
    onRemove(event) {
      this.$emit('on-remove', event)
    }
  }
}
</script>

<style lang="css" scoped>
.btn-link:focus {
  outline: 2px solid var(--bs-dark);
  outline-offset: 0px;
  box-shadow: none;
}
</style>
