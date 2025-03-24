<template>
  <div :class="['position-relative', { 'd-inline-block': noWrap }]"
        :style="{ position: noWrap ? 'relative' : 'static' }"
        :aria-busy="show ? 'true' : 'false'"
        :aria-live="show ? 'polite' : null">
    <slot></slot>
    <div v-if="show" class="overlay position-absolute w-100 h-100"
      :class="{ 'd-flex': !noCenter, 'align-items-center': !noCenter, 'justify-content-center': !noCenter }"
      style="top: 0; left: 0;">
      <div class="w-100 h-100 position-absolute" :style="{ backgroundColor: overlayColor }"></div>
      <div class="position-relative">
        <slot name="overlay"></slot>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    show: { type: Boolean, default: false },
    opacity: { type: Number, default: 0.5 },
    noCenter: { type: Boolean, default: false },
    noWrap: { type: Boolean, default: false }
  },
  computed: {
    overlayColor: function () {
      return `rgba(255, 255, 255, ${this.opacity})`
    }
  }
}
</script>

<style scoped>
</style>
