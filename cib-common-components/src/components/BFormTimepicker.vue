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
  <div class="timepicker-container position-relative" @click.stop role="group" aria-labelledby="timepicker-label">
    <label id="timepicker-label" class="visually-hidden">{{ $t('bcomponents.timepicker.label') }}</label>
    <div class="input-group" :class="inputGroupClassComputed">
      <slot name="prepend"></slot>
      <input type="text" :value="formattedTime" @focus="openPicker" class="form-control" :class="inputClassComputed"
        readonly :aria-label="$t('bcomponents.timepicker.selectedTime')" :placeholder="placeholderText"/>
      <slot name="append"></slot>
    </div>
    <div v-if="showPicker" class="timepicker-dropdown bg-white border rounded p-2 position-absolute shadow-sm w-50 z-1"
      role="dialog" aria-modal="true" aria-label="Time picker">
      <div class="d-flex justify-content-around mb-2">
        <div class="time-control">
          <button class="btn btn-light btn-sm w-100" @click="incrementHour" :aria-label="$t('bcomponents.timepicker.incrementHour')">
            ▲
          </button>
          <input type="text" :value="displayHour" class="form-control text-center my-1" readonly
            :aria-label="$t('bcomponents.timepicker.hour')">
          <button class="btn btn-light btn-sm w-100" @click="decrementHour" :aria-label="$t('bcomponents.timepicker.decrementHour')">
            ▼
          </button>
        </div>
        <span class="mx-2 align-self-center" aria-hidden="true">:</span>
        <div class="time-control">
          <button class="btn btn-light btn-sm w-100" @click="incrementMinute" :aria-label="$t('bcomponents.timepicker.incrementMinute')">
            ▲
          </button>
          <input type="text" :value="displayMinute" class="form-control text-center my-1" readonly
            :aria-label="$t('bcomponents.timepicker.minute')">
          <button class="btn btn-light btn-sm w-100" @click="decrementMinute" :aria-label="$t('bcomponents.timepicker.decrementMinute')">
            ▼
          </button>
        </div>
      </div>
      <button class="btn btn-outline-danger btn-sm w-100 mt-2" @click="clearSelection" :aria-label="$t('bcomponents.timepicker.clearSelection')">
        {{ $t('bcomponents.timepicker.clearSelection') }}
      </button>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    modelValue: { type: String, default: null },
    value: { type: String, default: null },
    size: { type: String, default: null },
    inputClass: { type: String, default: null },
    inputGroupClass: { type: String, default: null },
  },
  data: function () {
    return {
      hour: null,
      minute: null,
      showPicker: false
    }
  },
  computed: {
    placeholderText: function () {
      return this.modelValue === null ? this.$t('bcomponents.timepicker.noTimeSelected') : ''
    },
    formattedTime: function () {
      return this.modelValue === null ? '' : `${this.paddedHour}:${this.paddedMinute}`
    },
    paddedHour: function () {
      return this.hour !== null ? String(this.hour).padStart(2, '0') : '--'
    },
    paddedMinute: function () {
      return this.minute !== null ? String(this.minute).padStart(2, '0') : '--'
    },
    displayHour: function () {
      return this.hour !== null ? this.paddedHour : '--'
    },
    displayMinute: function () {
      return this.minute !== null ? this.paddedMinute : '--'
    },
    inputClassComputed: function () {
      return [this.inputClass, this.size ? `form-control-${this.size}` : '']
        .filter(Boolean)
        .join(' ')
    },
    inputGroupClassComputed: function () {
      return [this.inputGroupClass, this.size ? `input-group-${this.size}` : '']
        .filter(Boolean)
        .join(' ')
    }
  },
  watch: {
    modelValue: {
      immediate: true,
      handler(newValue) {
        if (newValue) {
          const [hour, minute] = newValue.split(':').map(Number)
          this.hour = hour || 0
          this.minute = minute || 0
        } else {
          this.hour = null
          this.minute = null
        }
      }
    }
  },
  methods: {
    openPicker: function () {
      this.showPicker = true
      this.$eventBus.emit('timepicker-opened', this)
      document.addEventListener('click', this.handleDocumentClick)
      document.addEventListener('keydown', this.handleEscapeKey)
    },
    handleEscapeKey: function (event) {
      if (event.key === 'Escape') {
        this.closePicker()
      }
    },
    closePicker: function () {
      this.showPicker = false
      document.removeEventListener('click', this.handleDocumentClick)
      document.removeEventListener('keydown', this.handleEscapeKey)
    },
    handleDocumentClick: function (event) {
      if (this.showPicker && !this.$el.contains(event.target)) {
        this.closePicker()
      }
    },
    incrementHour: function () {
      if (this.hour === null) this.hour = 0
      this.hour = (this.hour + 1) % 24
      this.updateModel()
    },
    decrementHour: function () {
      if (this.hour === null) this.hour = 23
      this.hour = (this.hour - 1 + 24) % 24
      this.updateModel()
    },
    incrementMinute: function () {
      if (this.minute === null) this.minute = 0
      this.minute = (this.minute + 1) % 60
      this.updateModel()
    },
    decrementMinute: function () {
      if (this.minute === null) this.minute = 59
      this.minute = (this.minute - 1 + 60) % 60
      this.updateModel()
    },
    updateModel: function () {
      if (this.hour !== null && this.minute !== null) {
        const time = `${String(this.hour).padStart(2, '0')}:${String(this.minute).padStart(2, '0')}`
        this.$emit('update:modelValue', time)
        this.$emit('input', time)
      }
    },
    clearSelection: function () {
      if (this.modelValue === null) {
        this.hour = null
        this.minute = null
      } else {
        this.hour = 0
        this.minute = 0
        this.updateModel()
      }
    }
  },
  created: function () {
    this.$eventBus.on('timepicker-opened', (openedPicker) => {
      if (openedPicker !== this) {
        this.closePicker()
      }
    })
  },
  beforeUnmount: function () {
    this.$eventBus.off('timepicker-opened')
    document.removeEventListener('click', this.handleDocumentClick)
  }
}
</script>

<style scoped>
</style>
