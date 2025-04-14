import { mount } from '@vue/test-utils';
import { describe, it, expect } from 'vitest'
import { i18n } from '@/i18n'
import Piechart from '@/components/processes/dashboard/PieChart.vue'

describe('Piechart', () => {
  it('init', () => {
    const wrapper = mount(Piechart, {
      global: {
        stubs: {
          'router-link': true  // or use a custom stub if needed
        },
        directives: {
          'b-popover': {}
        },
        plugins: [i18n] // Use the i18n instance in the global config
      },
    });
    expect(wrapper.text()).toContain('');
  });

  describe('shadeColor', () => {
    const wrapper = mount(Piechart, {
      global: {
        stubs: {
          'router-link': true  // or use a custom stub if needed
        },
        directives: {
          'b-popover': {}
        },
        plugins: [i18n] // Use the i18n instance in the global config
      },
    });
    const shadeColor = wrapper.vm.shadeColor

    it('lightens a color when percent is positive', () => {
      expect(shadeColor('#336699', 20)).toBe('#3e7bb8')
      expect(shadeColor('#abcdef', 20)).toBe('#cef6ff')
    })

    it('darkens a color when percent is negative', () => {
      expect(shadeColor('#336699', -20)).toBe('#29527b')
      expect(shadeColor('#abcdef', -20)).toBe('#89a4c0')
    })

    it('returns same color when percent is 0', () => {
      expect(shadeColor('#000000', 0)).toBe('#000000')
      expect(shadeColor('#336699', 0)).toBe('#336699')
      expect(shadeColor('#abcdef', 0)).toBe('#abcdef')
      expect(shadeColor('#ffffff', 0)).toBe('#ffffff')
    })

    it('clamps color values to 0 when result would be below black', () => {
      expect(shadeColor('#000000', -50)).toBe('#000000')
      expect(shadeColor('#010101', -100)).toBe('#000000')
    });

    it('clamps color values to 255 when result would be above white', () => {
      expect(shadeColor('#ffffff', 50)).toBe('#ffffff')
      expect(shadeColor('#fefefe', 10)).toBe('#ffffff')
    });

    it('pads single hex digits correctly (output always 6 characters)', () => {
      expect(shadeColor('#010203', 0)).toBe('#010203')
      expect(shadeColor('#0a0b0c', 0)).toMatch(/^#[0-9a-f]{6}$/)
    });

    it('works with uppercase hex input', () => {
      expect(shadeColor('#ABCDEF', 20)).toBe('#cef6ff')
      expect(shadeColor('#ABCDEF', -20)).toBe('#89a4c0')
    });

    it('works without leading #', () => {
      expect(shadeColor('336699', 20)).toBe('#3e7bb8')
      expect(shadeColor('ABCDEF', -20)).toBe('#89a4c0')
    });

    it('returns black when input is black and negative percent', () => {
      expect(shadeColor('#000000', -100)).toBe('#000000')
    });

    it('returns white when input is white and positive percent', () => {
      expect(shadeColor('#ffffff', 100)).toBe('#ffffff')
    });
  })

})
