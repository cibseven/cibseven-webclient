<template>
  <div tabindex="0" @focus="focused = process" style="flex: auto; margin: 10px" class="row border rounded-lg shadow-sm bg-white" :style="viewStyles[view].cardSize">
    <div class="h-100 container position-relative" @mouseenter="focused = process" @mouseleave="focused = null">
      <transition name="fade">
        <div v-show="focused" class="text-center position-absolute py-3" style="bottom: 0; left: 0; right: 0; z-index: 2"
          :style="focused ? 'background: rgba(255, 255, 255, 0.8)' : ''">
          <b-button v-if="process.suspended !== 'true'" @blur="focused = null" :disabled="process.loading" variant="primary" @click="$emit('start-process', process)">
            <b-spinner class="me-2" v-if="process.loading" small></b-spinner>
            {{ $t('process.start') }}
          </b-button>
        </div>
      </transition>
      <div class="row text-center pt-2 pe-3" :style="viewStyles[view].imgBlock">
        <div class="col-11 p-0 d-flex align-items-center">
          <img :alt="processName" v-b-popover.hover.top="showDescription(process.key)" :style="viewStyles[view].imgSize" @error="onImageLoadFailure($event)" :src="'assets/images/process/' + process.key + '.svg'">
        </div>
        <div class="col-1 p-0 text-end">
          <b-button :title="$t('process.favorite')" tabindex="-1" @click="$emit('favorite', process)" variant="link" class="mdi mdi-24px text-primary p-0" style="z-index: 1"
          :class="process.favorite ? 'mdi-star text-primary' : 'mdi-star-outline text-secondary'"></b-button>
        </div>
      </div>
      <div v-b-popover.hover.top="showDescription(process.key)" :style="viewStyles[view].textBlock" v-block-truncate="{ text: textHtml }">
        <div v-html="textHtml"></div>
      </div>
    </div>
  </div>
</template>

<script>
import { permissionsMixin } from '@/permissions.js'
import processesMixin from '@/components/process/mixins/processesMixin.js'

export default {
mixins: [permissionsMixin, processesMixin],
props: { process: Object },
computed: {
  viewStyles: function() {
    return {
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
  },
  textHtml: function() {
    return '<h5 :title="' + this.processName + '">' + this.processName + '</h5>' +
      '<div>' + this.showDescription(this.process.key) + '</div>'
  }
}
}
</script>
