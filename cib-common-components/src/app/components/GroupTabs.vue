<template>
  <div class="about">
    <h1>Tabs</h1>
  </div>

  <BTabs content-class="mt-3" class="mb-4" title-link-class="py-1">
      <BTab id="1" title="Tab1">Content 1</BTab>
      <BTab id="2" title="Tab2">Content 2</BTab>
      <BTab id="3" title="Tab3">Content 3</BTab>
  </BTabs>

</template>

<script setup>
import BTab from "@/components/BTab.vue";
import BTabs from "@/components/BTabs.vue";
</script>

<style scoped>
</style>
