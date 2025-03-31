<template>
  <div class="d-flex flex-column">
    <div class="d-flex ps-3 py-2">
      <b-button :title="$t('start.admin')" variant="outline-secondary" :to="{ name: 'decision-list' }" class="mdi mdi-18px mdi-arrow-left border-0"></b-button>
      <h4 class="ps-1 m-0 align-items-center d-flex" style="border-width: 3px !important">{{ decisionName }}</h4>
    </div>
    <SidebarsFlow ref="sidebars" class="border-top overflow-auto" v-model:left-open="leftOpen" :left-caption="shortendLeftCaption">
      <template v-slot:left>
        <DecisionDetailsSidebar v-if="versions" ref="navbar" :versions="versions"></DecisionDetailsSidebar>
      </template>
      <router-view
        v-if="decision"
        :key="$route.fullPath"
        :loading="loading"
        :first-result="firstResult"
        :max-results="maxResults"
        @show-more="showMore"
        @instance-deleted="deleteInstance"
      />
    </SidebarsFlow>
  </div>
</template>

<script>
import DecisionDetailsSidebar from '@/components/decision/DecisionDetailsSidebar.vue'
import SidebarsFlow from '@/components/common-components/SidebarsFlow.vue'
import { mapGetters, mapActions, mapMutations } from 'vuex'

export default {
  name: 'DecisionView',
  components: { DecisionDetailsSidebar, SidebarsFlow },
  props: {
    decisionKey: String,
    versionIndex: { type: String, default: '' }
  },
  data() {
    return {
      leftOpen: true,
      rightOpen: false,
      firstResult: 0,
      maxResults: this.$root.config.maxProcessesResults,
      filter: '',
      loading: false
    }
  },
  watch: {
    '$route.params.versionIndex': {
      immediate: true,
      handler(versionIndex) {
        if (versionIndex && this.$store.state.decision.list.length > 0) {
          this.setSelectedDecisionVersion({ key: this.decisionKey, version: this.versionIndex})
        }
      }
    }
  },
  computed: {
    ...mapGetters(['getSelectedDecisionVersion', 'getDecisionVersions']),

    shortendLeftCaption() {
      return this.$t('decision.details.historyVersions')
    },
    decision() {
      return this.getSelectedDecisionVersion()
    },
    versions() {
      return this.getDecisionVersions(this.decisionKey)
    },
    decisionName() {
      if (!this.decision) return this.$t('decision.decision')
      return this.decision.name ? this.decision.name : this.decision.key
    }
  },
  async mounted() {
    await this.loadDecisionVersionsByKey(this.decisionKey, this.versionIndex)
  },
  methods: {
    ...mapActions(['getDecisionVersionsByKey']),
    ...mapMutations(['setSelectedDecisionVersion']),

    loadDecisionVersionsByKey(decisionKey, versionIndex) {
      this.getDecisionVersionsByKey({ key: decisionKey }).then(decisions => {
        if (!versionIndex) {
          versionIndex = decisions[0].version
          this.setSelectedDecisionVersion({key: decisionKey, version: versionIndex })
          this.$router.push('/seven/auth/decision/' + decisionKey + '/' + versionIndex)
        }
        this.setSelectedDecisionVersion({key: decisionKey, version: versionIndex })
      })
    },

    showMore() {
      this.firstResult += this.$root.config.maxProcessesResults
      this.loadInstances(true)
    }
  }
}
</script>
