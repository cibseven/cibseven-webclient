<template>
  <div>
    <router-link v-if="title" :to="link" :title="tooltip" class="text-decoration-none">
      <h5 class="link-dark text-center">{{ title }}</h5>
    </router-link>
    <div class="text-center" v-if="loading">
      <b-waiting-box class="d-inline" styling="width: 84%" :title="$t('admin.loading')" />
    </div>
		<div v-else class="container">
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
    items: Array
  },
  computed: {
    loading: function () {
      return !this.items
    },
    sortedItems() {
      return [...this.items].sort((a, b) => b.value - a.value)
    },
    values: function () {
      if (this.items.length === 0) {
        return [0]
      }
      return this.sortedItems.map((item) => item.value)
    },
    labels: function () {
      return this.sortedItems.map((item) => item.title)
    },
    chartOptions: function () {
      return {
        chart: {
          type: 'donut',
          events: {
            click: (event, chartContext, config) => {
              const item = this.items[config.dataPointIndex]
              let link = '/seven/auth/process/' + item.id
							link += item.tenantId ? ('?tenantId=' + item.tenantId) : ''
              this.$router.push(link)
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
                  formatter: (w) => {
                    return w.globals.seriesTotals.reduce((a, b) => a + b, 0)
                  }
                },
								value: {
									show: true,
									fontWeight: 600,
									fontSize: '36px'
								}
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
