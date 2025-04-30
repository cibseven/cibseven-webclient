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
