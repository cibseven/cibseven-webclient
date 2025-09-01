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
  <div class="d-flex flex-column">
    <div class="d-flex ps-3 py-2">
      <b-button :title="$t('start.cockpit.decisions.title')" variant="outline-secondary" :to="{ name: 'decision-list' }" class="mdi mdi-18px mdi-arrow-left border-0"></b-button>
      <h4 class="ps-1 m-0 align-items-center d-flex" style="border-width: 3px !important">{{ decisionName }}</h4>
    </div>
    <SidebarsFlow ref="sidebars" class="border-top overflow-auto" v-model:left-open="leftOpen" :left-caption="shortendLeftCaption">
      <template v-slot:left>
        <DecisionVersionListSidebar v-if="versions.length > 0" ref="navbar"
          :versions="versions" @refresh-decision-versions="loadDecisionVersionsByKey(decisionKey, versionIndex, $event)"></DecisionVersionListSidebar>
      </template>
      <router-view
        v-if="decision && versionLoaded"
        :key="$route.fullPath"
        :loading="loading"
      />
    </SidebarsFlow>
  </div>
</template>

<script>

import { DecisionService } from '@/services.js'
import DecisionVersionListSidebar from '@/components/decision/DecisionVersionListSidebar.vue'
import SidebarsFlow from '@/components/common-components/SidebarsFlow.vue'
import { mapGetters, mapActions, mapMutations } from 'vuex'

export default {
  name: 'DecisionView',
  components: { DecisionVersionListSidebar, SidebarsFlow },
  props: {
    decisionKey: String,
    versionIndex: { type: String, default: '' }
  },
  data() {
    return {
      leftOpen: true,
      rightOpen: false,
      filter: '',
      loading: false,
      versionLoaded: false
    }
  },
  watch: {
    '$route.params.versionIndex': {
      handler(versionIndex) {
        if (versionIndex && this.$store.state.decision.list.length > 0) {
          const version = this.getDecisionVersion({ key: this.decisionKey, version: this.versionIndex })
          DecisionService.getDecisionDefinitionById(version.id, true).then(result => {
            if (result) this.updateVersion({ key: this.decisionKey, newVersion: result })
          })
          this.setSelectedDecisionVersion({ key: this.decisionKey, version: this.versionIndex })
        }
      }
    }
  },
  computed: {
    ...mapGetters(['getSelectedDecisionVersion', 'getDecisionVersions', 'getDecisionVersion']),
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
  mounted() {
    this.loadDecisionVersionsByKey(this.decisionKey, this.versionIndex, this.$root.config.lazyLoadHistory)
  },
  methods: {
    ...mapActions(['getDecisionVersionsByKey']),
    ...mapMutations(['setSelectedDecisionVersion', 'updateVersion']),
    loadDecisionVersionsByKey(decisionKey, versionIndex, lazyLoad) {
      this.versionLoaded = false
      this.getDecisionVersionsByKey({ key: decisionKey, lazyLoad: lazyLoad })
      .then(versions => {
        let version = versions[0]
        this.setSelectedDecisionVersion({ key: decisionKey, version: versionIndex })
        if (!versionIndex || versionIndex === 'undefined') {
          versionIndex = version.version
          this.$router.push(`/seven/auth/decision/${decisionKey}/${versionIndex}`)
        } else {
          version = this.getDecisionVersion({ key: decisionKey, version: versionIndex })
        }
        DecisionService.getDecisionDefinitionById(version.id, true).then(result => {
          if (result) this.updateVersion({ key: decisionKey, newVersion: result })
        })
        this.versionLoaded = true
      })
    }
  }
}
</script>
