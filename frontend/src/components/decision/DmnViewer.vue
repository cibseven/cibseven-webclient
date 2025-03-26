<template>
  <div class="h-100 position-relative">
    <BWaitingBox
      v-if="loader"
      class="h-100"
      ref="loader"
      styling="width:25%;height:25%;align-items:center;justify-content:center;position:absolute;top:36%;right:36%"
    />
    <div :class="loader ? 'invisible' : 'visible'" class="h-100">
      <div class="h-100" ref="diagram"></div>

      <!-- Zoom Controls -->
      <div class="btn-group-vertical position-absolute" style="right:15px; bottom:15px;">
        <b-button size="sm" class="border" variant="light" title="Zoom In" @click="zoomIn">
          <span class="mdi mdi-18px mdi-plus"></span>
        </b-button>
        <b-button size="sm" class="border" variant="light" title="Zoom Out" @click="zoomOut">
          <span class="mdi mdi-18px mdi-minus"></span>
        </b-button>
        <b-button size="sm" class="border" variant="light" title="Reset Zoom" @click="resetZoom">
          <span class="mdi mdi-18px mdi-target"></span>
        </b-button>
      </div>
    </div>
  </div>
</template>

<script>
// Import dmn-js with full modeling capability
import DmnJS from 'dmn-js'

// UI loading indicator
import { BWaitingBox } from 'cib-common-components'

// Required styles
import 'dmn-js/dist/assets/diagram-js.css'
import 'dmn-js/dist/assets/dmn-js-shared.css'
import 'dmn-js/dist/assets/dmn-js-decision-table.css'
import 'dmn-js/dist/assets/dmn-js-drd.css'
import 'dmn-js/dist/assets/dmn-js-literal-expression.css'
import 'dmn-js/dist/assets/dmn-font/css/dmn.css'

// Navigation modules for zoom/pan
import zoomScrollModule from 'diagram-js/lib/navigation/zoomscroll'
import moveCanvasModule from 'diagram-js/lib/navigation/movecanvas'

export default {
  name: 'DmnViewer',
  components: { BWaitingBox },
  data() {
    return {
      viewer: null,
      loader: true
    }
  },
  mounted() {
    // Initialize viewer with zoom and pan support
    this.viewer = new DmnJS({
      container: this.$refs.diagram,
      keyboard: { bindTo: window },
      drd: {
        additionalModules: [zoomScrollModule, moveCanvasModule]
      }
    })
  },
  methods: {
    showDiagram(xml) {
      this.loader = true

      this.viewer.importXML(xml).then(() => {
        // Open the first decision if available
        const decisions =
          this.viewer.getDefinitions()?.drgElement?.filter(el => el.$type === 'dmn:Decision') || []

        if (decisions.length > 0 && typeof this.viewer.openDecision === 'function') {
          this.viewer.openDecision(decisions[0].id)
        }

        // Auto-fit the viewport
        const activeViewer = this.viewer.getActiveViewer()
        if (activeViewer && activeViewer.get('canvas')) {
          activeViewer.get('canvas').zoom('fit-viewport')
        }

        this.loader = false
        this.$emit('loaded')
      }).catch(err => {
        console.error('Error loading DMN diagram:', err)
        this.$emit('error', err)
        this.loader = false
      })
    },

    // Zoom controls
    zoomIn() {
      this.viewer.getActiveViewer()?.get('zoomScroll')?.stepZoom(1)
    },
    zoomOut() {
      this.viewer.getActiveViewer()?.get('zoomScroll')?.stepZoom(-1)
    },
    resetZoom() {
      this.viewer.getActiveViewer()?.get('canvas')?.zoom('fit-viewport')
    }
  }
}
</script>
