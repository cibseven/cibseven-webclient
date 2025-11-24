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
  <div class="d-flex flex-column bg-light" :style="{ height: 'calc(100% - 55px)' }">
    <div class="container pt-4">
      <div class="row align-items-center pb-2">
        <div :class="isMobile() ? 'col-12 pb-2' : 'col-md-3 p-0'">
          <b-button squared @click="selectedOptionHandler('all')" variant="light" :style="setOptionSelected('all')">{{ $t('process.sections.all') }}</b-button>
          <b-button squared @click="selectedOptionHandler('favorites')" variant="light" :style="setOptionSelected('favorites')">
            <span class="mdi mdi-star" style="line-height: initial"></span> {{ $t('process.sections.favorites') }}
          </b-button>
        </div>
        <div :class="isMobile() ? 'col-12' : 'col-md-6 p-0 d-flex'">
          <b-input-group size="sm">
            <template #prepend>
              <b-button :title="$t('searches.search')" aria-hidden="true" class="rounded-left" variant="secondary"><span class="mdi mdi-magnify" style="line-height: initial"></span></b-button>
            </template>
            <b-form-input  :title="$t('searches.search')" :placeholder="$t('searches.search')" v-model.trim="filter"></b-form-input>
          </b-input-group>
        </div>
        <div v-if="!isMobile()" class="col-md-3 d-flex align-items-center justify-content-end p-0">
          <div class="d-inline me-1">{{ $t('process.' + view) }}</div>
          <b-dropdown ref="viewDropdown" variant="outline-secondary" toggle-class="border-0 p-0" right class="d-inline-flex">
            <template v-slot:button-content>
              <span :title="$t('process.' + view)"><span :class="activeViewMode"></span></span>
            </template>
            <b-dropdown-item @click="changeViewMode('image-outline')" :active="view === 'image-outline'">
              <span class="mdi mdi-24px mdi-image-outline centered-icon">{{ $t('process.image-outline') }}</span>
            </b-dropdown-item>
            <b-dropdown-item @click="changeViewMode('view-module')" :active="view === 'view-module'">
              <span class="mdi mdi-24px mdi-view-module centered-icon">{{ $t('process.view-module') }}</span>
            </b-dropdown-item>
            <b-dropdown-item @click="changeViewMode('view-comfy')" :active="view === 'view-comfy'">
              <span class="mdi mdi-24px mdi-view-comfy centered-icon">{{ $t('process.view-comfy') }}</span>
            </b-dropdown-item>
            <b-dropdown-item @click="changeViewMode('view-list')" :active="view === 'view-list'">
              <span class="mdi mdi-24px mdi-view-list centered-icon">{{ $t('process.view-list') }}</span>
            </b-dropdown-item>
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
      <div v-if="!processesByOptions.length">
        <img :alt="$t(textEmptyProcessesList)" src="@/assets/images/process/empty_processes_list_dark_background.svg" class="d-block mx-auto mt-5 mb-3" style="max-width: 250px">
        <div class="h5 text-secondary text-center">{{ $t(textEmptyProcessesList) }}</div>
      </div>
      <StartProcess ref="processStart" @process-started="$refs.processStarted.show(10)" hideProcessSelection></StartProcess>
      <SuccessAlert top="0" style="z-index: 1031" ref="processStarted">
        <span>
          {{ $t('process.processCheck') }}
          <router-link to="/seven/auth/tasks" place="tasks">{{ $t('process.tasks') }}</router-link>
        </span>
      </SuccessAlert>
      <b-modal static ref="diagramModal" size="xl" :title="$t('process.diagram.title', [processName(selected)])" dialog-class="h-75" content-class="h-100" :ok-only="true">
        <div class="container-fluid h-100 p-0">
          <BpmnViewer class="h-100" ref="diagram"></BpmnViewer>
        </div>
      </b-modal>
    </div>
  </div>
</template>

<script>
import { permissionsMixin } from '@/permissions.js'
import ProcessTable from '@/components/start-process/ProcessTable.vue'
import ProcessAdvanced from '@/components/process/ProcessAdvanced.vue'
import ProcessCard from '@/components/process/ProcessCard.vue'
import StartProcess from '@/components/start-process/StartProcess.vue'
import BpmnViewer from '@/components/process/BpmnViewer.vue'
import { SuccessAlert } from '@cib/common-frontend'
import { ProcessService } from '@/services.js'

export default {
  name: 'StartProcessList',
  components: { ProcessTable, ProcessAdvanced, ProcessCard, StartProcess, BpmnViewer, SuccessAlert },
  inject: ['loadProcesses', 'isMobile'],
  mixins: [permissionsMixin],
  data: function () {
    return {
      selected: null,
      filter: '',
      showProcesses: true,
      view: 'image-outline',
      selectedOption: localStorage.getItem('optionSelected') || 'all'
    }
  },
  watch: {
    '$route.query.key': function(key) {
      this.checkProcessInUrl(key)
    }
  },
  created: function() {
    this.loadProcesses(false) // the method takes localStorage.getItem('favorites') into account
    this.view = this.isMobile() ? 'image-outline' : localStorage.getItem('viewMode') || 'image-outline'
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
    isTable: function() { return this.view === 'view-list' },
    activeViewMode: function() { return 'mdi mdi-24px mdi-' + this.view },
    processesByOptions: function() { return this[this.selectedOption + 'Filter'](this.processesFiltered) },
    textEmptyProcessesList: function() {
      return this.selectedOption === 'all' && this.filter === ''  ? 'process.emptyProcessList' : 'process.emptyProcessListFiltered'
    }
  },
  mounted: function() {
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
      localStorage.setItem('viewMode', this.view)
      this.$refs.viewDropdown.hide()
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
        this.$refs.diagram.showDiagram(response.bpmn20Xml)
        this.$refs.diagramModal.show()
      })
    }
  }
}
</script>

<style lang="css" scoped>
.centered-icon {
  /* vertically center the Material Design icon with the text */
  display: inline-flex;
  align-items: center;
  justify-content: center;
  vertical-align: middle;

  /* add gap between icon and text */
  gap: 6px;

  /* others */
  line-height: initial;
}
</style>
