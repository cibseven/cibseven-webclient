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
  <div v-bind="$attrs" class="p-0 mt-3 bg-white border rounded shadow-sm">
    <div class="d-flex justify-content-between align-items-center px-3 pt-3">
      <h5 class="mb-0">
        {{ title }}
        <span v-if="info" v-b-popover.hover.right="info" class="mdi mdi-18px mdi-information-outline text-info"></span>
      </h5>
      <div>
        <slot name="actions"></slot>
      </div>
    </div>
    <hr class="mb-0">
    <div class="row m-3">
      <slot></slot>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ContentBlock',
  props: {
    title: String,
    info: String
  },
}
</script>
