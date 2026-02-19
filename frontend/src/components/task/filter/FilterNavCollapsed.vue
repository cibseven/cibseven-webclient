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
  <div>
    <b-button variant="light" class="rounded-0 text-nowrap position-absolute" :aria-label="$t('seven.filters')" :aria-expanded="String(leftOpen)" @click="$emit('update:leftOpen', true)"
      style="right: 100%; top: 0; transform: rotate(-90deg); transform-origin: right top; height: 40px"
      :style="favoriteFilters.length > 0 ? 'width: ' + sizes.arrow + 'px' : ''">
      <span v-if="favoriteFilters.length <= 0">{{ $t('nav-bar.filtersTitle') }}</span>
      <i class="mdi mdi-18px mdi-chevron-down"></i>
    </b-button>
    <template v-for="(filter, key) in favoritesDisplayed">
      <b-button v-if="filter && filter.display" :title="filter.name" variant="light"
        class="rounded-0 border-0 text-truncate position-absolute"
        style="right: 100%; transform: rotate(-90deg); transform-origin: right top; height: 40px" :style="getStyles(filter, key)"
        @click="selectFilter(filter)" :key="filter.id"> <span class="h5">{{ filter.name }}</span>
      </b-button>
    </template>
    <b-dropdown v-if="favoritesNoDisplayed.length > 0" variant="link" toggle-class="px-0" no-caret class="position-absolute" style="left: 0" :style="getDotsStyle()">
      <template #button-content>
        <div class="mx-2" :title="$t('task.showMore')">
          <i class="mdi mdi-18px mdi-dots-horizontal"></i>
        </div>
      </template>
      <b-dd-item-btn v-for="filter in favoritesNoDisplayed" @click="selectFilter(filter)" :key="filter.id" :class="isActive(filter) ? 'active' : ''">
        {{ filter.name }}
      </b-dd-item-btn>
    </b-dropdown>
  </div>
</template>

<script>
export default {
  name: 'FilterNavCollapsed',
  props: { leftOpen: Boolean },
  emits: ['update:leftOpen', 'selected-filter'],
  data: function() {
    return {
      sizes: { filter: 140, arrow: 40, header: 55, dots: 40 }
    }
  },
  watch: {
    favoriteFilters: function() {
      this.setDisplayFavorites()
    }
  },
  computed: {
    favoriteFilters: function() {
      const filters = this.$store.state.filter.list.filter(f => {
        return f.favorite
      })
      return filters.sort(function(a, b) {
        return a.properties.priority - b.properties.priority
      })
    },
    favoritesDisplayed: function() {
      return this.favoriteFilters.filter(filter => {
        return filter.display
      })
    },
    favoritesNoDisplayed: function() {
      return this.favoriteFilters.filter(filter => {
        return !filter.display
      })
    }
  },
  mounted: function() {
    this.setDisplayFavorites()
    window.addEventListener('resize', this.setDisplayFavorites)
  },
  methods: {
    setDisplayFavorites: function() {
      this.favoriteFilters.forEach((filter, key) => {
        if ((key * this.sizes.filter + this.sizes.arrow + this.sizes.header +
          this.sizes.filter + this.sizes.dots) < window.innerHeight) {
            filter.display = true
        } else filter.display = false
      })
    },
    getStyles: function(filter, key) {
      const styles = { top: key * this.sizes.filter + this.sizes.arrow + 'px', width: this.sizes.filter + 'px' }
      if (this.$store.state.filter.selected.id === filter.id) {
        styles['border-top'] = '5px solid!important'
        styles['border-top-color'] = 'var(--bs-primary)!important'
      }
      return styles
    },
    getDotsStyle: function() {
      const styles = { width: this.sizes.dots + 'px' }
      styles.top = this.favoritesDisplayed.length * this.sizes.filter + this.sizes.arrow + 'px'
      return styles
    },
    isActive(filter) {
      return this.$store.state.filter.selected.id === filter.id
    },
    selectFilter: function(filter) {
      const selectedFilter = this.$store.state.filter.list.find(f => {
        return f.id === filter.id
      })
      if (selectedFilter) {
        this.$store.state.filter.selected = selectedFilter
        this.$emit('selected-filter', selectedFilter.id)
        localStorage.setItem('filter', JSON.stringify(selectedFilter))
        const path = '/seven/auth/tasks/' + selectedFilter.id +
          (this.$route.params.taskId ? '/' + this.$route.params.taskId : '')
        if (this.$route.path !== path) this.$router.replace(path)
      }
    }
  },
  beforeUnmount: function() {
    window.removeEventListener('resize', this.setDisplayFavorites)
  }
}
</script>
