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
  <div @mousedown="resizerMixin.handleMouseDown"
    class="v-resizable position-absolute w-100" style="left: 0"
    :style="'height: ' + resizerMixin.bpmnViewerHeight + 'px; ' + resizerMixin.toggleTransition">

    <slot></slot>

    <b-button size="sm" variant="light" class="me-1 position-absolute" style="bottom: 90px; right: 11px;"
      @click="resizerMixin.toggleContent"
      :title="resizerMixin.toggleIcon === 'mdi-chevron-up' ? $t('task.collapse') : $t('task.expand')">
      <span class="mdi mdi-18px" :class="resizerMixin.toggleIcon"></span>
    </b-button>

  </div>
</template>

<script>
export default {
  name: 'ViewerFrame',
  props: {
    resizerMixin: {
      type: Object,
      required: true,
    }
  },
}
</script>
