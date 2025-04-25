<template>
  <div class="input-group-prepend">
    <span v-if="text" class="input-group-text" :role="role" :aria-hidden="ariaHidden">{{ text }}</span>
    <slot v-else></slot>
  </div>
</template>

<script>
export default {
  props: {
    text: {
      type: String,
      default: null
    },
    role: {
      type: String,
      default: 'presentation'
    },
    ariaHidden: {
      type: Boolean,
      default: true
    }
  }
}
</script>

<style scoped>
</style>
