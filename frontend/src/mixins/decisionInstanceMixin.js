
    // Copyright CIB software GmbH and/or licensed to CIB software GmbH
    // under one or more contributor license agreements. See the NOTICE file
    // distributed with this work for additional information regarding copyright
    // ownership. CIB software licenses this file to you under the Apache License,
    // Version 2.0; you may not use this file except in compliance with the License.
    // You may obtain a copy of the License at

    //      http://www.apache.org/licenses/LICENSE-2.0

    //  Unless required by applicable law or agreed to in writing, software
    //  distributed under the License is distributed on an "AS IS" BASIS,
    //  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    //  See the License for the specific language governing permissions and
    //  limitations under the License.


import { mapGetters } from 'vuex'

export default {
  data() {
    return {
      _mutationObserver: null,
      _isApplying: false
    }
  },
  computed: {
    ...mapGetters('diagram', ['isDiagramReady'])
  },
  watch: {
    isDiagramReady(isReady) {
      if (isReady && this.instance) this.applyInstanceValues()
    },
    instance: {
      handler() {
        if (this.isDiagramReady) this.applyInstanceValues()
      },
      deep: true
    }
  },
  methods: {
    applyInstanceValues() {
      this.clearModifications()
      this.observeDiagram()
    },

    observeDiagram() {
      const container = this.$refs.diagram?.$refs?.diagram
      if (!container) return

      if (this._mutationObserver) this._mutationObserver.disconnect()

      this._mutationObserver = new MutationObserver(() => {
        const table = container.querySelector('.tjs-table') || container.querySelector('table')
        if (table) this.modifyHeaders(table)
      })

      this._mutationObserver.observe(container, { childList: true, subtree: true })

      const table = container.querySelector('.tjs-table') || container.querySelector('table')
      if (table) this.modifyHeaders(table)
    },

    modifyHeaders(table) {
      if (this._isApplying) return
      this._isApplying = true
      try {
        this.applyInputHeaders(table)
        this.applyOutputRows(table)
      } catch (e) {
        console.warn('decisionInstanceMixin.modifyHeaders error', e)
      } finally {
        this._isApplying = false
      }
    },

    applyInputHeaders(table) {
      const inputThs = table.querySelectorAll('th.input-cell')
      inputThs.forEach(th => {
        const colId = th.getAttribute('data-col-id')
        const input = this.instance?.inputs?.find(i => i.clauseId === colId)
        if (!colId || !input) return

        const clauseDiv = th.querySelector('div.clause')
        const originalLabel = clauseDiv?.textContent.trim() || 'When'

        this.applyBadge(th, colId, originalLabel, input.value)
      })
    },

    applyOutputRows(table) {
      const headerRow = table.querySelector('thead tr')
      if (!headerRow) return
      const headerCells = Array.from(headerRow.children)

      const tbody = table.querySelector('tbody')
      if (!tbody) return

      // Build input column map: colIndex → expected input value
      const inputColumns = []
      table.querySelectorAll('th.input-cell').forEach(th => {
        const colId = th.getAttribute('data-col-id')
        const input = this.instance?.inputs?.find(i => i.clauseId === colId)
        if (colId && input) {
          inputColumns.push({
            colIndex: headerCells.indexOf(th),
            value: String(input.value).trim()
          })
        }
      })

      table.querySelectorAll('th.output-cell').forEach(th => {
        const colId = th.getAttribute('data-col-id')
        const output = colId
          ? this.instance?.outputs?.find(o => o.clauseId === colId)
          : this.instance?.outputs?.[0]
        if (!output || !output.value) return

        const outputColIndex = headerCells.indexOf(th)
        const outputValue = String(output.value).trim()

        Array.from(tbody.rows).forEach(row => {
          const td = row.children[outputColIndex]
          if (!td) return

          // Use data-original-text if already modified to avoid false mismatch after DOM mutation
          const tdText = td.getAttribute('data-original-text') ?? td.textContent

          // Output column must match exactly
          if (this.normalizeCell(tdText) !== outputValue) {
            this.applyRowHighlight(row, td, false)
            return
          }

          // For input columns that contain a DMN string literal ("value"),
          // check if it matches the input value. If it doesn't → disqualify the row.
          // Cells with expressions (< 250, [250..1000]) are skipped since they can't be evaluated here.
          const disqualified = inputColumns.some(({ colIndex, value }) => {
            const cell = row.children[colIndex]
            if (!cell) return false
            const raw = (cell.getAttribute('data-original-text') ?? cell.textContent).trim()
            if (!this.isDmnStringLiteral(raw)) return false // expression, skip
            return this.normalizeCell(raw) !== value
          })

          this.applyRowHighlight(row, td, !disqualified)
        })
      })
    },

    applyBadge(element, colId, label, value) {
      const clauseDiv = element.querySelector('div.clause')
      if (!clauseDiv) return
      if (clauseDiv.getAttribute('data-original-text')) return

      // Apply badge using DOM nodes to avoid innerHTML and escaping
      const original = clauseDiv.textContent.trim()
      clauseDiv.setAttribute('data-original-text', original)
      clauseDiv.textContent = ''

      const badgeDiv = document.createElement('div')
      badgeDiv.className = 'dmn-instance-badge'
      const labelNode = document.createTextNode(label + ' ')
      badgeDiv.appendChild(labelNode)

      const span = document.createElement('span')
      span.className = 'dmn-instance-bold'
      span.textContent = '= ' + String(value)
      badgeDiv.appendChild(span)

      clauseDiv.appendChild(badgeDiv)
    },

    applyRowHighlight(row, td, isMatched) {
      if (isMatched) {
        row.classList.add('dmn-instance-row-matched')
        row.classList.remove('dmn-instance-row-default')
      } else {
        row.classList.remove('dmn-instance-row-matched')
        row.classList.add('dmn-instance-row-default')
      }

      if (isMatched && !td.getAttribute('data-original-text')) {
        td.setAttribute('data-original-text', td.textContent.trim())
        const originalValue = td.getAttribute('data-original-text')
        // Build content using DOM nodes to avoid innerHTML
        td.textContent = originalValue + ' '
        const span = document.createElement('span')
        span.className = 'dmn-instance-bold'
        span.textContent = '= ' + String(originalValue)
        td.appendChild(span)
      }
    },

    // Strips surrounding DMN double-quotes and trims whitespace for exact comparison
    normalizeCell(text) {
      return text.trim().replace(/^"|"$/g, '').trim()
    },

    // Returns true if the cell text is a DMN string literal (wrapped in double quotes)
    isDmnStringLiteral(text) {
      return /^".*"$/.test(text.trim())
    },

    clearModifications() {
      if (this._mutationObserver) {
        this._mutationObserver.disconnect()
        this._mutationObserver = null
      }
      this._isApplying = false

      const container = this.$refs.diagram?.$refs?.diagram
      if (!container) return
      const table = container.querySelector('.tjs-table') || container.querySelector('table')
      if (!table) return

      // If there are no DOM markers we added, nothing to do
      const hasModifications = table.querySelector('th[data-original-text], td[data-original-text], tr.dmn-instance-row-matched, tr.dmn-instance-row-default')
      if (!hasModifications) return

      // Restore headers
      table.querySelectorAll('th.input-cell, th.output-cell').forEach(th => {
        const clauseDiv = th.querySelector('div.clause')
        const original = clauseDiv?.getAttribute('data-original-text')
        if (original) {
          clauseDiv.textContent = original
          clauseDiv.removeAttribute('data-original-text')
        }
      })

      // Restore TDs
      table.querySelectorAll('td[data-original-text]').forEach(td => {
        td.textContent = td.getAttribute('data-original-text')
        td.removeAttribute('data-original-text')
      })

      // Remove our classes and clear inline background from any modified rows
      table.querySelectorAll('tr.dmn-instance-row-matched, tr.dmn-instance-row-default').forEach(row => {
        row.classList.remove('dmn-instance-row-matched', 'dmn-instance-row-default')
        row.style.backgroundColor = ''
      })

      // no flag to clear
    }
  },

  beforeUnmount() {
    this.clearModifications()
  }
}