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
  <b-alert dismissible fade variant="success" :show="countdown" @dismissed="countdown = 0" class="d-flex"
    style="position: fixed; z-index: 1021; left: 50%; transform: translate(-50%, 0)" :style="'top: ' + top">
    <!-- .input-group-append .btn, .input-group-prepend .btn has z-index: 2 -->

    <div class="container-fluid">
      <div class="d-flex align-items-center">
        <div class="me-4">
          <span class="mdi-36px mdi mdi-check-circle-outline text-success me-4"></span>
        </div>
        <div class="me-3">
          <slot></slot>
        </div>
      </div>
    </div>
  </b-alert>
</template>

<script>
export default {
  name: 'SuccessAlert',
  props: { top: { type: String, default: '60px' } },
  data: function() { return { countdown: 0 } },
  methods: {
    show: function(time) { this.countdown = time || 5 }
  }
}
</script>
