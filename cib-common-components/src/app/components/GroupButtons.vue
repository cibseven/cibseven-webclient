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
