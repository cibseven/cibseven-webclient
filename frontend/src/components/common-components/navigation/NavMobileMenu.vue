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
  <nav class="cib-nav-mobile" :aria-label="$t('navigation.menu')">
    <slot name="prepend"></slot>
    <template v-for="(list, listIdx) in lists" :key="listIdx">
      <div v-if="listIdx > 0" class="cib-nav-mobile-separator" aria-hidden="true"></div>
      <ul class="cib-nav-mobile-list list-unstyled m-0">
        <li v-for="cluster in list" :key="cluster.id" class="cib-nav-mobile-cluster">
          <button v-if="hasMenu(cluster)" type="button" class="cib-nav-mobile-row"
            :aria-expanded="expanded === cluster.id ? 'true' : 'false'" @click="toggle(cluster.id)">
            <span v-if="cluster.avatar" class="mdi mdi-24px mdi-account" aria-hidden="true"></span>
            <span v-else :class="['mdi', 'mdi-24px', cluster.icon]" aria-hidden="true"></span>
            <span class="cib-nav-mobile-label">{{ cluster.label }}</span>
            <span :class="['mdi', 'mdi-24px', expanded === cluster.id ? 'mdi-chevron-down' : 'mdi-chevron-right']" aria-hidden="true"></span>
          </button>
          <router-link v-else-if="cluster.to" :to="cluster.to" class="cib-nav-mobile-row" @click="$emit('navigate')">
            <span :class="['mdi', 'mdi-24px', cluster.icon]" aria-hidden="true"></span>
            <span class="cib-nav-mobile-label">{{ cluster.label }}</span>
          </router-link>
          <button v-else type="button" class="cib-nav-mobile-row" @click="$emit('select', cluster)">
            <span :class="['mdi', 'mdi-24px', cluster.icon]" aria-hidden="true"></span>
            <span class="cib-nav-mobile-label">{{ cluster.label }}</span>
          </button>

          <div v-if="expanded === cluster.id && hasMenu(cluster)">
            <div v-if="$slots['cluster-prepend']" class="cib-nav-mobile-prepend"><slot name="cluster-prepend" :cluster="cluster"></slot></div>
            <ul v-for="(section, sIdx) in cluster.sections" :key="sIdx" class="cib-nav-mobile-section list-unstyled m-0">
              <li v-for="item in section" :key="item.key">
                <router-link v-if="item.to" :to="item.to" class="cib-nav-mobile-item" :class="{ active: item.active }"
                  :aria-current="item.active ? 'page' : null" @click="$emit('navigate')">
                  <span v-if="item.tag" class="cib-nav-mobile-tag">{{ item.tag }}</span>{{ item.label }}
                </router-link>
                <a v-else-if="item.href" :href="item.href" class="cib-nav-mobile-item"
                  :target="item.external ? '_blank' : null" :rel="item.external ? 'noopener noreferrer' : null" @click="$emit('navigate')">
                  {{ item.label }}<span v-if="item.external" class="mdi mdi-14px mdi-open-in-new ms-1" aria-hidden="true"></span>
                </a>
                <button v-else type="button" class="cib-nav-mobile-item" :class="{ active: item.active }"
                  :aria-current="item.checkable && item.active ? 'true' : null" @click="$emit('select', item)">
                  <span v-if="item.tag" class="cib-nav-mobile-tag">{{ item.tag }}</span>{{ item.label }}
                </button>
              </li>
            </ul>
          </div>
        </li>
      </ul>
    </template>
  </nav>
</template>

<script>
/**
 * Full-screen mobile navigation: every cluster is an accordion row (tapping a row expands it,
 * it does not navigate), main clusters first, then the standard menus (help, language, user).
 * Uses the same cluster/item model as NavCluster. Opens with the current section expanded.
 */
export default {
  name: 'NavMobileMenu',
  props: {
    main: { type: Array, default: () => [] },
    utility: { type: Array, default: () => [] }
  },
  emits: ['select', 'navigate'],
  data() {
    return {
      expanded: this.main.find(cluster => cluster.active)?.id ?? null
    }
  },
  computed: {
    lists() {
      return [this.main, this.utility].filter(list => list.length)
    }
  },
  // Prepended cluster items come from the parent and don't close the menu themselves (e.g. one that
  // opens a modal); bound here rather than in the template, as their wrapper is not interactive.
  // The menu covers the page, so the page behind it must not scroll (notably on iOS).
  mounted() {
    this.$el.addEventListener('click', this.onClick)
    this.previousBodyOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
  },
  beforeUnmount() {
    this.$el.removeEventListener('click', this.onClick)
    document.body.style.overflow = this.previousBodyOverflow
  },
  methods: {
    hasMenu(cluster) {
      return !!cluster.sections?.some(section => section.length)
    },
    toggle(id) {
      this.expanded = this.expanded === id ? null : id
    },
    onClick(event) {
      if (event.target.closest?.('.cib-nav-mobile-prepend')) this.$emit('navigate')
    }
  }
}
</script>

<style scoped>
.cib-nav-mobile {
  position: fixed;
  inset: 55px 0 0 0;
  overflow: auto;
  background: #fff;
  z-index: 1029;
}
.cib-nav-mobile-cluster {
  border-top: 1px solid var(--bs-gray-500);
}
/* Closing line under the very last row only; between the lists the separator draws the line */
.cib-nav-mobile-list:last-of-type > .cib-nav-mobile-cluster:last-child {
  border-bottom: 1px solid var(--bs-gray-500);
}
/* One stronger line between main and standard clusters, replacing the row's own top border */
.cib-nav-mobile-separator {
  border-top: 2px solid var(--bs-gray-500);
}
.cib-nav-mobile-separator + .cib-nav-mobile-list > .cib-nav-mobile-cluster:first-child {
  border-top: 0;
}
.cib-nav-mobile-row {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  min-height: 44px;
  padding: 0 12px 0 16px;
  border: 0;
  background: transparent;
  color: var(--bs-gray-900);
  text-align: left;
  text-decoration: none;
}
.cib-nav-mobile-row > .mdi:first-child {
  color: var(--bs-secondary);
}
.cib-nav-mobile-row > .mdi:last-child:not(:first-child) {
  color: var(--bs-gray-800);
}
.cib-nav-mobile-label {
  flex: 1;
  min-width: 0;
  font-size: 1rem;
  font-weight: 600;
}
.cib-nav-mobile-section {
  padding: 4px 0;
  border-top: 1px solid var(--bs-gray-400);
}
.cib-nav-mobile-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 34px;
  padding: 6px 16px 6px 54px;
  border: 0;
  background: transparent;
  color: var(--bs-gray-800);
  font-size: 14px;
  text-align: left;
  text-decoration: none;
}
.cib-nav-mobile-item.active {
  background: var(--bs-gray-300);
  color: var(--bs-gray-900);
}
.cib-nav-mobile-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--bs-primary);
}
.cib-nav-mobile-tag {
  padding: 1px 5px;
  border-radius: 2px;
  background: var(--bs-gray-300);
  font-size: 11.2px;
  font-weight: 700;
}
</style>
