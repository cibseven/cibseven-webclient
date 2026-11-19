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

    <p class="mb-0" v-if="descriptionItems.length > 0">
      <template v-for="(item, index) in descriptionItems" :key="item.title">
        <br v-if="index > 0">{{ item.title }}: <strong>{{ item.value }}</strong>
      </template>
    </p>

    <!-- No label-for: the group label is rendered as the <legend> of a <fieldset>, which names
      the radio group as a whole. The id makes that legend's id unique per modal instance
      (otherwise every instance renders the same "form-group__label" id). -->
    <b-form-group :id="`${idPrefix}Group`" :label="$t('historyTimeToLive.ttl')" class="mb-0">
      <div class="list-group">

        <!-- "Unlimited" saves null: the definition gets no TTL and its history is never cleaned up.
          It is deliberately not offered as "default to system": the engine-wide default only
          applies when a model is deployed, never to a definition whose TTL is set to null
          afterwards (see utils/historyTimeToLive.js). -->
        <div class="list-group-item border-0 border-top">
          <div class="form-check">
            <!-- The hint below the label (why the option is disabled, or that it is the engine
              default) is linked through aria-describedby, so screen readers announce it together
              with the option. -->
            <input
              class="form-check-input"
              type="radio"
              :name="`${idPrefix}Choice`"
              :id="`${idPrefix}ChoiceUnlimited`"
              value="unlimited"
              v-model="choice"
              :disabled="unlimitedChoiceDisabled"
              :aria-describedby="unlimitedChoiceDisabled || unlimitedIsEngineDefault ? `${idPrefix}ChoiceUnlimitedHint` : null">
            <label class="form-check-label w-100" :for="`${idPrefix}ChoiceUnlimited`">
              {{ captionValue($t('historyTimeToLive.choiceUnlimited')) }}
              <span class="text-muted float-end">{{ captionHint($t('historyTimeToLive.choiceUnlimited')) }}</span>
            </label>
            <!-- The reason the option is disabled is shown as visible text rather than only as a
              title tooltip, which keyboard and touch users cannot reach. The two hints never
              apply together: unlimitedIsEngineDefault is false while the option is disabled. -->
            <div v-if="unlimitedChoiceDisabled" :id="`${idPrefix}ChoiceUnlimitedHint`" class="form-text mt-0">
              {{ $t('historyTimeToLive.enforcedHint') }}
            </div>
            <div v-else-if="unlimitedIsEngineDefault" :id="`${idPrefix}ChoiceUnlimitedHint`" class="form-text mt-0">
              {{ $t('historyTimeToLive.engineDefaultHint') }}
            </div>
          </div>
        </div>

        <div class="list-group-item border-0 border-top">
          <div class="form-check">
            <input
              class="form-check-input"
              type="radio"
              :name="`${idPrefix}Choice`"
              :id="`${idPrefix}ChoiceZero`"
              value="zero"
              v-model="choice"
              :aria-describedby="zeroIsEngineDefault ? `${idPrefix}ChoiceZeroHint` : null">
            <label class="form-check-label w-100" :for="`${idPrefix}ChoiceZero`">
              {{ captionValue($t('historyTimeToLive.choiceZero')) }}
              <span class="text-muted float-end">{{ captionHint($t('historyTimeToLive.choiceZero')) }}</span>
            </label>
            <div v-if="zeroIsEngineDefault" :id="`${idPrefix}ChoiceZeroHint`" class="form-text mt-0">
              {{ $t('historyTimeToLive.engineDefaultHint') }}
            </div>
          </div>
        </div>

        <!-- Only when the engine reports a default of n > 0 days: offers exactly that value as its
          own choice (saving n), so users can apply the administrator's retention policy without
          typing it into the custom field. Unlimited (-1) and 0 are already choices of their own and
          only get the hint; an unknown default (engines before 3.0.0) adds nothing. -->
        <div v-if="engineDefaultDays !== null" class="list-group-item border-0 border-top">
          <div class="form-check">
            <input
              class="form-check-input"
              type="radio"
              :name="`${idPrefix}Choice`"
              :id="`${idPrefix}ChoiceEngineDefault`"
              value="engineDefault"
              v-model="choice"
              :aria-describedby="`${idPrefix}ChoiceEngineDefaultHint`">
            <label class="form-check-label w-100" :for="`${idPrefix}ChoiceEngineDefault`">
              {{ engineDefaultLabel }}
            </label>
            <div :id="`${idPrefix}ChoiceEngineDefaultHint`" class="form-text mt-0">
              {{ $t('historyTimeToLive.engineDefaultHint') }}
            </div>
          </div>
        </div>

        <div class="list-group-item border-0 border-top border-bottom">
          <div class="form-check">
            <input
              class="form-check-input"
              type="radio"
              :name="`${idPrefix}Choice`"
              :id="`${idPrefix}ChoiceCustom`"
              value="custom"
              v-model="choice">
            <label class="form-check-label w-100" :for="`${idPrefix}ChoiceCustom`" :id="`${idPrefix}ChoiceCustomLabel`">
              {{ captionValue($t('historyTimeToLive.choiceCustom')) }}
              <span class="text-muted float-end">{{ captionHint($t('historyTimeToLive.choiceCustom')) }}</span>
            </label>
          </div>

          <!-- The number input has no visible label of its own: its accessible name is composed
            through aria-labelledby from the "Custom" radio label and the "days" unit next to it
            (e.g. "Custom days"). The placeholder is not a label and disappears once typed into.
            aria-invalid is set by b-form-input itself from :state (state false -> "true");
            the validation message is linked through aria-describedby while it is shown. -->
          <div class="input-group my-2" style="max-width: 320px; padding-left: 20px;">
            <b-form-input
              v-model="editTtlValue"
              :id="`${idPrefix}Value`"
              type="number"
              step="1"
              :min="customValueMin"
              :max="customValueMax"
              :placeholder="$t('historyTimeToLive.placeholder')"
              :aria-labelledby="`${idPrefix}ChoiceCustomLabel ${idPrefix}ValueUnit`"
              :aria-describedby="errorInCustomChoice ? `${idPrefix}ValueFeedback` : null"
              :state="choice === 'custom' ? !errorInCustomChoice : null"
              :disabled="choice !== 'custom'">
            </b-form-input>
            <span class="input-group-text" :id="`${idPrefix}ValueUnit`">{{ $t('historyTimeToLive.days') }}</span>
          </div>

          <!-- The alert role makes screen readers announce the message as soon as it appears,
            the same way b-form-group renders its own invalid feedback. -->
          <div v-if="errorInCustomChoice" :id="`${idPrefix}ValueFeedback`" role="alert" class="text-danger small mt-1" style="padding-left: 20px;">
            {{ $t('historyTimeToLive.validation', { min: customValueMin, max: customValueMax }) }}
          </div>
        </div>

      </div>
    </b-form-group>

    <template #modal-footer>
      <b-button variant="light" @click="$refs.modal.hide()">
        {{ $t('confirm.cancel') }}
      </b-button>
      <b-button variant="primary" :disabled="saveDisabled" @click="saveTtlValue">
        {{ $t('commons.save') }}
      </b-button>
    </template>
  </b-modal>
</template>

<script>
import { HISTORY_TTL_LAST_CUSTOM_VALUE_STORAGE_KEY } from '@/constants'
import { formatHistoryTimeToLive } from '@/utils/historyTimeToLive.js'

const DEFAULT_CUSTOM_VALUE = 30

// Strictly parses a number of days from the input field (a string, as `<input type="number">`
// hands it over through v-model), from localStorage (a string, or null if nothing is stored)
// or from a plain number. Returns the integer, or NaN if the value is not a whole number.
// - Number.parseInt is not used, since it silently stops at the first non-digit and turns
//   '1e3' into 1 and '1.5' into 1, which would then pass validation and be saved.
// - Number() alone is not enough either, since it turns '', '  ' and null into 0, which is a
//   real TTL ("history is removed by the next cleanup run"), so those are rejected up front.
// '1e3' is accepted as 1000 (the number input itself accepts the exponent notation), while
// '1.5' and other fractions are rejected rather than rounded.
function parseDays(value) {
  if (typeof value === 'number') {
    return Number.isInteger(value) ? value : Number.NaN
  }
  if (typeof value !== 'string' || value.trim() === '') {
    return Number.NaN
  }
  const days = Number(value)
  return Number.isInteger(days) ? days : Number.NaN
}

// One modal instance is mounted per version in the sidebars, so element ids must be unique
// per instance - a duplicated id makes <label for> toggle the radio of another instance.
let nextInstanceId = 0

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
      idPrefix: `historyTimeToLive${++nextInstanceId}`,
      selectedItem: null,
      // 'unlimited' (saves null), 'zero' (saves 0), 'engineDefault' (saves the engine default n,
      // only offered for n > 0) or 'custom' (saves N)
      choice: 'unlimited',
      editTtlValue: null,
    }
  },
  computed: {
    // An enforcing engine rejects setting a definition's TTL to null (NotAllowedException), so
    // 'unlimited' is only available when enforcement is off.
    // Engines since 3.0.0 always report enforceHistoryTimeToLive as a known boolean; older
    // engines do not report it, which /info passes through as null ("unknown", see
    // EngineConfiguration.java). Only a known `true` disables the option - an unknown value is
    // left selectable (the engine may well have enforcement switched off) and if the engine
    // does enforce, the rejected save surfaces through the regular error dialog.
    unlimitedChoiceDisabled() {
      return this.$root.config.enforceHistoryTimeToLive === true
    },
    // The engine-wide default TTL in days (config.historyTimeToLive, from /info): the value the
    // engine gives newly deployed models without their own TTL, i.e. the administrator's
    // retention policy. It is never applied to an existing definition by the engine itself, so
    // the dialog only points it out (hint) or offers it as a choice of its own.
    // - null: unknown (engines before 3.0.0 do not report it) -> the dialog adds nothing
    // - -1: no default configured -> hint on 'unlimited'
    // - 0: hint on 'zero'
    // - n > 0: an extra 'engineDefault' choice saving n
    engineDefault() {
      const engineDefault = this.$root.config.historyTimeToLive
      return Number.isInteger(engineDefault) ? engineDefault : null
    },
    // Without a configured default, models without their own TTL get none and keep their history
    // indefinitely - but only while enforcement is off. With enforcement on, the engine rejects
    // such models instead, and 'unlimited' is disabled, so it is not marked as the default then.
    unlimitedIsEngineDefault() {
      return this.engineDefault !== null && this.engineDefault < 0 && !this.unlimitedChoiceDisabled
    },
    zeroIsEngineDefault() {
      return this.engineDefault === 0
    },
    // The engine default in days if it is n > 0 (then offered as the 'engineDefault' choice),
    // otherwise null.
    engineDefaultDays() {
      return this.engineDefault > 0 ? this.engineDefault : null
    },
    // e.g. "180 days", the same label the definition details views show for that value
    engineDefaultLabel() {
      return formatHistoryTimeToLive(this.$t, this.engineDefaultDays)
    },

    customValueMin() {
      return 0
    },
    customValueMax() {
      return 356000 // roughly 1,000 years in days
    },
    errorInCustomChoice() {
      if (this.choice !== 'custom') return false
      const value = parseDays(this.editTtlValue)
      return Number.isNaN(value) || value < this.customValueMin || value > this.customValueMax
    },
    // Safeguard on top of the preselection in show(): never send a choice the user cannot
    // select. A disabled 'unlimited' would save null, which an enforcing engine rejects, and
    // 'engineDefault' has no value to save if the engine no longer reports a default n > 0.
    saveDisabled() {
      return this.errorInCustomChoice ||
        (this.choice === 'unlimited' && this.unlimitedChoiceDisabled) ||
        (this.choice === 'engineDefault' && this.engineDefaultDays === null)
    }
  },
  methods: {
    // Parent calls show(item) via ref to open the dialog
    show(item) {
      this.selectedItem = item
      if ((item.historyTimeToLive === null || item.historyTimeToLive === undefined) && this.unlimitedChoiceDisabled) {
        // The definition has no TTL (deployed before enforcement was switched on, or with
        // enforcement skipped), but the engine now enforces one: the 'unlimited' choice (which
        // saves null) is disabled, because the engine rejects null with a NotAllowedException.
        // Never preselect a disabled radio - preselect what the engine would apply instead.
        this.preselectEnforcedChoice()
      } else if (item.historyTimeToLive === null || item.historyTimeToLive === undefined) {
        // No TTL means unlimited (whatever the engine default is), so that is what is preselected
        this.choice = 'unlimited'
        this.editTtlValue = this.loadLastCustomValue()
      } else if (item.historyTimeToLive === 0) {
        this.choice = 'zero'
        this.editTtlValue = this.loadLastCustomValue()
      } else if (item.historyTimeToLive === this.engineDefaultDays) {
        // the definition already uses the engine default - show it as that choice
        this.choice = 'engineDefault'
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

    // Preselection for a definition without a TTL on an engine that enforces one. Mirrors what
    // the engine applies at deployment time to models without their own TTL:
    // - engine default 0      -> 'zero'
    // - engine default n > 0  -> 'engineDefault', so saving keeps the engine's policy
    // - no (known) default    -> 'custom' prefilled with the last custom value (or 30 days)
    // The user still has to confirm with Save; nothing is sent until then.
    preselectEnforcedChoice() {
      if (this.zeroIsEngineDefault) {
        this.choice = 'zero'
      } else if (this.engineDefaultDays !== null) {
        this.choice = 'engineDefault'
      } else {
        this.choice = 'custom'
      }
      this.editTtlValue = this.loadLastCustomValue()
    },

    resetTtlDialog() {
      this.selectedItem = null
      this.choice = 'unlimited'
      this.editTtlValue = this.loadLastCustomValue()
    },

    async saveTtlValue() {
      if (this.saveDisabled) {
        return
      }

      // 'unlimited' -> null: the definition gets no TTL and its history is never cleaned up. The
      // engine does not fall back to its configured default for it, neither when saving nor at
      // cleanup time - the default is only applied to models at deployment time.
      // 'zero' -> 0: history is removed by the next cleanup run - the opposite of 'unlimited',
      // never conflate the two. 'engineDefault' -> the engine default n (not remembered as the
      // last custom value, since it was not typed in). 'custom' -> N days.
      let ttlValue
      if (this.choice === 'unlimited') {
        ttlValue = null
      } else if (this.choice === 'zero') {
        ttlValue = 0
      } else if (this.choice === 'engineDefault') {
        ttlValue = this.engineDefaultDays
      } else {
        // validated by errorInCustomChoice above (saveDisabled), so this is a whole number in range
        ttlValue = parseDays(this.editTtlValue)
        this.saveLastCustomValue(ttlValue)
      }
      // same label the definition details views show for the saved value
      const ttlDisplay = formatHistoryTimeToLive(this.$t, ttlValue)

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
    // 'a (b) (x)' => '(b) (x)'
    captionHint(value) {
      const v = value?.split(' (').slice(1).join(' (')
      if (!v) return ''
      // v is a string like 'b)' or 'b) (x)'
      return v.replace(/\)$/, '')
    },

    // The last custom value is only a convenience, so storage errors (e.g. blocked storage or a
    // full quota) are ignored: they must neither keep the dialog from opening nor lose a save.
    loadLastCustomValue() {
      let stored = Number.NaN
      try {
        // getItem() returns null when nothing is stored, which parseDays() rejects (never 0)
        stored = parseDays(localStorage.getItem(HISTORY_TTL_LAST_CUSTOM_VALUE_STORAGE_KEY))
      } catch {
        // storage not available - fall back to the default below
      }
      const valid = !Number.isNaN(stored) && stored >= this.customValueMin && stored <= this.customValueMax
      return valid ? stored : DEFAULT_CUSTOM_VALUE
    },
    saveLastCustomValue(value) {
      if (!value) return
      try {
        localStorage.setItem(HISTORY_TTL_LAST_CUSTOM_VALUE_STORAGE_KEY, value)
      } catch {
        // storage not available - the value is just not remembered
      }
    }
  }
}
</script>
