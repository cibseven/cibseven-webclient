<template>
  <div class="about">
    <h1>Buttons</h1>
  </div>

  <div>
    <pre><code v-pre>&lt;BButton&gt;Simple BButon&lt;/BButton&gt;</code></pre>
    <BButton>Simple BButon</BButton>
  </div>

  <div>
    <pre><code v-pre>&lt;BButton variant="light" ref="ref2" class="py-0"&gt;My grey button&lt;/BButton&gt;</code></pre>
    <BButton variant="light" ref="ref2" class="py-0">My grey button</BButton>
  </div>

  <div>
    <pre><code v-pre>&lt;BButtonClose :ariaLabel="'hi'" :title="'title'"&gt;&lt;/BButtonClose&gt;</code></pre>
    <BButtonClose :ariaLabel="'hi'" :title="'title'"></BButtonClose>
  </div>

  <div>
    <pre><code v-pre>i18n: &lt;BButton @click="$refs.assignationModal.hide()"&gt;{{$t('bcomponents.ariaLabelClose')}}&lt;/BButton&gt;</code></pre>
    <BButton @click="$refs.assignationModal.hide()">{{$t('bcomponents.ariaLabelClose')}}</BButton>
  </div>
</template>

<script setup>
import BButton from "@/components/BButton.vue";
import BButtonClose from "@/components/BButtonClose.vue";
</script>

<style scoped>
pre {
  margin:0;
  margin-top: 2em;
}
</style>
