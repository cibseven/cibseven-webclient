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
  <div class="tab-pane fade" :class="{ 'show active': isActive }"
    :id="'nav-' + id" role="tabpanel" :aria-labelledby="'nav-' + id + '-tab'">
    <slot></slot>
  </div>
</template>

<script>
export default {
  props: {
    id: { type: String, required: true },
    title: { type: String, required: true }
  },
  inject: ['registerTab', 'activeTab', 'registerClickHandler'],
  computed: {
    isActive() {
      return this.activeTab() === this.id;
    }
  },
  methods: {
    handleClick: function () {
      this.$emit('click')
    },
    externalClick: function (tabId) {
      if (tabId === this.id) {
        this.handleClick()
      }
    }
  },
  created() {
    this.registerTab({ id: this.id, title: this.title })
    this.registerClickHandler(this.externalClick)
  }
}
</script>

<style scoped>
</style>
