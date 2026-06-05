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
  <div class="alert alert-warning m-3 d-flex align-items-center" role="alert">
    <div class="me-3">
      <span class="mdi-36px mdi mdi-alert-outline text-warning"></span>
    </div>
    <div>
      <p v-for="(line, index) in message.split('\n')" :key="index" style="overflow-wrap: break-word">
        <!-- make strong each quoted word -->
        <span v-html="line.replace(/&quot;(.*?)&quot;/g, '&quot;<strong>$1</strong>&quot;')"></span>
      </p>
      <slot></slot>
    </div>
  </div>
</template>

<script>
export default {
  name: 'WarningBox',
  props: { 
    message: { type: String, default: '' },
  },
}
</script>
