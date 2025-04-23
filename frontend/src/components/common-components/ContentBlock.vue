<template>
  <div v-bind="$attrs" class="p-0 mt-3 bg-white border rounded shadow-sm">
    <div class="d-flex justify-content-between align-items-center px-3 pt-3">
      <h5 class="mb-0">
        {{ title }}
        <span v-if="info" v-b-popover.hover.right="info" class="mdi mdi-18px mdi-information-outline text-info"></span>
      </h5>
      <div>
        <slot name="actions"></slot>
      </div>
    </div>
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
