<template>
  <li @click="$emit('click', $event)">
    <a class="dropdown-item" :class="{ active: active }" :href="url" role="menuitem" :aria-current="active ? 'page' : null">
      <slot></slot>
    </a>
  </li>
</template>

<script>
export default {
  props: {
    active: Boolean,
    href: String,
    to: String
  },
  computed: {
    url: function () { /*jshint scripturl:true */
      if (this.href) return this.href
      else if (this.to) return '#' + this.to
      else return 'javascript:void(0)'
      // https://stackoverflow.com/a/46997175
    }
  }
}
</script>

<style scoped>
</style>
