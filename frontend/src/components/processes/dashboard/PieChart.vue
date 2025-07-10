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
  <div class="my-2">
    <router-link v-if="title" :to="link" @click.stop :title="tooltip" class="text-decoration-none">
      <h5 class="link-dark text-center">{{ title }}</h5>
    </router-link>
    <div class="text-center waiting-box-container" v-if="loading">
      <b-waiting-box class="d-inline" styling="width: 150px" :title="$t('admin.loading')" />
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
              this.$emit('click', { item: this.sortedItems[config.dataPointIndex], link: this.link })
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
              size: '59%', // ratio is 1.7
              labels: {
                show: true,
                total: {
                  show: true,
                  label: '',
                  fontFamily: '-apple-system, BlinkMacSystemFont, Segoe UI, Roboto, Helvetica Neue, sans-serif',
                  fontWeight: 500,
                  color: '#0C1A29',
                  fontSize: '28.6px',
                  offsetY: 16,
                  formatter: () => this.isEmptyChart ? '0' : this.values.reduce((a, b) => a + b, 0)
                },
                value: {
                  show: true,
                  fontFamily: '-apple-system, BlinkMacSystemFont, Segoe UI, Roboto, Helvetica Neue, sans-serif',
                  fontWeight: 500,
                  color: '#0C1A29',
                  fontSize: '28.6px',
                  offsetY: 16,
                },
              },
            },
          },
        },
        states: {
          hover: {
            filter: {
              // Disable Hover Effect on Donut Chart
              type: this.isEmptyChart ? 'none' : 'darken', // or 'lighten'
              value: 0.15 // lighten by 15%
            }
          }
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
          // set the tooltip background color in ApexCharts from styles
          fillSeriesColor: false,
        },
        colors: this.isEmptyChart ? ['#C1CEDD'] : [
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

/* set the tooltip background color in ApexCharts  */
.apexcharts-tooltip {
  --bs-bg-opacity: 1;
  background-color: rgba(var(--bs-dark-rgb),
      var(--bs-bg-opacity))!important;
}
</style>
