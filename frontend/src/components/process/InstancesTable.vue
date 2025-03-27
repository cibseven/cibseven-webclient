<template>
  <FlowTable striped resizable thead-class="sticky-header" :items="instances" primary-key="id" prefix="process."
    :sort-by="sortByDefaultKey" :sort-desc="sortDesc" :fields="[
    { label: 'state', key: 'state', class: 'col-1', thClass: 'border-end', tdClass: 'justify-content-center text-center py-0 border-end border-top-0' },
    { label: 'businessKey', key: 'businessKey', class: 'col-2', thClass: 'border-end', tdClass: 'border-end py-1 border-top-0 position-relative' },
    { label: 'startTime', key: 'startTimeOriginal', class: 'col-2', thClass: 'border-end', tdClass: 'border-end py-1 border-top-0' },
    { label: 'endTime', key: 'endTimeOriginal', class: 'col-2', thClass: 'border-end', tdClass: 'border-end py-1 border-top-0' },
    { label: 'id', key: 'id', class: 'col-2', thClass: 'border-end', tdClass: 'border-end py-1 border-top-0 position-relative' },
    { label: 'startUserId', key: 'startUserId', class: 'col-1', thClass: 'border-end', tdClass: 'border-end py-1 border-top-0' },
    { label: 'actions', key: 'actions', class: 'col-2', sortable: false, tdClass: 'py-0 border-top-0' }]"
    @click="selectInstance($event)">
    <template v-slot:cell(state)="table">
      <span :title="getIconTitle(table.item.state)" class="mdi mdi-18px" :class="getIconState(table.item.state)"></span>
      <span :title="$t('process.instanceIncidents')" v-if="table.item.incidents.length > 0" class="mdi mdi-18px mdi-alert-outline text-warning"></span>
    </template>
    <template v-slot:cell(id)="table">
      <div :title="table.item.id" class="text-truncate w-100" :class="focusedCell === table.item.id ? 'pe-4': ''" @mouseenter="focusedCell = table.item.id" @mouseleave="focusedCell = null">
        {{ table.item.id }}
        <span v-if="table.item.id && focusedCell === table.item.id" @click.stop="copyValueToClipboard(table.item.id)"
          class="mdi mdi-18px mdi-content-copy px-2 position-absolute end-0 text-secondary lh-sm"></span>
      </div>
    </template>
    <template v-slot:cell(businessKey)="table">
      <div :title="table.item.businessKey" class="text-truncate w-100" :class="focusedCell === table.item.businessKey ? 'pe-4': ''" @mouseenter="focusedCell = table.item.businessKey" @mouseleave="focusedCell = null">
        {{ table.item.businessKey }}
        <span v-if="table.item.businessKey && focusedCell === table.item.businessKey" @click.stop="copyValueToClipboard(table.item.businessKey)"
          class="mdi mdi-18px mdi-content-copy px-2 float-end text-secondary lh-sm" style="right: 4px; top: 4px;"></span>
      </div>
    </template>
    <template v-slot:cell(startTimeOriginal)="table">
      <span :title="table.item.startTime" class="text-truncate d-block">{{ table.item.startTime }}</span>
    </template>
    <template v-slot:cell(endTimeOriginal)="table">
      <span :title="table.item.endTime" class="text-truncate d-block">{{ table.item.endTime }}</span>
    </template>
    <template v-slot:cell(actions)="table">
      <b-button v-if="table.item.state === 'ACTIVE'" @click.stop="showConfirm({ ok: suspendInstance, instance: table.item })"
      size="sm" variant="outline-secondary" class="border-0 mdi mdi-18px mdi-pause-circle-outline" :title="$t('process.suspendInstance')"></b-button>
      <b-button v-if="table.item.state === 'SUSPENDED'" @click.stop="showConfirm({ ok: activateInstance, instance: table.item  })"
      size="sm" variant="outline-secondary" class="border-0 mdi mdi-18px mdi-play-circle-outline" :title="$t('process.activateInstance')"></b-button>
      <b-button @click="selectInstance(table.item)" size="sm" variant="outline-secondary" class="border-0 mdi mdi-18px mdi-eye-outline" :title="$t('process.showInstance')"></b-button>
      <b-button v-if="['ACTIVE', 'SUSPENDED'].includes(table.item.state) && processByPermissions($root.config.permissions.deleteProcessInstance, table.item)"
      @click.stop="showConfirm({ ok: deleteInstance, instance: table.item  })"
      size="sm" variant="outline-secondary" class="border-0 mdi mdi-18px mdi-delete-outline" :title="$t('process.deleteInstance')"></b-button>
      <b-button v-if="['COMPLETED', 'EXTERNALLY_TERMINATED'].includes(table.item.state) && processByPermissions($root.config.permissions.deleteProcessInstance, table.item)"
      @click.stop="showConfirm({ ok: deleteHistoryInstance, instance: table.item })"
      size="sm" variant="outline-secondary" class="border-0 mdi mdi-18px mdi-delete-outline" :title="$t('process.deleteHistoryInstance')"></b-button>
    </template>
  </FlowTable>
  <ConfirmDialog ref="confirm" @ok="$event.ok($event.instance)">
  {{ $t('confirm.performOperation') }}
  </ConfirmDialog>
  <SuccessAlert top="0" style="z-index: 1031" ref="success"> {{ $t('alert.successOperation') }}</SuccessAlert>
  <SuccessAlert ref="messageCopy"> {{ $t('process.copySuccess') }} </SuccessAlert>
</template>

<script>
import { ProcessService, HistoryService } from '@/services.js'
import { permissionsMixin } from '@/permissions.js'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import FlowTable from '@/components/common-components/FlowTable.vue'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'
import ConfirmDialog from '@/components/common-components/ConfirmDialog.vue'

export default {
  name: 'InstancesTable',
  components: { FlowTable, SuccessAlert, ConfirmDialog },
  mixins: [copyToClipboardMixin, permissionsMixin],
  props: { instances: Array, sortDesc: Boolean, sortByDefaultKey: String },
  data: function() {
    return {
      focusedCell: null
    }
  },
  methods: {
    selectInstance: function(instance) {
      this.$router.push({
        name: 'process',
        params: {
          processKey: instance.processDefinitionKey,
          versionIndex: instance.processDefinitionVersion,
          instanceId: instance.id,
        }
      })
    },
    showConfirm: function(type) { this.$refs.confirm.show(type) },
    deleteInstance: function(instance) {
      ProcessService.deleteInstance(instance.id).then(() => {
        this.$router.push({
          name: 'process',
          params: {
            processKey: instance.processDefinitionKey,
            versionIndex: instance.processDefinitionVersion
          }
        })
        this.$refs.success.show()
      })
    },
    deleteHistoryInstance: function(instance) {
      HistoryService.deleteProcessInstanceFromHistory(instance.id).then(() => {
        this.$router.push({
          name: 'process',
          params: {
            processKey: instance.processDefinitionKey,
            versionIndex: instance.processDefinitionVersion
          }
        })
        this.$refs.success.show()
      })
    },
    suspendInstance: function(instance) {
      ProcessService.suspendInstance(instance.id, true).then(() => {
        instance.state = 'SUSPENDED'
        this.$refs.success.show()
      })
    },
    activateInstance: function(instance) {
      ProcessService.suspendInstance(instance.id, false).then(() => {
        instance.state = 'ACTIVE'
        this.$refs.success.show()
      })
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
          return this.$t('process.instanceRunning')
        case 'SUSPENDED':
          return this.$t('process.instanceIncidents')
      }
      return this.$t('process.instanceFinished')
    },
  }
}
</script>
