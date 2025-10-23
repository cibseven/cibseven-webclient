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
  <div class="p-0 m-0 d-flex flex-column gap-4">
    <ContentBlock :title="$t('admin.preferences.dates.title')">
      <b-form-group>
        <label class="form-label">
          {{ $t('admin.preferences.dates.formatDefault') }}
          <span v-b-popover.hover.right="$t('admin.preferences.dates.formatDefaultDescription')" class="mdi mdi-18px mdi-information-outline text-info"></span>
        </label>
        <b-form-select v-model="formatDefault" :options="dateFormatOptions" 
          class="col-lg-6 col-md-8 col-sm-12"
        />
      </b-form-group>

      <b-form-group>
        <label class="form-label">
          {{ $t('admin.preferences.dates.formatLong') }}
          <span v-b-popover.hover.right="$t('admin.preferences.dates.formatLongDescription')" class="mdi mdi-18px mdi-information-outline text-info"></span>
        </label>
        <b-form-select v-model="formatLong" :options="dateFormatOptions"
          class="col-lg-6 col-md-8 col-sm-12"
        />

      </b-form-group>
    </ContentBlock>

    <ContentBlock :title="$t('admin.preferences.notifications.title')"
      v-if="$root.config.notifications.tasks.enabled || $root.config.layout.showPopoverHowToAssign"
    >
      <b-form-group v-if="$root.config.notifications.tasks.enabled">
        <b-form-checkbox v-model="tasksCheckNotificationsDisabled">
          <span class="fw-semibold">{{ $t('admin.preferences.notifications.tasksCheckNotificationsDisabled.label') }}</span>
          <br/>
        <span class="small text-secondary">{{ $t('admin.preferences.notifications.tasksCheckNotificationsDisabled.description') }}</span>
        </b-form-checkbox>
      </b-form-group>

      <b-form-group v-if="$root.config.layout.showPopoverHowToAssign">
        <b-form-checkbox v-model="showPopoverHowToAssign">
          <span class="fw-semibold">{{ $t('admin.preferences.notifications.showPopoverHowToAssign.label') }}</span>
          <br/>
        <span class="small text-secondary">{{ $t('admin.preferences.notifications.showPopoverHowToAssign.description') }}</span>
        </b-form-checkbox>
      </b-form-group>
    </ContentBlock>

    <component :is="ProfilePreferencesPlugin" v-if="ProfilePreferencesPlugin"></component>
  </div>
</template>

<script>
import { formatDate } from '@/utils/dates.js'
import ContentBlock from '@/components/common-components/ContentBlock.vue'

export default {
  name: 'ProfilePreferencesTab',
  components: {
    ContentBlock
  },
  computed: {
    dateFormatOptions() {
      return [
        // Standard International
        { 
          label: this.$t('admin.preferences.dates.groupStandard'),
          options: this.createDateOptions([
            'L',
            'L LT',
            'L LTS',
            'LL',
            'LLL',
            'LLLL',
            'LL HH:mm',
            'LL HH:mm:ss',
            'LL HH:mm:ss.SSS',
            'dddd, MMMM Do YYYY, h:mm A',
            'dddd, MMMM Do YYYY, h:mm:ss A',
            'dddd, MMMM Do YYYY, h:mm:ss.SSS A',
            'MMM D, YYYY, h:mm A',
            'MMM D, YYYY, h:mm:ss A',
            'MMM D, YYYY, h:mm:ss.SSS A',
            'ddd, MMM D, YYYY h:mm A',
            'ddd, MMM D, YYYY h:mm:ss A',
            'MMMM Do, YYYY HH:mm',
            'MMMM Do, YYYY HH:mm:ss',
            'Do MMMM YYYY, HH:mm',
            'Do MMMM YYYY, HH:mm:ss',
          ])
        },
        // European Formats
        {
          label: this.$t('admin.preferences.dates.groupEuropean'),
          options: this.createDateOptions([
            'DD.MM.YYYY HH:mm',
            'DD.MM.YYYY HH:mm:ss',
            'DD.MM.YYYY HH:mm:ss.SSS',
            'DD.MM.YY HH:mm',
            'DD.MM.YY HH:mm:ss',
            'DD/MM/YYYY HH:mm',
            'DD/MM/YYYY HH:mm:ss',
            'DD/MM/YYYY HH:mm:ss.SSS',
            'DD-MM-YYYY HH:mm',
            'DD-MM-YYYY HH:mm:ss',
            'DD MMM YYYY HH:mm',
            'DD MMM YYYY HH:mm:ss',
            'DD MMM YY HH:mm',
            'DD MMM YY HH:mm:ss',
            'dddd DD/MM/YYYY HH:mm',
            'dddd DD/MM/YYYY HH:mm:ss',
          ])
        },
        // US Formats
        {
          label: this.$t('admin.preferences.dates.groupUS'),
          options: this.createDateOptions([
            'MM/DD/YYYY hh:mm A',
            'MM/DD/YYYY hh:mm:ss A',
            'MM/DD/YYYY hh:mm:ss.SSS A',
            'MM/DD/YY h:mm A',
            'MM/DD/YY h:mm:ss A',
            'MM/DD/YY h:mm:ss.SSS A',
            'MM-DD-YYYY HH:mm',
            'MM-DD-YYYY HH:mm:ss',
            'MM-DD-YYYY HH:mm:ss.SSS'
          ])
        },
        // ISO/Technical Formats
        {
          label: this.$t('admin.preferences.dates.groupISO'),
          options: this.createDateOptions([
            'YYYY-MM-DD HH:mm',
            'YYYY-MM-DD HH:mm:ss',
            'YYYY-MM-DD HH:mm:ss.SSS',
            'YYYY-MM-DD hh:mm A',
            'YYYY-MM-DD hh:mm:ss A',
            'YYYY-MM-DD hh:mm:ss.SSS A',
            'YYYY/MM/DD HH:mm',
            'YYYY/MM/DD HH:mm:ss',
            'YYYY/MM/DD HH:mm:ss.SSS'
          ])
        }
      ]
    },
    formatDefault: {
      get() {
        return localStorage.getItem('cibseven:preferences:formatDefault') || 'LL HH:mm'
      },
      set(val) {
        localStorage.setItem('cibseven:preferences:formatDefault', val)
      }
    },
    formatLong: {
      get() {
        return localStorage.getItem('cibseven:preferences:formatLong') || 'LL HH:mm:ss.SSS'
      },
      set(val) {
        localStorage.setItem('cibseven:preferences:formatLong', val)
      }
    },
    tasksCheckNotificationsDisabled: {
      get() {
        return localStorage.getItem('tasksCheckNotificationsDisabled') === 'true' || false
      },
      set(val) {
        localStorage.setItem('tasksCheckNotificationsDisabled', val)
      }
    },
    showPopoverHowToAssign: {
      get() {
        return localStorage.getItem('showPopoverHowToAssign') !== 'false' || true
      },
      set(val) {
        localStorage.setItem('showPopoverHowToAssign', val)
      }
    },
    ProfilePreferencesPlugin() {
      return this.$options.components && this.$options.components.ProfilePreferencesPlugin
        ? this.$options.components.ProfilePreferencesPlugin
        : null
    },
  },
  methods: {
    createDateOptions(formats) {
      return formats.map(value => ({ value, text: formatDate(new Date(), value) }))
    }
  }
}
</script>
