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
      <p class="mb-3" v-if="descriptionItems.length > 0">
        <template v-for="(item, index) in descriptionItems" :key="item.title">
          <br v-if="index > 0">{{ item.title }}: <strong>{{ item.value }}</strong>
        </template>
      </p>

      <div class="form-check mb-2">
        <input
          class="form-check-input"
          type="radio"
          name="historyTimeToLiveChoice"
          id="historyTimeToLiveChoiceDefault"
          value="default"
          v-model="choice"
          :disabled="defaultChoiceDisabled">
        <label class="form-check-label" for="historyTimeToLiveChoiceDefault" :title="defaultChoiceDisabled ? $t('historyTimeToLive.enforcedTooltip') : null">
          {{ captionValue(defaultChoiceLabel) }}
          <span class="text-muted">{{ captionHint(defaultChoiceLabel) }}</span>
        </label>
      </div>

      <div class="form-check mb-2">
        <input
          class="form-check-input"
          type="radio"
          name="historyTimeToLiveChoice"
          id="historyTimeToLiveChoiceZero"
          value="zero"
          v-model="choice">
        <label class="form-check-label" for="historyTimeToLiveChoiceZero">
          {{ captionValue($t('historyTimeToLive.choiceZero')) }}
          <span class="text-muted">{{ captionHint($t('historyTimeToLive.choiceZero')) }}</span>
        </label>
      </div>

      <div class="form-check mb-2">
        <input
          class="form-check-input"
          type="radio"
          name="historyTimeToLiveChoice"
          id="historyTimeToLiveChoiceCustom"
          value="custom"
          v-model="choice">
        <label class="form-check-label" for="historyTimeToLiveChoiceCustom">
          {{ captionValue($t('historyTimeToLive.choiceCustom')) }}
          <span class="text-muted">{{ captionHint($t('historyTimeToLive.choiceCustom')) }}</span>
        </label>
      </div>

      <div class="input-group mt-1" style="max-width: 320px; padding-left: 20px;">
        <b-form-input
          v-model="editTtlValue"
          id="ttlValue"
          type="number"
          :min="customValueMin"
          :max="customValueMax"
          :placeholder="$t('historyTimeToLive.placeholder')"
          :state="choice === 'custom' ? !errorInCustomChoice : null"
          :disabled="choice !== 'custom'">
        </b-form-input>
        <span class="input-group-text">{{ $t('historyTimeToLive.days') }}</span>
      </div>

      <div v-if="errorInCustomChoice" class="text-danger small mt-1">
        {{ $t('historyTimeToLive.validation') }}
      </div>
    </div>

    <template #modal-footer>
      <b-button variant="light" @click="$refs.modal.hide()">
        {{ $t('confirm.cancel') }}
      </b-button>
      <b-button variant="primary" :disabled="errorInCustomChoice" @click="saveTtlValue">
        {{ $t('commons.save') }}
      </b-button>
    </template>
  </b-modal>
</template>

<script>
import { HISTORY_TTL_LAST_CUSTOM_VALUE_STORAGE_KEY } from '@/constants'

export default {
  name: 'EditHistoryTimeToLiveModal',
  emits: ['ttl-updated'],
  props: {
    // Array of { title, value } pairs, e.g. [{ title: 'Process Name', value: 'Invoice' }].
    // Titles are expected to already be translated by the caller; rendered one per line.
    descriptionItems: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      selectedItem: null,
      choice: 'default', // 'default', 'zero', or 'custom'
      editTtlValue: null,
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
    // The engine reports its default in days; -1 means history is kept indefinitely, 0 means
    // it is cleaned up immediately, null/undefined means the engine did not report it.
    defaultChoiceLabel() {
      const systemDefault = this.$root.config.historyTimeToLive
      if (systemDefault === null || systemDefault === undefined) {
        return this.$t('historyTimeToLive.choiceDefaultUnknown')
      }
      if (systemDefault < 0) {
        return this.$t('historyTimeToLive.choiceDefaultUnlimited')
      }
      if (systemDefault === 0) {
        return this.$t('historyTimeToLive.choiceDefaultZero')
      }
      return this.$t('historyTimeToLive.choiceDefault', { value: systemDefault })
    },

    customValueMin() {
      return 0
    },
    customValueMax() {
      return 356000 // roughly 1,000 years in days
    },
    errorInCustomChoice() {
      if (this.choice !== 'custom') return false
      const value = Number.parseInt(this.editTtlValue)
      return Number.isNaN(value) || value < this.customValueMin || value > this.customValueMax
    }
  },
  methods: {
    // Parent calls show(item) via ref to open the dialog
    show(item) {
      this.selectedItem = item
      if (item.historyTimeToLive === null || item.historyTimeToLive === undefined) {
        this.choice = 'default'
        this.editTtlValue = this.loadLastCustomValue()
      } else if (item.historyTimeToLive === 0) {
        this.choice = 'zero'
        this.editTtlValue = this.loadLastCustomValue()
      } else {
        this.choice = 'custom'
        this.editTtlValue = item.historyTimeToLive
      }

      this.$nextTick(() => {
        if (this.$refs.modal) {
          this.$refs.modal.show()
        }
      })
    },

    resetTtlDialog() {
      this.selectedItem = null
      this.choice = 'default'
      this.editTtlValue = this.loadLastCustomValue()
    },

    async saveTtlValue() {
      if (this.errorInCustomChoice) {
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
        this.saveLastCustomValue(ttlValue)
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
    },

    // 'a (b)' => 'a'
    captionValue(value) {
      return value?.split(' (')[0] || ''
    },
    // 'a (b)' => '(b)'
    captionHint(value) {
      const v = value?.split(' (')[1]
      return v ? `(${v}` : ''
    },

    loadLastCustomValue() {
      return localStorage.getItem(HISTORY_TTL_LAST_CUSTOM_VALUE_STORAGE_KEY) ?? 30
    },
    saveLastCustomValue(value) {
      localStorage.setItem(HISTORY_TTL_LAST_CUSTOM_VALUE_STORAGE_KEY, value)
    },

  }
}
</script>
