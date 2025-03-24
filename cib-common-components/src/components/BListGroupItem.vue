<template>
  <component
    :is="itemTag" :class="['list-group-item', itemClasses]"
    :role="isButton ? 'button' : 'listitem'" :aria-current="active ? 'true' : null"
    :aria-disabled="disabled ? 'true' : null" :tabindex="disabled ? -1 : 0"
    @click="handleClick" @mousedown="$emit('mousedown', $event)" style="cursor: pointer"
    @keydown.space.prevent="handleClick" @keydown.enter.prevent="handleClick"
    @mouseenter="$emit('mouseenter', $event)" @mouseleave="$emit('mouseleave', $event)">
      <slot></slot>
  </component>
</template>

<script>
export default {
  props: {
    to: { type: [String, Object], default: null },
    href: { type: String, default: null },
    button: { type: Boolean, default: false },
    active: { type: Boolean, default: false },
    disabled: { type: Boolean, default: false },
    variant: { type: String, default: 'light' }
  },
  computed: {
    itemClasses: function () {
      const classes = {
        'list-group-item-action': this.isButton || this.isLink,
        active: this.active,
        disabled: this.disabled
      }
      if (this.variant) {
        classes[`list-group-item-${this.variant}`] = true
      }
      return classes
    },
    isLink: function () {
      return this.to || this.href
    },
    isButton: function () {
      return this.button
    },
    itemTag: function () {
      return this.isLink ? 'a' : this.isButton ? 'button' : 'li'
    }
  },
  methods: {
    handleClick: function (event) {
      if (this.disabled) {
        event.preventDefault()
        return
      }
      if (this.isLink) {
        if (this.to) {
          this.$router.push(this.to)
        } else if (this.href) {
          window.location.href = this.href
        }
      } else {
        this.$emit('click', event)
      }
    }
  }
}
</script>

<style scoped>
</style>
