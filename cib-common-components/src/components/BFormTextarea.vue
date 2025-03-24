<template>
  <div :class="['form-group', wrapperClass]">
    <textarea ref="textarea" :id="id" :class="['form-control', textareaClass]"
      :placeholder="placeholder" :rows="parsedRows" :style="{ resize: resizeStyle }"
      :aria-describedby="ariaDescribedby"
      :aria-label="ariaLabel"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
      @blur="handleBlur"
      @focus="handleFocus">
    </textarea>
  </div>
</template>

<script>
export default {
  props: {
    modelValue: { type: [String, Number], default: '' },
    placeholder: { type: String, default: '' },
    id: { type: String, default: null },
    rows: { type: [Number, String], default: 3 },
    resize: { type: String, default: 'vertical' },
    textareaClass: { type: String, default: '' },
    wrapperClass: { type: String, default: '' },
    ariaDescribedby: { type: String, default: null },
    ariaLabel: { type: String, default: null },
    noResize: { type: Boolean, default: false }
  },
  computed: {
    resizeStyle: function () {
      if (this.noResize === true) return 'none'
      return ['vertical', 'horizontal', 'both', 'none'].includes(this.resize) ? this.resize : 'vertical'
    },
    parsedRows: function () {
      return typeof this.rows === 'string' ? parseInt(this.rows, 10) : this.rows
    }
  },
  methods: {
    handleBlur: function (event) { this.$emit('blur', event) },
    handleFocus: function (event) { this.$emit('focus', event) },
    focus: function () { this.$refs.textarea.focus() }
  }
}
</script>

<style scoped>
</style>
