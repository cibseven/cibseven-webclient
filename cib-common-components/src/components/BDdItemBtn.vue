<template>
  <button
    :class="['dropdown-item', buttonClass]"
    type="button"
    :disabled="disabled"
    :aria-disabled="disabled">
    <slot></slot>
  </button>
</template>

<script>
export default {
  props: {
    buttonClass: {
      type: [String, Array, Object],
      default: ''
    },
    disabled: {
      type: Boolean,
      default: false
    }
  }
}
</script>

<style scoped>
</style>
