<template>
  <transition name="slide-in" mode="out-in">
    <BWaitingBox v-if="$store.state.process.list === null" class="h-100 d-flex justify-content-center" styling="width:20%"></BWaitingBox>
    <ProcessList v-else></ProcessList>
  </transition>
</template>

<script>
import ProcessList from '@/components/processes/list/ProcessList.vue'
import { BWaitingBox } from 'cib-common-components'

export default {
  name: 'ProcessListView',
  components: { ProcessList, BWaitingBox }
}
</script>
