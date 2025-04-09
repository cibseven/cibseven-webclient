<template>
  <div class="h-100 d-flex flex-column bg-light">
    <div class="overflow-auto h-100">
      <b-button style="top: 2px; right: 60px" class="border-0 position-absolute"
        variant="btn-outline-primary" size="sm" :disabled="loading" :title="$t('decision.loadVersions')"
        @click="getVersions(false)">
        <span class="mdi mdi-18px mdi-cog-refresh-outline"></span>
      </b-button>
      <b-button style="top: 2px; right: 30px" class="border-0 position-absolute"
        variant="btn-outline-primary" size="sm" :disabled="loading" :title="$t('decision.refreshVersions')"
        @click="getVersions(true)">
        <span class="mdi mdi-18px mdi-refresh"></span>
      </b-button>
      <b-list-group class="mx-3 mb-3">
        <b-list-group-item @click="selectVersion(version)" v-for="version of versions" :key="version.id"
        class="rounded-0 mt-3 p-2 bg-white border-0" :class="markSelectedVersion(version.version) ? 'active shadow' : ''" style="cursor: pointer">
          <div class="d-flex align-items-center">
            <h6 style="font-size: 1rem">
              <span class="font-weight-bold">{{ $t('decision.details.definitionVersion') + ': ' + version.version }}</span>
            </h6>
            <div class="d-flex ms-auto" :id="version.id">
              <span class="mdi mdi-18px mdi-information-outline text-info"></span>
            </div>
          </div>
          <div class="mb-1">
            <div>{{ $t('decision.details.totalInstances') + ': ' + version.allInstances }}</div>
          </div>
          <b-popover :target="version.id" triggers="hover" placement="right" boundary="viewport" max-width="350px">
            <DecisionDefinitionDetails :version="version" @onUpdateHistoryTimeToLive="onUpdateHistoryTimeToLive"></DecisionDefinitionDetails>
          </b-popover>
        </b-list-group-item>
      </b-list-group>
    </div>
    <SuccessAlert ref="successOperation"> {{ $t('alert.successOperation') }}</SuccessAlert>
  </div>
</template>

<script>
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'
import DecisionDefinitionDetails from '@/components/decision/DecisionDefinitionDetails.vue'

export default {
  name: 'DecisionVersionListSidebar',
  components: { SuccessAlert, DecisionDefinitionDetails },
  mixins: [copyToClipboardMixin],
  props: {
    versions: Object
  },
  data() {
    return {
      loading: false
    }
  },
  computed: {
    decisionName() {
      return this.decision.name ? this.decision.name : this.decision.key
    }
  },
  methods: {
    selectVersion(version) {
      this.$router.push({ name: 'decision-version', params: { decisionKey: version.key, versionIndex: version.version } })
    },
    onUpdateHistoryTimeToLive(versionId, historyTimeToLive) {
      const version = this.versions.find(v => v.id === versionId)
      if (version) {
        version.historyTimeToLive = historyTimeToLive
        this.$refs.successOperation.show()
      }
    },
    markSelectedVersion(versionToCheck) {
      return String(versionToCheck) === this.$route.params.versionIndex
    }
  }
}
</script>
