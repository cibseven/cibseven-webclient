<template>
  <div class="d-flex flex-column bg-light overflow-auto">
    <div class="container bg-light pt-3 px-0">

      <div class="col-12 p-0 my-3 bg-white border rounded shadow-sm">
        <h5 class="ps-3 pt-3">{{ $t('processes-dashboard.headerActive') }}</h5>
        <hr>
        <div class="row">
          <PieChart class="col-12 col-md-4 px-0 m-0" :items="runningInstancesSeries"
            :title="$t('processes-dashboard.items.running-instances.title')"
            :tooltip="$t('processes-dashboard.items.running-instances.tooltip')"
            link="/seven/auth/processes/list"
          ></PieChart>
          <PieChart class="col-12 col-md-4 px-0 m-0" :items="openIncidentsSeries"
            :title="$t('processes-dashboard.items.open-incidents.title')"
            :tooltip="$t('processes-dashboard.items.open-incidents.tooltip')"
            link="/seven/auth/processes/list"
          ></PieChart>
          <PieChart class="col-12 col-md-4 px-0 m-0" :items="openHumanTasksSeries"
            :title="$t('processes-dashboard.items.open-human-tasks.title')"
            :tooltip="$t('processes-dashboard.items.open-human-tasks.tooltip')"
            link="/seven/auth/human-tasks"
          ></PieChart>
        </div>
      </div>

      <div class="col-12 p-0 my-3 bg-white border rounded shadow-sm">
        <h5 class="ps-3 pt-3">{{ $t('processes-dashboard.headerDeployed') }}</h5>
        <hr class="mb-0">
        <div class="row p-3">
          <DeploymentItem v-for="(item, index) in deploymentItems" :key="index"
            class="col-12 col-md-3"
            :title="$t(item.title)"
            :tooltip="$t(item.tooltip)"
            :count="item.count"
            :link="item.link"
          ></DeploymentItem>
        </div>
      </div>

    </div>
  </div>
</template>

<script>
import { AnalyticsService } from '@/services.js'
import DeploymentItem from '@/components/processes/dashboard/DeploymentItem.vue'
import PieChart from './PieChart.vue'

export default {
  name: 'ProcessesDashboardView',
  components: { DeploymentItem, PieChart },
  data() {
    return {
      errorLoading: false,
      runningInstances: null,
      openIncidents: null,
      openHumanTasks: null,
      deploymentItems: [
        { title: 'processes-dashboard.items.processes.title',
        tooltip: 'processes-dashboard.items.processes.tooltip', count: null, link: '/seven/auth/processes/list' },
        { title: 'processes-dashboard.items.decisions.title',
        tooltip: 'processes-dashboard.items.decisions.tooltip', count: null, link: '/seven/auth/decisions' },
        { title: 'processes-dashboard.items.deployments.title',
        tooltip: 'processes-dashboard.items.deployments.tooltip', count: null, link: '/seven/auth/deployments' },
        { title: 'processes-dashboard.items.batches.title',
        tooltip: 'processes-dashboard.items.batches.tooltip', count: null, link: '/seven/auth/batches' },
      ],
    }
  },
  created() {
    this.loadAnalytics()
  },
  computed: {
    runningInstancesSeries() {
      return this.runningInstances ? this.runningInstances : []
    },
    openIncidentsSeries() {
      return this.openIncidents ? this.openIncidents : []
    },
    openHumanTasksSeries() {
      return this.openHumanTasks ? this.openHumanTasks : []
    },
  },
  methods: {
    async loadAnalytics() {
      try {
        this.errorLoading = false
        const analytics = await AnalyticsService.getAnalytics()

        // Prepare data for charts
        this.runningInstances = analytics.runningInstances
        this.openIncidents = analytics.openIncidents
        this.openHumanTasks = analytics.openHumanTasks

        this.deploymentItems[0].count = analytics.processDefinitionsCount
        this.deploymentItems[1].count = analytics.decisionDefinitionsCount
        this.deploymentItems[2].count = analytics.deploymentsCount
        this.deploymentItems[3].count = analytics.batchesCount

      } catch (error) {

        console.error('Error loading analytics:', error)
        this.errorLoading = true
        this.deploymentItems[0].count = 'x'
        this.deploymentItems[1].count = 'x'
        this.deploymentItems[2].count = 'x'
        this.deploymentItems[3].count = 'x'
      }
    }
  }
}
</script>