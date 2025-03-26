<template>
  <div class="d-flex flex-column">
    <div class="d-flex ps-3 py-2">
      <b-button :title="$t('start.admin')" variant="outline-secondary" href="#/seven/auth/decisions/list" class="mdi mdi-18px mdi-arrow-left border-0"></b-button>
      <h4 class="ps-1 m-0 align-items-center d-flex" style="border-width: 3px !important">{{ decisionName }}</h4>
    </div>
    <SidebarsFlow ref="sidebars" class="border-top overflow-auto" v-model:left-open="leftOpen" :left-caption="shortendLeftCaption">
      <template v-slot:left>
        <DecisionDetailsSidebar ref="navbar" v-if="instances" :decision="decision" :selected-instance="selectedInstance"
          @load-version-decision="loadVersionDecision($event)" @load-instances="loadInstances()"
          :instances="instances"></DecisionDetailsSidebar>
      </template>
      <transition name="slide-in" mode="out-in">
        <Decision ref="decision" v-if="instances && !selectedInstance" :activity-id="activityId" :loading="loading" :process="process" :instances="instances" :first-result="firstResult" :max-results="maxResults" @show-more="showMore()"
        @instance-deleted="clearInstance()" @instance-selected="setSelectedInstance($event)" @task-selected="setSelectedTask($event)" @filter-instances="filterInstances($event)"
        @update-items="updateItems"
        ></Decision>
      </transition>
      <transition name="slide-in" mode="out-in">
      </transition>
    </SidebarsFlow>
  </div>
</template>

<script>
import { nextTick } from 'vue'
import moment from 'moment'
import { DecisionService } from '@/services.js'
import Decision from '@/components/decision/Decision.vue'
import DecisionDetailsSidebar from '@/components/decision/DecisionDetailsSidebar.vue'
import SidebarsFlow from '@/components/common-components/SidebarsFlow.vue'

export default {
  name: 'DecisionView',
  components: { Decision, DecisionDetailsSidebar, SidebarsFlow },
  props: {
    decisionKey: String,
    versionIndex: { type: String, default: '' }
   },
  data: function() {
    return {
      leftOpen: true,
      rightOpen: false,
      decision: null,
      instances: null,
      decisionDefinitions: null,
      selectedInstance: null,
      firstResult: 0,
      maxResults: this.$root.config.maxProcessesResults,
      filter: '',
      loading: false
    }
  },
  computed: {
    shortendLeftCaption: function() {
      return this.$t('decision.details.historyVersions')
    },
    decisionName: function() {
      if (!this.decision) return this.$t('decision.decision')
      return this.decision.name ? this.decision.name : this.decision.key
    }
  },
  created: function() {
      this.loadDecisionByKey(this.processKey, this.versionIndex)
  },
  methods: {
    updateItems: function(sortedItems) {
      this.instances = sortedItems
    },
    loadDecisionByKey: function(decisionKey, versionIndex) {
      if (!versionIndex) {
        this.$store.dispatch('getDecisionByKey', { key: decisionKey }).then(decision => {
          this.loadDecisionVersion(decision)
        })
      }
      else {
        DecisionService.getDecisionVersionsByKey(decisionKey).then(decisionDefinitions => {
          this.decisionDefinitions = decisionDefinitions
          let requestedDefinition = decisionDefinitions.find(decisionDefinition => decisionDefinition.version === this.versionIndex)
          if (requestedDefinition) {
              this.loadDecisionVersion(requestedDefinition)
          }
          else {
            // definition is no longer available
            // let's redirect to the latest one
            this.loadDecisionByKey(decisionKey, undefined)
            this.$router.push('/seven/auth/decision/' + decisionKey)
          }
        })
      }
    },
    loadDecisionVersion: function(decision) {
      if (!this.decision || this.decision.id !== decision.id) {
        this.firstResult = 0
        this.decision = decision
        this.loadInstances()
      }
    },
    loadInstances: function(showMore) {
      if (this.$root.config.camundaHistoryLevel !== 'none') {
        this.loading = true
        /*
        HistoryService.findProcessesInstancesHistoryById(this.process.id, this.activityId,
          this.firstResult, this.maxResults, this.filter
        ).then(instances => {
          this.loading = false
          if (!showMore) this.instances = instances
          else this.instances = !this.instances ? instances : this.instances.concat(instances)
        })
          */
      }
      else {
        DecisionService.getDecisionVersionsByKey(this.decision.key).then(decisionDefinitions => {
          this.decisionDefinitions = decisionDefinitions
          var promises = []
          this.decisionDefinitions.forEach(() => {
            /*promises.push(HistoryService.findProcessesInstancesHistoryById(this.process.id, this.activityId, this.firstResult,
              this.maxResults, this.filter))
            */
          })
          Promise.all(promises).then(response => {
            if (!showMore) this.instances = []
            var i = 0
            response.forEach(instances => {
              instances.forEach(instance => {
                //instance.processDefinitionId = processDefinitions[i].id
                //instance.processDefinitionVersion = processDefinitions[i].version
              })
              this.instances = this.instances.concat(instances)
              i++
            })
          })
        })
      }
    },
    clearInstance: function() {
      this.setSelectedInstance({ selectedInstance: null })
      this.$refs.process.clearState()
      this.firstResult = 0
      this.loadInstances()
    },
    setSelectedInstance: function(evt) {
      var selectedInstance = evt.selectedInstance
      if (!selectedInstance) {
        this.rightOpen = false
        this.selectedInstance = null
      } else this.rightOpen = true
      this.task = null
      this.activityInstance = null
      this.activityInstanceHistory = selectedInstance ? this.activityInstanceHistory : null
      if (selectedInstance) {
        if (this.selectedInstance && this.selectedInstance.id === selectedInstance.id && !evt.reload) return
        this.selectedInstance = selectedInstance
        if (this.selectedInstance.state === 'ACTIVE') {
          //Management
          /*
          ProcessService.findActivityInstance(selectedInstance.id).then(activityInstance => {
            this.activityInstance = activityInstance
            HistoryService.findActivitiesInstancesHistory(selectedInstance.id).then(activityInstanceHistory => {
              this.activityInstanceHistory = activityInstanceHistory
            })
          })
          */
        } else {
          //History
          if (this.$root.config.camundaHistoryLevel !== 'none') {
            /*
            HistoryService.findActivitiesInstancesHistory(selectedInstance.id).then(activityInstanceHistory => {
              this.activityInstanceHistory = activityInstanceHistory
            })
            */
          }
        }
      }
    },
    filterByActivityId: function(event) {
      this.activityId = event
      this.instances = []
      this.firstResult = 0
      this.loadInstances()
    },
    showMore: function() {
      this.firstResult += this.$root.config.maxProcessesResults
      this.loadInstances(true)
    },
    filterInstances: function(filter) {
      this.filter = filter
      this.firstResult = 0
      this.loadInstances()
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
    loadVersionDecision: function(event) {
      this.selectedInstance = null
      nextTick(function() {
        this.activityId = ''
        this.$refs.process.clearState()
        this.loadVersionDecision(event)
      }.bind(this))
    }
  }
}
</script>
