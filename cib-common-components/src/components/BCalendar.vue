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
  <div class="calendar-container" role="region" aria-labelledby="calendar-header">
    <input type="text" class="form-control mb-3 text-center" v-model="formattedSelectedDate" readonly
      :aria-label="$t('bcomponents.calendar.selectedDate')" :placeholder="$t('bcomponents.calendar.noDate')" />
    <div class="calendar-header d-flex justify-content-between align-items-center mb-2">
      <div class="d-flex">
        <button class="btn btn-sm btn-outline-secondary me-2" @click="prevYear"
          :aria-label="$t('bcomponents.calendar.prevYear')">«</button>
        <button class="btn btn-sm btn-outline-secondary" @click="prevMonth"
          :aria-label="$t('bcomponents.calendar.prevMonth')">‹</button>
      </div>

      <span class="month-year" aria-live="polite">{{ formattedMonthYear }}</span>

      <div class="d-flex">
        <button class="btn btn-sm btn-outline-secondary" @click="nextMonth"
          :aria-label="$t('bcomponents.calendar.nextMonth')">›</button>
        <button class="btn btn-sm btn-outline-secondary ms-2" @click="nextYear"
          :aria-label="$t('bcomponents.calendar.nextYear')">»</button>
      </div>
    </div>
    <div class="calendar-grid border rounded">
      <div class="row fw-bold g-0">
        <div class="col text-center" v-for="day in daysOfWeek" :key="day" role="columnheader">{{ day }}</div>
      </div>
      <div v-for="(week, weekIndex) in weeksInMonth" :key="weekIndex" class="row g-0" role="row">
        <button class="border-0 col py-2" v-for="(day, dayIndex) in week" :key="dayIndex"
          :disabled="isDisabled(day.date)" :class="[
            isSelected(day.date) ? 'bg-primary text-white fw-bold' : '',
            !isSelected(day.date) && day.isOtherMonth && !isDisabled(day.date) ? 'bg-light text-dark' : '',
            !isSelected(day.date) && !day.isOtherMonth && !isDisabled(day.date) && !isToday(day.date) ? 'bg-white text-dark' : '',
            !isSelected(day.date) && isToday(day.date) ? 'bg-white text-primary' : ''
          ]" role="gridcell" :aria-selected="isSelected(day.date) ? 'true' : 'false'"
          :aria-label="formatDateForAccessibility(day.date)" @click="!isDisabled(day.date) && selectDate(day.date)">
          {{ day.day }}
        </button>
      </div>
    </div>
    <div v-if="showToday || showReset" class="calendar-footer mt-3 d-flex">
      <BButton v-if="showToday" size="sm" variant="outline-primary" @click="selectToday">{{ $t('bcomponents.calendar.today') }}</BButton>
      <BButton v-if="showReset" size="sm" variant="outline-danger" class="ms-auto" @click="clearSelection">
        {{ $t('bcomponents.calendar.clearSelection') }}
      </BButton>
    </div>
  </div>
</template>

<script>
import BButton from './BButton.vue'

export default {
  components: {
    BButton
  },
  props: {
    modelValue: {
      type: [Date, String],
      default: () => null
    },
    dateDisabledFn: { type: Function, default: null },
    showReset: {
      type: Boolean,
      default: true
    },
    showToday: {
      type: Boolean,
      default: true
    }
  },
  data: function () {
    return {
      currentDate: this.parseToDate(this.modelValue || new Date()),
      selectedDate: this.parseToDate(this.modelValue || null)
    }
  },
  watch: {
    modelValue: function (newValue) {
      this.updateDates(newValue)
    }
  },
  computed: {
    weeksInMonth: function () {
      const weeks = []
      let week = []

      this.daysInMonth.forEach((day, index) => {
        week.push(day)
        if ((index + 1) % 7 === 0) {
          weeks.push(week)
          week = []
        }
      })

      if (week.length > 0) {
        const daysToFill = 7 - week.length
        const lastDayOfMonth = this.daysInMonth[this.daysInMonth.length - 1].date

        for (let i = 1; i <= daysToFill; i++) {
          const nextDate = new Date(
            lastDayOfMonth.getFullYear(),
            lastDayOfMonth.getMonth(),
            lastDayOfMonth.getDate() + i
          )
          week.push({ date: nextDate, day: nextDate.getDate(), isOtherMonth: true })
        }
        weeks.push(week)
      }

      return weeks
    },
    daysOfWeek: function () {
      return this.$t('bcomponents.calendar.daysOfWeek').split(',')
    },
    formattedMonthYear: function () {
      return this.currentDate.toLocaleString('default', { month: 'long', year: 'numeric' })
    },
    formattedSelectedDate: function () {
      return this.selectedDate ? this.selectedDate.toLocaleDateString() : ''
    },
    daysInMonth: function () {
      const year = this.currentDate.getFullYear()
      const month = this.currentDate.getMonth()
      const firstDay = new Date(year, month, 1)
      const lastDay = new Date(year, month + 1, 0)

      const days = []
      var date = ''

      // Days from previous month
      for (let i = firstDay.getDay() - 1; i > 0; i--) {
        date = new Date(year, month, 1 - i)
        days.push({ date, day: date.getDate(), isOtherMonth: true })
      }

      // Days of the current month
      for (let i = 1; i <= lastDay.getDate(); i++) {
        date = new Date(year, month, i)
        days.push({ date, day: i, isOtherMonth: false })
      }

      // Fill the rest of the week with next month's days
      for (let i = lastDay.getDay(); i < 6; i++) {
        date = new Date(year, month + 1, i - lastDay.getDay() + 1)
        days.push({ date, day: date.getDate(), isOtherMonth: true })
      }

      return days
    }
  },
  methods: {
    updateDates: function (newValue) {
      if (newValue) {
        const parsedDate = this.parseToDate(newValue)
        this.currentDate = parsedDate
        this.selectedDate = parsedDate
      }
    },
    parseToDate: function (value) {
      if (typeof value === 'string') {
        return new Date(value)
      }
      return value
    },
    prevMonth: function () {
      this.currentDate = new Date(this.currentDate.getFullYear(), this.currentDate.getMonth() - 1, 1)
    },
    nextMonth: function () {
      this.currentDate = new Date(this.currentDate.getFullYear(), this.currentDate.getMonth() + 1, 1)
    },
    prevYear: function () {
      this.currentDate = new Date(this.currentDate.getFullYear() - 1, this.currentDate.getMonth(), 1)
    },
    nextYear: function () {
      this.currentDate = new Date(this.currentDate.getFullYear() + 1, this.currentDate.getMonth(), 1)
    },
    isToday: function (date) {
      const today = new Date()
      return (
        date.getDate() === today.getDate() &&
        date.getMonth() === today.getMonth() &&
        date.getFullYear() === today.getFullYear()
      )
    },
    selectDate: function (date) {
      this.selectedDate = date
      this.$emit('update:modelValue', date)
    },
    selectToday: function () {
      this.selectDate(new Date())
    },
    clearSelection: function () {
      this.selectedDate = null
      this.$emit('update:modelValue', null)
    },
    isSelected: function (date) {
      return this.selectedDate && this.selectedDate.toDateString() === date.toDateString()
    },
    isDisabled: function (date) {
      return this.dateDisabledFn && this.dateDisabledFn(date.toISOString().split('T')[0], date)
    },
    formatDateForAccessibility: function (date) {
      return date.toLocaleDateString(undefined, { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })
    }
  }
}
</script>

<style scoped></style>
