<template>
  <div class="about">
    <h1>Waiting box</h1>
  </div>

  <div>
    <pre><code v-pre>&lt;BWaitingBox styling="width: 15%"&gt;&lt;/BWaitingBox&gt;</code></pre>
    <BWaitingBox styling="width: 15%"></BWaitingBox>
  </div>

  <div>
    <pre><code v-pre>&lt;BWaitingBox class="d-inline me-2" styling="width: 35px"&gt;&lt;/BWaitingBox&gt;</code></pre>
    <BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox>
  </div>

  <div>
    <pre><code v-pre>&lt;BWaitingBox class="d-inline me-2" styling="width: 35px"&gt;&lt;/BWaitingBox&gt;</code></pre>
    <BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox>
  </div>

</template>

<script setup>
import BWaitingBox from "@/components/BWaitingBox.vue";
</script>

<style scoped>
pre {
  margin:0;
  margin-top: 2em;
}
</style>
