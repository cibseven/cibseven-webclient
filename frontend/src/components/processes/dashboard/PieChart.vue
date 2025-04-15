<template>
    <div>
        <h5 v-if="title" class="chart-title">
            <router-link :to="link" :title="tooltip" class="text-decoration-none">
                <span class="chart-title">{{ title }}</span>
            </router-link>
        </h5>
        <div class="waiting-box-container" v-if="loading">
            <b-waiting-box class="d-inline" styling="width: 84%" :title="$t('admin.loading')" />
        </div>
        <apex-chart v-else
                type="donut"
                :options="chartOptions"
                :series="values"
        />
    </div>
</template>
  
<script>
  import { BWaitingBox } from 'cib-common-components'
  
  export default {
    name: 'PieChart',
    components: { BWaitingBox },
    props: {
      title: String,
      tooltip: String,
      link: String,
      items: {
        type: Array,
        optional: false
      }
    },
    methods: {
        onTitleClick() {
            if (this.link) {
                this.$router.push(this.link)
            }
        }
    },
    computed: {
        loading: function() {
            return !this.items
        },
        sortedItems() {
            return [...this.items].sort((a, b) => b.value - a.value)
        },
        values: function() {
            if (this.items.length === 0) {
                return [0]
            }
            return this.sortedItems.map(item => item.value)
        },
        labels: function() {
            return this.sortedItems.map(item => item.title)
        },
        chartOptions: function() {
            return {
                chart: {
                    type: 'donut',
                    events: {
                        click: (event, chartContext, config) => {
                            const item = this.items[config.dataPointIndex]
                            const link = '/seven/auth/process/' + item.id
                            this.$router.push(link)
                        },
                        dataPointMouseEnter: (event) => {
                            event.target.style.cursor = 'pointer'
                        }
                    }
                },
                labels: this.labels,
                plotOptions: {
                    pie: {
                        donut: {
                            size: '40%',
                            labels: {
                                show: true,
                                total: {
                                    show: true,
                                    // label: '',
                                    formatter: (w) => {
                                        return w.globals.seriesTotals.reduce((a, b) => a + b, 0)
                                    }
                                },
                            },
                        },
                    },
                },
                dataLabels: {
                    enabled: false
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
                            formatter: (seriesName) => `${seriesName}`
                        }
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
                    '#E0E6EE'
                ],
            }
        }
    }
}
</script>

<style scoped>
.chart-title {
    text-align: center;
    font-size: 20px;
    color: #4D6278;
    cursor: pointer;
    margin-bottom: 10px;
    font-weight: bold;
}
.waiting-box-container {
    text-align: center;
}
</style>