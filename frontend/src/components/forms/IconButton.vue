<template>
  <b-button :size="size" :variant="variant" :title="text" :disabled="disabled" class="mx-1">
    <span v-if="!loading" :class="setIconClasses" class="d-inline-block align-middle" style="line-height: 0;"></span>
    <b-spinner v-else small></b-spinner>
    <span class="d-none d-sm-inline">{{ text }}</span>
  </b-button>
</template>

<script>
import { inject, computed } from 'vue';

export default {
  props: {
    variant: { type: String, default: 'outline-secondary' },
    icon: String,
    font: { type: Number, default: 18 },
    size: { type: String, default: 'md' },
    disabled: { type: Boolean, default: false },
    text: String,
    loading: { type: Boolean, default: false }
  },
  setup(props) {
    const isMobile = inject('isMobile');

    const setIconClasses = computed(() => {
      return `icon-cib-${props.font} icon-${props.icon}`;
    });

    return {
      isMobile,
      setIconClasses
    };
  }
};
</script>
