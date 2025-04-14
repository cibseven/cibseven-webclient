<template>
  <div class="container-fluid bg-light pt-3 overflow-auto">
    <div class="row justify-content-around">

      <div class="col-12 col-md-11 p-3 my-3 bg-white border rounded shadow-sm">
        <h3>{{ $t('processes-dashboard.headerActive') }}</h3>
        <hr>
        <div class="row">
          <PieChart class="col-12 col-md-4 px-0 m-0" :items="runningInstances"
            :title="$t('processes-dashboard.items.running-instances.title')"
            :tooltip="$t('processes-dashboard.items.running-instances.tooltip')"
            link="/seven/auth/processes/list"
            :total-zero="errorLoading ? 'x': undefined"
          ></PieChart>
          <PieChart class="col-12 col-md-4 px-0 m-0" :items="openIncidents"
            :title="$t('processes-dashboard.items.open-incidents.title')"
            :tooltip="$t('processes-dashboard.items.open-incidents.tooltip')"
            link="/seven/auth/processes/list"
            :total-zero="errorLoading ? 'x': $t('processes-dashboard.items.open-incidents.none')"
          ></PieChart>
          <PieChart class="col-12 col-md-4 px-0 m-0" :items="openHumanTasks"
            :title="$t('processes-dashboard.items.open-human-tasks.title')"
            :tooltip="$t('processes-dashboard.items.open-human-tasks.tooltip')"
            link="/seven/auth/human-tasks"
            :total-zero="errorLoading ? 'x': undefined"
          ></PieChart>
        </div>
      </div>

      <div class="col-12 col-md-11 p-3 my-3 bg-white border rounded shadow-sm">
        <h3>{{ $t('processes-dashboard.headerDeployed') }}</h3>
        <hr>
        <div class="row">
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
import PieChart from '@/components/processes/dashboard/PieChart.vue'
import DeploymentItem from '@/components/processes/dashboard/DeploymentItem.vue'

export default {
  name: 'ProcessesDashboardView',
  components: { PieChart, DeploymentItem },
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
  mounted() {
    this.loadAnalytics()
  },
  methods: {
    async loadAnalytics() {
      try {
        this.errorLoading = false
        const analytics = await AnalyticsService.getAnalytics()
        console.debug(analytics)
        this.runningInstances = Array.from(analytics.runningInstances).map(item => {
          return {
            ...item,
            link: '/seven/auth/process/' + item.id
          }
        })
        this.openIncidents = analytics.openIncidents
        this.openHumanTasks = analytics.openHumanTasks
        this.deploymentItems[0].count = analytics.processDefinitionsCount
        this.deploymentItems[1].count = analytics.decisionDefinitionsCount
        this.deploymentItems[2].count = analytics.deploymentsCount
        this.deploymentItems[3].count = analytics.batchesCount
      } catch (error) {
        console.error('Error loading analytics:', error)
        this.errorLoading = true
        this.runningInstances = []
        this.openIncidents = []
        this.openHumanTasks = []
        this.deploymentItems[0].count = 'x'
        this.deploymentItems[1].count = 'x'
        this.deploymentItems[2].count = 'x'
        this.deploymentItems[3].count = 'x'
      }
    }
  }
}
</script>
