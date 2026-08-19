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
import { moment } from '@/globals'
import { SystemService } from '@/services.js'
import ExecutionMetrics from '@/components/system/ExecutionMetrics.vue'

vi.mock('@/services.js', () => ({
  SystemService: {
    getMetricsData: vi.fn()
  }
}))

describe('ExecutionMetrics', () => {
  let wrapper

  beforeEach(() => {
    SystemService.getMetricsData.mockResolvedValue([
      { subscriptionYear: 2026, subscriptionMonth: 7, metric: 'process-instances', sum: 34022 },
    ])
  })

  afterEach(() => {
    wrapper?.unmount()
    moment.locale('en')
  })

  it('formats chart data labels, y-axis labels and tooltip values using the active locale', async () => {
    i18n.global.locale = 'en'

    wrapper = mount(ExecutionMetrics, {
      global: {
        stubs: {
          'apexchart': true,
          'FlowTable': true,
          'BWaitingBox': true,
        },
        plugins: [i18n],
      },
    })

    await flushPromises()

    const { options } = wrapper.vm

    expect(options.dataLabels.formatter(0)).toBe('')
    expect(options.dataLabels.formatter(34022)).toBe('34,022')
    expect(options.yaxis.labels.formatter(34022)).toBe('34,022')
    expect(options.tooltip.y.formatter(34022)).toBe('34,022')
  })

  it('recomputes chart options when the active locale changes, so vue3-apexcharts redraws it', async () => {
    i18n.global.locale = 'en'

    wrapper = mount(ExecutionMetrics, {
      global: {
        stubs: {
          'apexchart': true,
          'FlowTable': true,
          'BWaitingBox': true,
        },
        plugins: [i18n],
      },
    })

    await flushPromises()

    const optionsBefore = wrapper.vm.options
    i18n.global.locale = 'de'
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.options).not.toBe(optionsBefore)
  })

  it('keeps chart series matched to their months when moment.locale() changes independently of Vue', async () => {
    i18n.global.locale = 'en'
    moment.locale('en')

    wrapper = mount(ExecutionMetrics, {
      global: {
        stubs: {
          'apexchart': true,
          'FlowTable': true,
          'BWaitingBox': true,
        },
        plugins: [i18n],
      },
    })

    await flushPromises()

    expect(wrapper.vm.series[0].data).toContain(34022)

    i18n.global.locale = 'de'
    moment.locale('de')
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.series[0].data).toContain(34022)
  })
})
