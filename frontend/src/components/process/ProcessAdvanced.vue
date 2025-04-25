<template>
  <div tabindex="0" @focus="focused = process" style="flex: auto; margin: 10px" class="row border rounded-lg shadow-sm bg-white" :style="viewStyles[view].cardSize"
    v-b-popover.hover.top="showDescription(process.key)">
    <div class="h-100 container position-relative" @mouseenter="focused = true" @mouseleave="focused = null">
      <transition name="fade">
        <div v-show="focused" class="text-center position-absolute py-3" style="bottom: 0; left:0; right: 0; z-index: 2"
          :style="focused ? 'background: rgba(255, 255, 255, 0.8)' : ''">
          <b-button v-if="process.suspended !== 'true'" @blur="focused = null" :disabled="process.loading" variant="primary" @click="$emit('start-process', process)">
            <b-spinner class="me-2" v-if="process.loading" small></b-spinner>
            {{ $t('process.start') }}
          </b-button>
        </div>
      </transition>
      <div :style="viewStyles[view].textBlock" class="row pt-2 pe-3 align-items-center">
        <h5 class="m-0 align-items-center d-flex col-11" :style="viewStyles[view].textBlock"
          style="word-wrap: anywhere" :title="processName">{{ shortProcessName(processName) }}</h5>
        <div class="col-1 p-0">
          <b-button :title="$t('process.favorite')" tabindex="-1" @click="$emit('favorite', process)" variant="link" class="mdi mdi-24px text-primary p-0" style="z-index: 1"
          :class="process.favorite ? 'mdi-star text-primary' : 'mdi-star-outline text-secondary'"></b-button>
        </div>
      </div>
      <div v-if="process.tenantId" class="mt-1 position-absolute fst-italic">{{ process.tenantId }}</div>
      <div class="row text-center pt-2" :style="viewStyles[view].imgBlock">
        <div class="w-100">
          <img :alt="processName" :style="viewStyles[view].imgSize" @error="onImageLoadFailure($event)" :src="'assets/images/process/' + process.key + '.svg'">
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { permissionsMixin } from '@/permissions.js'
import processesMixin from '@/components/process/mixins/processesMixin.js'

export default {
  name: 'ProcessAdvanced',
  mixins: [permissionsMixin, processesMixin],
  props: { process: Object },
  computed: {
    viewStyles: function() {
      return {
        'image-outline': {
          cardSize: 'width: 290px; height: 160px',
          imgSize: 'height: 100px',
          imgBlock: 'height: 110px',
          textBlock: 'height: 45px'
        }
      }
    }
  },
  methods: {
    shortProcessName: function(processName) {
      if (processName) {
        if (processName.length > 35) return processName.substring(0, 32).trim().concat('...')
        return processName
      }
    }
  }
}
</script>
