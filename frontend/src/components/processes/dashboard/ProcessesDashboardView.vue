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
  <div class="d-flex flex-column bg-light overflow-auto">
    <div class="container bg-light pt-3 px-0">
      <ContentBlock :title="$t('processes-dashboard.headerActive')">
        <PieChart
          class="col-12 col-md-4 px-0 m-0"
          :items="runningInstances"
          :title="$t('processes-dashboard.items.running-instances.title')"
          :tooltip="$t('processes-dashboard.items.running-instances.tooltip')"
          @click="navigateToProcess($event)"
          link="/seven/auth/processes/list"
          :loading="loading"
        ></PieChart>
        <PieChart
          class="col-12 col-md-4 px-0 m-0"
          :items="openIncidents"
          :title="$t('processes-dashboard.items.open-incidents.title')"
          :tooltip="$t('processes-dashboard.items.open-incidents.tooltip')"
          @click="navigateToProcess($event, 'incidents')"
          link="/seven/auth/processes/list?onlyIncidents=true"
          :loading="loading"
        ></PieChart>
        <PieChart
          class="col-12 col-md-4 px-0 m-0"
          :items="openHumanTasks"
          :title="$t('processes-dashboard.items.open-human-tasks.title')"
          :tooltip="$t('processes-dashboard.items.open-human-tasks.tooltip')"
          link="/seven/auth/human-tasks"
          @click="navigateToHumanTasks($event)"
          :loading="loading"
        ></PieChart>
      </ContentBlock>

      <ContentBlock :title="$t('processes-dashboard.headerDeployed')" class="mb-3">
        <DeploymentItem
          v-for="(item, index) in deploymentItems"
          :key="index"
          class="col-12 col-md-3"
          :title="$t(item.title)"
          :tooltip="$t(item.tooltip)"
          :count="item.count"
          :link="item.link"
        ></DeploymentItem>
      </ContentBlock>
    </div>
  </div>
</template>

<script>
import { AnalyticsService } from '@/services.js'
import DeploymentItem from '@/components/processes/dashboard/DeploymentItem.vue'
import PieChart from './PieChart.vue'
import ContentBlock from '@/components/common-components/ContentBlock.vue'

export default {
  name: 'ProcessesDashboardView',
  components: { DeploymentItem, PieChart, ContentBlock },
  data() {
    return {
      errorLoading: false,
      loading: true,
      runningInstances: [],
      openIncidents: [],
      openHumanTasks: [],
      deploymentItems: [
        {
          title: 'processes-dashboard.items.processes.title',
          tooltip: 'processes-dashboard.items.processes.tooltip',
          count: null,
          link: '/seven/auth/processes/list',
        },
        {
          title: 'processes-dashboard.items.decisions.title',
          tooltip: 'processes-dashboard.items.decisions.tooltip',
          count: null,
          link: '/seven/auth/decisions',
        },
        {
          title: 'processes-dashboard.items.deployments.title',
          tooltip: 'processes-dashboard.items.deployments.tooltip',
          count: null,
          link: '/seven/auth/deployments',
        },
        {
          title: 'processes-dashboard.items.batches.title',
          tooltip: 'processes-dashboard.items.batches.tooltip',
          count: null,
          link: '/seven/auth/batches',
        },
      ],
    }
  },
  created() {
    this.loadAnalytics()
  },
  methods: {
    async loadAnalytics() {
      try {
        this.errorLoading = false
        const analytics = await AnalyticsService.getAnalytics()
        // Prepare data for charts
        analytics.runningInstances.forEach((data) => {
          this.normalizeTitle(data)
        })
        this.runningInstances = analytics.runningInstances

        analytics.openIncidents.forEach((data) => {
          this.normalizeTitle(data)
        })
        this.openIncidents = analytics.openIncidents

        analytics.openHumanTasks.forEach((data) => {
          data.title = this.$t('processes-dashboard.items.open-human-tasks.' + data.title)
        })
        this.openHumanTasks = analytics.openHumanTasks
        this.loading = false
        this.deploymentItems[0].count = analytics.processDefinitionsCount
        this.deploymentItems[1].count = analytics.decisionDefinitionsCount
        this.deploymentItems[2].count = analytics.deploymentsCount
        this.deploymentItems[3].count = analytics.batchesCount
      } catch (error) {
        console.error('Error loading analytics:', error)
        this.errorLoading = true
        this.loading = false
        this.deploymentItems[0].count = 'x'
        this.deploymentItems[1].count = 'x'
        this.deploymentItems[2].count = 'x'
        this.deploymentItems[3].count = 'x'
      }
    },
    normalizeTitle(data) {
      data.title = data.title || this.$t('processes-dashboard.unnamedProcess')
      if (data.title === 'others' && !data.id) data.title = this.$t('processes-dashboard.others')
    },
    navigateToProcess(data, tab) {
      if (!data.item || !data.item?.id) {
        this.$router.push(data.link)
      }
      else {
        const [key, tenantId] = data.item.id.split(':')

        const params = new URLSearchParams()
        if (tenantId) params.set('tenantId', tenantId)
        if (tab) params.set('tab', tab)

        const queryString = params.toString() ? `?${params.toString()}` : ''
        this.$router.push(`/seven/auth/process/${key}${queryString}`)
      }
    },
    navigateToHumanTasks(data) {
      this.$router.push(data.link)
    },
  },
}
</script>
