<template>
  <form :class="['b-dropdown-form', formClass]" @submit.prevent="onSubmit" :aria-label="ariaLabel">
    <slot></slot>
  </form>
</template>

<script>
export default {
  props: {
    formClass: {
      type: String,
      default: ''
    },
    ariaLabel: {
      type: String,
      default: ''
    }
  },
  methods: {
    onSubmit: function () {
      this.$emit('submit')
    }
  }
}
</script>

<style scoped>
</style>
