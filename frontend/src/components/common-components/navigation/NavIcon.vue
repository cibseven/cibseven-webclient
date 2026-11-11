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
  <span v-if="image" class="cib-nav-icon cib-nav-icon-image" :style="[boxStyle, maskStyle]" aria-hidden="true"></span>
  <span v-else :class="['cib-nav-icon', 'mdi', `mdi-${size}px`, icon]" :style="boxStyle" aria-hidden="true"></span>
</template>

<script>
/**
 * An icon of the navigation (NavCluster, NavMobileMenu): an image file when there is one, else an `mdi-*` font icon.
 * Without either it is an empty box of the font size, which keeps a column of labels lined up.
 */
export default {
  name: 'NavIcon',
  emits: [],
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
/* On its own class rather than MDI's, which stays untouched: the glyph centred in the box */
.cib-nav-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
/* The image only gives the shape (mask), the colour is the current one as for the font icons. Forced colours
   (Windows High Contrast) would paint the background in the page's colour and hide the icon: preserve-parent-color
   keeps the background and takes the forced text colour of the link or button around it, as the font icons do.
   Browsers without it fall back to none, which keeps the icon visible in its normal colour */
.cib-nav-icon-image {
  forced-color-adjust: none;
  forced-color-adjust: preserve-parent-color;
  background-color: currentColor;
  -webkit-mask: var(--cib-nav-icon) center / contain no-repeat;
  mask: var(--cib-nav-icon) center / contain no-repeat;
}
/* preserve-parent-color only takes an inherited colour: one set on the icon itself (e.g. text-secondary, which is
   !important) would stay as it is, so in forced colours the icon inherits like the font icons are forced to */
@media (forced-colors: active) {
  .cib-nav-icon-image {
    color: inherit !important;
  }
}
</style>
