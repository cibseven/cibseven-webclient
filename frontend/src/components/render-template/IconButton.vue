<template>
  <b-button :size="size" :variant="variant" :title="text" :disabled="disabled">
    <span :class="setIconClasses" class="d-inline-block align-middle" style="line-height: 0;"></span>
    <span class="d-none d-sm-inline">{{ text }}</span>
  </b-button>
</template>

<script>
export default {
  name: 'IconButton',
  props: {
    variant: { type: String, default: 'outline-secondary' },
    icon: String,
    font: { type: Number, default: 18 },
    size: { type: String, default: 'sm' },
    disabled: { type: Boolean, default: false },
    text: String
  },
  computed: {
    setIconClasses: function() {
      return 'icon-cib-' + this.font + 'px icon-' + this.icon
    }
  }
}
</script>
