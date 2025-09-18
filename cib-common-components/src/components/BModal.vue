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
  <div class="modal fade" ref="modal" tabindex="-1" role="dialog" :aria-labelledby="'modal-title'">
    <div class="modal-dialog" :class="modalClasses" role="document">
      <div class="modal-content" :class="{ 'modal-dialog-scrollable': scrollable }">
        <div class="modal-header" :class="headerClass">
          <h5 class="modal-title" :id="'modal-title'">{{ title }}</h5>
          <button type="button" class="btn-close" @click="hide('headerclose')" :aria-label="$t('bcomponents.ariaLabelClose')"></button>
        </div>
        <div class="modal-body" :class="bodyClass">
          <slot></slot>
        </div>
        <div v-if="!hideFooter" class="modal-footer" :class="footerClass">
          <slot name="modal-footer" :cancel="cancel" :ok="ok">
            <BButton variant="primary" @click="hide('ok')">{{$t('confirm.ok')}}</BButton>
            <BButton v-if="!okOnly" variant="secondary" @click="hide('cancel')">{{$t('confirm.cancel')}}</BButton>
          </slot>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import * as Bootstrap from 'bootstrap'
import BButton from './BButton.vue'

export default {
  components: {
    BButton
  },
  props: {
    okOnly: Boolean,
    title: String,
    size: String,
    scrollable: Boolean,
    fullscreen: Boolean,
    hideFooter: Boolean,
    bodyClass: String,
    headerClass: String,
    footerClass: [String, Object]
  },
  data: function() {
    return {
      lastFocused: null
    }
  },
  mounted: function () {
    this.modal = new Bootstrap.Modal(this.$refs.modal, {
      backdrop: 'static',
      keyboard: false
    })

    if (this.$refs.modal.parentNode !== document.body) {
      document.body.appendChild(this.$refs.modal)
    }

    this.$refs.modal.addEventListener('show.bs.modal', () => { this.$emit('show') })
    this.$refs.modal.addEventListener('shown.bs.modal', () => this.$emit('shown'))
    this.$refs.modal.addEventListener('hide.bs.modal', () => { this.$emit('hide') })
    this.$refs.modal.addEventListener('hidden.bs.modal', () => this.$emit('hidden'))
    this.$refs.modal.addEventListener('hidePrevented.bs.modal', () => this.hide('close'))
  },
  computed: {
    sizeClass: function () {
      return this.size === 'xl' ? 'modal-xl' :
        this.size === 'lg' ? 'modal-lg' :
          this.size === 'sm' ? 'modal-sm' : ''
    },
    modalClasses() {
      return [
  		  this.sizeClass,
	      { 'modal-dialog-scrollable': this.scrollable, 'modal-fullscreen': this.fullscreen }
      ]
    }
  },
  methods: {
    cancel: function () {
      this.hide('cancel')
    },
    ok: function () {
      this.hide('ok')
    },
    hide: function (trigger) {
      this.modal.hide()
      this.$emit('hide', trigger)
      if (this.lastFocused && typeof this.lastFocused.focus === 'function') {
        this.lastFocused.focus();
      }
    },
    show: function () {
      this.lastFocused = document.activeElement
      this.modal.show()
    }
  }
}
</script>

<style scoped>
</style>
