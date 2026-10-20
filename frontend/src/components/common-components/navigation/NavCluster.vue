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
  <div class="cib-nav-cluster" :class="{ 'cib-nav-cluster-current': cluster.active, 'cib-nav-cluster-open': open }"
    role="group" :aria-label="cluster.label">

    <!-- Split control: the icon leads to the section, the chevron opens its menu -->
    <template v-if="isSplit">
      <router-link ref="main" :to="cluster.to" class="cib-nav-btn cib-nav-btn-main"
        :aria-label="cluster.label" :aria-describedby="tooltipId"
        @mouseenter="showTip = true" @mouseleave="showTip = false" @focus="showTip = true" @blur="showTip = false">
        <span class="cib-nav-current-bar" aria-hidden="true"></span>
        <span v-if="cluster.avatar" class="cib-nav-avatar" aria-hidden="true">{{ cluster.avatar }}</span>
        <span v-else :class="['mdi', 'mdi-24px', cluster.icon]" aria-hidden="true"></span>
        <span v-if="cluster.badge" class="cib-nav-badge" aria-hidden="true">{{ cluster.badge }}</span>
      </router-link>
      <button v-if="hasMenu" ref="toggle" type="button" class="cib-nav-btn cib-nav-btn-chevron"
        :aria-label="$t('cib-header.openMenu', { label: cluster.label })" aria-haspopup="true" :aria-expanded="open ? 'true' : 'false'"
        @click="toggle" @pointerenter="hoverOpen" @keydown.down.prevent="openAndFocusFirst" @keydown.esc="closeAndFocus">
        <span class="mdi mdi-18px mdi-chevron-down" aria-hidden="true"></span>
      </button>
    </template>

    <!-- Single control: one button, for menus without a landing page (engine, help, language) or plain actions -->
    <button v-else ref="toggle" type="button" class="cib-nav-btn cib-nav-btn-single"
      :aria-label="cluster.label" :aria-describedby="tooltipId"
      :aria-haspopup="hasMenu ? 'true' : null" :aria-expanded="hasMenu ? (open ? 'true' : 'false') : null"
      @click="hasMenu ? toggle() : $emit('select', cluster)" @pointerenter="hasMenu ? hoverOpen($event) : (showTip = true)"
      @mouseenter="showTip = !hasMenu" @mouseleave="showTip = false" @focus="showTip = !open" @blur="showTip = false"
      @keydown.down.prevent="hasMenu && openAndFocusFirst()" @keydown.esc="closeAndFocus">
      <span class="cib-nav-current-bar" aria-hidden="true"></span>
      <span v-if="cluster.avatar" class="cib-nav-avatar" aria-hidden="true">{{ cluster.avatar }}</span>
      <span v-else :class="['mdi', 'mdi-24px', cluster.icon]" aria-hidden="true"></span>
      <span v-if="hasMenu" class="mdi mdi-18px mdi-chevron-down" aria-hidden="true"></span>
    </button>

    <span v-if="showTip && !open" :id="tooltipId" role="tooltip" class="cib-nav-tooltip">{{ cluster.label }}</span>

    <div v-if="open" ref="menu" class="cib-nav-menu" :class="align === 'end' ? 'cib-nav-menu-end' : 'cib-nav-menu-start'"
      role="menu" tabindex="-1" :aria-label="cluster.label" @keydown.down.prevent="moveFocus(1)" @keydown.up.prevent="moveFocus(-1)"
      @keydown.esc="closeAndFocus">
      <router-link v-if="cluster.titleTo" :to="cluster.titleTo" class="cib-nav-menu-title cib-nav-menu-title-link"
        role="menuitem" @click="close">{{ cluster.label }}</router-link>
      <div v-else class="cib-nav-menu-title" role="presentation">{{ cluster.label }}</div>
      <div v-for="(section, sIdx) in cluster.sections" :key="sIdx" class="cib-nav-menu-section" role="group">
        <template v-for="item in section" :key="item.key">
          <router-link v-if="item.to" :to="item.to" class="cib-nav-menu-item" :class="{ active: item.active }"
            role="menuitem" :title="item.tooltip" :aria-current="item.active ? 'page' : null" @click="close">
            <span v-if="item.tag" class="cib-nav-menu-tag">{{ item.tag }}</span>{{ item.label }}
          </router-link>
          <a v-else-if="item.href" :href="item.href" class="cib-nav-menu-item" role="menuitem" :title="item.tooltip"
            :target="item.external ? '_blank' : null" :rel="item.external ? 'noopener noreferrer' : null" @click="close">
            {{ item.label }}<span v-if="item.external" class="mdi mdi-14px mdi-open-in-new ms-1" aria-hidden="true"></span>
          </a>
          <button v-else type="button" class="cib-nav-menu-item" :class="{ active: item.active }" role="menuitem"
            :title="item.tooltip" :aria-checked="item.checkable ? (item.active ? 'true' : 'false') : null"
            @click="select(item)">
            <span v-if="item.tag" class="cib-nav-menu-tag">{{ item.tag }}</span>{{ item.label }}
          </button>
        </template>
      </div>
    </div>
  </div>
</template>

<script>
let uid = 0

// Grace period before a hover-opened menu closes, so moving the pointer from the chevron
// down into the menu (or across a small gap) does not close it.
const HOVER_CLOSE_DELAY = 150

/**
 * One control of the top navigation (a "cluster"): either a split control (icon to the
 * section's landing page + chevron for its menu) or a single button (menu only, or a plain
 * action). The menu opens on click and on mouse hover; which cluster is open is owned by
 * the parent (v-model:open) so only one menu is open at a time.
 *
 * cluster: { id, label, icon, avatar?, badge?, to?, titleTo?, active?, sections: [[item]] }
 * item:    { key, label, tooltip?, to?, href?, external?, tag?, active?, checkable? }
 * Items without to/href are emitted through `select`.
 */
export default {
  name: 'NavCluster',
  props: {
    cluster: { type: Object, required: true },
    open: { type: Boolean, default: false },
    // Which edge of the control the menu lines up with
    align: { type: String, default: 'start' }
  },
  emits: ['update:open', 'select'],
  data() {
    return {
      showTip: false,
      closeTimer: null,
      tooltipId: `cib-nav-tip-${++uid}`
    }
  },
  computed: {
    hasMenu() {
      return !!this.cluster.sections?.some(section => section.length)
    },
    isSplit() {
      return !!this.cluster.to
    }
  },
  watch: {
    open(isOpen) {
      if (isOpen) {
        this.showTip = false
        document.addEventListener('mousedown', this.onOutside)
      } else {
        document.removeEventListener('mousedown', this.onOutside)
      }
    }
  },
  // Keeps a hover-opened menu alive while the pointer or the focus is anywhere in the cluster
  // (bound here rather than in the template: the wrapper itself is not an interactive element)
  mounted() {
    this.$el.addEventListener('mouseenter', this.cancelClose)
    this.$el.addEventListener('mouseleave', this.scheduleClose)
    this.$el.addEventListener('focusin', this.cancelClose)
    this.$el.addEventListener('focusout', this.onFocusOut)
  },
  beforeUnmount() {
    clearTimeout(this.closeTimer)
    document.removeEventListener('mousedown', this.onOutside)
    this.$el.removeEventListener('mouseenter', this.cancelClose)
    this.$el.removeEventListener('mouseleave', this.scheduleClose)
    this.$el.removeEventListener('focusin', this.cancelClose)
    this.$el.removeEventListener('focusout', this.onFocusOut)
  },
  methods: {
    setOpen(value) {
      if (value !== this.open) this.$emit('update:open', value)
    },
    toggle() {
      this.cancelClose()
      this.setOpen(!this.open)
    },
    close() {
      this.cancelClose()
      this.setOpen(false)
    },
    closeAndFocus() {
      if (!this.open) return
      this.close()
      this.$refs.toggle?.focus()
    },
    // Touch and pen open menus by tapping; only a mouse opens them by hovering
    hoverOpen(event) {
      if (event?.pointerType && event.pointerType !== 'mouse') return
      if (!this.hasMenu) return
      this.cancelClose()
      this.setOpen(true)
    },
    scheduleClose() {
      if (!this.open) return
      this.cancelClose()
      this.closeTimer = setTimeout(() => this.setOpen(false), HOVER_CLOSE_DELAY)
    },
    cancelClose() {
      clearTimeout(this.closeTimer)
      this.closeTimer = null
    },
    // Tabbing out of the cluster closes its menu
    onFocusOut(event) {
      if (this.open && !this.$el.contains(event.relatedTarget)) this.close()
    },
    onOutside(event) {
      if (!this.$el.contains(event.target)) this.close()
    },
    select(item) {
      this.close()
      this.$emit('select', item)
    },
    menuItems() {
      return Array.from(this.$refs.menu?.querySelectorAll('[role="menuitem"]') || [])
    },
    async openAndFocusFirst() {
      this.setOpen(true)
      await this.$nextTick()
      this.menuItems()[0]?.focus()
    },
    moveFocus(step) {
      const items = this.menuItems()
      if (!items.length) return
      const index = items.indexOf(document.activeElement)
      items[(index + step + items.length) % items.length].focus()
    }
  }
}
</script>

<style scoped>
.cib-nav-cluster {
  position: relative;
  display: flex;
  align-items: center;
}
.cib-nav-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  height: 36px;
  border: 0;
  background: transparent;
  color: var(--bs-gray-800);
  text-decoration: none;
  cursor: pointer;
}
.cib-nav-btn:hover,
.cib-nav-btn:focus-visible,
.cib-nav-cluster-open .cib-nav-btn-chevron,
.cib-nav-cluster-open .cib-nav-btn-single {
  background: var(--bs-gray-300);
  color: var(--bs-gray-900);
}
.cib-nav-btn:active {
  background: var(--bs-gray-400);
}
.cib-nav-btn-main {
  padding: 0 4px 0 6px;
  border-radius: 4px 0 0 4px;
}
.cib-nav-btn-chevron {
  padding: 0 6px 0 2px;
  border-radius: 0 4px 4px 0;
}
.cib-nav-btn-single {
  gap: 2px;
  padding: 0 6px;
  border-radius: 4px;
}
/* Current section: dark icon plus a red bar on the header's bottom edge */
.cib-nav-cluster-current .cib-nav-btn {
  color: var(--bs-gray-900);
}
.cib-nav-current-bar {
  position: absolute;
  left: -3px;
  bottom: -10px;
  width: 40px;
  height: 3px;
  pointer-events: none;
}
.cib-nav-cluster-current .cib-nav-current-bar {
  background: var(--bs-primary);
}
.cib-nav-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 50rem;
  background: var(--bs-gray-300);
  font-size: 12px;
  font-weight: 600;
}
.cib-nav-badge {
  position: absolute;
  left: 0;
  bottom: 2px;
  min-width: 14px;
  height: 14px;
  padding: 0 3px;
  border-radius: 50rem;
  background: var(--bs-info);
  color: #fff;
  font-size: 9px;
  font-weight: 700;
  line-height: 14px;
  text-align: center;
}
.cib-nav-tooltip {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 8px;
  padding: 6px 8px;
  border-radius: .3rem;
  background: var(--bs-gray-900);
  color: #fff;
  font-size: 12px;
  line-height: 1.25;
  white-space: nowrap;
  pointer-events: none;
  z-index: 1045;
}
.cib-nav-menu {
  position: absolute;
  top: 100%;
  min-width: 236px;
  /* margin, not padding: the hover gap still belongs to the cluster via the delayed close */
  margin-top: 6px;
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--bs-gray-600);
  border-radius: .3rem;
  box-shadow: 0 8px 8px 0 rgba(89, 121, 155, .2);
  z-index: 1040;
  animation: cib-nav-drop 120ms cubic-bezier(.2, 0, .2, 1);
}
.cib-nav-menu-start {
  left: 0;
}
.cib-nav-menu-end {
  right: 0;
  min-width: 220px;
}
@keyframes cib-nav-drop {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: none; }
}
.cib-nav-menu-title {
  display: block;
  padding: 12px 16px;
  border-bottom: 1px solid var(--bs-gray-500);
  color: var(--bs-gray-900);
  font-size: 1rem;
  font-weight: 600;
  text-decoration: none;
}
.cib-nav-menu-title-link:hover,
.cib-nav-menu-title-link:focus-visible {
  background: var(--bs-gray-200);
}
.cib-nav-menu-section {
  padding: 4px 0;
  border-bottom: 1px solid var(--bs-gray-400);
}
.cib-nav-menu-section:last-child {
  border-bottom: 0;
}
.cib-nav-menu-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 7px 16px;
  border: 0;
  background: transparent;
  color: var(--bs-gray-800);
  font-size: 14px;
  text-align: left;
  text-decoration: none;
  white-space: nowrap;
  cursor: pointer;
}
.cib-nav-menu-item:hover,
.cib-nav-menu-item:focus-visible {
  background: var(--bs-gray-200);
  color: var(--bs-gray-900);
}
.cib-nav-menu-item.active {
  background: var(--bs-gray-300);
  color: var(--bs-gray-900);
}
.cib-nav-menu-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: var(--bs-primary);
}
.cib-nav-menu-tag {
  padding: 1px 5px;
  border-radius: 2px;
  background: var(--bs-gray-300);
  color: var(--bs-gray-800);
  font-size: 11.2px;
  font-weight: 700;
}
</style>
