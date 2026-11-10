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
  <span v-if="image" class="cib-nav-icon-image" :style="[boxStyle, maskStyle]" aria-hidden="true"></span>
  <span v-else :class="['mdi', `mdi-${size}px`, icon]" :style="boxStyle" aria-hidden="true"></span>
</template>

<script>
/**
 * An icon of the navigation (NavCluster, NavMobileMenu): an image file when there is one, else an `mdi-*` font icon.
 * Without either it is an empty box of the font size, which keeps a column of labels lined up.
 */
export default {
  name: 'NavIcon',
  props: {
    icon: { type: String, default: null },
    image: { type: String, default: null },
    // In px, one of MDI's size classes (18, 24, 36, 48)
    size: { type: Number, default: 24 }
  },
  computed: {
    // The box is set here, not left to the glyph: MDI sizes only its ::before, and an empty icon has none
    boxStyle() {
      return { width: `${this.size}px`, height: `${this.size}px` }
    },
    // Quoted: small files are inlined as data URIs, which hold characters an unquoted url() cannot
    maskStyle() {
      return { '--cib-nav-icon': `url("${this.image}")` }
    }
  }
}
</script>

<style scoped>
.cib-nav-icon-image,
.mdi {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
/* The image only gives the shape (mask), the colour is the current one as for the font icons */
.cib-nav-icon-image {
  background-color: currentColor;
  -webkit-mask: var(--cib-nav-icon) center / contain no-repeat;
  mask: var(--cib-nav-icon) center / contain no-repeat;
}
</style>
