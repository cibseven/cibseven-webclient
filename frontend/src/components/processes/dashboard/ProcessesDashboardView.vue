<template>
  <div class="container-fluid bg-light pt-3 overflow-auto">
    <div class="row justify-content-around">

      <div class="bg-white col-md-11 col-12 shadow p-3 mt-3 mb-3">
        <h3>{{ $t('processes-dashboard.headerActive') }}</h3>
        <hr>
        <div class="row g-0">
          <div class="col-4 px-0">
            <PieChart :items="runningInstances"
              title-prefix="processes-dashboard.items.running-instances"
              link="/seven/auth/processes/list"
            ></PieChart>
          </div>
          <div class="col-4 px-0">
            <PieChart :items="openIncidents"
              title-prefix="processes-dashboard.items.open-incidents"
              link="/seven/auth/processes/list"
            ></PieChart>
          </div>
          <div class="col-4 px-0 m-0">
            <PieChart :items="openHumanTasks"
              title-prefix="processes-dashboard.items.open-human-tasks"
              link="/seven/auth/human-tasks"
            ></PieChart>
          </div>
        </div>
      </div>

      <div class="bg-white col-md-11 col-12 shadow p-3 mt-3">
        <h3>{{ $t('processes-dashboard.headerDeployed') }}</h3>
        <hr>
        <div class="row g-0">
          <DeploymentItem v-for="(item, index) in deploymentItems" :key="index"
            :title-prefix="item.titlePrefx"
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
      runningInstances: [],
      openIncidents: [],
      openHumanTasks: [],
      deploymentItems: [
        { titlePrefx: 'processes-dashboard.items.processes', count: null, link: '/seven/auth/processes/list' },
        { titlePrefx: 'processes-dashboard.items.decisions', count: null, link: '/seven/auth/decisions' },
        { titlePrefx: 'processes-dashboard.items.deployments', count: null, link: '/seven/auth/deployments' },
        { titlePrefx: 'processes-dashboard.items.batches', count: null, link: '/seven/auth/batches' },
      ]
    }
  },
  mounted() {
    this.loadAnalytics()
  },
  methods: {
    async loadAnalytics() {
      try {
        const analytics = await AnalyticsService.getAnalytics()
        console.log(analytics)
        this.runningInstances = analytics.runningInstances
        this.openIncidents = analytics.openIncidents
        this.openHumanTasks = analytics.openHumanTasks
        this.deploymentItems[0].count = analytics.deploymentItems.processes
        this.deploymentItems[1].count = analytics.deploymentItems.decisions
        this.deploymentItems[2].count = analytics.deploymentItems.deployments
        this.deploymentItems[3].count = analytics.deploymentItems.batches
      } catch (error) {
        console.error('Error loading analytics:', error)

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

<style scoped>
</style>
