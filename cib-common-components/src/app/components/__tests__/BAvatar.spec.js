import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '@/app/i18n'
import { BAvatar } from '@/library'

describe('BAvatar', () => {
  it('text', () => {
    const wrapper = mount(BAvatar, {
      global: {
        plugins: [i18n] // Use the i18n instance in the global config
      },
      props: { text: 'Name' }
    })
    expect(wrapper.text()).toContain('NA')
  })
})
