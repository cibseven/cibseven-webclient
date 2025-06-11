<template>
  <button
    :title="title || displayValue"
    class="btn btn-link text-truncate p-0 text-info text-start"
    :class="isHovered ? 'pe-4' : ''"
    @mouseenter="isHovered = true"
    @mouseleave="isHovered = false"
    @click="handleClick"
  >
    {{ displayValue }}
    <span
      v-if="copyValue && isHovered"
      @click.stop="handleCopy"
      :title="$t('commons.copyValue')"
      class="mdi mdi-18px mdi-content-copy px-2 position-absolute end-0 text-secondary lh-sm"
    ></span>
  </button>
</template>

<script>
export default {
  name: 'CopyableActionButton',
  props: {
    /**
     * The value to display in the button
     */
    displayValue: {
      type: String,
      default: '',
    },
    /**
     * The value to copy to clipboard (defaults to displayValue)
     */
    copyValue: {
      type: String,
      default: null,
    },
    /**
     * Custom title attribute (defaults to displayValue)
     */
    title: {
      type: String,
      default: null,
    },
  },
  emits: ['click', 'copy'],
  data() {
    return {
      isHovered: false,
    }
  },
  computed: {
    valueToCopy() {
      return this.copyValue || this.displayValue
    },
  },
  methods: {
    handleClick() {
      this.$emit('click')
    },
    handleCopy() {
      this.$emit('copy', this.valueToCopy)
    },
  },
}
</script>
