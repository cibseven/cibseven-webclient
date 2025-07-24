<!--

    Copyright CIB software GmbH and/or licensed to CIB software GmbH
    under one or more contributor license agreements. See the NOTICE file
    distributed with this work for additional information regarding copyright
    ownership. CIB software licenses this file to you under the Apache License,
    Version 2.0; you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.

-->
<template>
    <div class="container bg-light pt-3 overflow-auto">
      <div class="row justify-content-around">
        <div>
          <RuntimeBatches></RuntimeBatches>
          <HistoricBatches></HistoricBatches>
        </div>
        <div class="mb-3">
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
