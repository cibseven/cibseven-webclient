<template>
  <div class="row g-0 position-relative">
    <transition name="slide-left"> <!-- In a fixed component the margin-top must be applied right in the component -->
      <div v-show="leftOpen" class="border border-top-0" :class="colClasses(leftSize)" style="position: absolute; top: 0; left: 0; bottom: 0; z-index: 0">
        <b-button v-if="leftCaption" variant="light" :block="true" class="rounded-0 border border-top-0 border-end-0" @click="$emit('update:leftOpen', false)">
          <span class="mdi mdi-chevron-left float-end"></span>
          {{ leftCaption }}
        </b-button>
        <div :style="{ height: leftCaption ? 'calc(100% - 36px)' : '100%' }">
          <slot name="left"></slot>
        </div>
      </div>
    </transition>

    <div class="sidebars-middle h-100" :class="middleClasses"
      :style="{ 'padding-left': leftCaption && !leftOpen ? '36px' : '0', 'padding-right': rightCaption && !rightOpen ? '36px' : '0' }">
      <slot></slot>
    </div>
    <transition name="fade"> <!-- In a fixed component the margin-top must be applied right in the component -->
      <b-button v-if="!leftOpen && leftCaption" variant="light" class="rounded-0 border border-end-0 text-nowrap w-auto" @click="$emit('update:leftOpen', true)"
        style="position: absolute; right: 100%; transform: rotate(-90deg); transform-origin: right top">
        {{ leftCaption }}
        <i class="mdi mdi-chevron-down"></i>
      </b-button>
    </transition>

    <transition name="slide-right"> <!-- In a fixed component the margin-top must be applied right in the component -->
      <div v-show="rightOpen" class="border border-top-0" :class="colClasses(rightSize)" style="position: absolute; top: 0; right: 0; bottom: 0">
        <b-button v-if="rightCaption" variant="light" :block="true" class="rounded-0 border border-top-0 border-start-0" @click="$emit('update:rightOpen', false)">
          <span class="mdi mdi-chevron-right float-start"></span>
          {{ rightCaption }}
        </b-button>
        <div :style="{ height: rightCaption ? 'calc(100% - 36px)' : '100%' }">
          <slot name="right"></slot>
        </div>
      </div>
    </transition>
    <transition name="fade"> <!-- In a fixed component the margin-top must be applied right in the component -->
      <b-button v-if="!rightOpen && rightCaption" variant="light" class="rounded-0 border border-start-0 text-nowrap" @click="$emit('update:rightOpen', true)"
        style="position: absolute; left: 100%; transform: rotate(90deg); transform-origin: left top">
        <i class="mdi mdi-chevron-down"></i>
        {{ rightCaption }}
      </b-button>
    </transition>
  </div>
</template>

<script>
const breakpoints = ['', 'sm-', 'md-', 'lg-', 'xl-']

export default {
  name: 'Sidebars',
  props: { leftOpen: Boolean, rightOpen: Boolean, leftCaption: String, rightCaption: String,
    leftSize: { type: Array, default: function() { return [12, 6, 4, 3, 3] } },
    rightSize: { type: Array, default: function() { return [12, 6, 4, 3, 3] } }
  },
  computed: {
    middleClasses: function() {
      if (this.leftOpen) {
        var middleSize = this.leftSize.map(function(val, i) { return 12 - val - (this.rightOpen ? this.rightSize[i] : 0) }.bind(this))
        var offset = this.leftSize.map(function(size, i) { return 'offset-' + breakpoints[i] + size }).join(' ')
        return this.colClasses(middleSize) + ' ' + offset
      } else if (this.rightOpen) return this.colClasses(this.rightSize.map(function(val) { return 12 - val }))
      else return 'col-12'
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
      return sizes.map(function(size, i) {
        if (!size) return 'd-none'
        else if (size && i > 0 && !sizes[i-1]) return 'd-' + breakpoints[i] + 'block col-' + breakpoints[i] + size
        else return 'col-' + breakpoints[i] + size
      }).join(' ')
    }
  }
}
</script>
