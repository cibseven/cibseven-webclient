<template>
  <div class="text-center">
    <h5 v-if="title">
      <router-link :to="link" :title="tooltip" class="text-decoration-none">
        <span class="link-dark">{{ title }}</span>
      </router-link>
    </h5>

    <div class="donut-chart-container">
      <svg :width="size" :height="size" viewBox="0 0 100 100" class="donut-chart-svg">
        <g v-for="(item, index) in chartData" :key="index">
          <path v-if="item.value === 0"
            :d="getSlicePath(item, index)"
            fill="#C1CEDD"
            :transform="item.transform"
          />
          <router-link v-else :to="item.link ? item.link : link">
            <path
              :d="getSlicePath(item, index)"
              :fill="hoveredIndex === index ? shadeColor(sliceColor(item, index), 10) : sliceColor(item, index)"
              :transform="item.transform"
              @mouseover="hoveredIndex = index"
              @mouseleave="hoveredIndex = null"
              class="donut-chart-slice"
              v-b-popover.hover.right="sliceTitle(item)"
            />
          </router-link>
        </g>
      </svg>
      <div class="donut-chart-center">
        <h5 v-if="isSpecialZeroOutput" class="link-dark">
          <router-link :to="link" :title="tooltip" class="text-decoration-none">
            <span class="link-dark p-1" :class="totalClass">{{ totalZero }}</span>
          </router-link>
        </h5>
        <h2 v-else class="link-dark">
          <span v-if="loading"><BWaitingBox class="d-inline" styling="width: 24px" :title="$t('admin.loading')"></BWaitingBox></span>
          <router-link v-else :to="link" :title="tooltip" class="text-decoration-none">
            <span class="link-dark p-1" :class="totalClass">{{ totalWithZero }}</span>
          </router-link>
        </h2>
      </div>
    </div>
  </div>
</template>

<script>
import { BWaitingBox } from 'cib-common-components'

export default {
  name: 'PieChart',
  components: { BWaitingBox },
  props: {
    size: { type: Number, default: 250 },
    innerRadius: { type: Number, default: 23 }, // Inner radius for the donut hole
    outerRadius: { type: Number, default: 40 }, // Outer radius of the donut
    title: String,
    tooltip: String,
    link: String,
    items: {
      type: Array,
      default: () => []
    },
    totalZero: {
      type: String,
      default: '0'
    },
    palette: {
      type: Array,
      default: () => [
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
  data() {
    return {
      hoveredIndex: null, // Tracks the hovered slice index
    }
  },
  computed: {
    loading: function() {
      return this.items === null
    },
    total: function () {
      if (this.items === null) {
        return 0
      }
      return this.items.reduce((sum, item) => sum + item.value, 0)
    },
    totalWithZero: function () {
      return this.total === 0 ? this.totalZero : this.total
    },
    totalClass: function() {
      return this.totalWithZero === 'x' ? 'text-warning' : ''
    },
    isSpecialZeroOutput: function() {
      return this.total === 0 && (this.totalZero !== 'x' && this.totalZero !== '0')
    },
    chartData: function() {
      let offsetAngle = -90 // Start at 12 o'clock

      if (this.items === null) {
        return [{
          value: 0,
          startAngle: offsetAngle,
          endAngle: offsetAngle + 359
        }]
      }
      else if (this.items.length === 0 || this.total === 0) {
        return [{
          value: 0,
          startAngle: offsetAngle,
          endAngle: offsetAngle + 359
        }]
      }

      const itemsSorted = [...this.items]
      itemsSorted.sort((a, b) => b.value - a.value)

      return itemsSorted.map((item) => {
        const percentage = item.value / this.total
        const startAngle = offsetAngle
        const endAngle = offsetAngle + percentage * 360
        offsetAngle += percentage * 360

        return {
          ...item,
          startAngle,
          endAngle,
        }
      })
    }
  },
  methods: {
    // This function returns the 'd' attribute value for the SVG path element
    getSlicePath(item, index) {
      const startAngle = this.chartData[index].startAngle
      const endAngle = this.chartData[index].endAngle

      // Enlarge radius on hover
      const outerRadius = this.hoveredIndex === index ? this.outerRadius + 1 : this.outerRadius
      const innerRadius = this.hoveredIndex === index ? this.innerRadius - 1 : this.innerRadius

      // Convert angles from degrees to radians
      const startAngleRad = (Math.PI / 180) * startAngle
      const endAngleRad = (Math.PI / 180) * endAngle

      // Calculate the starting and ending points for the path (outer and inner radii)
      const x1Outer = 50 + outerRadius * Math.cos(startAngleRad)
      const y1Outer = 50 + outerRadius * Math.sin(startAngleRad)
      const x2Outer = 50 + outerRadius * Math.cos(endAngleRad)
      const y2Outer = 50 + outerRadius * Math.sin(endAngleRad)

      const x1Inner = 50 + innerRadius * Math.cos(startAngleRad)
      const y1Inner = 50 + innerRadius * Math.sin(startAngleRad)
      const x2Inner = 50 + innerRadius * Math.cos(endAngleRad)
      const y2Inner = 50 + innerRadius * Math.sin(endAngleRad)

      // Create the path for the slice (arc with inner and outer radius)
      const largeArcFlag = endAngle - startAngle > 180 ? 1 : 0

      const pathData = [
        `M ${x1Outer} ${y1Outer}`, // Move to the outer starting point
        `A ${outerRadius} ${outerRadius} 0 ${largeArcFlag} 1 ${x2Outer} ${y2Outer}`, // Outer arc
        `L ${x2Inner} ${y2Inner}`, // Line to the inner arc
        `A ${innerRadius} ${innerRadius} 0 ${largeArcFlag} 0 ${x1Inner} ${y1Inner}`, // Inner arc
        'Z', // Close the path
      ].join(' ')

      return pathData
    },
    sliceTitle: function(item) {
      return item.title + ' (' + item.value + ')'
    },
    sliceColor: function(item, index) {
      return item.color ? item.color : this.palette[index]
    },
    shadeColor: function(color, percent) {
      return '#' + color
        .replace(/^#/, '')
        .replace(/../g,
          color => (0+Math.min(255, Math.max(0, Math.ceil(parseInt(color, 16) * (100 + percent) / 100))).toString(16)).substr(-2)
        )
    }
  }
}
</script>

<style scoped>
.donut-chart-container {
  position: relative;
  width: 100%;
  max-width: 400px;
  margin: auto;
}

.donut-chart-svg {
  display: block;
  width: 100%;
}

.donut-chart-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.donut-chart-slice {
  cursor: pointer;
  transition: transform 0.5s ease;
}
</style>
