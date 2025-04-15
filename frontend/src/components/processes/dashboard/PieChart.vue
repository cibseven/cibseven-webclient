<template>
    <div>
        <b-waiting-box v-if="loading" />
        <apexchart v-else
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
        default: () => [],
      },
      totalZero: {
        type: String,
        default: '0'
      },
    },
    computed: {
        loading: function() {
            return !this.items || this.items.length === 0
        },
        values: function() {
            return this.items.sort((a, b) => b.value - a.value).map(item => item.value)
        },
        labels: function() {
            return this.items.sort((a, b) => b.value - a.value).map(item => item.title)
        },
        chartOptions: function() {
            return {
                chart: {
                    type: 'donut',
                    events: {
                        dataPointSelection: (event, chartContext, config) => {
                            const item = this.items[config.dataPointIndex]
                            const link = '/seven/auth/process/' + item.id
                            this.$router.push(link)
                        }
                    }
                },
                title: {
                    text: this.title,
                    align: 'center',
                    style: {
                        fontFamily: 'Segoe UI, Arial',
                        fontSize: '20px',
                        color: '#4D6278'
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
                                label: '',
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
