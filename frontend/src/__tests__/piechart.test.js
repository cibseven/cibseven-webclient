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
import { mount } from '@vue/test-utils'
import { describe, it, expect, vi } from 'vitest'
import { i18n } from '@/i18n'
import PieChart from '@/components/processes/dashboard/PieChart.vue'

describe('PieChart', () => {
  it('renders loading state when items are not provided', () => {
    const wrapper = mount(PieChart, {
      props: {
        items: [],
        loading: true
      },
      global: {
        stubs: {
          'router-link': true,
          'b-waiting-box': true,
          'apexchart': true
        },
        plugins: [i18n],
      },
    })

    expect(wrapper.find('.waiting-box-container').exists()).toBe(true)
    expect(wrapper.find('.apex-container').exists()).toBe(false)
  })

  it('renders chart when items are provided', () => {
    const items = [
      { id: 1, title: 'Item 1', value: 10 },
      { id: 2, title: 'Item 2', value: 20 },
    ]

    const wrapper = mount(PieChart, {
      props: {
        items,
      },
      global: {
        stubs: {
          'router-link': true,
          'b-waiting-box': true,
          'apexchart': true
        },
        plugins: [i18n],
      },
    })

    expect(wrapper.find('.waiting-box-container').exists()).toBe(false);
    expect(wrapper.find('.apex-container').exists()).toBe(true);
    expect(wrapper.vm.values).toEqual([20, 10]); // Sorted values
    expect(wrapper.vm.labels).toEqual(['Item 2', 'Item 1']); // Sorted labels
  })

  it('handles chart click event correctly', async () => {
    const items = [
      { id: 1, title: 'Item 1', value: 10 },
      { id: 2, title: 'Item 2', value: 20 },
    ]

    const mockRouterPush = vi.fn();
    const wrapper = mount(PieChart, {
      props: {
        items,
      },
      global: {
        mocks: {
          $router: {
            push: mockRouterPush,
          },
        },
        stubs: {
          'router-link': true,
          'b-waiting-box': true,
          'apexchart': true
        },
        plugins: [i18n],
      },
    })

    const chartOptions = wrapper.vm.chartOptions
    const clickEvent = { target: {} }
    const chartContext = {}
    const config = { dataPointIndex: 1 } // Simulate clicking on the second item

    chartOptions.chart.events.click(clickEvent, chartContext, config)

    expect(wrapper.emitted('click')).toBeTruthy()
    const emittedArgs = wrapper.emitted('click')[0][0]
    expect(emittedArgs.item.id).toBe(1)
  })

  it('applies correct cursor style on dataPointMouseEnter', () => {
    const items = [
      { id: 1, title: 'Item 1', value: 10 },
      { id: 2, title: 'Item 2', value: 20 },
    ]

    const wrapper = mount(PieChart, {
      props: {
        items,
      },
      global: {
        stubs: {
          'router-link': true,
          'b-waiting-box': true,
          'apexchart': true
        },
        plugins: [i18n],
      },
    })

    const chartOptions = wrapper.vm.chartOptions
    const mockEvent = { target: { style: {} } }

    chartOptions.chart.events.dataPointMouseEnter(mockEvent)

    expect(mockEvent.target.style.cursor).toBe('pointer')
  })
})