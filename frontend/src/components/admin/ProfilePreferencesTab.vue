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
  <div class="p-4 d-flex flex-column gap-4">
    <b-card class="p-4 pb-0 shadow-sm border rounded" :title="$t('admin.preferences.dates.title')">
      <b-card-text class="border-top pt-4 mt-3">
        <b-form-group>

          <label class="form-label">
            {{ $t('admin.preferences.dates.formatDefault') }}
            <span v-b-popover.hover.right="$t('admin.preferences.dates.formatDefaultDescription')" class="mdi mdi-18px mdi-information-outline text-info"></span>
          </label>
          <b-form-select v-model="formatDefault" :options="dateFormatOptions" 
            class="col-lg-6 col-md-8 col-sm-12"
          />

          <label class="form-label">
            {{ $t('admin.preferences.dates.formatLong') }}
            <span v-b-popover.hover.right="$t('admin.preferences.dates.formatLongDescription')" class="mdi mdi-18px mdi-information-outline text-info"></span>
          </label>
          <b-form-select v-model="formatLong" :options="dateFormatOptions"
            class="col-lg-6 col-md-8 col-sm-12"
          />

        </b-form-group>
      </b-card-text>
    </b-card>

    <template v-if="!$root.config.notifications.tasks.enabled">
      <b-card class="p-4 shadow-sm border rounded" :title="$t('admin.preferences.notifications.title')">
        <b-card-text class="border-top pt-4 mt-3">
          <b-form-group>
            <b-form-checkbox v-model="tasksCheckNotificationsDisabled">
              {{ $t('admin.preferences.notifications.tasksCheckNotificationsDisabled.label') }}
              <br/>
            <span class="small">{{ $t('admin.preferences.notifications.tasksCheckNotificationsDisabled.description') }}</span>
            </b-form-checkbox>
          </b-form-group>
        </b-card-text>
      </b-card>
    </template>

  </div>
</template>

<script>
import { formatDate } from '@/utils/dates.js'

export default {
  name: 'ProfilePreferencesTab',
  data() {
    return {
      dateFormatOptions: [
        // Standard International
        { 
          label: this.$t('admin.preferences.dates.groupStandard'),
          options: [
            { value: 'L', text: formatDate(new Date(), 'L') },
            { value: 'L LT', text: formatDate(new Date(), 'L LT') },
            { value: 'L LTS', text: formatDate(new Date(), 'L LTS') },
            { value: 'LL', text: formatDate(new Date(), 'LL') },
            { value: 'LLL', text: formatDate(new Date(), 'LLL') },
            { value: 'LLLL', text: formatDate(new Date(), 'LLLL') },
            { value: 'LL HH:mm', text: formatDate(new Date(), 'LL HH:mm') },
            { value: 'LL HH:mm:ss', text: formatDate(new Date(), 'LL HH:mm:ss') },
            { value: 'LL HH:mm:ss.SSS', text: formatDate(new Date(), 'LL HH:mm:ss.SSS') },
            { value: 'dddd, MMMM Do YYYY, h:mm A', text: formatDate(new Date(), 'dddd, MMMM Do YYYY, h:mm A') },
            { value: 'dddd, MMMM Do YYYY, h:mm:ss A', text: formatDate(new Date(), 'dddd, MMMM Do YYYY, h:mm:ss A') },
            { value: 'dddd, MMMM Do YYYY, h:mm:ss.SSS A', text: formatDate(new Date(), 'dddd, MMMM Do YYYY, h:mm:ss.SSS A') },
            { value: 'MMM D, YYYY, h:mm A', text: formatDate(new Date(), 'MMM D, YYYY, h:mm A') },
            { value: 'MMM D, YYYY, h:mm:ss A', text: formatDate(new Date(), 'MMM D, YYYY, h:mm:ss A') },
            { value: 'MMM D, YYYY, h:mm:ss.SSS A', text: formatDate(new Date(), 'MMM D, YYYY, h:mm:ss.SSS A') },
            { value: 'ddd, MMM D, YYYY h:mm A', text: formatDate(new Date(), 'ddd, MMM D, YYYY h:mm A') },
            { value: 'ddd, MMM D, YYYY h:mm:ss A', text: formatDate(new Date(), 'ddd, MMM D, YYYY h:mm:ss A') },
            { value: 'MMMM Do, YYYY HH:mm', text: formatDate(new Date(), 'MMMM Do, YYYY HH:mm') },
            { value: 'MMMM Do, YYYY HH:mm:ss', text: formatDate(new Date(), 'MMMM Do, YYYY HH:mm:ss') },
            { value: 'Do MMMM YYYY, HH:mm', text: formatDate(new Date(), 'Do MMMM YYYY, HH:mm') },
            { value: 'Do MMMM YYYY, HH:mm:ss', text: formatDate(new Date(), 'Do MMMM YYYY, HH:mm:ss') }
          ]
        },
        // European Formats
        {
          label: this.$t('admin.preferences.dates.groupEuropean'),
          options: [
            { value: 'DD.MM.YYYY HH:mm', text: formatDate(new Date(), 'DD.MM.YYYY HH:mm') },
            { value: 'DD.MM.YYYY HH:mm:ss', text: formatDate(new Date(), 'DD.MM.YYYY HH:mm:ss') },
            { value: 'DD.MM.YYYY HH:mm:ss.SSS', text: formatDate(new Date(), 'DD.MM.YYYY HH:mm:ss.SSS') },
            { value: 'DD.MM.YY HH:mm', text: formatDate(new Date(), 'DD.MM.YY HH:mm') },
            { value: 'DD.MM.YY HH:mm:ss', text: formatDate(new Date(), 'DD.MM.YY HH:mm:ss') },
            { value: 'DD/MM/YYYY HH:mm', text: formatDate(new Date(), 'DD/MM/YYYY HH:mm') },
            { value: 'DD/MM/YYYY HH:mm:ss', text: formatDate(new Date(), 'DD/MM/YYYY HH:mm:ss') },
            { value: 'DD/MM/YYYY HH:mm:ss.SSS', text: formatDate(new Date(), 'DD/MM/YYYY HH:mm:ss.SSS') },
            { value: 'DD-MM-YYYY HH:mm', text: formatDate(new Date(), 'DD-MM-YYYY HH:mm') },
            { value: 'DD-MM-YYYY HH:mm:ss', text: formatDate(new Date(), 'DD-MM-YYYY HH:mm:ss') },
            { value: 'DD MMM YYYY HH:mm', text: formatDate(new Date(), 'DD MMM YYYY HH:mm') },
            { value: 'DD MMM YYYY HH:mm:ss', text: formatDate(new Date(), 'DD MMM YYYY HH:mm:ss') },
            { value: 'DD MMM YY HH:mm', text: formatDate(new Date(), 'DD MMM YY HH:mm') },
            { value: 'DD MMM YY HH:mm:ss', text: formatDate(new Date(), 'DD MMM YY HH:mm:ss') },
            { value: 'dddd DD/MM/YYYY HH:mm', text: formatDate(new Date(), 'dddd DD/MM/YYYY HH:mm') },
            { value: 'dddd DD/MM/YYYY HH:mm:ss', text: formatDate(new Date(), 'dddd DD/MM/YYYY HH:mm:ss') }
          ]
        },
        // US Formats
        {
          label: this.$t('admin.preferences.dates.groupUS'),
          options: [
            { value: 'MM/DD/YYYY hh:mm A', text: formatDate(new Date(), 'MM/DD/YYYY hh:mm A') },
            { value: 'MM/DD/YYYY hh:mm:ss A', text: formatDate(new Date(), 'MM/DD/YYYY hh:mm:ss A') },
            { value: 'MM/DD/YYYY hh:mm:ss.SSS A', text: formatDate(new Date(), 'MM/DD/YYYY hh:mm:ss.SSS A') },
            { value: 'MM/DD/YY h:mm A', text: formatDate(new Date(), 'MM/DD/YY h:mm A') },
            { value: 'MM/DD/YY h:mm:ss A', text: formatDate(new Date(), 'MM/DD/YY h:mm:ss A') },
            { value: 'MM/DD/YY h:mm:ss.SSS A', text: formatDate(new Date(), 'MM/DD/YY h:mm:ss.SSS A') },
            { value: 'MM-DD-YYYY HH:mm', text: formatDate(new Date(), 'MM-DD-YYYY HH:mm') },
            { value: 'MM-DD-YYYY HH:mm:ss', text: formatDate(new Date(), 'MM-DD-YYYY HH:mm:ss') },
            { value: 'MM-DD-YYYY HH:mm:ss.SSS', text: formatDate(new Date(), 'MM-DD-YYYY HH:mm:ss.SSS') }
          ]
        },
        // ISO/Technical Formats
        {
          label: this.$t('admin.preferences.dates.groupISO'),
          options: [
            { value: 'YYYY-MM-DD HH:mm', text: formatDate(new Date(), 'YYYY-MM-DD HH:mm') },
            { value: 'YYYY-MM-DD HH:mm:ss', text: formatDate(new Date(), 'YYYY-MM-DD HH:mm:ss') },
            { value: 'YYYY-MM-DD HH:mm:ss.SSS', text: formatDate(new Date(), 'YYYY-MM-DD HH:mm:ss.SSS') },
            { value: 'YYYY-MM-DD hh:mm A', text: formatDate(new Date(), 'YYYY-MM-DD hh:mm A') },
            { value: 'YYYY-MM-DD hh:mm:ss A', text: formatDate(new Date(), 'YYYY-MM-DD hh:mm:ss A') },
            { value: 'YYYY-MM-DD hh:mm:ss.SSS A', text: formatDate(new Date(), 'YYYY-MM-DD hh:mm:ss.SSS A') },
            { value: 'YYYY/MM/DD HH:mm', text: formatDate(new Date(), 'YYYY/MM/DD HH:mm') },
            { value: 'YYYY/MM/DD HH:mm:ss', text: formatDate(new Date(), 'YYYY/MM/DD HH:mm:ss') },
            { value: 'YYYY/MM/DD HH:mm:ss.SSS', text: formatDate(new Date(), 'YYYY/MM/DD HH:mm:ss.SSS') }
          ]
        }
      ]
    }
  },
  computed: {
    formatDefault: {
      get: function() {
        return localStorage.getItem('cibseven:preferences:formatDefault') || 'LL HH:mm'
      },
      set: function(val) {
        localStorage.setItem('cibseven:preferences:formatDefault', val)
      }
    },
    formatLong: {
      get: function() {
        return localStorage.getItem('cibseven:preferences:formatLong') || 'LL HH:mm:ss.SSS'
      },
      set: function(val) {
        localStorage.setItem('cibseven:preferences:formatLong', val)
      }
    },
    tasksCheckNotificationsDisabled: {
      get: function() {
        return localStorage.getItem('tasksCheckNotificationsDisabled') === 'true' || false
      },
      set: function(val) {
        localStorage.setItem('tasksCheckNotificationsDisabled', val)
      }
    },
  }
}
</script>
