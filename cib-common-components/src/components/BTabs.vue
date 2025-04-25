<template>
  <div :class="customClass">
    <nav>
      <div class="nav nav-tabs" role="tablist">
        <button v-for="(tab) in tabs" :key="tab.id"
          class="nav-link" :class="[titleLinkClass, { active: activeTab === tab.id }]"
          :id="'nav-' + tab.id + '-tab'"
          data-bs-toggle="tab" :data-bs-target="'#nav-' + tab.id"
          type="button" role="tab"
          :aria-controls="'nav-' + tab.id" :aria-selected="activeTab === tab.id"
          @click="selectTab(tab.id)">
          {{ tab.title }}
        </button>
      </div>
    </nav>
    <div class="tab-content" :class="contentClass">
      <slot></slot>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    class: {
      type: String,
      default: ''
    },
    contentClass: {
      type: String,
      default: ''
    },
    titleLinkClass: {
      type: String,
      default: ''
    }
  },
  data: function () {
    return {
      tabs: [],
      activeTab: null,
      tabClickHandlers: []
    };
  },
  computed: {
    customClass: function () {
      return this.class
    }
  },
  methods: {
    selectTab: function (tabId) {
      this.activeTab = tabId
      this.notifyTabClick(tabId)
    },
    registerTab: function (tab) {
      this.tabs.push(tab)
      if (this.tabs.length === 1) {
        this.activeTab = tab.id
      }
    },
    registerClickHandler: function (callback) {
      this.tabClickHandlers.push(callback)
    },
    notifyTabClick: function (tabId) {
      this.tabClickHandlers.forEach(callback => callback(tabId))
    }
  },
  provide() {
    return {
      registerTab: this.registerTab,
      activeTab: () => this.activeTab,
      registerClickHandler: this.registerClickHandler
    }
  },
  mounted() {
    if (this.tabs.length > 0) {
      this.activeTab = this.tabs[0].id
    }
  }
}
</script>

<style scoped>
</style>
