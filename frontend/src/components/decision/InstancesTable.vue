<template>
  <FlowTable striped resizable thead-class="sticky-header" :items="instances" primary-key="id" prefix="decision."
    :sort-by="sortByDefaultKey" :sort-desc="sortDesc" :fields="[
    { label: 'id', key: 'id', class: 'col-2', thClass: 'border-end', tdClass: 'border-end py-1 border-top-0 position-relative' },
    { label: 'evaluationTime', key: 'evaluationTime', class: 'col-2', thClass: 'border-end', tdClass: 'border-end py-1 border-top-0' },
    { label: 'callingProcess', key: 'processDefinitionKey', class: 'col-3', thClass: 'border-end', tdClass: 'border-end py-1 border-top-0' },
    { label: 'callingInstanceId', key: 'processInstanceId', class: 'col-3', thClass: 'border-end', tdClass: 'border-end py-1 border-top-0' },
    { label: 'activityId', key: 'activityId', class: 'col-2', thClass: 'border-end', tdClass: 'border-end py-1 border-top-0' }]"
    @click="selectInstance($event)">
    <template v-slot:cell(id)="table">
      <div :title="table.item.id" class="text-truncate w-100" :class="focusedCell === table.item.id ? 'pe-4': ''" @mouseenter="focusedCell = table.item.id" @mouseleave="focusedCell = null">
        {{ table.item.id }}
        <span v-if="table.item.id && focusedCell === table.item.id" @click.stop="copyValueToClipboard(table.item.id)"
          class="mdi mdi-18px mdi-content-copy px-2 position-absolute end-0 text-secondary lh-sm"></span>
      </div>
    </template>
    <template v-slot:cell(actions)="table">
      <b-button v-if="table.item.state === 'ACTIVE'" @click.stop="showConfirm({ ok: suspendInstance, instance: table.item })"
      size="sm" variant="outline-secondary" class="border-0 mdi mdi-18px mdi-pause-circle-outline" :title="$t('decision..suspendInstance')"></b-button>
      <b-button v-if="table.item.state === 'SUSPENDED'" @click.stop="showConfirm({ ok: activateInstance, instance: table.item  })"
      size="sm" variant="outline-secondary" class="border-0 mdi mdi-18px mdi-play-circle-outline" :title="$t('decision..activateInstance')"></b-button>
      <b-button @click="selectInstance(table.item, true); viewDecision()" size="sm" variant="outline-secondary" class="border-0 mdi mdi-18px mdi-eye-outline" :title="$t('decision.showInstance')"></b-button>
      <b-button v-if="['ACTIVE', 'SUSPENDED'].includes(table.item.state) && decisionByPermissions($root.config.permissions.deleteDecisionInstance, table.item)"
      @click.stop="showConfirm({ ok: deleteInstance, instance: table.item  })"
      size="sm" variant="outline-secondary" class="border-0 mdi mdi-18px mdi-delete-outline" :title="$t('decision.deleteInstance')"></b-button>
      <b-button v-if="['COMPLETED', 'EXTERNALLY_TERMINATED'].includes(table.item.state) && decisionByPermissions($root.config.permissions.deleteDecisionInstance, table.item)"
      @click.stop="showConfirm({ ok: deleteHistoryInstance, instance: table.item })"
      size="sm" variant="outline-secondary" class="border-0 mdi mdi-18px mdi-delete-outline" :title="$t('decision.deleteHistoryInstance')"></b-button>
    </template>
  </FlowTable>
  <ConfirmDialog ref="confirm" @ok="$event.ok($event.instance)">
  {{ $t('confirm.performOperation') }}
  </ConfirmDialog>
  <SuccessAlert top="0" style="z-index: 1031" ref="success"> {{ $t('alert.successOperation') }}</SuccessAlert>
  <SuccessAlert ref="messageCopy"> {{ $t('decision.copySuccess') }} </SuccessAlert>
</template>

<script>

import { permissionsMixin } from '@/permissions.js'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import FlowTable from '@/components/common-components/FlowTable.vue'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'
import ConfirmDialog from '@/components/common-components/ConfirmDialog.vue'

/* 
  TODO: Refactor this class.
  TODO[ivan]: could we reuse this class from Process? i dont think, has to be checked after process refactor.
  TODO[ivan]: implement deleteInstance
  TODO[ivan]: implement deleteHistoryInstance
*/

export default {
  name: 'InstancesTable',
  components: { FlowTable, SuccessAlert, ConfirmDialog },
  emits: ['select-instance', 'view-decision', 'instance-deleted'],
  mixins: [copyToClipboardMixin, permissionsMixin],
  props: { instances: Array, sortDesc: Boolean, sortByDefaultKey: String },
  data: function() {
    return {
      focusedCell: null
    }
  },
  methods: {
    selectInstance: function(instance, reload) {
      this.$emit('select-instance', { instance, reload })
    },
    viewDecision: function() {
      this.$emit('view-decision')
    },
    showConfirm: function(type) { this.$refs.confirm.show(type) },
    deleteInstance: function(instance) {
    },
    deleteHistoryInstance: function(instance) {
    },
    getIconState: function(state) {
      switch(state) {
        case 'ACTIVE':
          return 'mdi-chevron-triple-right text-success'
        case 'SUSPENDED':
          return 'mdi-close-circle-outline'
      }
      return 'mdi-flag-triangle'
    },
    getIconTitle: function(state) {
      switch(state) {
        case 'ACTIVE':
          return this.$t('decision.instanceRunning')
        case 'SUSPENDED':
          return this.$t('decision.instanceIncidents')
      }
      return this.$t('decision.instanceFinished')
    },
  }
}
</script>
