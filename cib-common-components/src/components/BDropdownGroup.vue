<template>
  <div :class="['dropdown-group', groupClass]" role="group" :aria-label="$t('bcomponents.ariaLabelDropdownGroup')">
    <slot></slot>
  </div>
</template>

<script>
export default {
  props: {
    groupClass: {
      type: String,
      default: ''
    }
  }
}
</script>

<style scoped>
</style>
