<template>
  <b-modal ref="modal" :title="$t('error.title')" :ok-title="$t('error.ok')" :ok-only="true">
    <div class="container-fluid">
      <div class="d-flex align-items-center">
        <div class="me-4">
          <span class="mdi-36px mdi mdi-alert-octagon-outline text-danger"></span>
        </div>
        <!-- min-width: auto leads to min-width: longest word so it will overflow the parent despite the overflow-wrap: break-word -->
        <div style="min-width: 0">
          <p class="my-4" style="overflow-wrap: break-word">{{ message }}</p>
        </div>
      </div>
    </div>
  </b-modal>
</template>

<script>
export default {
  name: 'ErrorDialog',
  data: function() { return { message: '' } },
  methods: {
    show: function(error) {
      this.message = error.type ? this.$t('errors.' + error.type, error.params) : error
      this.$refs.modal.show()
    }
  }
}
</script>
