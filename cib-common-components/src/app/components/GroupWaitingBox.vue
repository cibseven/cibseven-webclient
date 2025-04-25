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
