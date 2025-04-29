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
  <nav :class="['pagination-wrapper', wrapperClass]" role="navigation" :aria-label="$t('bcomponents.pagination.label')">
    <ul class="pagination justify-content-center">
      <li class="page-item" :class="{ disabled: currentPage === 1 }">
        <button class="page-link" @click="goToPage(currentPage - 1)"
          :aria-label="$t('bcomponents.pagination.prevPage')" :aria-disabled="currentPage === 1">
          <span aria-hidden="true">&laquo;</span>
        </button>
      </li>

      <li v-for="page in pages" :key="page" class="page-item"
        :class="{ active: page === currentPage }">
        <button class="page-link" @click="goToPage(page)"
          :aria-current="page === currentPage ? 'page' : null">
          {{ page }}
        </button>
      </li>

      <li class="page-item" :class="{ disabled: currentPage === totalPages }">
        <button class="page-link" @click="goToPage(currentPage + 1)"
          :aria-label="$t('bcomponents.pagination.nextPage')" :aria-disabled="currentPage === totalPages">
          <span aria-hidden="true">&raquo;</span>
        </button>
      </li>
    </ul>
  </nav>
</template>

<script>
export default {
  props: {
    modelValue: { type: Number, default: 1 },
    totalRows: { type: Number, required: true },
    perPage: { type: Number, default: 10 },
    wrapperClass: { type: String, default: '' }
  },
  data: function () {
    return {
      currentPage: this.modelValue
    }
  },
  computed: {
    totalPages: function () {
      return Math.ceil(this.totalRows / this.perPage)
    },
    pages: function () {
      const half = Math.floor(5 / 2)
      let start = Math.max(1, this.currentPage - half)
      let end = Math.min(this.totalPages, start + 5 - 1)

      if (end - start < 5 - 1) {
        start = Math.max(1, end - 5 + 1)
      }

      return Array.from({ length: end - start + 1 }, (v, i) => start + i)
    }
  },
  watch: {
    modelValue: function (newValue) { this.currentPage = newValue },
    currentPage: function (newValue) {
      this.$emit('update:modelValue', newValue)
    }
  },
  methods: {
    goToPage: function (page) {
      if (page >= 1 && page <= this.totalPages) {
        this.currentPage = page
      }
    }
  }
}
</script>

<style scoped>
</style>
