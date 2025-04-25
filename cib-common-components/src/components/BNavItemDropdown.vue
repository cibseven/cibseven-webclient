<template>
  <li class="nav-item dropdown">
  <a class="nav-link dropdown-toggle" href="javascript:void(0)" role="button" data-bs-toggle="dropdown"
    aria-expanded="false" aria-haspopup="true" :aria-label="$t('bcomponents.navbar.toggleDropdown')">
    <slot name="button-content"></slot>
  </a>
    <ul class="dropdown-menu dropdown-menu-end" role="menu">
      <slot></slot>
    </ul>
  </li>
</template>

<script>
export default {
}
</script>

<style scoped>
</style>
