<template>
  <div :class="['card-text', textClass]" role="article">
    <slot></slot>
  </div>
</template>

<script>
export default {
  props: {
    textClass: { type: String, default: '' }
  }
}
</script>

<style scoped>
</style>
