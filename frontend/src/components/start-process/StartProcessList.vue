<template>
  <div class="d-flex flex-column bg-light" :style="{ height: 'calc(100% - 55px)' }">
    <div class="container pt-4">
      <div class="row align-items-center">
          <div :class="isMobile() ? 'col-12 pb-2' : 'col-md-2 p-0'">
          <b-button squared @click="selectedOptionHandler('all')" variant="light" :style="setOptionSelected('all')">{{ $t('process.sections.all') }}</b-button>
          <b-button squared @click="selectedOptionHandler('favorites')" variant="light" :style="setOptionSelected('favorites')">
            <span class="mdi mdi-star" style="line-height: initial"></span> {{ $t('process.sections.favorites') }}
          </b-button>
          </div>
          <div :class="isMobile() ? 'col-12' : 'col-md-3 p-0'">
          <b-input-group size="sm">
            <template #prepend>
              <b-button :title="$t('searches.search')" aria-hidden="true" class="rounded-left" variant="secondary"><span class="mdi mdi-magnify" style="line-height: initial"></span></b-button>
            </template>
            <b-form-input  :title="$t('searches.search')" :placeholder="$t('searches.search')" v-model="filter"></b-form-input>
          </b-input-group>
          </div>
        <div v-if="!isMobile()" class="col-md-7 d-flex align-items-center justify-content-end">
          <div class="d-inline me-1">{{ $t('process.' + view) }}</div>
          <b-dropdown variant="outline-secondary" toggle-class="border-0 p-0" right class="d-inline-flex">
            <template v-slot:button-content>
              <span :title="$t('process.' + view)"><span :class="activeViewMode"></span></span>
            </template>
            <b-dropdown-item-button @click="changeViewMode('image-outline')">
              <span class="mdi mdi-24px mdi-image-outline" style="line-height: initial"> {{ $t('process.image-outline') }}</span>
            </b-dropdown-item-button>
            <b-dropdown-item-button @click="changeViewMode('view-module')">
              <span class="mdi mdi-24px mdi-view-module" style="line-height: initial"> {{ $t('process.view-module') }}</span>
            </b-dropdown-item-button>
            <b-dropdown-item-button @click="changeViewMode('view-comfy')">
              <span class="mdi mdi-24px mdi-view-comfy" style="line-height: initial"> {{ $t('process.view-comfy') }}</span>
            </b-dropdown-item-button>
            <b-dropdown-item-button @click="changeViewMode('view-list')">
              <span class="mdi mdi-24px mdi-view-list" style="line-height: initial"> {{ $t('process.view-list') }}</span>
            </b-dropdown-item-button>
          </b-dropdown>
        </div>
      </div>
      </div>
    <div class="container-fluid overflow-auto h-100" :class="!isTable ? 'bg-light' : ''">
      <div v-if="processesByOptions.length && isTable" class="d-flex h-100">
        <ProcessTable :processes="processesByOptions" @start-process="startProcess($event)" @view-process="viewProcess($event)" @favorite="favoriteHandler($event)"></ProcessTable>
      </div>
      <div v-if="processesByOptions.length && !isTable" class="d-flex flex-wrap px-5 pt-3 justify-content-center">
        <div v-for="process in processesByOptions" :key="process.id">
          <ProcessCard v-if="view !== 'image-outline'" :process="process" :process-name="processName(process)" :view="view"
            @start-process="startProcess($event)" @view-process="viewProcess($event)" @favorite="favoriteHandler($event)"></ProcessCard>
          <ProcessAdvanced v-else :process="process" :process-name="processName(process)" :view="view"
            @start-process="startProcess($event)" @view-process="viewProcess($event)" @favorite="favoriteHandler($event)"></ProcessAdvanced>
        </div>
      </div>
      <StartProcess ref="processStart" @process-started="$refs.processStarted.show(10)" hideProcessSelection></StartProcess>
      <div v-if="!processesByOptions.length">
        <img :alt="$t(textEmptyProcessesList)" src="/assets/images/process/empty_processes_list.svg" class="d-block mx-auto mt-5 mb-3" style="max-width: 250px">
        <div class="h5 text-secondary text-center">{{ $t(textEmptyProcessesList) }}</div>
      </div>
      <SuccessAlert top="0" style="z-index: 1031" ref="processStarted">
        <span>
          {{ $t('process.processCheck') }}
          <router-link to="/seven/auth/tasks" place="tasks">{{ $t('process.tasks') }}</router-link>
        </span>
      </SuccessAlert>
      <b-modal static ref="diagramModal" size="xl" :title="$t('process.diagram.title', [processName(selected)])" dialog-class="h-75" content-class="h-100">
        <div class="container-fluid h-100 p-0">
          <BpmnViewer class="h-100" ref="diagram"></BpmnViewer>
        </div>
        <template v-slot:modal-footer><b-button variant="secondary" @click="$refs.diagramModal.hide()">{{ $t('start.close') }}</b-button></template>
      </b-modal>
    </div>
  </div>
</template>

<script>
import { permissionsMixin } from '@/permissions.js'
import ProcessTable from '@/components/process/ProcessTable.vue'
import ProcessAdvanced from '@/components/process/ProcessAdvanced.vue'
import ProcessCard from '@/components/process/ProcessCard.vue'
import StartProcess from '@/components/process/StartProcess.vue'
import BpmnViewer from '@/components/process/BpmnViewer.vue'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'
import { ProcessService } from '@/services.js'

export default {
  name: 'StartProcessList',
  components: { ProcessTable, ProcessAdvanced, ProcessCard, StartProcess, BpmnViewer, SuccessAlert },
  inject: ['isMobile'],
  mixins: [permissionsMixin],
  data: function () {
    return {
      selected: null,
      filter: '',
      showProcesses: true,
      view: 'image-outline',
      isTable: false,
      selectedOption: localStorage.getItem('optionSelected') || 'all'
    }
  },
  watch: {
    '$route.query.key': function(key) {
      this.checkProcessInUrl(key)
    }
  },
  created: function() {
    this.view = this.isMobile() ? 'image-outline' : localStorage.getItem('viewMode') || 'image-outline'
    this.isTable = this.view === 'view-list'
  },
  computed: {
    processesFiltered: function() {
      if (!this.$store.state.process.list) return []
      return this.$store.state.process.list.filter(process => {
        return ((process.key.toUpperCase().includes(this.filter.toUpperCase()) ||
            ((process.name) ? process.name.toUpperCase().includes(this.filter.toUpperCase()) : false)) &&
            (!process.revoked))
      }).sort((objA, objB) => {
        var nameA = objA.name ? objA.name.toUpperCase() : objA.name
        var nameB = objB.name ? objB.name.toUpperCase() : objB.name
        var comp = nameA < nameB ? -1 : nameA > nameB ? 1 : 0

        if (this.$root.config.subProcessFolder) {
          if (objA.resource.indexOf(this.$root.config.subProcessFolder) > -1) comp = 1
          else if (objB.resource.indexOf(this.$root.config.subProcessFolder) > -1) comp = -1
        }
        return comp
      })
    },
    activeViewMode: function() { return 'mdi mdi-24px mdi-' + this.view },
    processesByOptions: function() { return this[this.selectedOption + 'Filter'](this.processesFiltered) },
    textEmptyProcessesList: function() {
      return this.selectedOption === 'all' && this.filter === ''  ? 'process.emptyProcessList' : 'process.emptyProcessListFiltered'
    }
  },
  mounted: function() {
    if (localStorage.getItem('favorites')) {
      this.$store.dispatch('setFavorites', { favorites: JSON.parse(localStorage.getItem('favorites')) })
    }
    if (this.$route.query.key) {
      setTimeout(() => { this.checkProcessInUrl(this.$route.query.key) }, 1000)
    }
  },
  methods: {
    checkProcessInUrl: function (processKey) {
      var index = this.processesByOptions.findIndex(process => { return process.key === processKey })
      if (index > -1) this.startProcess(this.processesByOptions[index])
      else {
        this.$root.$refs.error.show({ type: 'processNotFound', params: [processKey] })
      }
    },
    processName: function(process) {
      if (process) return process.name ? process.name : process.key
    },
    favoritesFilter: function(processes) {
      return this.allFilter(processes).filter(process => {
        return process.favorite
      })
    },
    allFilter: function(processes) {
      return processes.filter(function(process) {
        return process.startableInTasklist && process.suspended !== 'true'
      })
    },
    favoriteHandler: function(process) {
      process.favorite = !process.favorite
      var favorites = this.favoritesFilter(this.$store.state.process.list).map(r => { return r.key })
      localStorage.setItem('favorites', JSON.stringify(favorites))
    },
    changeViewMode: function(mdi) {
      this.view = mdi
      this.isTable = this.view === 'view-list'
      localStorage.setItem('viewMode', this.view)
    },
    setOptionSelected: function(option) {
      return option === this.selectedOption ?
        'border-bottom: 3px solid var(--bs-primary); background: var(--bs-light)' : 'background: var(--bs-white)'
    },
    selectedOptionHandler: function(option) {
      this.selectedOption = option
      localStorage.setItem('optionSelected', option)
    },
    startProcess: function(process) { this.$refs.processStart.startProcess(process) },
    viewProcess: function(process) {
      this.selected = process
      ProcessService.fetchDiagram(process.id).then(response => {
        this.$refs.diagram.showDiagram(response.bpmn20Xml, null, null)
        this.$refs.diagramModal.show()
      })
    }
  }
}
</script>
