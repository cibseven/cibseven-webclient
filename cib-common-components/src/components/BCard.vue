<template>
  <div class="card">
    <h5 v-if="title" class="card-title" :aria-level="headingLevel" role="heading">{{ title }}</h5>
    <slot></slot>
  </div>
</template>

<script>
export default {
  props: {
    title: String,
    headingLevel: { type: Number, default: 2 }
  }
}
</script>

<style scoped>
</style>
