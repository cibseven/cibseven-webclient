<template>
    <div class="container-fluid bg-light pt-3 overflow-auto">
      <div class="row justify-content-around">
        <div class=" col-md-7 col-12 p-3">
          <RuntimeBatches ref="runtimeBatches"></RuntimeBatches>
          <HistoricBatches ref="historicBatches"></HistoricBatches>
        </div>
        <div class="col-md-5 col-12 p-3">
          <BatchDetails @batch-deleted="handleBatchDeleted"></BatchDetails>
        </div>
      </div>
    </div>
</template>

<script>
import RuntimeBatches from '@/components/batches/tables/RuntimeBatches.vue'
import HistoricBatches from '@/components/batches/tables/HistoricBatches.vue'
import BatchDetails from '@/components/batches/tables/BatchDetails.vue'

export default {
  name: 'BatchesView',
  components: { RuntimeBatches, HistoricBatches, BatchDetails },
  methods: {
    handleBatchDeleted: function(batch) {
      if (batch.type === 'history')
        this.$refs.historicBatches.removeBatch(batch.id)
      else 
        this.$refs.runtimeBatches.removeBatch(batch.id)
    }
  }
}
</script>
