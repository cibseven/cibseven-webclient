<template>
  <li @click="$emit('click', $event)">
    <button class="dropdown-item" type="button" role="menuitem">
      <slot></slot>
    </button>
  </li>
</template>

<script>
export default {
}
</script>

<style scoped>
</style>
