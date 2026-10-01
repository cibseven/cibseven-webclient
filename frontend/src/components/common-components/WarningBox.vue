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
        <span><template v-for="(part, i) in quotedParts(line)" :key="i"><strong v-if="part.strong">{{ part.text }}</strong><template v-else>{{ part.text }}</template></template></span>
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
  methods: {
    /**
     * Splits a line into the quoted words and the segments around them, keeping
     * the quotes themselves as text. The segments are rendered as text nodes,
     * so the markup this box emits is only the <strong> element in the
     * template - the message is never turned into HTML.
     */
    quotedParts(line) {
      const parts = []
      // Splitting on a pattern with one capturing group keeps the quoted
      // tokens in the result, at the odd positions.
      line.split(/("[^"]*")/g).forEach((segment, index) => {
        if (index % 2 === 0) {
          if (segment) parts.push({ text: segment, strong: false })
          return
        }
        parts.push({ text: '"', strong: false })
        parts.push({ text: segment.slice(1, -1), strong: true })
        parts.push({ text: '"', strong: false })
      })
      return parts
    },
  },
}
</script>

<style lang="css" scoped>
/* Strip margin from the last paragraph inside the warning box */
div p:last-child {
  margin-bottom: 0;
}
</style>
