<template>
  <div class="progress" :class="$attrs.class" :style="progressStyle">
    <slot v-if="$slots.default"></slot>
    <BProgressBar v-else-if="value !== null"
      :value="value"
      :max="max"
      :label="label"
      :variant="variant"
      :animated="animated"
      :striped="striped"
      :showValue="showValue"
      :showProgress="showProgress"
      :precision="precision">
    </BProgressBar>
  </div>
</template>

<script>
import BProgressBar from './BProgressBar.vue'
export default {
  components: { BProgressBar },
    props: {
      variant: { type: String, default: '' },
      animated: { type: Boolean, default: false },
      striped: { type: Boolean, default: false },
      value: { type: Number, default: null },
      max: { type: Number, default: 100 },
      label: { type: String, default: '' },
      height: { type: [String, Number], default: null },
    showValue: { type: Boolean, default: false },
      showProgress: { type: Boolean, default: false },
      precision: { type: Number, default: 0 }
    },
    computed: {
      progressStyle: function() {
          return this.height ? { height: typeof this.height === 'number' ? this.height + 'px' : this.height } : {};
      }
    }
}
</script>

<style scoped>
</style>
