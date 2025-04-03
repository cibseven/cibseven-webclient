<template>
  <ConfirmDialog v-if="params" ref="confirm"
    @ok="params.ok(params.processDefinition)"
    :ok-title="$t('process.deleteProcessDefinition.confirm.title')">

    <i18n-t keypath="process.deleteProcessDefinition.confirm.line1" tag="p" scope="global"></i18n-t>
    <i18n-t keypath="process.deleteProcessDefinition.confirm.line2" tag="p" scope="global">
      <template #name>
        <strong>{{ params?.processDefinition?.name }}</strong>
        <br>
      </template>
      <template #version>
        <strong>{{ params?.processDefinition?.version }}</strong>
        <br>
      </template>
    </i18n-t>

  </ConfirmDialog>
</template>

<script>
import ConfirmDialog from '@/components/common-components/ConfirmDialog.vue'

export default {
  name: 'DeleteProcessDefinitionModal',
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
