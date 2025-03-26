<template>
  <div class="h-100 d-flex flex-column bg-light">
    <div class="overflow-auto h-100">
      <b-button style="top: 2px; right: 60px" class="border-0 position-absolute"
        variant="btn-outline-primary" size="sm" :disabled="loading" :title="$t('decision.loadVersions')"
        @click="getVersions(false); $emit('load-instances')">
        <span class="mdi mdi-18px mdi-cog-refresh-outline"></span>
      </b-button>
      <b-button style="top: 2px; right: 30px" class="border-0 position-absolute"
        variant="btn-outline-primary" size="sm" :disabled="loading" :title="$t('decision.refreshVersions')"
        @click="getVersions(true); $emit('load-instances')">
        <span class="mdi mdi-18px mdi-refresh"></span>
      </b-button>
      <b-list-group class="mx-3 mb-3">
        <b-list-group-item @click="selectVersion(version)" v-for="version of versions" :key="version.id"
        class="rounded-0 mt-3 p-2 bg-white border-0" :class="version.id === selectedVersionId ? 'active shadow' : ''" style="cursor: pointer">
          <b-button v-if="processByPermissions($root.config.permissions.deleteProcessDefinition, version)" @click.stop="showConfirm({ ok: deleteProcessDefinition, version: version })"
            variant="link" class="border-0 shadow-none position-absolute px-2 text-danger" style="bottom: 5px; right: 0" :title="$t('decision.deleteProcessDefinition')">
            <span class="mdi mdi-18px mdi-delete-outline"></span>
          </b-button>
          <div class="d-flex align-items-center">
            <h6 style="font-size: 1rem">
              <span class="font-weight-bold">{{ $t('decision.details.definitionVersion') + ': ' + version.version }}</span>
            </h6>
            <div class="d-flex ms-auto" :id="version.id">
              <span class="mdi mdi-18px mdi-information-outline text-info"></span>
            </div>
          </div>
          <div class="mb-1">
            <div class="mb-1">{{ $t('decision.details.unfinishedInstances') + ': ' + version.runningInstances }}</div>
            <div>{{ $t('decision.details.totalInstances') + ': ' + version.allInstances }}</div>
          </div>
          <b-popover :target="version.id" triggers="hover" placement="right" boundary="viewport" max-width="350px">
            <DecisionDefinitionDetails :version="version" :instances="instances" @onUpdateHistoryTimeToLive="onUpdateHistoryTimeToLive"></DecisionDefinitionDetails>
          </b-popover>
        </b-list-group-item>
      </b-list-group>
    </div>
    <SuccessAlert ref="successOperation"> {{ $t('alert.successOperation') }}</SuccessAlert>
    <ConfirmDialog ref="confirm" @ok="$event.ok($event.version)">
      {{ $t('confirm.performOperation') }}
    </ConfirmDialog>
  </div>
</template>

<script>
import { DecisionService } from '@/services.js'
import { permissionsMixin } from '@/permissions.js'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'
import ConfirmDialog from '@/components/common-components/ConfirmDialog.vue'
import DecisionDefinitionDetails from '@/components/decision/DecisionDefinitionDetails.vue'

/* 
  TODO: Refactor this class.
  TODO[ivan]: Request directly from store, not from services.
  TODO[ivan]: Create methods in permissionsMixin that applies to decisions
  TODO[ivan]: Establish same arquitecture than Process, to navigate easily via URL
*/

export default {
  name: 'DecisionDetailsSidebar',
  components: { SuccessAlert, ConfirmDialog, DecisionDefinitionDetails },
  mixins: [copyToClipboardMixin, permissionsMixin],
  props: { decision: Object, instances: Array, selectedInstance: Object, selectedTask: [Object, Boolean] },
  data: function() {
    return {
      selectedVersionId: this.decision.id,
      versions: [],
      decisionVisible: true,
      loading: false,
      lazyLoadHistory: this.$root.config.lazyLoadHistory
    }
  },
  watch:  {
    selectedVersionId: function() {
      this.findAndUpdateDecision()
    }
  },
  computed: {
    decisionName: function() {
      return this.decision.name ? this.decision.name : this.decision.key
    }
  },
  mounted: function() {
    this.getVersions(this.lazyLoadHistory)
  },
  methods: {
    getVersions: function(lazyLoadHistory) {
      this.loading = true
      return DecisionService.getDecisionVersionsByKey(this.decision.key, lazyLoadHistory)
        .then(versions => {
        if (lazyLoadHistory) {
          versions.forEach(v => {
            v.runningInstances = '-'
            v.allInstances = '-'
            v.completedInstances = '-'
          })
        }
        this.versions = versions
        this.loading = false
        if (lazyLoadHistory) this.findAndUpdateDecision()
        return versions
      })
    },
    findAndUpdateDecision: function() {
      DecisionService.getDecisionDefinitionById(this.selectedVersionId, true).then(process => {
        for (let v of this.versions) {
          if (v.id === process.id) {
            Object.assign(v, process)
            break
          }
        }
      })
    },
    showConfirm: function(data) { this.$refs.confirm.show(data) },
    selectVersion: function(version) {
      if (this.selectedVersionId !== version.id) {
        this.selectedVersionId = version.id
        this.$emit('load-version-decision', version)
        this.$router.push('/seven/auth/decision/' + version.key + '/' + version.version)
      }
    },
    deleteDecisionDefinition: function(decision) {
      DecisionService.deleteProcessDefinition(decision.id, true).then(() => {
        this.getVersions(this.lazyLoadHistory).then(versions => {
          if (versions.length === 0) {
            this.$router.push('/seven/auth/decisions')
          } else {
            this.$emit('load-version-decision', versions[0])
            this.selectedVersionId = versions[0].id
            this.$router.push('/seven/auth/decision/' + versions[0].key)
          }
        })
        this.$refs.successOperation.show()
      })
    },
    onUpdateHistoryTimeToLive(versionId, historyTimeToLive) {
      this.versions.forEach(version => {
        if (version.id === versionId) {
          version.historyTimeToLive = historyTimeToLive
          this.$refs.successOperation.show()
        }
      })
    }
  }
}
</script>
