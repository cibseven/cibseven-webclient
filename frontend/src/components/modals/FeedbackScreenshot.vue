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
  <div @focusin="focused = true" @focusout="focused = false" @paste="screenShoot" style="position: relative; border-radius: 20px"
    :style="{ border: focused && !disabled ? '2px solid var(--primary)' : '2px solid var(--gray)',
      'background-image': imgSrc ? 'url(' + imgSrc + ')' : 'none', 'background-size': 'cover' }">

    <div style="opacity: 0; width: 100%; height: 100%; position: absolute"
      :style="focused && !disabled ? { 'background-color': 'var(--primary)', opacity: 0.05 } : null"></div>

    <div style="text-align: center"
      :style="{ 'text-decoration': focused && !disabled ? 'underline' : null, visibility: imgSrc && 'hidden', 'margin-top': (height || 43)/4 + 'px' }">
      {{ $t('snapshot.titleAbove') }}
    </div>

    <img :src="img || '@/assets/images/common-layout/files/screenshot.svg'" style="display: block; margin-left: auto; margin-right: auto"
      :style="{ visibility: imgSrc && 'hidden', height: (height || 43) + 'px' }">

    <div style="text-align: center"
      :style="{ 'text-decoration': focused && !disabled ? 'underline' : null, visibility: imgSrc && 'hidden', 'margin-bottom': (height || 43)/4 + 'px' }">
      {{ $t('snapshot.titleBelow') }}
    </div>
  </div>
</template>

<script>
export default {
  name: 'FeedbackScreenshot',
  props: { disabled: Boolean, img: String, height: Number },
  data: function() { return { focused: false, imgSrc: null } },
  methods: {
    screenShoot: function(evt) {
      var reader = new FileReader()
      reader.onload = function() { this.imgSrc = reader.result }.bind(this)
      for (var i = 0; i < evt.clipboardData.items.length; i++) {
        var item = evt.clipboardData.items[i];
        if (item.type.startsWith('image/')) {
          reader.readAsDataURL(item.getAsFile())
          this.$emit('update:modelValue', item.getAsFile())
        }
      }
    }
  }
}
</script>
