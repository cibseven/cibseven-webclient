<template>
  <div :class="['form-group', wrapperClass]">
    <select
      :id="id"
      :class="['form-select', selectClass]"
      :style="selectStyle"
      :value="modelValue"
      :disabled="disabled"
      :aria-describedby="ariaDescribedby"
      @change="$emit('update:modelValue', $event.target.value)"
      @blur="handleBlur"
      @focus="handleFocus">
      <slot name="first"></slot>
      <template v-for="(group, groupIndex) in groupedOptions">
        <optgroup v-if="group.label" :key="'group-' + groupIndex" :label="group.label">
          <option v-for="(option, index) in group.options"
              :key="'group-' + groupIndex + '-option-' + index"
              :value="option.value" :selected="option.value === modelValue">
            {{ option.text }}
          </option>
        </optgroup>
        <option v-else v-for="(option, index) in group.options"
      :key="'ungrouped-' + groupIndex + '-option-' + index"
          :value="option.value" :selected="option.value === modelValue">
          {{ option.text }}
        </option>
      </template>
    </select>
  </div>
</template>

<script>
export default {
  props: {
    modelValue: { type: [String, Number], default: null },
    options: {
      type: Array,
      default: () => []
    },
    id: { type: String, default: null },
    selectClass: { type: String, default: '' },
    selectStyle: { type: Object, default: () => ({}) },
    wrapperClass: { type: String, default: '' },
    disabled: { type: Boolean, default: false },
    ariaDescribedby: { type: String, default: null }
  },
  computed: {
    groupedOptions: function () {
      return this.options.map(option => {
        if (typeof option === 'object' && option.label && Array.isArray(option.options)) {
          return option
        }
        let normalizedOption = {}
        if (typeof option === 'object') normalizedOption = option
        else normalizedOption = { text: option, value: option }
        return { options: [normalizedOption] }
      })
    }
  },
  methods: {
    handleBlur: function (event) {
      this.$emit('blur', event)
    },
    handleFocus: function (event) {
      this.$emit('focus', event)
    }
  }
}
</script>

<style scoped>
</style>
