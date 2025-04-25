<template>
  <transition name="slide-in" mode="out-in">
    <BWaitingBox v-if="$store.state.decision.list === null" class="h-100 d-flex justify-content-center" styling="width:20%"></BWaitingBox>
    <DecisionList v-else></DecisionList>
  </transition>
</template>

<script>
import DecisionList from '@/components/decisions/list/DecisionList.vue'
import { BWaitingBox } from 'cib-common-components'

export default {
  name: 'DecisionListView',
  components: { DecisionList, BWaitingBox }
}
</script>
