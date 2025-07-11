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
  <slot></slot>

  <div v-if="loading && showLoadingSpinner" class="text-center">
    <b-waiting-box class="d-inline me-2" styling="width: 35px"></b-waiting-box> {{ $t('admin.loading') }}
  </div>
  <div v-else-if="!loading && allLoaded && loadedCount > 0" class="text-center text-muted py-2">
    {{ $t('commons.noMoreResults') }}
  </div>
  <div v-else-if="!loading && !allLoaded" class="text-center text-muted py-2">
    <button :title="$t('commons.loadMore.tooltip', { chunkSize: chunkSize })" class="btn btn-link p-0 text-info" @click="$emit('load-next-page')">
      {{ $t('commons.loadMore.title') }}
    </button>
  </div>
</template>

<script>
export default {
  name: 'PagedScrollableContent',
  emits: ['load-next-page'],
  props: {
    loading: { type: Boolean, required: true },
    loadedCount: { type: Number, required: true },
    totalCount: { type: Number },
    chunkSize: { type: Number, default: 20 },
    showLoadingSpinner: { type: Boolean, default: true },

    /**
     * The scrollable area where the search results will be displayed.
     * This is used to handle scrolling and loading more results.
     */
    scrollableArea: {
      type: Object,
      default: null
    },
  },
  data() {
    return {
      lastScrollableArea: null
    }
  },
  computed: {
    allLoaded() {
      if (this.totalCount === undefined || this.totalCount === null) {
        // totalCount is not defined, assume we don't know how many items there are
        return false
      }
      return this.loadedCount >= this.totalCount
    }
  },
  mounted() {
    this.attachScrollListener(this.scrollableArea)
  },
  beforeUnmount() {
    this.detachScrollListener(this.lastScrollableArea)
  },
  watch: {
    scrollableArea(newVal, oldVal) {
      this.detachScrollListener(oldVal)
      this.attachScrollListener(newVal)
    }
  },
  methods: {
    attachScrollListener(area) {
      if (area) {
        area.addEventListener('scroll', this.onScroll, { passive: true })
        this.lastScrollableArea = area
      }
    },
    detachScrollListener(area) {
      if (area) {
        area.removeEventListener('scroll', this.onScroll)
      }
    },
    onScroll(e) {
      if (this.loading || this.allLoaded) return

      // Check if user is near the bottom of the page
      const el = e.target
      const threshold = 100
      if (el.scrollTop + el.clientHeight >= el.scrollHeight - threshold) {
        this.$emit('load-next-page')
      }
    },
  }
}
</script>
