<template>
  <transition name="slide-in" mode="out-in">
    <BWaitingBox v-if="$store.state.process.list === null" class="h-100 d-flex justify-content-center" styling="width:20%"></BWaitingBox>
    <StartProcessList v-else></StartProcessList>
  </transition>
</template>

<script>
import StartProcessList from '@/components/start-process/StartProcessList.vue'
import { BWaitingBox } from 'cib-common-components'

export default {
  name: 'StartProcessView',
  components: { StartProcessList, BWaitingBox }
}
</script>
