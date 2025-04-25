<template>
  <ConfirmDialog v-if="params" ref="confirm"
    @ok="params.ok(params.variable)"
    :ok-title="$t('confirm.delete')">

    <p>{{ $t('process-instance.variables.confirmDelete') }}</p>

    <p>
      {{ $t('process-instance.variables.name') }}: <strong>{{ params?.variable?.name }}</strong>
      <br>
      {{ $t('process-instance.variables.type') }}: <strong>{{ params?.variable?.type }}</strong>
    </p>

  </ConfirmDialog>
</template>

<script>
import ConfirmDialog from '@/components/common-components/ConfirmDialog.vue'

export default {
  name: 'DeleteVariableModal',
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
