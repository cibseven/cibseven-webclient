<template>
  <div class="about">
    <h1>Avatars</h1>
  </div>

  <div>
    <pre><code v-pre>&lt;BAvatar&gt;&lt;/BAvatar&gt;</code></pre>
    <BAvatar></BAvatar>
  </div>
  <div>
    <pre><code v-pre>&lt;BAvatar :text="'Name'"&gt;&lt;/BAvatar&gt;</code></pre>
    <BAvatar :text="'Name'"></BAvatar>
  </div>
  <div>
    <pre><code v-pre>&lt;BAvatar :text="'Name'" :rounded=false&gt;&lt;/BAvatar&gt;</code></pre>
     <BAvatar :text="'Name'" :rounded="false"></BAvatar>
  </div>
  <div>
    <pre><code v-pre>&lt;BAvatar :text="'Name'" :variant="secondary"&gt;&lt;/BAvatar&gt;</code></pre>
    <BAvatar :text="'Name'" :variant="'secondary'"></BAvatar>
  </div>
  <div>
    <pre><code v-pre>&lt;BAvatar :variant="'secondary'" :rounded="false" :src="'https://www.cib.de/wp-content/uploads/2023/10/CIB_Logo_.svg'" :alt="'CIB Logo'"&gt;&lt;/BAvatar&gt;</code></pre>
    <BAvatar :variant="'secondary'" :rounded="false" :src="'https://www.cib.de/wp-content/uploads/2023/10/CIB_Logo_.svg'" :alt="'CIB Logo'"></BAvatar>
  </div>

</template>

<script setup>
import BAvatar from "@/components/BAvatar.vue";
</script>

<style scoped>
pre {
  margin:0;
  margin-top: 2em;
}
</style>
