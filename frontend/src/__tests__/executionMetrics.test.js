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
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { i18n } from '@/i18n'
import { SystemService } from '@/services.js'
import ExecutionMetrics from '@/components/system/ExecutionMetrics.vue'

vi.mock('@/services.js', () => ({
  SystemService: {
    getMetricsData: vi.fn()
  }
}))

describe('ExecutionMetrics', () => {
  beforeEach(() => {
    SystemService.getMetricsData.mockResolvedValue([
      { subscriptionYear: 2026, subscriptionMonth: 7, metric: 'process-instances', sum: 34022 },
    ])
  })

  it('delegates chart data label, y-axis label and tooltip formatting to i18n.global.n so any locale is respected', async () => {
    const wrapper = mount(ExecutionMetrics, {
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
    const nSpy = vi.spyOn(i18n.global, 'n')

    expect(options.dataLabels.formatter(0)).toBe('')
    expect(nSpy).not.toHaveBeenCalled()

    options.dataLabels.formatter(34022)
    options.yaxis.labels.formatter(34022)
    options.tooltip.y.formatter(34022)

    expect(nSpy).toHaveBeenCalledTimes(3)
    nSpy.mock.calls.forEach((call) => expect(call).toEqual([34022]))
  })
})
