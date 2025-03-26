<template>
  <div v-if="decision" class="h-100">
    <div @mousedown="handleMouseDown" class="v-resizable position-absolute w-100" style="left: 0" :style="'height: ' + bpmnViewerHeight + 'px; ' + toggleTransition">
      <DmnViewer ref="diagram"
        :process-definition-id="decision.id"
        class="h-100" />
    </div>

    <ul class="nav nav-tabs position-absolute border-0 bg-light" style="left: -1px" :style="'top: ' + (bottomContentPosition - toggleButtonHeight) + 'px; ' + toggleTransition">
      <span role="button" size="sm" variant="light" class="border-bottom-0 bg-white rounded-top border py-1 px-2 me-1" @click="toggleContent">
        <span class="mdi mdi-18px" :class="toggleIcon"></span>
      </span>
      <li class="nav-item m-0" v-for="(tab, index) in tabs" :key="index">
        <a role="button" @click="changeTab(tab)" class="nav-link py-2" :class="{ 'active': tab.active, 'bg-light border border-bottom-0': !tab.active }">
          {{ $t('decision.' + tab.id) }}
        </a>
      </li>
    </ul>

    <div class="position-absolute w-100" style="left: 0; bottom: 0" :style="'top: ' + bottomContentPosition + 'px; ' + toggleTransition">
      <div v-if="activeTab === 'instances'">
        <div ref="filterTable" class="bg-light d-flex position-absolute w-100">
          <div class="col-3 p-3">
            <b-input-group size="sm">
              <template #prepend>
                <b-button :title="$t('searches.search')" aria-hidden="true" size="sm" class="rounded-left" variant="secondary"><span class="mdi mdi-magnify" style="line-height: initial"></span></b-button>
              </template>
              <b-form-input :title="$t('searches.search')" size="sm" :placeholder="$t('searches.search')" @input="onInput"></b-form-input>
            </b-input-group>
          </div>
        </div>
        <div ref="rContent" class="overflow-auto bg-white position-absolute w-100" style="top: 60px; left: 0; bottom: 0" @scroll="handleScrollDecisions">
          <InstancesTable ref="instancesTable" v-if="!loading && instances && !sorting" :instances="instances" :sortByDefaultKey="sortByDefaultKey" :sortDesc="sortDesc"></InstancesTable>
          <div v-else-if="loading" class="py-3 text-center w-100">
            <BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('admin.loading') }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>

import { permissionsMixin } from '@/permissions.js'
import DmnViewer from '@/components/decision/DmnViewer.vue'
import InstancesTable from '@/components/decision/InstancesTable.vue'
import resizerMixin from '@/components/process/mixins/resizerMixin.js'
import { debounce } from '@/utils/debounce.js'
import { BWaitingBox } from 'cib-common-components'

/* 
  TODO: Refactor this class.
  TODO[ivan]: Establish same arquitecture than Process, to navigate easily via URL
*/

export default {
  name: 'Decision',
  components: { DmnViewer, InstancesTable, BWaitingBox },
  inject: ['loadDecisions'],
  mixins: [permissionsMixin, resizerMixin],
  props: { instances: Array, decision: Object, firstResult: Number, maxResults: Number,
    activityInstance: Object, activityInstanceHistory: Array, activityId: String, loading: Boolean },
  data: function() {
    return {
      selectedInstance: null,
      selectedTask: null,
      topBarHeight: 0,
      tabs: [
        { id: 'instances', active: true },
      ],
      activeTab: 'instances',
      sortByDefaultKey: 'startTimeOriginal',
      sorting: false,
      sortDesc: true
    }
  },
  watch: {
    'decision.id': function() {
      this.$store.dispatch('getXmlById', this.decision.id).then(response => {
        setTimeout(() => {
          this.$refs.diagram.showDiagram(response.dmnXml, null, null)
        }, 100)
      })
      .catch(error => {
        console.error("Error loading diagram:", error)
      })
    },
    activeTab: function() {

    }
  },
  mounted: function() {
    if(this.decision) {
      this.$store.dispatch('getXmlById', this.decision.id)
      .then(response => {
        setTimeout(() => {
          this.$refs.diagram.showDiagram(response.dmnXml, null, null)
        }, 100)
      })
      .catch(error => {
        console.error("Error loading diagram:", error)
      })
    }
  },
  computed: {
    decisionActions: function() {
      return this.$options.components && this.$options.components.DecisionActions
        ? this.$options.components.DecisionActions
        : null
    },
    decisionName: function() {
      return this.decision.name !== null ? this.decision.name : this.decision.key
    }
  },
  methods: {
    changeTab: function(selectedTab) {
      this.tabs.forEach((tab) => {
        tab.active = tab.id === selectedTab.id
      })
      this.activeTab = selectedTab.id
    },
    selectInstance: function(event) {
      this.selectedInstance = event.instance
      this.$emit('instance-selected', { selectedInstance: event.instance, reload: event.reload })
    },
    viewDecision: function() {
      this.$store.dispatch('getXmlById', this.decision.id)
      .then(response => {
        setTimeout(() => {
          this.$refs.diagram.showDiagram(response.dmnXml, null, null)
        }, 100)
      })
      .catch(error => {
        console.error("Error loading diagram:", error)
      })
    },
    clearState: function() {
      this.selectedInstance = null
      this.selectedTask = null
      this.$emit('instance-selected', { selectedInstance: null })
    },
    handleScrollDecisions: function(el) {
      if (this.instances.length < this.firstResult) return
      if (Math.ceil(el.target.scrollTop + el.target.clientHeight) >= el.target.scrollHeight) {
        this.$emit('show-more')
      }
    },
    onInput: debounce(800, function(evt) { this.$emit('filter-instances', evt) })
  }
}
</script>
