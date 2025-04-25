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
  <div class="row g-0 position-relative h-100 bg-light">
    <transition name="slide-left"> <!-- In a fixed component the margin-top must be applied right in the component -->
      <div v-show="leftOpen" class="position-absolute border-end bg-white" :class="colClasses(leftSize)" style="top: 0; left: 0; bottom: 0; z-index: 0">
        <b-button v-if="leftCaption" ref="leftSidebar" :aria-label="leftCaption + (number ? number + $t('process.tasks') : '')" variant="light" style="min-height: 40px; line-height: 20px;" :block="true" class="rounded-0 border-bottom text-start" @click="$emit('update:leftOpen', false)">
          <slot name="leftIcon">
            <span class="mdi mdi-18px mdi-chevron-left float-end"></span>
          </slot>
          <h5 class="m-0 w-75" style="word-wrap: break-word">{{ leftCaption }}<span v-if="number" class="h5 ps-2"><b-badge pill variant="light">{{ number }}</b-badge></span></h5>
        </b-button>
        <div :style="{ height: leftCaption ? 'calc(100% - ' + headerCalc + ')' : '100%' }">
          <slot name="left"></slot>
        </div>
      </div>
    </transition>

    <div class="sidebars-middle h-100 position-relative" :class="middleClasses"
      :style="{ 'padding-left': leftCaption && !leftOpen ? '40px' : '0', 'padding-right': rightCaption && !rightOpen ? '40px' : '0' }">
      <slot></slot>
    </div>
    <transition name="fade"> <!-- In a fixed component the margin-top must be applied right in the component -->
      <slot name="filter">
        <b-button v-if="!leftOpen && leftCaption" variant="light" class="rounded-0 border border-end-0 text-nowrap position-absolute w-auto" @click="$emit('update:leftOpen', true)"
          style="right: 100%; transform: rotate(-90deg); transform-origin: right top">
          {{ leftCaption }} <span v-if="number"><b-badge pill variant="light">{{ number }}</b-badge></span>
          <i class="mdi mdi-18px mdi-chevron-down"></i>
        </b-button>
      </slot>
    </transition>

    <transition name="slide-right"> <!-- In a fixed component the margin-top must be applied right in the component -->
      <div v-show="rightOpen" class="border position-absolute border-top-0" :class="colClasses(rightSize)" style="top: 0; right: 0; bottom: 0">
        <b-button v-if="rightCaption" variant="light" style="min-height: 40px; line-height: 20px;" :block="true" class="rounded-0 border-bottom border-end" @click="$emit('update:rightOpen', false)">
          <span class="mdi mdi-18px mdi-chevron-right float-start"></span>
          <h5 class="m-0 w-100">{{ rightCaption }}</h5>
        </b-button>
        <div :style="{ height: rightCaption ? 'calc(100% - ' + headerCalc + ')' : '100%' }">
          <slot name="right"></slot>
        </div>
      </div>
    </transition>
    <transition name="fade"> <!-- In a fixed component the margin-top must be applied right in the component -->
      <b-button v-if="!rightOpen && rightCaption" variant="light" class="rounded-0 border border-start-0 text-nowrap position-absolute w-auto" @click="$emit('update:rightOpen', true)"
        style="left: 100%; transform: rotate(90deg); transform-origin: left top">
        <i class="mdi mdi-18px mdi-chevron-down"></i>
        {{ rightCaption }}
      </b-button>
    </transition>
  </div>
</template>

<script>
export default {
  name: 'SidebarsFlow',
  props: { leftOpen: Boolean, rightOpen: Boolean, leftCaption: String, rightCaption: String,
    leftSize: { type: Array, default: function() { return [12, 6, 4, 3, 3] } },
    rightSize: { type: Array, default: function() { return [12, 6, 4, 2, 2] } },
    number: Number, headerCalc: { type: String, default: '40px' }
  },
  computed: {
    middleClasses: function() {
      if (this.leftOpen) {
        var middleSize = this.leftSize.map((val, i) => { return 12 - val - (this.rightOpen ? this.rightSize[i] : 0) })
        var offset = this.leftSize.map((size, i) => { return 'offset-' + this.breakpoints[i] + size }).join(' ')
        return this.colClasses(middleSize) + ' ' + offset
      } else if (this.rightOpen) return this.colClasses(this.rightSize.map(val => { return 12 - val }))
      else return 'col-12'
    },
    breakpoints: function() {
      return ['', 'sm-', 'md-', 'lg-', 'xl-']
    }
  },
  methods: { // https://help.optimizely.com/Build_Campaigns_and_Experiments/Use_screen_measurements_to_design_for_responsive_breakpoints
    showMain: function(keepRight) {
      if (window.innerWidth < 576) { // sm breakpoint
        this.$emit('update:leftOpen', false)
        this.$emit('update:rightOpen', false)
      } else if (window.innerWidth < 768) { // md breakpoint
        if (this.rightOpen && keepRight) this.$emit('update:leftOpen', false)
        else if (this.leftOpen && !keepRight) this.$emit('update:rightOpen', false)
      }
    },
    showRight: function() {
      this.$emit('update:rightOpen', true)
      if (window.innerWidth < 768) this.$emit('update:leftOpen', false)
    },
    colClasses: function(sizes) {
      return sizes.map((size, i) => {
        if (!size) return 'd-none'
        else if (size && i > 0 && !sizes[i-1]) return 'd-' + this.breakpoints[i] + 'block col-' + this.breakpoints[i] + size
        else return 'col-' + this.breakpoints[i] + size
      }).join(' ')
    }
  }
}
</script>
