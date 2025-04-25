<template>
  <component
    :is="tag"
    ref="button"
    class="btn"
    :class="[sizeClass, classes]"
    :href="computedHref"
    :to="isRouterLink ? to : null"
    :role="tag === 'button' ? 'button' : null"
    :aria-disabled="disabled ? 'true' : null"
    :disabled="disabled && tag === 'button'">
      <slot></slot>
  </component>
</template>

<script>
export default {
  props: {
    variant: { type: String, default: 'primary' },
    block: { type: Boolean, default: false },
    href: { type: String, default: null },
    to: { type: [String, Object], default: null },
    size: { type: String, default: null },
    disabled: { type: Boolean, default: false }
  },
  computed: {
    tag: function () {
      return this.href ? 'a' : (this.to ? 'router-link' : 'button')
    },
    classes: function () {
      const res = []
      res.push('btn-' + this.variant)
      if (this.block) res.push('w-100')
      return res
    },
    sizeClass: function () {
      return this.size === 'sm' ? 'btn-sm' : this.size === 'lg' ? 'btn-lg' : ''
    },
    computedHref: function () {
      return this.href ? this.href : null
    },
    isRouterLink: function () {
      return !!this.to
    }
  },
  methods: {
    click: function (event) {
      if (this.disabled) {
        event.preventDefault()
        event.stopImmediatePropagation()
        return
      }
      if (!this.href && !this.to) {
        this.$emit('click', event)
      }
    },
    submit: function (event) {
      if (this.disabled) {
        event.preventDefault()
        event.stopImmediatePropagation()
        return
      }
      if (!this.href && !this.to) {
        this.$emit('submit', event)
      }
    },
    focus: function () {
      const element = this.$refs.button
      if (element && element.focus) {
        element.focus()
      }
    }
  }
}
</script>

<style scoped>
</style>
