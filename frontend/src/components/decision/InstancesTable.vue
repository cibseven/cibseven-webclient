<template>
  <FlowTable striped resizable thead-class="sticky-header" :items="instances" primary-key="id" prefix="decision."
    :sort-by="sortByDefaultKey" :sort-desc="sortDesc" :fields="[
    { label: 'id', key: 'id', class: 'col-2', thClass: 'border-end', tdClass: 'border-end py-1 border-top-0 position-relative' },
    { label: 'evaluationTime', key: 'evaluationTime', class: 'col-2', thClass: 'border-end', tdClass: 'border-end py-1 border-top-0' },
    { label: 'callingProcess', key: 'processDefinitionKey', class: 'col-3', thClass: 'border-end', tdClass: 'border-end py-1 border-top-0 position-relative' },
    { label: 'callingInstanceId', key: 'processInstanceId', class: 'col-3', thClass: 'border-end', tdClass: 'border-end py-1 border-top-0 position-relative' },
    { label: 'activityId', key: 'activityId', class: 'col-2', thClass: 'border-end', tdClass: 'border-end py-1 border-top-0' }]">
    <template v-slot:cell(id)="table">
      <button :title="table.item.id" class="text-truncate w-100 btn btn-link text-start" :class="focusedCell === table.item.id ? 'pe-4': ''" 
        @mouseenter="focusedCell = table.item.id" @mouseleave="focusedCell = null" @click="goToInstance(table.item)">
        {{ table.item.id }}
        <span v-if="table.item.id && focusedCell === table.item.id" @click.stop="copyValueToClipboard(table.item.id)"
          class="mdi mdi-18px mdi-content-copy px-2 position-absolute end-0 text-secondary lh-sm"></span>
      </button>
    </template>
    <template v-slot:cell(processDefinitionKey)="table">
      <button :title="table.item.processDefinitionKey" class="text-truncate w-100 btn btn-link text-start" @click="goToProcess(table.item)"
        @mouseenter="focusedCell = table.item.id + table.item.processDefinitionKey" @mouseleave="focusedCell = null">
        {{ table.item.processDefinitionKey }}
        <span v-if="table.item.id && focusedCell === (table.item.id + table.item.processDefinitionKey)"
          @click.stop="copyValueToClipboard(table.item.processDefinitionKey)" class="mdi mdi-18px mdi-content-copy px-2 position-absolute end-0 text-secondary lh-sm"></span>
      </button>
    </template>
    <template v-slot:cell(processInstanceId)="table">
      <button :title="table.item.processInstanceId" class="text-truncate w-100 btn btn-link text-start" @click="goToProcessInstance(table.item)"
        @mouseenter="focusedCell = table.item.processInstanceId" @mouseleave="focusedCell = null">
        {{ table.item.processInstanceId }}
        <span v-if="table.item.id && focusedCell === table.item.processInstanceId"
          @click.stop="copyValueToClipboard(table.item.processInstanceId)" class="mdi mdi-18px mdi-content-copy px-2 position-absolute end-0 text-secondary lh-sm"></span>
      </button>
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
import { mapMutations, mapGetters } from 'vuex'

export default {
  name: 'InstancesTable',
  components: { FlowTable, SuccessAlert, ConfirmDialog },
  mixins: [copyToClipboardMixin, permissionsMixin],
  props: { instances: Array, sortDesc: Boolean, sortByDefaultKey: String },
  data() {
    return {
      focusedCell: null
    }
  },
  computed: {
    ...mapGetters(['getSelectedDecisionVersion'])
  },
  methods: {
    ...mapMutations(['setSelectedInstance']),
    goToInstance(instance) {
      this.setSelectedInstance(instance)
      this.$router.push({
        name: 'decision-instance',
        params: {
          versionIndex: this.getSelectedDecisionVersion.version,
          instanceId: instance.id
        }
      })
    },
    showConfirm(type) { this.$refs.confirm.show(type) },
    getIconState(state) {
      switch (state) {
        case 'ACTIVE': return 'mdi-chevron-triple-right text-success'
        case 'SUSPENDED': return 'mdi-close-circle-outline'
        default: return 'mdi-flag-triangle'
      }
    },
    getIconTitle(state) {
      switch (state) {
        case 'ACTIVE': return this.$t('decision.instanceRunning')
        case 'SUSPENDED': return this.$t('decision.instanceIncidents')
        default: return this.$t('decision.instanceFinished')
      }
    },
    goToProcess(instance) {
      let processData = instance.processDefinitionId.split(':')
      this.$router.push({
        name: 'process',
        params: {
          processKey: processData[0],
          versionIndex: processData[1]
        }
      })
    },
    goToProcessInstance(instance) {
      let processData = instance.processDefinitionId.split(':')
      this.$router.push({
        name: 'process',
        params: {
          processKey: processData[0],
          versionIndex: processData[1],
          instanceId: instance.processInstanceId
        }
      })
    }
  }
}
</script>
