<template>
  <transition name="fade">
    <div ref="alert" v-if="show_" role="alert" class="alert alert-dismissible" :class="'alert-' + variant" aria-live="assertive" aria-atomic="true">
      <slot></slot>
      <BButtonClose @click="dismiss"></BButtonClose>
    </div>
  </transition>
</template>

<script>
import BButtonClose from './BButtonClose.vue'

export default {
  props: {
    variant: { type: String, default: 'primary' },
    show: { type: [Number, Boolean], default: false }
  },
  components: {
    BButtonClose
  },
  data: function () {
    return {
      show_: false,
      timeoutId: null
    }
  },
  watch: {
    show: function (val) {
      this.show_ = val
      if (typeof val === 'number' && val > 0) {
        this.clearTimeout()
        this.timeoutId = setTimeout(() => {
            this.dismiss()
        }, val * 1000)
      }
    }
  },
  methods: {
    dismiss: function () {
      this.clearTimeout()
      this.show_ = false
      this.$emit('dismissed')
    },
    clearTimeout: function () {
      if (this.timeoutId) {
        clearTimeout(this.timeoutId)
        this.timeoutId = null
      }
    }
  },
  beforeUnmount: function () {
      this.clearTimeout()
  }
}
</script>

<style scoped>
</style>
