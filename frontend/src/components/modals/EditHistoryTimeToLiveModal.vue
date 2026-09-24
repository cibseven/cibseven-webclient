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
  <b-modal
    ref="modal"
    :title="$t('historyTimeToLive.title')"
    hide-header-close
    no-close-on-backdrop
    size="md"
    @hidden="resetTtlDialog">

    <div class="mb-3">
      <p class="mb-3">
        {{ descriptionText }}
      </p>

      <div class="form-check mb-2">
        <input
          class="form-check-input"
          type="radio"
          name="historyTimeToLiveChoice"
          id="historyTimeToLiveChoiceDefault"
          value="default"
          v-model="choice"
          :disabled="defaultChoiceDisabled"
          @change="onChoiceChanged">
        <label class="form-check-label" for="historyTimeToLiveChoiceDefault" :title="defaultChoiceDisabled ? $t('historyTimeToLive.enforcedTooltip') : null">
          {{ defaultChoiceLabel }}
        </label>
      </div>

      <div class="form-check mb-2">
        <input
          class="form-check-input"
          type="radio"
          name="historyTimeToLiveChoice"
          id="historyTimeToLiveChoiceZero"
          value="zero"
          v-model="choice"
          @change="onChoiceChanged">
        <label class="form-check-label" for="historyTimeToLiveChoiceZero">
          {{ $t('historyTimeToLive.choiceZero') }}
        </label>
      </div>

      <div class="form-check mb-2">
        <input
          class="form-check-input"
          type="radio"
          name="historyTimeToLiveChoice"
          id="historyTimeToLiveChoiceCustom"
          value="custom"
          v-model="choice"
          @change="onChoiceChanged">
        <label class="form-check-label" for="historyTimeToLiveChoiceCustom">
          {{ $t('historyTimeToLive.choiceCustom') }}
        </label>
      </div>

      <div class="input-group mt-1" style="max-width: 220px; padding-left: 20px;">
        <b-form-input
          v-model="editTtlValue"
          id="ttlValue"
          type="number"
          min="0"
          max="9999"
          :placeholder="$t('historyTimeToLive.placeholder')"
          :state="ttlValidationState"
          :disabled="choice !== 'custom'"
          @input="validateTtl($event)">
        </b-form-input>
        <span class="input-group-text">{{ $t('historyTimeToLive.days') }}</span>
      </div>

      <div v-if="ttlValidationState === false" class="text-danger small mt-1">
        {{ $t('historyTimeToLive.validation') }}
      </div>
    </div>

    <template #modal-footer>
      <b-button variant="light" @click="$refs.modal.hide()">
        {{ $t('confirm.cancel') }}
      </b-button>
      <b-button variant="primary" @click="saveTtlValue">
        {{ $t('commons.save') }}
      </b-button>
    </template>
  </b-modal>
</template>

<script>
export default {
  name: 'EditHistoryTimeToLiveModal',
  emits: ['ttl-updated'],
  props: {
    descriptionText: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      selectedItem: null,
      choice: 'default',
      editTtlValue: null,
      ttlValidationState: null
    }
  },
  computed: {
    // A real engine config always reports enforceHistoryTimeToLive as a concrete boolean;
    // it only ever comes back null/undefined when the deployment can't be determined
    // (see EngineProvider's atomic-pair normalization). Only a known `true` disables the
    // option - an unknown value is left selectable and a rejected save surfaces normally.
    defaultChoiceDisabled() {
      return this.$root.config.enforceHistoryTimeToLive === true
    },
    defaultChoiceLabel() {
      const systemDefault = this.$root.config.historyTimeToLive
      return systemDefault
        ? this.$t('historyTimeToLive.choiceDefault', { value: systemDefault })
        : this.$t('historyTimeToLive.choiceDefaultUnknown')
    }
  },
  methods: {
    // Parent calls show(item) via ref to open the dialog
    show(item) {
      this.selectedItem = item
      if (item.historyTimeToLive === null || item.historyTimeToLive === undefined) {
        this.choice = 'default'
        this.editTtlValue = null
      } else if (item.historyTimeToLive === 0) {
        this.choice = 'zero'
        this.editTtlValue = null
      } else {
        this.choice = 'custom'
        this.editTtlValue = item.historyTimeToLive
      }
      this.ttlValidationState = null

      this.$nextTick(() => {
        if (this.$refs.modal) {
          this.$refs.modal.show()
        }
      })
    },

    resetTtlDialog() {
      this.selectedItem = null
      this.choice = 'default'
      this.editTtlValue = null
      this.ttlValidationState = null
    },

    onChoiceChanged() {
      if (this.choice === 'custom') {
        if (!this.editTtlValue) {
          this.editTtlValue = this.selectedItem?.historyTimeToLive || 30
        }
        this.ttlValidationState = null
        this.$nextTick(() => {
          this.validateTtl()
        })
      } else {
        this.ttlValidationState = true
      }
    },

    validateTtl(event = null) {
      if (this.choice !== 'custom') {
        this.ttlValidationState = true
        return true
      }

      // Get value directly from input event or fall back to editTtlValue
      const inputValue = event ? event.target.value : this.editTtlValue
      const value = Number.parseInt(inputValue)

      if (Number.isNaN(value) || value < 0 || value > 9999) {
        this.ttlValidationState = false
        return false
      }

      this.ttlValidationState = true
      return true
    },

    async saveTtlValue() {
      if (!this.validateTtl()) {
        return
      }

      // 'default' -> null (unlimited retention to the engine unless it falls back to a
      // configured system default at cleanup time), 'zero' -> 0 (immediate cleanup
      // eligibility - the opposite of 'default', never conflate the two), 'custom' -> N.
      let ttlValue
      let ttlDisplay
      if (this.choice === 'default') {
        ttlValue = null
        ttlDisplay = this.defaultChoiceLabel
      } else if (this.choice === 'zero') {
        ttlValue = 0
        ttlDisplay = '0'
      } else {
        ttlValue = Number.parseInt(this.editTtlValue)
        ttlDisplay = `${ttlValue} ${this.$t('historyTimeToLive.days')}`
      }

      // Emit event with TTL data - let parent handle the actual API call and success handling
      this.$emit('ttl-updated', {
        item: this.selectedItem,
        ttlValue,
        ttlDisplay
      })

      // Close the modal
      this.$nextTick(() => {
        if (this.$refs.modal) {
          this.$refs.modal.hide()
        }
      })
    }
  }
}
</script>
