<template>
  <span
    :class="['badge', badgeVariant, 'me-1']"
    role="button"
    :aria-disabled="disabled.toString()"
    @click="handleClick">
    <slot></slot>
    <button
      v-if="!disabled"
      type="button"
      class="btn-close ms-2"
      :aria-label="$t('bcomponents.formTag.remove')"
      @click="handleRemove">
    </button>
  </span>
</template>

<script>
export default {
  props: {
    variant: {
      type: String,
      default: 'primary'
    },
    disabled: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    badgeVariant: function () {
      return `bg-${this.variant}`
    }
  },
  methods: {
    handleRemove: function (event) {
      event.stopPropagation()
      this.$emit('remove')
    },
    handleClick: function (event) {
      if (!this.disabled) {
        this.$emit('click', event)
      }
    }
  }
}
</script>

<style scoped>
</style>
