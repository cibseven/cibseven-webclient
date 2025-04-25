<template>
  <b-alert dismissible fade variant="success" :show="countdown" @dismissed="countdown = 0" class="d-flex"
    style="position: fixed; z-index: 1021; left: 50%; transform: translate(-50%, 0)" :style="'top: ' + top">
    <!-- .input-group-append .btn, .input-group-prepend .btn has z-index: 2 -->

    <div class="container-fluid">
      <div class="d-flex align-items-center">
        <div class="me-4">
          <span class="mdi-36px mdi mdi-check-circle-outline text-success me-4"></span>
        </div>
        <div class="me-3">
          <slot></slot>
        </div>
      </div>
    </div>
  </b-alert>
</template>

<script>
export default {
  name: 'SuccessAlert',
  props: { top: { type: String, default: '60px' } },
  data: function() { return { countdown: 0 } },
  methods: {
    show: function(time) { this.countdown = time || 5 }
  }
}
</script>
