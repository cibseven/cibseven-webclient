<template>
  <div class="d-flex flex-column bg-light" :style="{ height: 'calc(100% - 55px)' }">
    <div class="container pt-4">
      <div class="row align-items-center pb-2">
        <div class="col-4">
          <b-input-group size="sm">
            <template #prepend>
              <b-button :title="$t('searches.search')" aria-hidden="true" class="rounded-left" variant="secondary"><span class="mdi mdi-magnify" style="line-height: initial"></span></b-button>
            </template>
            <b-form-input :title="$t('searches.search')" :placeholder="$t('searches.search')" v-model="filter"></b-form-input>
          </b-input-group>
        </div>
        <div class="col-5">
          <div class="d-flex row">
            <div class="d-inline-block align-content-start" style="width: 210px">
              <b-form-checkbox v-model="onlyIncidents" switch>
                {{ $t('process.onlyIncidents') }}
              </b-form-checkbox>
            </div>
            <div class="d-inline-block align-content-start" style="width: 210px">
              <b-form-checkbox v-model="onlyActive" switch>
                {{ $t('process.onlyActive') }}
              </b-form-checkbox>
            </div>
          </div>
        </div>
        <div class="col-3 text-end">
          <b-button class="border" size="sm" variant="light" to="/seven/auth/deployments/" :title="$t('process.organizeDeployment')">
            <span class="mdi mdi-file-eye-outline"></span> {{ $t('process.organizeDeployment') }}
          </b-button>
        </div>
      </div>
    </div>
    <div class="container overflow-auto h-100 bg-white shadow-sm border rounded g-0">
      <FlowTable :items="processesFiltered" thead-class="sticky-header" striped primary-key="id" prefix="process." :fields="fields" @click="goToShowProcessHistory($event)" @select="focused = $event[0]" @mouseenter="focused = $event" @mouseleave="focused = null">
        <template v-slot:cell(incidents)="table">
          <span v-if="loadingInstances"><b-spinner small></b-spinner></span>
          <div v-else-if="table.item.incidents > 0">
            <span :title="$t('process.instanceIncidents')" class="mdi mdi-18px mdi-alert-outline text-warning"></span><span>{{ table.item.incidents }}</span>
          </div>
          <span :title="$t('process.instanceFinished')" v-else class="mdi mdi-18px mdi-check-circle-outline text-success"></span>
        </template>
        <template v-slot:cell(runningInstances)="table">
          <span v-if="loadingInstances"><b-spinner small></b-spinner></span>
          <div v-else-if="table.item.runningInstances > 0">
            <span :title="$t('process.details.totalInstances') + ': ' + table.item.runningInstances" class="mdi mdi-18px mdi-chevron-triple-right text-success"></span><span class="ms-1">{{ table.item.runningInstances }}</span>
          </div>
          <span v-else>___</span>
        </template>
        <template v-slot:cell(description)="table">
          <div v-if="$te('process-descriptions.' + table.item.key)" v-b-popover.hover.left="$t('process-descriptions.' + table.item.key)" class="text-truncate">
          {{ $t('process-descriptions.' + table.item.key) }}</div>
        </template>
        <template v-slot:cell(actions)="table">
          <component :is="ProcessDefinitionActions" v-if="ProcessDefinitionActions" :focused="focused" :item="table.item"></component>
          <b-button :disabled="focused !== table.item" style="opacity: 1" @click.stop="goToShowProcessHistory(table.item)" class="px-2 border-0 shadow-none" :title="$t('process.showManagement')" variant="link">
            <span class="mdi mdi-18px mdi-account-tie-outline"></span>
          </b-button>
          <span class="border-start h-50" :class="focused === table.item ? 'border-secondary' : ''"></span>
          <b-button :disabled="focused !== table.item" style="opacity: 1" @click.stop="goToDeployment(table.item)" class="px-2 border-0 shadow-none" :title="$t('process.showDeployment')" variant="link">
            <span class="mdi mdi-18px mdi-file-eye-outline"></span>
          </b-button>
          <span class="border-start h-50" :class="focused === table.item ? 'border-secondary' : ''"></span>
          <b-button :disabled="focused !== table.item" style="opacity: 1" @click.stop="goToCockpit(table.item)" class="px-2 border-0 shadow-none" :title="$t('process.showCockpit')" variant="link">
            <span class="mdi mdi-18px mdi-radar"></span>
          </b-button>
        </template>
      </FlowTable>
      <div v-if="!processesFiltered.length">
        <img :alt="$t(textEmptyProcessesList)" src="/assets/images/process/empty_processes_list.svg" class="d-block mx-auto mt-5 mb-3" style="max-width: 250px">
        <div class="h5 text-secondary text-center">{{ $t(textEmptyProcessesList) }}</div>
      </div>
    </div>
  </div>
</template>

<script>
import { permissionsMixin } from '@/permissions.js'
import FlowTable from '@/components/common-components/FlowTable.vue'

export default {
  name: 'ProcessList',
  components: { FlowTable },
  inject: ['loadProcesses'],
  mixins: [permissionsMixin],
  data: function() {
    return {
      selected: null,
      filter: '',
      focused: null,
      loadingInstances: true
    }
  },
  computed: {
    onlyIncidents: {
      get: function() {
        return this.$route.query.onlyIncidents === 'true'
      },
      set: function(value) {
        this.$router.push({ query: { ...this.$route.query, onlyIncidents: value } })
      }
    },
    onlyActive: {
      get: function() {
        return this.$route.query.onlyActive === undefined || this.$route.query.onlyActive === 'true'
      },
      set: function(value) {
        this.$router.push({ query: { ...this.$route.query, onlyActive: value } })
      }
    },
    ProcessDefinitionActions: function() {
      return this.$options.components && this.$options.components.ProcessDefinitionActions
        ? this.$options.components.ProcessDefinitionActions
        : null
    },
    processesFiltered: function() {
      if (!this.$store.state.process.list) return []
      var processes = this.$store.state.process.list.filter(process => {
        return ((process.key.toUpperCase().includes(this.filter.toUpperCase()) ||
            ((process.name) ? process.name.toUpperCase().includes(this.filter.toUpperCase()) : false)))
      })
      processes = processes.filter(process => {
        var incidents = this.onlyIncidents ? process.incidents > 0 : true
        var onlyActive = this.onlyActive ? process.suspended === 'false' : true
        return incidents && onlyActive
      })
      processes.sort((objA, objB) => {
        var nameA = objA.name ? objA.name.toUpperCase() : objA.name
        var nameB = objB.name ? objB.name.toUpperCase() : objB.name
        var comp = nameA < nameB ? -1 : nameA > nameB ? 1 : 0

        if (this.$root.config.subProcessFolder) {
          if (objA.resource.indexOf(this.$root.config.subProcessFolder) > -1) comp = 1
          else if (objB.resource.indexOf(this.$root.config.subProcessFolder) > -1) comp = -1
        }
        return comp
      })
      return processes
    },
    textEmptyProcessesList: function() {
      return this.filter === '' ? 'process.emptyProcessList' : 'process.emptyProcessListFiltered'
    },
    fields: function() {
      return [
        { label: 'status', key: 'incidents', thClass:'py-0', tdClass:'py-0 ps-0 border-end border-top-0', class: 'col-1 d-flex align-items-center justify-content-center' },
        { label: 'runningInstances', key: 'runningInstances', class: 'col-1 d-flex justify-content-center', tdClass: 'border-end py-1 border-top-0' },
        { label: 'key', key: 'key', class: 'col-3', tdClass: 'border-end py-1 border-top-0' },
        { label: 'name', key: 'name', class: 'col-3', tdClass: 'border-end py-1 border-top-0' },
        { label: 'tenant', key: 'tenantId', class: 'col-2', tdClass: 'border-end py-1 border-top-0' },
        { label: 'actions', key: 'actions', sortable: false, class: 'col-2 d-flex justify-content-center', tdClass: 'border-end py-0 border-top-0' },
      ]
    }
  },
  created: function() {
    this.loadProcesses(true).then(() => {
      this.loadingInstances = false
    })
  },
  methods: {
    goToDeployment: function(process) {
      this.$router.push('/seven/auth/deployments/' + process.deploymentId)
    },
    goToCockpit: function(process) {
      window.open(this.$root.config.cockpitUrl + '#/process-definition/' + process.id, '_blank')
    },
    goToShowProcessHistory: function(process) {
      let url = '/seven/auth/process/' + process.key
      url += process.tenantId ? ('?tenantId=' + process.tenantId) : ''
      this.$router.push(url)
    }
  }
}
</script>
