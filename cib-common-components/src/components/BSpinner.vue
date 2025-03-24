<template>
  <div :class="['d-inline-block', wrapperClass]" role="status" :aria-live="ariaLive" :aria-busy="ariaBusy">
    <span v-if="label" class="visually-hidden">{{ label }}</span>
    <div :class="[
      'spinner-' + type,
      variant ? 'text-' + variant : '',
      {
        'spinner-grow': isGrow,
        'spinner-border': !isGrow,
        'spinner-border-sm': small && !isGrow,
        'spinner-grow-sm': small && isGrow
      }]" :style="computedStyle"></div>
  </div>
</template>

<script>
export default {
  props: {
    type: { type: String, default: 'border' },
    variant: { type: String, default: '' },
    label: { type: String, default: 'Loading...' },
    small: { type: Boolean, default: false },
    wrapperClass: { type: String, default: '' },
    ariaLive: { type: String, default: 'polite' },
    ariaBusy: { type: Boolean, default: true },
    style: { type: String, default: '' }
  },
  computed: {
    isGrow: function () {
      return this.type === 'grow'
    },
    computedStyle() {
      return this.small ? '' : this.style
    }
  }
}
</script>

<style scoped>
</style>
