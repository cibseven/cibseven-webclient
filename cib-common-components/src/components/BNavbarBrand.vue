<template>
  <a class="navbar-brand" :href="'#' + to" :aria-label="$t('bcomponents.navbar.brand')">
    <slot></slot>
  </a>
</template>

<script>
export default {
  props: {
    to: String
  }
}
</script>

<style scoped>
</style>
