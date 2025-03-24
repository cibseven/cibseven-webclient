<template>
  <div
    :class="colClasses"
    :role="role"
    :aria-label="ariaLabel">
    <slot></slot>
  </div>
</template>

<script>
export default {
  props: {
    cols: {
      type: [Number, String],
      default: null
    },
    offset: {
      type: [Number, String],
      default: null
    },
    order: {
      type: [Number, String],
      default: null
    },
    alignSelf: {
      type: String,
      default: null
    },
    role: {
      type: String,
      default: null
    },
    ariaLabel: {
      type: String,
      default: null
    }
  },
  computed: {
    colClasses: function () {
      const classes = []

      if (this.cols) classes.push(`col-${this.cols}`)
      else classes.push('col')

      if (this.offset) classes.push(`offset-${this.offset}`)
      if (this.order) classes.push(`order-${this.order}`)
      if (this.alignSelf) classes.push(`align-self-${this.alignSelf}`)

      return classes.join(' ')
    }
  }
}
</script>

<style scoped>
</style>
