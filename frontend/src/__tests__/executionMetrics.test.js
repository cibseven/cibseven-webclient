/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
import { flushPromises, mount } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { i18n } from '@/i18n'
import { moment } from '@/globals.js'
import { SystemService } from '@/services.js'
import ExecutionMetrics from '@/components/system/ExecutionMetrics.vue'

vi.mock('@/services.js', () => ({
  SystemService: {
    getMetricsData: vi.fn(),
    getTelemetryData: vi.fn()
  }
}))

// Trimmed but representative subset of the real backend payload (monthly, groupBy=month):
// all-zero months plus the two most recent months which carry non-zero sums.
const MONTHLY_DATA = [
  { metric: 'process-instances', sum: 0, subscriptionYear: 2025, subscriptionMonth: 8 },
  { metric: 'decision-instances', sum: 0, subscriptionYear: 2025, subscriptionMonth: 8 },
  { metric: 'task-users', sum: 0, subscriptionYear: 2025, subscriptionMonth: 8 },
  { metric: 'process-instances', sum: 0, subscriptionYear: 2025, subscriptionMonth: 9 },
  { metric: 'decision-instances', sum: 0, subscriptionYear: 2025, subscriptionMonth: 9 },
  { metric: 'task-users', sum: 0, subscriptionYear: 2025, subscriptionMonth: 9 },
  { metric: 'process-instances', sum: 2, subscriptionYear: 2026, subscriptionMonth: 7 },
  { metric: 'decision-instances', sum: 4, subscriptionYear: 2026, subscriptionMonth: 7 },
  { metric: 'task-users', sum: 0, subscriptionYear: 2026, subscriptionMonth: 7 },
  { metric: 'process-instances', sum: 5, subscriptionYear: 2026, subscriptionMonth: 8 },
  { metric: 'decision-instances', sum: 8, subscriptionYear: 2026, subscriptionMonth: 8 },
  { metric: 'task-users', sum: 1, subscriptionYear: 2026, subscriptionMonth: 8 },
]

// Backend payload (groupBy=year); subscriptionMonth is always 0 for annual rows.
const ANNUAL_DATA = [
  { metric: 'process-instances', sum: 7, subscriptionYear: 2026, subscriptionMonth: 0 },
  { metric: 'decision-instances', sum: 12, subscriptionYear: 2026, subscriptionMonth: 0 },
  { metric: 'task-users', sum: 1, subscriptionYear: 2026, subscriptionMonth: 0 },
  { metric: 'process-instances', sum: 0, subscriptionYear: 2025, subscriptionMonth: 0 },
  { metric: 'decision-instances', sum: 0, subscriptionYear: 2025, subscriptionMonth: 0 },
  { metric: 'task-users', sum: 0, subscriptionYear: 2025, subscriptionMonth: 0 },
]

const TELEMETRY_DATA = { instanceId: 'abc-123', edition: 'ee' }

const toggleDataPointSelection = vi.fn()
const successAlertShow = vi.fn()

const ApexChartStub = {
  name: 'apexchart',
  props: ['options', 'series', 'height'],
  methods: { toggleDataPointSelection },
  template: '<div class="apexchart-stub"></div>'
}

const SuccessAlertStub = {
  name: 'SuccessAlert',
  methods: { show: successAlertShow },
  template: '<div><slot /></div>'
}

const FlowTableStub = {
  name: 'FlowTable',
  props: ['items', 'fields'],
  emits: ['click'],
  template: `
    <table>
      <tbody>
        <tr v-for="item in items" :key="item.index" class="row-stub" @click="$emit('click', item)">
          <td v-for="field in fields" :key="field.key">
            <slot :name="'cell(' + field.key + ')'" :item="item" />
          </td>
        </tr>
      </tbody>
    </table>
  `
}

function mountComponent() {
  return mount(ExecutionMetrics, {
    global: {
      stubs: {
        apexchart: ApexChartStub,
        FlowTable: FlowTableStub,
        SuccessAlert: SuccessAlertStub,
        BWaitingBox: true,
        'router-link': true,
      },
      plugins: [i18n],
    },
  })
}

describe('ExecutionMetrics', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // jsdom does not implement execCommand
    document.execCommand = vi.fn(() => true)
    SystemService.getMetricsData.mockImplementation((params) => {
      if (params.groupBy === 'year') return Promise.resolve(ANNUAL_DATA)
      return Promise.resolve(MONTHLY_DATA)
    })
    SystemService.getTelemetryData.mockResolvedValue(TELEMETRY_DATA)
  })

  afterEach(() => {
    i18n.global.locale = 'en'
    moment.locale('en')
  })

  describe('loading lifecycle', () => {
    it('shows the loading indicator before data resolves', () => {
      const wrapper = mountComponent()

      expect(wrapper.text()).toContain('admin.loading')
      expect(wrapper.findComponent(ApexChartStub).exists()).toBe(false)
      expect(wrapper.findComponent(FlowTableStub).exists()).toBe(false)
    })

    it('loads annual and monthly metrics on mount and stops loading', async () => {
      const wrapper = mountComponent()

      await flushPromises()

      expect(SystemService.getMetricsData).toHaveBeenCalledTimes(2)
      expect(wrapper.vm.loading).toBe(false)
      expect(wrapper.vm.metrics.annual).toEqual(ANNUAL_DATA)
      expect(wrapper.vm.metrics.monthly).toEqual(MONTHLY_DATA)
      expect(wrapper.findComponent(ApexChartStub).exists()).toBe(true)
      expect(wrapper.findAllComponents(FlowTableStub)).toHaveLength(2)
    })
  })

  describe('loadAnnualMetrics', () => {
    it('requests data grouped by year and stores the result', async () => {
      const wrapper = mountComponent()
      await flushPromises()
      wrapper.vm.metrics.annual = []

      await wrapper.vm.loadAnnualMetrics()

      expect(SystemService.getMetricsData).toHaveBeenLastCalledWith({ groupBy: 'year' })
      expect(wrapper.vm.metrics.annual).toEqual(ANNUAL_DATA)
    })
  })

  describe('loadMonthlyMetrics', () => {
    it('requests the last 12 months grouped by month for the three tracked metrics', async () => {
      const wrapper = mountComponent()
      await flushPromises()
      wrapper.vm.metrics.monthly = []

      await wrapper.vm.loadMonthlyMetrics()

      const params = SystemService.getMetricsData.mock.calls.at(-1)[0]
      expect(params.groupBy).toBe('month')
      expect(params.metrics).toBe('process-instances,decision-instances,task-users')

      const start = moment(params.startDate, 'YYYY-MM-DD[T]HH:mm:ss.SSSZZ')
      const subscriptionStart = moment(params.subscriptionStartDate, 'YYYY-MM-DD[T]HH:mm:ss.SSSZZ')
      expect(start.isValid()).toBe(true)
      expect(subscriptionStart.isValid()).toBe(true)
      expect(subscriptionStart.diff(start, 'years', true)).toBeCloseTo(1, 1)
      expect(start.hour()).toBe(0)
      expect(start.minute()).toBe(0)

      expect(wrapper.vm.metrics.monthly).toEqual(MONTHLY_DATA)
    })
  })

  describe('shouldDisableDate', () => {
    it('disables dates more than a year in the past', () => {
      const wrapper = mountComponent()
      const date = moment().subtract(2, 'years').toDate()
      expect(wrapper.vm.shouldDisableDate(null, date)).toBe(true)
    })

    it('disables dates in the future', () => {
      const wrapper = mountComponent()
      const date = moment().add(1, 'years').toDate()
      expect(wrapper.vm.shouldDisableDate(null, date)).toBe(true)
    })

    it('allows dates within the last year', () => {
      const wrapper = mountComponent()
      const date = moment().subtract(1, 'months').toDate()
      expect(wrapper.vm.shouldDisableDate(null, date)).toBe(false)
    })

    it('allows today and exactly one year ago', () => {
      const wrapper = mountComponent()
      expect(wrapper.vm.shouldDisableDate(null, moment().startOf('day').toDate())).toBe(false)
      expect(
        wrapper.vm.shouldDisableDate(null, moment().subtract(1, 'years').startOf('day').toDate())
      ).toBe(false)
    })
  })

  describe('computed: labels & series', () => {
    it('builds unique, chronologically sorted month labels', async () => {
      const wrapper = mountComponent()
      await flushPromises()

      expect(wrapper.vm.labels).toEqual([
        'August 2025',
        'September 2025',
        'July 2026',
        'August 2026',
      ])
    })

    it('builds one series per tracked metric aligned to the labels, defaulting to 0', async () => {
      const wrapper = mountComponent()
      await flushPromises()

      expect(wrapper.vm.series).toEqual([
        { name: i18n.global.t('admin.system.execution-metrics.process-instances'), data: [0, 0, 2, 5] },
        { name: i18n.global.t('admin.system.execution-metrics.decision-instances'), data: [0, 0, 4, 8] },
        { name: i18n.global.t('admin.system.execution-metrics.task-users'), data: [0, 0, 0, 1] },
      ])
    })

    it('defaults a metric to 0 for a month where the backend sent no row at all', async () => {
      SystemService.getMetricsData.mockImplementation((params) =>
        Promise.resolve(
          params.groupBy === 'year'
            ? ANNUAL_DATA
            : [{ metric: 'process-instances', sum: 3, subscriptionYear: 2026, subscriptionMonth: 8 }]
        )
      )
      const wrapper = mountComponent()
      await flushPromises()

      expect(wrapper.vm.labels).toEqual(['August 2026'])
      expect(wrapper.vm.series).toEqual([
        { name: i18n.global.t('admin.system.execution-metrics.process-instances'), data: [3] },
        { name: i18n.global.t('admin.system.execution-metrics.decision-instances'), data: [0] },
        { name: i18n.global.t('admin.system.execution-metrics.task-users'), data: [0] },
      ])
    })

    it('keeps series matched to their months when moment.locale() changes independently of Vue', async () => {
      // switchLanguage() in i18n.js calls moment.locale(lang) alongside i18n.global.locale - a
      // global change Vue doesn't track. labels()/series() used to derive their join key from
      // moment(...).format('MMMM YYYY'), so a stale cached `labels` (still in the old language)
      // stopped matching the freshly re-localized key computed inside `series()`, silently
      // zeroing out all data points.
      i18n.global.locale = 'en'
      moment.locale('en')
      const wrapper = mountComponent()
      await flushPromises()

      expect(wrapper.vm.series[0].data).toEqual([0, 0, 2, 5])

      i18n.global.locale = 'de'
      moment.locale('de')
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.series[0].data).toEqual([0, 0, 2, 5])
    })
  })

  describe('computed: options', () => {
    it('configures the chart with the computed labels and locale-aware formatters', async () => {
      const wrapper = mountComponent()
      await flushPromises()

      const { options } = wrapper.vm
      expect(options.chart.type).toBe('bar')
      expect(options.xaxis.categories).toEqual(wrapper.vm.labels)

      const nSpy = vi.spyOn(i18n.global, 'n')
      expect(options.dataLabels.formatter(0)).toBe('')
      expect(nSpy).not.toHaveBeenCalled()

      options.dataLabels.formatter(34022)
      options.yaxis.labels.formatter(34022)
      options.tooltip.y.formatter(34022)

      expect(nSpy).toHaveBeenCalledTimes(3)
      nSpy.mock.calls.forEach((call) => expect(call).toEqual([34022]))
    })

    it('recomputes when the active locale changes, so vue3-apexcharts redraws the chart', async () => {
      i18n.global.locale = 'en'
      const wrapper = mountComponent()
      await flushPromises()

      const optionsBefore = wrapper.vm.options
      i18n.global.locale = 'de'
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.options).not.toBe(optionsBefore)
    })
  })

  describe('computed: monthlyItems / monthlyItemsWithTotal', () => {
    it('groups monthly metrics per month, newest first, formatting sums via i18n', async () => {
      const wrapper = mountComponent()
      await flushPromises()

      const items = wrapper.vm.monthlyItems
      expect(items.map((i) => i.month)).toEqual([
        'August 2026',
        'July 2026',
        'September 2025',
        'August 2025',
      ])
      expect(items[0]).toMatchObject({
        index: 1,
        month: 'August 2026',
        'process-instances': i18n.global.n(5),
        'decision-instances': i18n.global.n(8),
        'task-users': i18n.global.n(1),
      })
    })

    it('appends a total row summing the raw sums across all months', async () => {
      const wrapper = mountComponent()
      await flushPromises()

      const withTotal = wrapper.vm.monthlyItemsWithTotal

      const total = withTotal.at(-1)
      expect(total.index).toBe(14)
      expect(total.month).toBe(i18n.global.t('admin.system.execution-metrics.total'))
      expect(total['process-instances']).toBe(7)
      expect(total['decision-instances']).toBe(12)
      expect(total['task-users']).toBe(1)
    })

    it('returns an empty array when there is no monthly data', async () => {
      SystemService.getMetricsData.mockImplementation((params) =>
        Promise.resolve(params.groupBy === 'year' ? ANNUAL_DATA : [])
      )
      const wrapper = mountComponent()
      await flushPromises()

      expect(wrapper.vm.monthlyItemsWithTotal).toEqual([])
    })
  })

  describe('computed: yearlyItems', () => {
    it('groups annual metrics per year, newest first, with translated date ranges', async () => {
      const wrapper = mountComponent()
      await flushPromises()

      const items = wrapper.vm.yearlyItems
      expect(items).toHaveLength(2)

      const currentYear = new Date().getFullYear()
      const currentYearEntry = items.find((i) =>
        i.year === i18n.global.t('admin.system.execution-metrics.fromUpToToday', {
          from: moment().year(currentYear).startOf('year').format('L')
        })
      )
      expect(currentYearEntry).toMatchObject({
        'process-instances': i18n.global.n(7),
        'decision-instances': i18n.global.n(12),
        'task-users': i18n.global.n(1),
      })

      const pastYear = currentYear - 1
      const startOfPastYear = moment().year(pastYear).startOf('year')
      const pastYearEntry = items.find((i) =>
        i.year === i18n.global.t('admin.system.execution-metrics.fromTo', {
          from: startOfPastYear.format('L'),
          to: moment(startOfPastYear).add(1, 'years').format('L')
        })
      )
      expect(pastYearEntry).toMatchObject({
        'process-instances': i18n.global.n(0),
        'decision-instances': i18n.global.n(0),
        'task-users': i18n.global.n(0),
      })

      expect(items[0]).toBe(currentYearEntry)
      expect(items[1]).toBe(pastYearEntry)
    })
  })

  describe('computed: monthlyFields / yearlyFields', () => {
    it('defines the monthly table columns', async () => {
      const wrapper = mountComponent()
      const keys = wrapper.vm.monthlyFields.map((f) => f.key)
      expect(keys).toEqual(['month', 'process-instances', 'decision-instances', 'task-users'])
    })

    it('defines the yearly table columns including the actions column', async () => {
      const wrapper = mountComponent()
      const keys = wrapper.vm.yearlyFields.map((f) => f.key)
      expect(keys).toEqual([
        'year',
        'process-instances',
        'decision-instances',
        'task-users',
        'actions',
      ])
    })
  })

  describe('onMonthlyRowClick', () => {
    it('selects the matching data point across all series on first click', async () => {
      const wrapper = mountComponent()
      await flushPromises()

      wrapper.vm.onMonthlyRowClick({ month: 'July 2026' })

      const dataPointIndex = wrapper.vm.labels.indexOf('July 2026')
      expect(toggleDataPointSelection).toHaveBeenCalledTimes(wrapper.vm.series.length)
      wrapper.vm.series.forEach((_, seriesIndex) => {
        expect(toggleDataPointSelection).toHaveBeenNthCalledWith(
          seriesIndex + 1,
          seriesIndex,
          dataPointIndex
        )
      })
      expect(wrapper.vm.selectedMonthIndex).toBe(dataPointIndex)
    })

    it('deselects the data point when the same row is clicked again', async () => {
      const wrapper = mountComponent()
      await flushPromises()

      wrapper.vm.onMonthlyRowClick({ month: 'July 2026' })
      toggleDataPointSelection.mockClear()

      wrapper.vm.onMonthlyRowClick({ month: 'July 2026' })

      expect(toggleDataPointSelection).toHaveBeenCalledTimes(wrapper.vm.series.length)
      expect(wrapper.vm.selectedMonthIndex).toBeNull()
    })

    it('switches selection, first clearing the previous month, then selecting the new one', async () => {
      const wrapper = mountComponent()
      await flushPromises()

      wrapper.vm.onMonthlyRowClick({ month: 'July 2026' })
      toggleDataPointSelection.mockClear()

      wrapper.vm.onMonthlyRowClick({ month: 'August 2026' })

      const previousIndex = wrapper.vm.labels.indexOf('July 2026')
      const newIndex = wrapper.vm.labels.indexOf('August 2026')
      const seriesCount = wrapper.vm.series.length
      // first pass clears the previously-selected month, second pass selects the new one
      expect(toggleDataPointSelection).toHaveBeenCalledTimes(seriesCount * 2)
      for (let s = 0; s < seriesCount; s++) {
        expect(toggleDataPointSelection).toHaveBeenNthCalledWith(s + 1, s, previousIndex)
      }
      for (let s = 0; s < seriesCount; s++) {
        expect(toggleDataPointSelection).toHaveBeenNthCalledWith(seriesCount + s + 1, s, newIndex)
      }
      expect(wrapper.vm.selectedMonthIndex).toBe(newIndex)
    })

    it('does nothing when the clicked row has no matching chart label', async () => {
      const wrapper = mountComponent()
      await flushPromises()

      wrapper.vm.onMonthlyRowClick({ month: 'Total' })

      expect(toggleDataPointSelection).not.toHaveBeenCalled()
      expect(wrapper.vm.selectedMonthIndex).toBeNull()
    })

    it('is wired to the monthly FlowTable click event', async () => {
      const wrapper = mountComponent()
      await flushPromises()

      const monthlyTable = wrapper.findAllComponents(FlowTableStub)[0]
      await monthlyTable.find('tr.row-stub').trigger('click')

      expect(toggleDataPointSelection).toHaveBeenCalled()
    })
  })

  describe('copyAnnualValueToClipboard', () => {
    const item = {
      year: 'January 1, 2026 up to today',
      'process-instances': '7',
      'decision-instances': '12',
      'task-users': '1',
    }

    it('fetches telemetry data once and copies a summary including it', async () => {
      const wrapper = mountComponent()
      await flushPromises()

      await wrapper.vm.copyAnnualValueToClipboard(item)

      expect(SystemService.getTelemetryData).toHaveBeenCalledTimes(1)
      expect(wrapper.vm.diagnostics).toEqual(TELEMETRY_DATA)
      expect(document.execCommand).toHaveBeenCalledWith('copy')
      expect(successAlertShow).toHaveBeenCalled()

      const textarea = document.querySelector('textarea')
      expect(textarea).toBeNull() // removed from the DOM again after copying
    })

    it('reuses cached diagnostics on subsequent calls instead of refetching', async () => {
      const wrapper = mountComponent()
      await flushPromises()

      await wrapper.vm.copyAnnualValueToClipboard(item)
      await wrapper.vm.copyAnnualValueToClipboard(item)

      expect(SystemService.getTelemetryData).toHaveBeenCalledTimes(1)
    })

    it('builds the copied text from the year and the three tracked metrics', async () => {
      const wrapper = mountComponent()
      await flushPromises()
      const copySpy = vi.spyOn(wrapper.vm, 'copyValueToClipboard')

      await wrapper.vm.copyAnnualValueToClipboard(item)

      expect(copySpy).toHaveBeenCalledTimes(1)
      const copiedText = copySpy.mock.calls[0][0]
      expect(copiedText).toContain(item.year)
      expect(copiedText).toContain('- PI: 7')
      expect(copiedText).toContain('- DI: 12')
      expect(copiedText).toContain('- TU: 1')
      expect(copiedText).toContain(JSON.stringify(TELEMETRY_DATA, null, 2))
    })

    it('omits the diagnostics block when the backend returns none', async () => {
      SystemService.getTelemetryData.mockResolvedValue(null)
      const wrapper = mountComponent()
      await flushPromises()
      const copySpy = vi.spyOn(wrapper.vm, 'copyValueToClipboard')

      await wrapper.vm.copyAnnualValueToClipboard(item)

      const copiedText = copySpy.mock.calls[0][0]
      expect(copiedText).toBe(
        [item.year, '- PI: 7', '- DI: 12', '- TU: 1', '', ''].join('\n')
      )
    })

    it('is wired to the yearly FlowTable actions column', async () => {
      const wrapper = mountComponent()
      await flushPromises()

      const yearlyTable = wrapper.findAllComponents(FlowTableStub)[1]
      await yearlyTable.find('button').trigger('click')
      await flushPromises()

      expect(SystemService.getTelemetryData).toHaveBeenCalledTimes(1)
      expect(successAlertShow).toHaveBeenCalled()
    })
  })
})
