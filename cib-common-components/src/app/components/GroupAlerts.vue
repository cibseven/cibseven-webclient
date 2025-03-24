<template>
  <div class="about">
    <h1>Alerts</h1>
  </div>

  <div>
    <button @click="toggleVisibility">Show alert</button>
    <pre><code v-pre>&lt;BAlert dismissible fade :show="visibilityValue"&gt;Hello&lt;/BAlert&gt;</code></pre>
    <BAlert dismissible fade :show="visibilityValue">Hello</BAlert>
  </div>

</template>

<script setup>
import { ref } from 'vue';
import BAlert from "@/components/BAlert.vue";

const visibilityValue = ref(false);

const toggleVisibility = () => {
  visibilityValue.value = !visibilityValue.value;
};
</script>

<style scoped>
</style>
