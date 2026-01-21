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
  <router-link :to="to" variant="link"
    class="btn btn-link col-4 mt-5 mx-2 mx-md-3 bg-white rounded border shadow-sm py-0 text-start" style="max-width: 330px; min-width: 250px; height:250px" tabindex="0"
    @focus="showOptions = true" @blur="showOptions = true" @click="showOptions = true" @mouseover="showOptions = true" @mouseleave="showOptions = false">
    <div class="row h-100">
      <div class="align-top" style="flex:auto">
        <div class="h-100 text-decoration-none text-reset">
          <div class="container">
            <div class="row ps-3" style="height:55px">
              <div class="col-12 align-items-center d-flex">
                <span class="border-start h-100 me-3 border-primary" style="border-width: 3px !important"></span>
                <h4 class="m-0">{{ title }}</h4>
              </div>
            </div>
            <div class="row text-center">
              <div class="col-12 p-0 pt-1">
                <img :alt="title" :src="src" style="height:180px;">
              </div>
            </div>
          </div>
        </div>
      </div>
      <b-overlay v-if="options" :show="showOptions" :opacity="0" no-center no-wrap>
        <template #overlay>
          <b-list-group class="py-2 bg-white rounded-bottom" style="opacity: .9; position: absolute; bottom: 1px; width: calc(100% - 2px); margin-left: 1px" @click.stop>
            <b-list-group-item v-for="option in options" :key="option.to" :to="option.to" class="py-1 px-3 border-start-0 border-top-0 border-end-0 h6 fw-normal mb-0" :title="option.tooltip">
              <span :class="['pe-1 mdi mdi-18px', option.icon]"></span>{{ option.title }}
            </b-list-group-item>
          </b-list-group>
        </template>
      </b-overlay>
    </div>
  </router-link>
</template>

<script>
export default {
  name: "StartViewItem",
  props: {
    to: {
      type: Object,
      required: true,
    },
    title: {
      type: String,
      required: true,
    },
    src: {
      type: String,
      required: true,
    },
    options: {
      type: Array,
      default: null,
      required: false,
    },
  },
  data() {
    return {
      showOptions: false,
    }
  }
}
</script>

<style scoped>
.btn-link,
.btn-link * {
  text-decoration: none !important;
}
.btn:focus {
  outline: 2px solid var(--bs-dark);
  outline-offset: 0px;
}
.btn:active {
  outline: 2px solid var(--bs-gray-600);
  outline-offset: 0px;
}

.list-group-item:focus {
  outline: 2px solid var(--bs-dark);
  outline-offset: 0px;
}
.list-group-item:active {
  outline: 0px solid var(--bs-gray-600);
  outline-offset: 0px;
}

</style>
