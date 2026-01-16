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
  <router-link v-bind="$attrs" v-if="to" :to="to" :title="title" class="text-decoration-none rounded hovered">
    <span :class="'mdi mdi-18px ' + icon"></span>
  </router-link>
  <button v-else v-bind="$attrs" @click.stop="onClick" class="btn btn-link hovered" :title="title">
    <span :class="'mdi mdi-18px ' + icon"></span>
  </button>
</template>

<script>
export default {
  name: 'CellActionButton',
  emits: ['click'],
  props: { 
    title: { type: String, required: true },
    icon: { type: String, required: true },
    to: { type: [String, Object], default: undefined }
  },
  methods: {
    onClick(event) {
      if (!this.to) {
        this.$emit('click', event)
      }
    }
  }
}
</script>

<style lang="css" scoped>
.hovered {
  display: block;
  transition: background-color 0.3s ease;
  padding: 4px 8px;
  color: var(--bs-gray-800);
}
.hovered:hover {
  background-color: var(--bs-gray-500) !important;
  color: var(--bs-dark) !important;
}
.hovered:focus {
  background-color: var(--bs-gray-500) !important;
  color: var(--bs-dark) !important;
  outline: 2px solid var(--bs-dark);
  outline-offset: 0px;
  padding: 2px 6px;
  box-shadow: none;
  margin: 2px;
}
</style>
