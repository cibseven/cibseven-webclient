<!--

    Copyright CIB software GmbH and/or licensed to CIB software GmbH
    under one or more contributor license agreements. See the NOTICE file
    distributed with this work for additional information regarding copyright
    ownership. CIB software licenses this file to you under the Apache License,
    Version 2.0; you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.

-->
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
