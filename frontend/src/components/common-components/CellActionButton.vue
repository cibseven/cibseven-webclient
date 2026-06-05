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
  <router-link v-bind="$attrs" v-if="to" :to="to" :title="title" class="text-decoration-none rounded hovered-link">
    <span v-if="icon" :class="'mdi mdi-18px ' + icon"></span>
    <slot></slot>
  </router-link>
  <button v-else v-bind="$attrs" @click.stop="onClick" class="btn btn-link hovered-button" :title="title">
    <span v-if="icon" :class="'mdi mdi-18px ' + icon"></span>
    <slot></slot>
  </button>
</template>

<script>
export default {
  name: 'CellActionButton',
  emits: ['click'],
  props: { 
    title: { type: String, required: true },
    icon: { type: String, required: false, default: undefined },
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
/* common styles for both link and button */

.hovered-link,
.hovered-button {
  display: block;
  transition: background-color 0.3s ease;
  color: var(--bs-gray-800);
}
.hovered-link:hover,
.hovered-button:hover  {
  background-color: var(--bs-gray-500) !important;
  color: var(--bs-dark) !important;
}
.hovered-link:focus,
.hovered-link:focus-visible,
.hovered-button:focus,
.hovered-button:focus-visible {
  background-color: var(--bs-gray-500) !important;
  color: var(--bs-dark) !important;
  outline: 2px solid var(--bs-dark);
  outline-offset: 0px !important;
}

/* Specific styles for link and button to prevent layout shift on focus */

.hovered-link {
  margin: 0px;
  padding: 4px 8px;
}
.hovered-link:focus {
  margin: 0px 2px;
  padding: 0px 6px;
}

.hovered-button {
  margin: 0px;
  padding: 4px 8px;
}
.hovered-button:focus {
  margin: 2px;
  padding: 0px 6px;
}

</style>
