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
        <b-button @click="addCriteria" variant="light" class="border mdi mdi-plus" :title="$t('multisort.add')">{{ $t('multisort.add') }}</b-button>
        <b-button @click="moveCriteria('up')" variant="light" size="sm" class="border mdi mdi-18px mdi-chevron-up" :disabled="selectedIndex === 0 || selectedIndex === null" :title="$t('multisort.moveUp')"></b-button>
        <b-button @click="moveCriteria('down')" variant="light" size="sm" class="border mdi mdi-18px mdi-chevron-down" :disabled="selectedIndex === (sortingCriteria.length - 1) || selectedIndex === null" :title="$t('multisort.moveDown')"></b-button>
        <b-button @click="removeCriteria" variant="outline-danger" size="sm" class="mdi mdi-18px mdi-delete-outline" :disabled="sortingCriteria.length === 1" :title="$t('multisort.remove')"></b-button>
      </div>
    </div>
    <template #modal-footer="{ cancel, ok }">
      <b-button @click="cancel" variant="link">{{ $t('multisort.cancel') }}</b-button>
      <b-button @click="applySorting(); ok()" variant="primary">{{ $t('multisort.apply') }}</b-button>
    </template>
  </b-modal>
</template>

<script>
export default {
  name: 'MultisortModal',
  props: { prefix: String, sortKeys: Array, items: Array},
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
      var temp = null
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
      var sortedItems = this.items.slice()
      sortedItems.sort((a, b) => {
        for (let i = 0; i < this.sortingCriteria.length; i++) {
          var criteria = this.sortingCriteria[i]
          var field = criteria.field
          if (a[field] < b[field]) return -1 * criteria.order
          if (a[field] > b[field]) return 1 * criteria.order
        }
        return 0
      })
      this.$emit('apply-sorting', sortedItems)
    }
  }
}
</script>
