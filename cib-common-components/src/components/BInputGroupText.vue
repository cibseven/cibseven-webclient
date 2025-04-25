<template>
  <span :class="['input-group-text', textClass]" role="presentation" :aria-hidden="ariaHidden">
    <slot>{{ text }}</slot>
  </span>
</template>

<script>
export default {
  props: {
    text: { type: String, default: '' },
    textClass: { type: String, default: '' },
    ariaHidden: { type: Boolean, default: true }
  }
}
</script>

<style scoped>
</style>
