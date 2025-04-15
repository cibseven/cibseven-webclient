<template>
  <div class="d-flex flex-column bg-light overflow-auto">
    <div class="container bg-light pt-3 px-0">

      <div class="col-12 p-0 my-3 bg-white border rounded shadow-sm">
        <h5 class="ps-3 pt-3">{{ $t('processes-dashboard.headerActive') }}</h5>
        <hr>
        <div class="row">
          <div class="col-12 col-md-4 px-0 m-0">
            <apexchart
              type="donut"
              :options="chartOptions"
              :series="runningInstancesSeries"
            ></apexchart>
          </div>
          <div class="col-12 col-md-4 px-0 m-0">
            <apexchart
              type="donut"
              :options="chartOptions"
              :series="openIncidentsSeries"
            ></apexchart>
          </div>
          <div class="col-12 col-md-4 px-0 m-0">
            <apexchart
              type="donut"
              :options="chartOptions"
              :series="openHumanTasksSeries"
            ></apexchart>
          </div>
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
import VueApexCharts from "vue3-apexcharts"

export default {
  name: 'ProcessesDashboardView',
  components: { DeploymentItem, apexchart: VueApexCharts },
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
      chartOptions: {
        chart: {
          type: 'donut',
        },
        labels: [],
        legend: {
          position: 'bottom',
        },
        tooltip: {
          y: {
            formatter: (val) => val,
          },
        },
      },
      runningInstancesSeries: [],
      openIncidentsSeries: [],
      openHumanTasksSeries: [],
    }
  },
  mounted() {
    this.loadAnalytics()
  },
  methods: {
    async loadAnalytics() {
      try {
        this.errorLoading = false
        const analytics = await AnalyticsService.getAnalytics()
        console.debug(analytics)

        // Prepare data for charts
        this.runningInstances = analytics.runningInstances
        this.openIncidents = analytics.openIncidents
        this.openHumanTasks = analytics.openHumanTasks

        this.runningInstancesSeries = this.runningInstances.map(item => item.value)
        this.openIncidentsSeries = this.openIncidents.map(item => item.value)
        this.openHumanTasksSeries = this.openHumanTasks.map(item => item.value)

        this.chartOptions.labels = this.runningInstances.map(item => item.title)
        this.deploymentItems[0].count = analytics.processDefinitionsCount
        this.deploymentItems[1].count = analytics.decisionDefinitionsCount
        this.deploymentItems[2].count = analytics.deploymentsCount
        this.deploymentItems[3].count = analytics.batchesCount
      } catch (error) {
        console.error('Error loading analytics:', error)
        this.errorLoading = true
        this.runningInstancesSeries = []
        this.openIncidentsSeries = []
        this.openHumanTasksSeries = []
        this.deploymentItems[0].count = 'x'
        this.deploymentItems[1].count = 'x'
        this.deploymentItems[2].count = 'x'
        this.deploymentItems[3].count = 'x'
      }
    }
  }
}
</script>