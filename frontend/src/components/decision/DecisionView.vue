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
    <SidebarsFlow ref="sidebars" class="border-top" :left-open="leftOpen" @update:left-open="onLeftOpenChanged" :left-caption="shortendLeftCaption">
      <template v-slot:left>
        <DecisionVersionListSidebar v-if="versions.length > 0" ref="navbar"
          :versions="versions" @refresh-decision-versions="loadDecisionVersionsByKey(decisionKey, versionIndex, $event)"></DecisionVersionListSidebar>
      </template>
      <router-view
        v-if="decision && versionLoaded"
        :key="$route.fullPath"
        :loading="loading"
        :left-open="leftOpen"
      />
    </SidebarsFlow>
  </div>
</template>

<script>

import { DecisionService } from '@/services.js'
import DecisionVersionListSidebar from '@/components/decision/DecisionVersionListSidebar.vue'
import { SidebarsFlow } from '@cib/common-frontend'
import { mapGetters, mapActions, mapMutations } from 'vuex'
import sidebarOpenPersistenceMixin from '@/mixins/sidebarOpenPersistenceMixin.js'

const SIDEBAR_SCOPE = 'decision'

export default {
  name: 'DecisionView',
  components: { DecisionVersionListSidebar, SidebarsFlow },
  mixins: [sidebarOpenPersistenceMixin],
  props: {
    decisionKey: String,
    versionIndex: { type: String, default: '' },
    tenantId: { type: String, default: null }
  },
  data() {
    return {
      leftOpen: this.getSavedLeftOpen(SIDEBAR_SCOPE),
      rightOpen: false,
      filter: '',
      loading: false,
      versionLoaded: false,
      // Counts the loads, so that an answer to an older one cannot overwrite a newer one
      loadRequest: 0
    }
  },
  watch: {
    // One watcher for both: a navigation can change the tenant and the version at once
    routeSelection(selection, previous) {
      if (selection.tenantId !== previous.tenantId) {
        this.loadDecisionVersionsByKey(this.decisionKey, this.versionIndex, this.$root.config.lazyLoadHistory)
      } else if (selection.versionIndex !== previous.versionIndex && selection.versionIndex) {
        const version = this.getDecisionVersion({ key: this.decisionKey, version: selection.versionIndex })
        if (version) this.selectVersion(this.decisionKey, version)
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
    },
    routeSelection() {
      return { tenantId: this.tenantId ?? null, versionIndex: this.versionIndex }
    }
  },
  mounted() {
    this.loadDecisionVersionsByKey(this.decisionKey, this.versionIndex, this.$root.config.lazyLoadHistory)
  },
  methods: {
    ...mapActions(['getDecisionVersionsByKey']),
    ...mapMutations(['setSelectedDecisionVersion', 'updateVersion']),
    onLeftOpenChanged(isOpen) {
      this.leftOpen = isOpen
      this.saveLeftOpen(SIDEBAR_SCOPE, isOpen)
    },
    async loadDecisionVersionsByKey(decisionKey, versionIndex, lazyLoad) {
      const request = ++this.loadRequest
      this.versionLoaded = false
      const tenantId = await this.tenantToOpen()
      if (request !== this.loadRequest) return
      if (tenantId !== (this.tenantId ?? null)) {
        // The watcher loads again for the corrected tenant
        this.replaceTenant(tenantId)
        return
      }

      const versions = await this.getDecisionVersionsByKey({ key: decisionKey, lazyLoad, tenantId })
      if (request !== this.loadRequest) return
      if (!versions?.length) {
        // An outdated link: the tenant has no version of this key any more
        if (tenantId) return this.replaceTenant(null)
        // A key deployed only for tenants: open the tenant of its newest version
        const everyTenantsVersions = await DecisionService.getDecisionVersionsByKey(decisionKey, true)
        if (request === this.loadRequest && everyTenantsVersions?.length) this.replaceTenant(everyTenantsVersions[0].tenantId)
        return
      }
      let version = versionIndex ? this.getDecisionVersion({ key: decisionKey, version: versionIndex }) : null
      if (!version) {
        // Not in this tenant: its latest version, keeping the rest of the query (e.g. ?tab=)
        version = versions[0]
        this.$router.replace({
          name: 'decision-version',
          params: { decisionKey, versionIndex: String(version.version) },
          query: this.$route.query
        })
      }
      this.selectVersion(decisionKey, version)
      this.versionLoaded = true
    },
    // The tenant whose versions to show: an instance's own, so that a link without (or with a wrong)
    // tenant still opens the instance; otherwise the link's
    async tenantToOpen() {
      const instanceId = this.$route.params.instanceId
      if (instanceId) {
        const instances = await DecisionService.getHistoricDecisionInstances({ decisionInstanceId: instanceId, maxResults: 1 })
        if (instances?.length) return instances[0].tenantId ?? null
      }
      return this.tenantId ?? null
    },
    replaceTenant(tenantId) {
      const query = { ...this.$route.query }
      if (tenantId) query.tenantId = tenantId
      else delete query.tenantId
      this.$router.replace({ query })
    },
    selectVersion(decisionKey, version) {
      this.setSelectedDecisionVersion({ key: decisionKey, version: String(version.version) })
      DecisionService.getDecisionDefinitionById(version.id, true).then(result => {
        if (result) this.updateVersion({ key: decisionKey, newVersion: result })
      })
    }
  }
}
</script>
