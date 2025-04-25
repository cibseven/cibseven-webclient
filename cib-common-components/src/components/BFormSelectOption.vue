<template>
  <option
    :value="modelValue"
    :disabled="disabled"
    :aria-disabled="disabled.toString()">
    <slot></slot>
  </option>
</template>

<script>
export default {
  props: {
    modelValue: {
      type: [String, Number, Object, Boolean],
      default: null
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
