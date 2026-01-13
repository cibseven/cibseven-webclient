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
  <b-modal ref="sortModal" size="xl" :title="$t('multisort.title')">
    <div>
      <div class="row">
        <div class="offset-2 col-5">
          <h6>{{ $t('multisort.column') }}</h6>
        </div>
        <div class="col-5">
          <h6>{{ $t('multisort.order') }}</h6>
        </div>
      </div>
      <div v-if="!moving">
        <div v-for="(criteria, index) in sortingCriteria" :key="index" class="row align-items-center py-3" :class="index === selectedIndex ? 'bg-light' : ''" @click="selectedIndex = index">
          <div class="col-2">
            <span v-if="index === 0">{{ $t('multisort.orderBy') }}</span>
            <span v-else>{{ $t('multisort.thenBy') }}</span>
          </div>
          <div class="col-5">
            <b-form-select v-model="criteria.field" :options="sortKeysWithText"></b-form-select>
          </div>
          <div class="col-5">
            <b-form-select v-model="criteria.order" :options="orders"></b-form-select>
          </div>
        </div>
      </div>
      <div class="mt-3">
        <b-button @click="addCriteria" variant="light" class="mdi mdi-plus" :title="$t('multisort.add')">{{ $t('multisort.add') }}</b-button>
        <b-button @click="moveCriteria('up')" variant="light" size="sm" class="mdi mdi-18px mdi-chevron-up" :disabled="selectedIndex === 0 || selectedIndex === null" :title="$t('multisort.moveUp')"></b-button>
        <b-button @click="moveCriteria('down')" variant="light" size="sm" class="mdi mdi-18px mdi-chevron-down" :disabled="selectedIndex === (sortingCriteria.length - 1) || selectedIndex === null" :title="$t('multisort.moveDown')"></b-button>
        <b-button @click="removeCriteria" variant="outline-danger" size="sm" class="mdi mdi-18px mdi-delete-outline" :disabled="sortingCriteria.length === 1" :title="$t('multisort.remove')"></b-button>
      </div>
    </div>
    <template #modal-footer="{ cancel, ok }">
      <b-button @click="cancel" variant="light">{{ $t('multisort.cancel') }}</b-button>
      <b-button @click="applySorting(); ok()" variant="primary">{{ $t('multisort.apply') }}</b-button>
    </template>
  </b-modal>
</template>

<script>
export default {
  name: 'MultisortModal',
  props: { prefix: String, sortKeys: Array },
  emits: ['apply-sorting'],
  data: function() {
    return {
      selectedIndex: null,
      sortingCriteria: [{ field: '', order: 1 }],
      orders: [
        { text: this.$t('multisort.asc'), value: 1 },
        { text: this.$t('multisort.desc'), value: -1 }
      ],
      moving: false
    }
  },
  computed: {
    sortKeysWithText() {
      return this.sortKeys.map(value => ({
        value: value,
        text: this.$t(this.prefix + value)
      }))
    }
  },
  methods: {
    show: function() {
      this.$refs.sortModal.show()
    },
    addCriteria: function() {
      this.sortingCriteria.push({ field: '', order: 1 })
    },
    removeCriteria: function() {
      this.sortingCriteria.splice(this.selectedIndex, 1)
      this.selectedIndex = null
    },
    moveCriteria: function(direction) {
      this.moving = true
      let temp = null
      if (direction === 'up' && this.selectedIndex > 0) {
        temp = this.sortingCriteria[this.selectedIndex];
        this.sortingCriteria[this.selectedIndex] = this.sortingCriteria[this.selectedIndex - 1]
        this.sortingCriteria[this.selectedIndex - 1] = temp
        this.selectedIndex = this.selectedIndex - 1
      } else if (direction === 'down' && this.selectedIndex < this.sortingCriteria.length - 1) {
        temp = this.sortingCriteria[this.selectedIndex]
        this.sortingCriteria[this.selectedIndex] = this.sortingCriteria[this.selectedIndex + 1]
        this.sortingCriteria[this.selectedIndex + 1] = temp
        this.selectedIndex = this.selectedIndex + 1
      }
      this.$nextTick(() => {
        this.moving = false
      })
    },
    applySorting: function() {
      // Convert frontend sorting criteria to backend format
      const sortingCriteria = this.sortingCriteria
        .filter(criteria => criteria.field) // Only include criteria with selected fields
        .map(criteria => ({
          field: criteria.field,
          order: criteria.order === 1 ? 'asc' : 'desc'
        }))
      
      this.$emit('apply-sorting', sortingCriteria)
    }
  }
}
</script>
