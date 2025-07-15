/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

export default {
  data() {
    return {
      showLeftButton: false,
      showRightButton: false
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.checkScrollButtons()
      window.addEventListener('resize', this.checkScrollButtons)
    })
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.checkScrollButtons)
  },
  methods: {
    scrollLeft() {
      const el = this.$refs.tabsContainer
      const pageWidth = el.clientWidth
      const newScrollLeft = Math.max(0, el.scrollLeft - pageWidth)

      el.scrollTo({ left: newScrollLeft, behavior: 'smooth' })

      // recheck after scroll ends
      setTimeout(this.checkScrollButtons, 400)
    },
    scrollRight() {
      const el = this.$refs.tabsContainer
      const pageWidth = el.clientWidth
      const maxScrollLeft = el.scrollWidth - pageWidth
      const newScrollLeft = Math.min(maxScrollLeft, el.scrollLeft + pageWidth)

      el.scrollTo({ left: newScrollLeft, behavior: 'smooth' })

      // recheck after scroll ends
      setTimeout(this.checkScrollButtons, 400)
    },
    checkScrollButtons() {
      const el = this.$refs.tabsContainer
      if (!el) return

      this.showLeftButton = el.scrollLeft > 0
      this.showRightButton = el.scrollLeft + el.clientWidth < el.scrollWidth - 1 // evita redondeo
    }
  }
}