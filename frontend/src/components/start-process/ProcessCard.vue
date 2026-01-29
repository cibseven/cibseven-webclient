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
  <div style="flex: auto; margin: 10px" class="row border rounded-lg shadow-sm bg-white" :style="viewStyles[view].cardSize" v-b-popover.hover.top="getTooltipHtml()">
    <button class="h-100 container position-relative btn"
      tabindex="0"
      @focus="focused = process"
      @mouseenter="focused = process" @focusin="focused = process"
      @mouseleave="focused = null" @focusout="focused = null">

      <transition name="fade">
        <div v-show="focused" class="text-center position-absolute py-3" style="bottom: 0; left: 0; right: 0; z-index: 2"
          :style="focused ? 'background: rgba(255, 255, 255, 0.8)' : ''">
          <b-button v-if="process.suspended !== 'true'" @blur="focused = null" :disabled="process.loading" variant="primary" @click="$emit('start-process', process)">
            <b-spinner class="me-2" v-if="process.loading" small></b-spinner>
            {{ $t('process.start') }}
          </b-button>
        </div>
      </transition>

      <template v-if="view === 'image-outline'">
        <div :style="viewStyles[view].textBlock" class="row pt-2 pe-3 align-items-center">
          <h5 class="m-0 align-items-center col-11 text-truncate pe-0" :title="processName">
            {{ processName }}
          </h5>
          <div class="col-1 p-0">
            <b-button :title="$t('process.favorite')" tabindex="-1" @click="$emit('favorite', process)" variant="link" class="mdi mdi-24px text-primary p-0" style="z-index: 1"
            :class="process.favorite ? 'mdi-star text-primary' : 'mdi-star-outline text-secondary'"></b-button>
          </div>
        </div>

        <div v-if="process.tenantId" class="mt-1 position-absolute fst-italic" :title="$t('process.tenant') + ': ' + process.tenantId">{{ process.tenantId }}</div>
        <div class="row text-center pt-2" :style="viewStyles[view].imgBlock">
          <div class="w-100">
            <img v-if="!imageLoadFailed" :alt="processName" :style="viewStyles[view].imgSize" @error="onImageLoadFailure()" :src="'assets/images/process/' + process.key + '.svg'">
            <img v-else :alt="processName" :style="viewStyles[view].imgSize" src="@/assets/images/process/default.svg">
          </div>
        </div>
      </template>

      <template v-else>
        <div class="row text-center pt-2 pe-3" :style="viewStyles[view].imgBlock">
          <div class="col-11 p-0 d-flex align-items-center">
            <img v-if="!imageLoadFailed" :alt="processName" :style="viewStyles[view].imgSize" @error="onImageLoadFailure()" :src="'assets/images/process/' + process.key + '.svg'">
            <img v-else :alt="processName" :style="viewStyles[view].imgSize" src="@/assets/images/process/default.svg">
          </div>
          <div class="col-1 p-0 text-end">
            <b-button :title="$t('process.favorite')" tabindex="-1" @click="$emit('favorite', process)" variant="link" class="mdi mdi-24px text-primary p-0" style="z-index: 1"
            :class="process.favorite ? 'mdi-star text-primary' : 'mdi-star-outline text-secondary'"></b-button>
          </div>
        </div>

        <div :style="viewStyles[view].textBlock">
          <div v-if="process.tenantId" class="fst-italic mb-2" :title="$t('process.tenant') + ': ' + process.tenantId">{{ process.tenantId }}</div>
          <h5 class="text-truncate" :title="processName">{{ processName }}</h5>
          <div v-html="getDescription(this.process)" class="inline-description"></div>
        </div>
      </template>

    </button>
  </div>
</template>

<script>
export default {
  name: 'ProcessCard',
  emits: ['favorite', 'start-process'],
  props: {
    process: Object,
    view: String,
  },
  data() {
    return {
      focused: null,
      imageLoadFailed: false
    }
  },
  computed: {
    processName() {
      return this.process?.name || this.process?.key
    },    
    viewStyles() {
      return {
        'image-outline': {
          cardSize: 'width: 290px; height: 160px',
          imgSize: 'height: 100px',
          imgBlock: 'height: 110px',
          textBlock: 'height: 45px'
        },
        'view-comfy': {
          cardSize: 'width: 280px; height: 250px',
          imgSize: 'width: 140px; height: 60px',
          imgBlock: 'height: 70px',
          textBlock: 'height: 150px'
        },
        'view-module': {
          cardSize: 'width: 400px; height: 250px',
          imgSize: 'width: 140px; height: 60px',
          imgBlock: 'height: 70px',
          textBlock: 'height: 150px'
        }
      }
    }
  },
  methods: {
    onImageLoadFailure() {
      this.imageLoadFailed = true
    },
    getDescription(process) {
      if (this.$te('process-descriptions.' + process.key)) return this.$t('process-descriptions.' + process.key)
      return process.description || ''
    },
    getTooltipHtml() {
      const description = this.getDescription(this.process)
      if (description) {
        return `<strong>${this.processName}</strong><br>${description}`
      } else if (this.processName.length > 35) {
        return `<strong>${this.processName}</strong>`
      }
      else {
        return undefined
      }
    }
  }
}
</script>

<style lang="css" scoped>
.inline-description {
  display: -webkit-box;
  line-clamp: 5;
  -webkit-line-clamp: 5;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
