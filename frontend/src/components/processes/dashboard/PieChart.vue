<template>
  <div>
    <router-link v-if="title" :to="link" :title="tooltip" class="text-decoration-none">
      <h5 class="link-dark text-center">{{ title }}</h5>
    </router-link>
    <div class="text-center waiting-box-container" v-if="loading">
      <b-waiting-box class="d-inline" styling="width: 84%" :title="$t('admin.loading')" />
    </div>
    <div v-else class="container apex-container">
      <apexchart type="donut" :options="chartOptions" :series="values" />
    </div>
  </div>
</template>
  
<script>
import { BWaitingBox } from 'cib-common-components'
import VueApexCharts from 'vue3-apexcharts'

export default {
  name: 'PieChart',
  components: { apexchart: VueApexCharts, BWaitingBox },
  props: {
    title: String,
    tooltip: String,
    link: String,
    items: Array,
    type: String,
    loading: Boolean,
  },
  computed: {
    isEmptyChart() {
      return this.items.length === 0
    },
    sortedItems() {
      return [...this.items].sort((a, b) => b.value - a.value)
    },
    values() {
      if (this.isEmptyChart) return [1]
      return this.sortedItems.map((item) => item.value)
    },
    labels() {
      return this.sortedItems.map((item) => item.title)
    },
    chartOptions() {
      return {
        chart: {
          type: 'donut',
          events: {
            click: (event, chartContext, config) => {
              const item = this.sortedItems[config.dataPointIndex]
              if (!item || this.type === 'humanTasks') this.$router.push(this.link)
              else {
                let link = '/seven/auth/process/' + item.id
                this.$router.push(link)
              }
            },
            dataPointMouseEnter: (event) => {
              event.target.style.cursor = 'pointer'
            },
          },
        },
        labels: this.labels,
        plotOptions: {
          pie: {
            donut: {
              size: '65%',
              labels: {
                show: true,
                total: {
                  show: true,
                  label: '',
									fontWeight: 600,
                  fontSize: '36px',
                  formatter: () => this.isEmptyChart ? '0' : this.values.reduce((a, b) => a + b, 0)
                },
                value: {
                  show: true,
                  fontWeight: 600,
                  fontSize: '36px',
                },
              },
            },
          },
        },
        dataLabels: {
          enabled: false,
        },
        legend: {
          show: false,
        },
        stroke: {
          show: false,
        },
        tooltip: {
					enabled: !this.isEmptyChart,
          y: {
            title: {
              formatter: (seriesName) => `${seriesName}`,
            },
          },
        },
        colors: [
          '#59799B',
          '#84B6E5',
          '#C1CEDD',
          '#628EC7',
          '#4D6278',
          '#869CB3',
          '#295E98',
          '#68CBC0',
          '#04859C',
          '#66AAEB',
          '#367DC9',
          '#33485E',
          '#9EAAB7',
          '#418A9E',
          '#97BFCA',
          '#B3E5DF',
          '#91CDFF',
          '#B2D8F8',
          '#A8C0DE',
          '#E0E6EE',
        ],
      }
    },
  },
}
</script>

<style>
.apexcharts-datalabel-value {
  cursor: pointer !important;
  pointer-events: auto !important;
}
</style>