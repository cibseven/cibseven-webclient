<template>
  <div class="about">
    <h1>Progress</h1>
  </div>

  <div>
    <pre><code v-pre>&lt;BProgress&gt;&lt;/BProgress&gt;</code></pre>
    <BProgress></BProgress>
  </div>

  <div>
    <pre><code v-pre>&lt;BProgress :value="20" :max="100"&gt;&lt;/BProgress&gt;</code></pre>
    <BProgress :value="20" :max="100"></BProgress>
  </div>

  <div>
    <pre><code v-pre>&lt;BProgress :value="40" :max="100" show-progress animated variant="primary"&gt;&lt;/BProgress&gt;</code></pre>
    <BProgress :value="40" :max="100" show-progress animated variant="primary"></BProgress>
  </div>

  <div>
    <pre><code v-pre>&lt;BProgress :value="100" :max="100" show-progress animated variant="success"&gt;&lt;/BProgress&gt;</code></pre>
    <BProgress :value="100" :max="100" show-progress animated variant="success"></BProgress>
  </div>

</template>

<script setup>
import BProgress from "@/components/BProgress.vue";
</script>

<style scoped>
pre {
  margin:0;
  margin-top: 2em;
}
</style>
