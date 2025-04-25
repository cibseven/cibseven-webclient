<template>
  <div :class="dropright ? 'dropend' : 'dropdown'" ref="dropdownContainer">
    <button class="btn" :class="classes" type="button" ref="toggleButton"
    aria-haspopup="true" :aria-expanded="isOpen.toString()" @click="toggle">
      <slot name="button-content">{{ title }}</slot>
    </button>
    <ul class="dropdown-menu" :class="{ 'dropdown-menu-end': right }" role="menu" @click.stop>
      <slot></slot>
    </ul>
  </div>
</template>

<script>
import { Dropdown } from 'bootstrap'

export default {
  props: {
    title: String,
    variant: String,
    toggleClass: String,
    right: Boolean,
    dropright: Boolean,
    noCaret: Boolean
  },
  computed: {
    classes: function () {
      let res = []
      res.push('btn-' + this.variant)
      if (this.toggleClass) res.push(this.toggleClass)
      if (!this.noCaret) res.push('dropdown-toggle')
      return res
    }
  },
  data: function () {
    return {
      dropdownInstance: null,
      isOpen: false
    }
  },
  mounted: function () {
    this.dropdownInstance = new Dropdown(this.$refs.toggleButton)
    document.addEventListener('click', this.handleDocumentClick)
  },
  beforeUnmount: function () {
    document.removeEventListener('click', this.handleDocumentClick)
  },
  methods: {
    show: function () {
      if (this.dropdownInstance && !this.isOpen) {
        this.dropdownInstance.show()
        this.isOpen = true
        this.$emit('shown')
      }
    },
    hide: function () {
      if (this.dropdownInstance && this.isOpen) {
        this.dropdownInstance.hide()
        this.isOpen = false
        this.$emit('hidden')
      }
    },
    toggle: function (event) {
      if (this.isOpen) {
        this.hide()
      } else {
        this.show()
      }
      if (event) {
        event.stopPropagation()
      }
    },
    handleDocumentClick: function (event) {
      if (this.$refs.dropdownContainer && this.$refs.dropdownContainer.contains(event.target)) {
        return
      }
      this.hide()
    }
  }
}
</script>

<style scoped>
</style>
