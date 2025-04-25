<template>
  <div
    class="row"
    :class="customClass"
    role="group"
    :aria-labelledby="ariaLabelledby"
    :aria-describedby="ariaDescribedby">
    <slot></slot>
  </div>
</template>

<script>
export default {
  props: {
    customClass: {
      type: String,
      default: ''
    },
    ariaLabelledby: {
      type: String,
      default: null
    },
    ariaDescribedby: {
      type: String,
      default: null
    }
  }
}
</script>

<style scoped>
</style>
