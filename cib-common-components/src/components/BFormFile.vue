<template>
  <div class="mb-3">
    <label v-if="$slots.label" :for="id" class="form-label">
      <slot name="label"></slot>
    </label>
    <label v-else :for="id" class="form-label">{{ label }}</label>
    <input :id="id" :class="classes" type="file" :accept="accept" :multiple="multiple" @change="handleChange" />
  </div>
</template>

<script>
export default {
  props: {
    id: { type: String },
    label: { type: String },
    accept: { type: String, default: '' },
    multiple: { type: Boolean, default: false },
    modelValue: { type: [File, Array], default: () => [] },
    variant: { type: String, default: 'primary' }
  },
  data: function () {
    return {
      files: []
    }
  },
  computed: {
    classes: function () {
      return ['form-control', `form-control-${this.variant}`]
    }
  },
  watch: {
    modelValue: {
      immediate: true,
      handler(newVal) {
        if (newVal && Array.isArray(newVal)) {
          this.files = newVal
        }
      }
    }
  },
  methods: {
    handleChange(event) {
      const selectedFiles = this.multiple ? Array.from(event.target.files) : event.target.files[0]
      this.files = selectedFiles
      this.$emit('update:modelValue', selectedFiles)
    }
  }
}
</script>

<style scoped>
</style>
