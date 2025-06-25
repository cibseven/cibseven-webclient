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
  <table class="table" :class="computedTableClass" :style="computedTableStyles" role="table" ref="table">
    <thead :class="theadClass" role="rowgroup">
      <tr :class="[ !nativeLayout && 'd-flex' ]" role="row">
        <th v-for="(field, index) in computedColumns"
          :key="index"
          :class="[field.class, field.thClass, getSortClass(field)]"
          role="columnheader"
          :aria-sort="getAriaSort(field)"
          @click.stop="handleColumnClick(field)"
          :style="{
            ...(resizable ? { width: columnWidths[index], position: 'relative' } : {}),
            cursor: field.sortable !== false ? 'pointer' : 'default'
          }">

          <span v-if="field.sortable !== false" class="sort-icon">
            <span v-if="isSortedByField(field)">
              <i v-if="isSortedByFieldAscending(field)" class="mdi mdi-chevron-up"></i>
              <i v-else class="mdi mdi-chevron-down"></i>
            </span>
            <i v-else class="mdi mdi-unfold-more-horizontal"></i>
          </span>

          <span v-if="field.label">
            <slot :name="'header(' + field.key +')'" :field="field">
              {{ $t(prefix + field.label) }}
            </slot>
          </span>

          <span :style="columnSelectionStyle" v-if="computedColumnSelection && index === computedColumns.length - 1">
            &nbsp;
            <button class="btn btn-link btn-sm p-0" type="button" data-bs-toggle="dropdown"
              aria-expanded="false" aria-haspopup="true" :aria-label="$t('table.selectColumns')"
              :title="$t('table.selectColumns')">
                <span class="visually-hidden">{{ $t('table.selectColumns') }}</span>
                <span class="mdi mdi-24px mdi-table-plus align-middle"></span>
            </button>
            <ul class="dropdown-menu dropdown-menu-end" role="menu">
              <template v-for="column in toggleableColumns" :key="column.key">
                <li v-if="column.groupSeparator === true"
                  class="dropdown-divider">
                </li>
                <li @click="toggleColumn(column)"
                  :title="$t('table.toggleColumn', { column: $t(column.label) })" class="dropdown-item" role="menuitem">
                  <input readonly type="checkbox" :id="column.key" tabindex="-1" :checked="computedColumns.some(col => col.key === column.key)"
                    :aria-label="$t('table.toggleColumn', { column: $t(column.label) })">
                  <label :for="column.key" class="ps-2" @click.stop=";">{{ $t(column.label) }}</label>
                </li>
              </template>
            </ul>
          </span>

          <span
            v-if="resizable && index !== computedColumns.length - 1"
            :style="resizeHandleStyle"
            @mousedown.stop="startResize(index, $event)">
          </span>
        </th>
      </tr>
    </thead>
    <tbody role="rowgroup">
      <tr v-for="(item, index) in sortedItems" :key="index"
        :class="[getRowClass(item), nativeLayout ? '' : 'd-flex']"
        @mouseenter="$emit('mouseenter', item)"
        @mouseleave="$emit('mouseleave', item)"
        @click.stop="$emit('click', item)"
        style="cursor: pointer"
        role="row">
        <td v-for="(field, colIndex) in computedColumns"
          :key="field.key"
          :class="[
            field.class,
            field.tdClass,
            nativeLayout ? '' : 'd-flex align-items-center'
          ]"
          :style="isResizableFlex ? { width: columnWidths[colIndex] } : {}"
          role="cell">
          <slot :name="'cell(' + field.key +')'" :item="item" :value="item[field.key]" :index="index">
            {{ item[field.key] }}
          </slot>
        </td>
      </tr>
    </tbody>
  </table>
</template>

<script>
export default {
  name: 'FlowTable',
  props: {
    items: { type: Array, default: () => [] },

    /**
     * Complete columns definitions to be displayed (API-1).
     * Each object should have at least 'key' and 'label' properties.
     */
    fields: { type: Array, default: () => [] },

    /**
     * Keys of columns to be displayed (API-2).
     *
     * API-2: Both 'columns' and 'columnDefinitions' should be non-empty arrays.
     */
    columns: {
      type: Array,
      default: () => []
    },
    /**
     * Complete column definitions (API-2).
     *
     * API-2: Both 'columns' and 'columnDefinitions' should be non-empty arrays.
     */
    columnDefinitions: {
      type: Array,
      default: () => []
    },
    /**
     * Whether to show column selection (API-2).
     * If true, columns can be selected/deselected.
     * Added for backward compatibility: use API-2 but without column selection.
     */
    columnSelection: { type: Boolean, default: true },
    /**
     * Use case for the table instance, used as key for localStorage to store column visibility settings.
     *
     * Example: USERS table can show different columns,
     * but you would like to have one visibility set of columns
     * for "tenant users" and another for "all users" table.
     */
    useCase: {
      type: String,
      default: 'FlowTable'
    },
    nativeLayout: { type: Boolean, default: false },
    tbodyTrClass: { type: [String, Function], default: '' },
    prefix: { type: String, default: '' },
    theadClass: { type: String, default: '' },
    tableClass: { type: String, default: '' },
    resizable: { type: Boolean, default: false },
    striped : { type: Boolean, default: false },
    sortBy: { type: String, default: null },
    sortDesc: { type: Boolean, default: false },
    /**
     * If true, sorting is handled externally.
     * Emits 'external-sort' event with { sortBy, sortDesc } parameters.
     * If false, component sorts items locally using sortKey and sortOrder.
     */
    externalSort: { type: Boolean, default: false },
  },
  data() {
    return {
      columnVisibility: {},
      sortKey: this.sortBy,
      sortOrder: this.sortDesc ? -1 : 1,
      columnWidths: [],
      skipClick: false,
      resizeObserver: null
    }
  },
  computed: {
    api2() {
      // Check if both columns and columnDefinitions are provided
      return this.columns.length > 0 && this.columnDefinitions.length > 0
    },
    localStorageKey() {
      return `cibseven:table:columnVisibility:${this.useCase}:${this.columnDefinitions.length}`
    },
    computedColumns() {
      if (this.api2) {
        // API-2: Use columnDefinitions to get full field definitions
        return this.columnDefinitions.filter(def => {
          if (def.disableToggle === true) {
            // If column is disabled for toggling, always include it regardless of `columnVisibility` visibility
            return this.columns.includes(def.key)
          }
          else if (this.columnVisibility[def.key] === undefined) {
            // User has never changed visibility => let's check whether this column is visible by default
            return this.columns.includes(def.key)
          }
          else {
            // User has changed visibility => use the stored value
            return this.columnVisibility[def.key]
          }
        })
      }
      // default, API-1
      return this.fields
    },
    toggleableColumns() {
      return this.columnDefinitions.filter(col => !col.disableToggle)
    },
    computedColumnSelection() {
      return this.columnSelection && this.api2
    },
    computedTableStyles() {
      return { tableLayout: 'fixed', width: '100%' }
    },
    computedTableClass() {
      return [
        this.striped ? 'table-striped' : '',
        this.tableClass
      ].filter(Boolean).join(' ')
    },
    sortedItems() {
      if (this.externalSort) {
        // If external sort is enabled, return items as is
        return this.items
      }
      if (!this.sortKey) return this.items
      return [...this.items].sort((a, b) => {
        if (a[this.sortKey] < b[this.sortKey]) return -1 * this.sortOrder
        if (a[this.sortKey] > b[this.sortKey]) return 1 * this.sortOrder
        return 0
      })
    },
    columnSelectionStyle() {
      return {
        position: 'absolute',
        top: '0',
        right: '0',
        width: '30px',
        height: '100%',
        display: 'flex',
        zIndex: '10',
      }
    },
    resizeHandleStyle() {
      return {
        position: 'absolute',
        top: '0',
        right: '0',
        width: '5px',
        height: '100%',
        cursor: 'col-resize',
        zIndex: '10',
        background: 'transparent'
      }
    },
    isResizableFlex() {
      return this.resizable && !this.nativeLayout
    }
  },
  methods: {
    /**
     * Handle column header click for sorting.
     * If externalSort is enabled, it emits an event with new sort parameters.
     * Otherwise, it sorts rows using local sortKey and sortOrder.
     */
    handleColumnClick(field) {
      if (this.skipClick) {
        this.skipClick = false
        return
      }
      if (!this.resizing) {
        if (!field.key || field.sortable === false) return
        if (this.externalSort) {
          // by default, each column is sortable
          this.$emit('external-sort', {
            sortBy: field.sortBy || field.key,
            sortDesc: this.sortBy === (field.sortBy || field.key) ? !this.sortDesc : true,
          })
        }
        else {
          if (this.sortKey === field.key) {
            this.sortOrder *= -1
          } else {
            this.sortKey = field.key
            this.sortOrder = 1
          }
        }
      }
    },
    /**
     * Check if the field is currently sorted by.
     * If externalSort is enabled, it checks against sortBy.
     * Otherwise, it checks against sortKey.
     */
    isSortedByField(field) {
      if (!field.key || field.sortable === false) return false
      if (this.externalSort) {
        return (field.sortBy || field.key) === this.sortBy
      }
      else {
        return field.key === this.sortKey
      }
    },
    /**
     * Check if the field is sorted by in ascending order.
     * If externalSort is enabled, it checks against sortDesc.
     * Otherwise, it checks against sortOrder.
     */
    isSortedByFieldAscending(field) {
      if (!this.isSortedByField(field)) return false
      if (this.externalSort) {
        return !this.sortDesc
      }
      else {
        return this.sortOrder === 1
      }
    },
    getSortClass(field) {
      if (field.sortable === false) return ''
      if (this.isSortedByField(field)) {
        return this.isSortedByFieldAscending(field) ? 'sorting-asc active' : 'sorting-desc active'
      }
      else {
        return 'sortable'
      }
    },
    getAriaSort(field) {
      if (this.isSortedByField(field)) {
        return this.$t(`bcomponents.${this.isSortedByFieldAscending(field) ? 'ariaSortAsc' : 'ariaSortDes'}`)
      }
      return this.$t('bcomponents.ariaSortNone')
    },
    getRowClass(item) {
      return typeof this.tbodyTrClass === 'function' ? this.tbodyTrClass(item) : this.tbodyTrClass
    },
    startResize(index, event) {
      if (!this.resizable) return

      const startX = event.clientX
      const startWidth = parseFloat(this.columnWidths[index])
      const adjacentIndex = index === this.columnWidths.length - 1 ? index - 1 : index + 1
      const adjacentStartWidth = parseFloat(this.columnWidths[adjacentIndex])
      const minWidth = 75
      this.resizing = true

      const onMouseMove = (e) => {
        const deltaWidth = e.clientX - startX

        const newWidth = startWidth + deltaWidth
        const newAdjacentWidth = adjacentStartWidth - deltaWidth

        if (newWidth < minWidth || newAdjacentWidth < minWidth) return

        this.columnWidths[index] = `${newWidth}px`
        this.columnWidths[adjacentIndex] = `${newAdjacentWidth}px`
      }

      const onMouseUp = () => {
        this.resizing = false
        this.skipClick = true
        setTimeout(() => {
          this.skipClick = false
        }, 0)

        document.removeEventListener('mousemove', onMouseMove)
        document.removeEventListener('mouseup', onMouseUp)
      }

      document.addEventListener('mousemove', onMouseMove)
      document.addEventListener('mouseup', onMouseUp)
    },
    restartColumnWidths: function() {
      if (this.resizable && this.$refs.table) {
        const ths = this.$refs.table.querySelectorAll('th')
        if (ths && ths.length > 0) {
          // Clean existing widths
          ths.forEach(th => {
            th.style.removeProperty('width')
          })
          // Get the container width
          const tableContainer = this.$refs.table.parentElement
          const containerWidth = tableContainer ? tableContainer.clientWidth : this.$refs.table.clientWidth
          // Calculate natural widths of the columns
          const naturalWidths = Array.from(ths).map(th => th.offsetWidth)
          const totalNaturalWidth = naturalWidths.reduce((sum, width) => sum + width, 0)
          // If total natural width exceeds container width, scale down
          if (totalNaturalWidth > containerWidth) {
            const scale = containerWidth / totalNaturalWidth
            this.columnWidths = naturalWidths.map(width => `${Math.floor(width * scale)}px`)
          } else {
            this.columnWidths = naturalWidths.map(width => `${width}px`)
          }
        }
      }
    },
    toggleColumn(column) {
      const visible = this.computedColumns.some(col => col.key === column.key)
      this.columnVisibility[column.key] = !visible
      localStorage.setItem(this.localStorageKey, JSON.stringify(this.columnVisibility))
      this.$nextTick(() => {
        this.restartColumnWidths()
      })
    }
  },
  mounted() {
    if (this.api2) {
      // Load columnVisibility from localStorage if available
      const storedVisibility = localStorage.getItem(this.localStorageKey)
      if (storedVisibility) {
        try {
          const obj = JSON.parse(storedVisibility)
          if (obj && typeof obj === 'object' && !Array.isArray(obj)) {
            this.columnVisibility = obj
          }
        } catch {
          // Ignore parse errors, fallback to empty object
        }
      }
    }
    if (this.resizable) {
      this.$nextTick(() => {
        const ths = this.$refs.table.querySelectorAll('th');
        this.columnWidths = Array.from(ths).map(th => `${th.offsetWidth}px`)
        if (window.ResizeObserver) {
          this.resizeObserver = new ResizeObserver(() => {
            this.restartColumnWidths()
          })
          const tableContainer = this.$refs.table.parentElement
          if (tableContainer) {
            this.resizeObserver.observe(tableContainer)
          }
        }
      })
    }
    window.addEventListener("resize", this.restartColumnWidths)
  },
  beforeUnmount() {
    window.removeEventListener("resize", this.restartColumnWidths)
    if (this.resizeObserver) {
      this.resizeObserver.disconnect()
      this.resizeObserver = null
    }
  }
}
</script>
