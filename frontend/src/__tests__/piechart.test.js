import { mount } from '@vue/test-utils';
import { describe, it, expect, vi } from 'vitest';
import { i18n } from '@/i18n';
import PieChart from '@/components/processes/dashboard/PieChart.vue';

describe('PieChart', () => {
  it('renders loading state when items are not provided', () => {
    const wrapper = mount(PieChart, {
      props: {
        items: null,
      },
      global: {
        stubs: {
          'router-link': true,
          'b-waiting-box': true,
        },
        plugins: [i18n],
      },
    });

    expect(wrapper.find('.waiting-box-container').exists()).toBe(true);
    expect(wrapper.find('apexchart').exists()).toBe(false);
  });

  it('renders chart when items are provided', () => {
    const items = [
      { id: 1, title: 'Item 1', value: 10 },
      { id: 2, title: 'Item 2', value: 20 },
    ];

    const wrapper = mount(PieChart, {
      props: {
        items,
      },
      global: {
        stubs: {
          'router-link': true,
          'b-waiting-box': true,
        },
        plugins: [i18n],
      },
    });

    expect(wrapper.find('.waiting-box-container').exists()).toBe(false);
    expect(wrapper.find('apexchart').exists()).toBe(true);
    expect(wrapper.vm.values).toEqual([20, 10]); // Sorted values
    expect(wrapper.vm.labels).toEqual(['Item 2', 'Item 1']); // Sorted labels
  });

  it('handles chart click event correctly', async () => {
    const items = [
      { id: 1, title: 'Item 1', value: 10 },
      { id: 2, title: 'Item 2', value: 20 },
    ];

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
        },
        plugins: [i18n],
      },
    });

    const chartOptions = wrapper.vm.chartOptions;
    const clickEvent = { target: {} };
    const chartContext = {};
    const config = { dataPointIndex: 1 }; // Simulate clicking on the second item

    chartOptions.chart.events.click(clickEvent, chartContext, config);

    expect(mockRouterPush).toHaveBeenCalledWith('/seven/auth/process/1');
  });

  it('applies correct cursor style on dataPointMouseEnter', () => {
    const items = [
      { id: 1, title: 'Item 1', value: 10 },
      { id: 2, title: 'Item 2', value: 20 },
    ];

    const wrapper = mount(PieChart, {
      props: {
        items,
      },
      global: {
        stubs: {
          'router-link': true,
          'b-waiting-box': true,
        },
        plugins: [i18n],
      },
    });

    const chartOptions = wrapper.vm.chartOptions;
    const mockEvent = { target: { style: {} } };

    chartOptions.chart.events.dataPointMouseEnter(mockEvent);

    expect(mockEvent.target.style.cursor).toBe('pointer');
  });
});