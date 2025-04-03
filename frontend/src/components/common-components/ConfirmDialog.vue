<template>
  <b-modal ref='modal' :title="$t('confirm.title')">
    <div class="row">
      <div class="col-2 d-flex justify-content-center">
        <span class="mdi-36px mdi mdi-alert-outline text-warning"></span>
      </div>
      <div class="col-10 d-flex align-items-center ps-0">
        <div>
          <slot :param="param"></slot>
        </div>
      </div>
    </div>
    <template v-slot:modal-footer>
      <b-button @click="$refs.modal.hide('cancel')" variant="link">{{ $t('confirm.cancel') }}</b-button>
      <b-button @click="$emit('ok', param); $refs.modal.hide('ok')" variant="primary">{{ okTitle || $t('confirm.ok') }}</b-button>
    </template>
  </b-modal>
</template>

<script>
export default {
  name: 'ConfirmDialog',
  props: {
    okTitle: { type: String, default: null },
  },
  data: function() { return { param: null } },
  methods: {
    show: function(param) {
      this.param = param
      this.$refs.modal.show()
    }
  }
}
</script>
