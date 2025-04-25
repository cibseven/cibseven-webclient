<template>
  <ConfirmDialog v-if="params" ref="confirm"
    @ok="params.ok(params.instance)"
    :ok-title="params?.okTitle">

    <p>{{ params?.message }}</p>
    <p>
      {{ $t('process.id') }}:
      <strong>{{ params?.instance?.id }}</strong>

      <br>{{ $t('process.startTime') }}:
      <strong>{{ params?.instance?.startTime }}</strong>

      <br>{{ $t('process.state') }}:
      <strong>{{ params?.instance?.state }}</strong>
    </p>

  </ConfirmDialog>
</template>

<script>
import ConfirmDialog from '@/components/common-components/ConfirmDialog.vue'

export default {
  name: 'ConfirmActionOnProcessInstanceModal',
  components: { ConfirmDialog },
  data: function() {
    return {
      params: Object
    }
  },
  methods: {
    show: function(params) {
      this.params = params
      this.$refs.confirm.show()
    }
  }
}
</script>
