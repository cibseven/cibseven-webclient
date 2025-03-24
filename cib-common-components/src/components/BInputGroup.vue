<template>
  <div :class="['input-group', wrapperClass, sizeClass]" :style="customStyle">
    <slot name="prepend"></slot>
    <input v-if="!hasDefaultSlot"
      :id="id"
      :type="type"
      :class="['form-control', inputClass]"
      :value="modelValue"
      :aria-labelledby="ariaLabelledby"
      :aria-describedby="ariaDescribedby"
      @input="$emit('update:modelValue', $event.target.value)" />
    <slot></slot>
    <slot name="append"></slot>
  </div>
</template>

<script>
export default {
  props: {
    modelValue: { type: [String, Number], default: '' },
    id: { type: String, default: null },
    type: { type: String, default: 'text' },
    inputClass: { type: String, default: '' },
    wrapperClass: { type: String, default: '' },
    customStyle: { type: [String, Object], default: '' },
    size: { type: String, default: null },
    ariaLabelledby: { type: String, default: null },
    ariaDescribedby: { type: String, default: null }
  },
  computed: {
    sizeClass: function () {
      return this.size === 'sm' ? 'input-group-sm' : this.size === 'lg' ? 'input-group-lg' : ''
    },
    hasDefaultSlot: function () {
      return !!this.$slots.default
    }
  }
}
</script>

<style scoped>
</style>
