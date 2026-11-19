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
/* "Simple icon" for both link and button: $secondary, $dark on hover plus a grey background, $dark plus
   an outline on keyboard focus only. The background belongs to hover alone, so there is no box after a
   mouse click; the outline is drawn inside the button so it is not clipped by the table cell */

.hovered-link,
.hovered-button {
  display: block;
  margin: 0px;
  padding: 4px 8px;
  border-radius: var(--bs-border-radius);
  background-color: transparent;
  color: var(--bs-secondary);
  transition: color 0.15s ease, background-color 0.15s ease;
}
.hovered-link:hover,
.hovered-button:not(:disabled):hover {
  background-color: var(--bs-gray-500);
}
.hovered-link:hover,
.hovered-button:not(:disabled):hover,
.hovered-link:focus-visible,
.hovered-button:focus-visible {
  color: var(--bs-dark) !important;
}
.hovered-link:focus:not(:focus-visible),
.hovered-button:focus:not(:focus-visible) {
  outline: 0 !important;
}
.hovered-link:focus-visible,
.hovered-button:focus-visible {
  outline: 2px solid var(--bs-dark) !important;
  outline-offset: -2px !important;
}

</style>
