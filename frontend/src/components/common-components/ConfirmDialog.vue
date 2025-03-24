<template>
  <b-modal ref='modal' :title="$t('confirm.title')">
  <div class="container-fluid">
    <div class="d-flex align-items-start">
    <div class="col-1">
      <span class="mdi-36px mdi mdi-alert-outline text-warning"></span>
    </div>
    <div class="container-fluid">
      <slot :param="param"></slot>
    </div>
    </div>
  </div>
  <template v-slot:modal-footer>
    <b-button variant="primary" @click="$emit('ok', param); $refs.modal.hide('ok')">{{ $t('confirm.ok') }}</b-button>
    <b-button @click="$refs.modal.hide('cancel')">{{ $t('confirm.cancel') }}</b-button>
  </template>
  </b-modal>
</template>

<script>
export default {
  name: 'ConfirmDialog',
  data: function() { return { param: null } },
  methods: {
    show: function(param) {
      this.param = param
      this.$refs.modal.show()
    }
  }
}
</script>
