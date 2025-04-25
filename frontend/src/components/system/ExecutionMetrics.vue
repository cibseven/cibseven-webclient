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
  <div class="container-fluid pt-4 d-flex flex-column h-100">
    <h4>{{ $t('admin.system.execution-metrics.title') }}</h4>
    <div class="alert alert-info">{{ $t('admin.system.execution-metrics.metricsHelp') }}</div>
		<div class="flex-fill overflow-auto">
			<div v-if="!loading">
				<h5>{{ $t('admin.system.execution-metrics.usageLast12MonthsByMonth') }}</h5>
				<apexchart height="380" :options="options" :series="series"></apexchart>
				<div class="pb-3">
					<FlowTable
						striped
						resizable
						thead-class="sticky-header"
						:items="monthlyItems"
						primary-key="index"
						prefix="admin.system.execution-metrics."
						:fields="monthlyFields"
					>
					</FlowTable>
				</div>
				<div class="pb-3">
					<h5>{{ $t('admin.system.execution-metrics.annualUsageBySubscriptionTerm') }}</h5>
					<FlowTable
						striped
						resizable
						thead-class="sticky-header"
						:items="yearlyItems"
						primary-key="index"
						prefix="admin.system.execution-metrics."
						:fields="yearlyFields"
					>
					</FlowTable>
				</div>
			</div>
			<div v-else class="py-3 text-center w-100">
				<BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox>
				{{ $t('admin.loading') }}
			</div>
		</div>
  </div>
</template>

<script>
import { SystemService } from '@/services.js'
import moment from 'moment'
import VueApexCharts from 'vue3-apexcharts'
import FlowTable from '@/components/common-components/FlowTable.vue'
import { BWaitingBox } from 'cib-common-components'

export default {
  name: 'ExecutionMetrics',
  components: { apexchart: VueApexCharts, FlowTable, BWaitingBox },
  data() {
    return {
      metrics: {
        annual: [],
        monthly: [],
      },
      metricNames: ['process-instances', 'decision-instances', 'task-users'],
      subsDate: new Date(),
      loading: true
    }
  },
  watch: {
    subsDate() {
      this.loading = true
      Promise.all([this.loadAnnualMetrics(), this.loadMonthlyMetrics()]).then(
        () => (this.loading = false)
      )
    }
  },
  computed: {
    labels() {
      return [
        ...new Set(
          this.metrics.monthly.map((d) =>
            moment(`${d.subscriptionYear}-${d.subscriptionMonth}`, 'YYYY-M').format('MMMM YYYY')
          )
        )
      ].sort((a, b) => moment(a, 'MMMM YYYY') - moment(b, 'MMMM YYYY'))
    },
    series() {
      return this.metricNames.map((metric) => ({
        name: this.$t('admin.system.execution-metrics.' + metric),
        data: this.labels.map((label) => {
          const match = this.metrics.monthly.find(
            (d) =>
              d.metric === metric &&
              moment(`${d.subscriptionYear}-${d.subscriptionMonth}`, 'YYYY-M').format(
                'MMMM YYYY'
              ) === label
          )
          return match ? match.sum : 0
        })
      }))
    },
    options() {
      return {
        chart: {
          type: 'bar',
          fontFamily: 'Segoe UI, sans-serif',
          toolbar: {
            show: false
          }
        },
        plotOptions: {
          bar: {
            dataLabels: {
              position: 'top'
            }
          }
        },
        dataLabels: {
          formatter: function (val) {
            return val === 0 ? '' : val
          },
          offsetY: -18,
          style: {
            colors: ['#4D6278']
          }
        },
        xaxis: {
          categories: this.labels
        },
        legend: {
          position: 'top',
          horizontalAlign: 'center'
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
        ]
      }
    },
    monthlyItems() {
      const grouped = {}
      this.metrics.monthly.forEach((item) => {
        const monthKey = moment(
          `${item.subscriptionYear}-${item.subscriptionMonth}`,
          'YYYY-M'
        ).format('MMMM YYYY')
        if (!grouped[monthKey]) {
          grouped[monthKey] = { month: monthKey }
          this.metricNames.forEach((metric) => {
            grouped[monthKey][metric] = 0
          })
        }
        grouped[monthKey][item.metric] = item.sum
      })
      return Object.values(grouped)
        .sort((a, b) => moment(b.month, 'MMMM YYYY') - moment(a.month, 'MMMM YYYY'))
        .map((entry, i) => ({ index: i + 1, ...entry }))
    },
    yearlyItems() {
      const grouped = {}
      this.metrics.annual.forEach((item) => {
        const year = item.subscriptionYear
        if (!grouped[year]) {
          grouped[year] = { year }
          this.metricNames.forEach((metric) => {
            grouped[year][metric] = 0
          })
        }
        grouped[year][item.metric] = item.sum
      })
      const sortedGroup = Object.values(grouped)
        .sort((a, b) => b.year - a.year)
        .map((entry, i) => ({ index: i + 1, ...entry }))
      if (sortedGroup.length > 0) {
        const subsDate = moment(this.subsDate).format('L')
        const prevDate = moment(this.subsDate).subtract(1, 'years').format('L')
        sortedGroup[0].year = this.$t('admin.system.execution-metrics.fromUpToToday', {
          from: subsDate
        })
        sortedGroup[1].year = this.$t('admin.system.execution-metrics.fromTo', {
          from: prevDate,
          to: subsDate
        })
      }
      return sortedGroup
    },
    monthlyFields() {
      return [
        {
          label: '',
          key: 'month',
          class: 'col-6',
          sortable: false,
          thClass: 'border-end',
          tdClass: 'border-end py-1 border-top-0',
        },
        {
          label: 'process-instances',
          key: 'process-instances',
          sortable: false,
          class: 'col-2',
          thClass: 'border-end',
          tdClass: 'border-end py-1 border-top-0',
        },
        {
          label: 'decision-instances',
          key: 'decision-instances',
          sortable: false,
          class: 'col-2',
          thClass: 'border-end',
          tdClass: 'border-end py-1 border-top-0',
        },
        {
          label: 'task-users',
          key: 'task-users',
          class: 'col-2',
          sortable: false,
          tdClass: 'py-1 border-top-0',
        }
      ]
    },
    yearlyFields() {
      return [
        {
          label: '',
          key: 'year',
          class: 'col-6',
          sortable: false,
          thClass: 'border-end',
          tdClass: 'border-end py-1 border-top-0',
        },
        {
          label: 'process-instances',
          key: 'process-instances',
          sortable: false,
          class: 'col-2',
          thClass: 'border-end',
          tdClass: 'border-end py-1 border-top-0',
        },
        {
          label: 'decision-instances',
          key: 'decision-instances',
          sortable: false,
          class: 'col-2',
          thClass: 'border-end',
          tdClass: 'border-end py-1 border-top-0',
        },
        {
          label: 'task-users',
          key: 'task-users',
          class: 'col-2',
          sortable: false,
          tdClass: 'py-1 border-top-0',
        }
      ]
    }
  },
  mounted() {
    Promise.all([this.loadAnnualMetrics(), this.loadMonthlyMetrics()]).then(
      () => (this.loading = false)
    )
  },
  methods: {
    loadAnnualMetrics() {
      const params = {
        subscriptionStartDate: moment(this.subsDate).format('YYYY-MM-DD[T]HH:mm:ss.SSSZZ'),
        groupBy: 'year',
      }
      return SystemService.getMetricsData(params).then((data) => {
        this.metrics.annual = data
      })
    },
    loadMonthlyMetrics() {
      const params = {
        subscriptionStartDate: moment(this.subsDate).format('YYYY-MM-DD[T]HH:mm:ss.SSSZZ'),
        groupBy: 'month',
        metrics: 'process-instances,decision-instances,task-users',
        startDate: moment(this.subsDate)
          .subtract(1, 'years')
          .startOf('day')
          .format('YYYY-MM-DD[T]HH:mm:ss.SSSZZ'),
      }
      return SystemService.getMetricsData(params).then((data) => {
        this.metrics.monthly = data
      })
    },
    shouldDisableDate: function (ymd, date) {
      const today = moment().startOf('day')
      const oneYearAgo = moment().subtract(1, 'years').startOf('day')
      const dateMoment = moment(date).startOf('day')
      return dateMoment.isBefore(oneYearAgo) || dateMoment.isAfter(today)
    }
  },
}
</script>
