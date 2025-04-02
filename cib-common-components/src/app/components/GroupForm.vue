<template>
  <div class="about">
    <h1>Form Elements</h1>
  </div>

  <div>
    <BFormSelect :options="[1,2,3]"></BFormSelect>
  </div>

</template>

<script setup>
import BFormSelect from "@/components/BFormSelect.vue";
</script>

<style scoped>
</style>
