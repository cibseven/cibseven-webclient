<template>
  <span
    :class="['badge', variantClass, pillClass]"
    :style="customStyle"
    role="status"
    :aria-label="ariaLabel">
    <slot></slot>
  </span>
</template>

<script>
export default {
  props: {
    variant: {
      type: String,
      default: 'primary'
    },
    pill: {
      type: Boolean,
      default: false
    },
    customStyle: {
      type: [String, Object],
      default: ''
    },
    ariaLabel: {
      type: String,
      default: ''
    }
  },
  computed: {
    variantClass: function () {
      return `text-bg-${this.variant}`
    },
    pillClass: function () {
      return this.pill ? 'rounded-pill' : ''
    }
  }
}
</script>

<style scoped>
</style>
