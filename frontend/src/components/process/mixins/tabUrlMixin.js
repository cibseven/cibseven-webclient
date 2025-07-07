/**
 * Mixin for synchronizing activeTab with URL query parameters
 * 
 * This mixin provides:
 * - Automatic initialization of activeTab from URL query parameter
 * - URL updates when activeTab changes
 * - Bidirectional synchronization between route and activeTab
 * - Browser history support for tab navigation (back/forward buttons)
 * 
 * Usage:
 * 1. Import the mixin: import tabUrlMixin from '@/components/process/mixins/tabUrlMixin.js'
 * 2. Add to mixins array: mixins: [tabUrlMixin]
 * 3. Set defaultTab in data
 */

export default {
  data() {
    return {
      activeTab: ''
    }
  },
  created() {
    // Initialize activeTab from URL or default
    this.activeTab = this.$route.query.tab || this.defaultTab
  },
  watch: {
    // Watch for URL changes (browser back/forward) and update activeTab
    '$route.query.tab': {
      handler(newTab) {
        const targetTab = newTab || this.defaultTab
        if (targetTab !== this.activeTab) {
          this.activeTab = targetTab
        }
      },
      immediate: false
    },
    // Watch for activeTab changes and update URL
    activeTab: function(newTab) {
      this.updateUrlTab(newTab)
    }
  },
  methods: {
    /**
     * Updates the URL query parameter for the current tab
     * @param {string} tab - The tab identifier
     */
    updateUrlTab(tab) {
      const query = { ...this.$route.query }
      query.tab = tab
      // Use push to add to browser history for tab navigation
      this.$router.push({ name: this.$route.name, params: this.$route.params, query })
    },
    /**
     * Changes the active tab and updates URL
     * @param {Object} selectedTab - Tab object with id property
     */
    changeTab(selectedTab) {
      this.activeTab = selectedTab.id
      // URL update is handled by the activeTab watcher
    }
  }
}
