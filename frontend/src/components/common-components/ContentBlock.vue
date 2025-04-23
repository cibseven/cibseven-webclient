<template>
  <div v-bind="$attrs" class="p-0 mt-3 bg-white border rounded shadow-sm">
    <h5 class="ps-3 pt-3">
      {{ title }}
      <span v-if="info" v-b-popover.hover.right="info" class="mdi mdi-18px mdi-information-outline text-info"></span>
    </h5>
    <hr class="mb-0">
    <div class="row m-3">
      <slot></slot>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ContentBlock',
  props: {
    title: String,
    info: String
  },
}
</script>
