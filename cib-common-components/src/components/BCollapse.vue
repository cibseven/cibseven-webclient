<template>
  <div class="collapse"
    :class="{ 'show': isOpen, 'navbar-collapse': isNav }"
    :id="id"
    role="region"
    :aria-expanded="isOpen.toString()"
    @transitionend="handleTransitionEnd">
    <slot></slot>
  </div>
</template>

<script>
export default {
  props: {
    id: {
      type: String,
      required: false,
    },
    isNav: {
      type: Boolean,
      default: false,
    },
    modelValue: {
      type: Boolean,
      default: false,
    },
    accordion: {
      type: String,
      default: null,
    }
  },
  data() {
    return {
      isOpen: this.modelValue
    }
  },
  watch: {
    modelValue(newVal) {
      this.isOpen = newVal
    },
    isOpen(newVal) {
      if (newVal !== this.modelValue) {
        this.$emit('update:modelValue', newVal)
      }
      if (this.accordion && newVal) {
        this.$eventBus.emit('toggle-accordion', { accordion: this.accordion, id: this.id })
      }
    }
  },
  methods: {
    toggle() {
      this.isOpen = !this.isOpen
      this.$emit('update:modelValue', this.isOpen)
    },
    handleTransitionEnd() { },
    close() {
      this.isOpen = false
      this.$emit('update:modelValue', false)
    },
  },
  mounted() {
    this.$eventBus.on('bv::toggle::collapse', (id) => {
      if (this.id === id) {
        this.toggle()
      }
    })
    if (this.accordion) {
      this.$eventBus.on('toggle-accordion', (obj) => {
        if (obj.accordion === this.accordion && this.id !== obj.id) {
          this.close()
        }
      })
    }

  },
  beforeUnmount() {
    this.$eventBus.off('bv::toggle::collapse')
    if (this.accordion) {
      this.$eventBus.off('toggle-accordion')
    }
  }
}
</script>

<style scoped>
</style>
