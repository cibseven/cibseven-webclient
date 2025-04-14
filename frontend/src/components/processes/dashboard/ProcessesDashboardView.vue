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
          ></PieChart>
          <PieChart class="col-12 col-md-4 px-0 m-0" :items="openIncidents"
            :title="$t('processes-dashboard.items.open-incidents.title')"
            :tooltip="$t('processes-dashboard.items.open-incidents.tooltip')"
            link="/seven/auth/processes/list"
            :total-zero="$t('processes-dashboard.items.open-incidents.none')"
          ></PieChart>
          <PieChart class="col-12 col-md-4 px-0 m-0" :items="openHumanTasks"
            :title="$t('processes-dashboard.items.open-human-tasks.title')"
            :tooltip="$t('processes-dashboard.items.open-human-tasks.tooltip')"
            link="/seven/auth/human-tasks"
          ></PieChart>
        </div>
      </div>

      <div class="col-12 col-md-11 p-3 my-3 bg-white border rounded shadow-sm">
        <h3>{{ $t('processes-dashboard.headerDeployed') }}</h3>
        <hr>
        <div class="row">
          <DeploymentItem v-for="(item, index) in deploymentItems" :key="index"
            class="col-12 col-md-3"
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
      ],
    }
  },
  mounted() {
    this.loadAnalytics()
  },
  methods: {
    async loadAnalytics() {
      /*try {
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
*/
        this.runningInstances = [
          { value: 1, title: "one", link: '/seven/auth/processes/list'},
          { value: 5, title: "five"},
          { value: 10, title: "ten"},
          { value: 2, title: "two"},
        ]
        this.openIncidents = []
        this.openHumanTasks = null
        this.deploymentItems[0].count = 'x'
        this.deploymentItems[1].count = 'x'
        this.deploymentItems[2].count = 'x'
        this.deploymentItems[3].count = 'x'
  //    }
    }
  }
}
</script>

<style scoped>
</style>
