<template>
    <div class="container-fluid bg-light pt-3 overflow-auto">
      <div class="row justify-content-around">
        <div class=" col-md-7 col-12 p-3">
          <RuntimeBatches></RuntimeBatches>
          <HistoricBatches></HistoricBatches>
        </div>
        <div class="col-md-5 col-12 p-3">
          <BatchDetails></BatchDetails>
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
  components: { RuntimeBatches, HistoricBatches, BatchDetails }
}
</script>
