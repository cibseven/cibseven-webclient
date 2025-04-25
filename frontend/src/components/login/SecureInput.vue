<template>
  <div class="input-group">
    <input ref="input" :value="modelValue" :placeholder="placeholder" :type="show ? 'text' : 'password'" class="form-control rounded-right pr-5"
      :required="required" :disabled="disabled" :autocomplete="autocomplete" @input="$emit('update:modelValue', $event.target.value)">
    <span @click="show = !show" class="mdi mdi-18px mdi-eye text-secondary"
    style="position: absolute; right: 11px; top: 4px; z-index: 3; cursor: pointer"></span>
    <div class="invalid-feedback">{{ $t('errors.invalid') }}</div>
  </div>
</template>

<script>
export default {
  name: 'SecureInput',
  props: { modelValue: String, placeholder: String, required: Boolean, disabled: Boolean, autocomplete: String },
  data: function() {
    return { show: false }
  }
}
</script>
