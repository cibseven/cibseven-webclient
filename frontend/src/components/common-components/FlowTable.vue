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
            <span v-if="sortKey === field.key">
              <i v-if="sortOrder === 1" class="mdi mdi-chevron-up"></i>
              <i v-else class="mdi mdi-chevron-down"></i>
            </span>
            <i v-else class="mdi mdi-unfold-more-horizontal"></i>
          </span>

          <span v-if="field.label">
            <slot :name="'header(' + field.key +')'" :field="field">
              {{ $t(prefix + field.label) }}
            </slot>
          </span>

          <span v-if="computedColumnSelection && index === computedColumns.length - 1">
            &nbsp;
            <button class="dropdown-toggle btn btn-link btn-sm p-0" type="button" data-bs-toggle="dropdown"
              aria-expanded="false" aria-haspopup="true" :aria-label="$t('table.selectColumns')"
              :title="$t('table.selectColumns')">
                <span class="visually-hidden">{{ $t('table.selectColumns') }}</span>
                <span class="mdi mdi-24px mdi-plus-box align-middle"></span>
            </button>
            <ul class="dropdown-menu dropdown-menu-end" role="menu">
              <template v-for="column in toggleableColumns" :key="column.key">
                <li v-if="column.groupSeparator === true"
                  class="dropdown-divider">
                </li>
                <li @click="toggleColumn(column)"
                  :title="$t('table.toggleColumn', { column: $t(column.label) })" class="dropdown-item" role="menuitem">
                  <input readonly type="checkbox" tabindex="-1" :checked="computedColumns.some(col => col.key === column.key)"
                    :aria-label="$t('table.toggleColumn', { column: $t(column.label) })">
                  <span class="ps-2">{{ $t(column.label) }}</span>
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
    sortDesc: { type: Boolean, default: false }
  },
  data() {
    return {
      columnVisibility: {},
      sortKey: this.sortBy,
      sortOrder: this.sortDesc ? -1 : 1,
      columnWidths: [],
      skipClick: false
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
      if (!this.sortKey) return this.items
      return [...this.items].sort((a, b) => {
        if (a[this.sortKey] < b[this.sortKey]) return -1 * this.sortOrder
        if (a[this.sortKey] > b[this.sortKey]) return 1 * this.sortOrder
        return 0
      })
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
    handleColumnClick(field) {
      if (this.skipClick) {
        this.skipClick = false
        return
      }
      if (!this.resizing) {
        this.sortColumn(field)
      }
    },
    sortColumn(field) {
      if (!field.key || field.sortable === false) return

      if (this.sortKey === field.key) {
        this.sortOrder *= -1
      } else {
        this.sortKey = field.key
        this.sortOrder = 1
      }
    },
    getSortClass(field) {
      if (field.sortable === false) return ''
      if (field.key === this.sortKey) {
        return this.sortOrder === 1 ? 'sorting-asc active' : 'sorting-desc active'
      }
      return 'sortable'
    },
    getAriaSort(field) {
      if (field.key === this.sortKey) {
        return this.$t(`bcomponents.${this.sortOrder === 1 ? 'ariaSortAsc' : 'ariaSortDes'}`)
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
        if (ths) {
          ths.forEach(th => {
            th.style.removeProperty('width')
          })
        }
        this.columnWidths = Array.from(ths).map(th => `${th.offsetWidth}px`)
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
      })
    }
    window.addEventListener("resize", this.restartColumnWidths)
  },
  beforeUnmount() {
    window.removeEventListener("resize", this.restartColumnWidths)
  }
}
</script>
