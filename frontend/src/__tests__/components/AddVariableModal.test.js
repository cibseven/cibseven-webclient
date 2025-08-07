import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { i18n } from '@/i18n'
import AddVariableModal from '@/components/process/modals/AddVariableModal.vue'
import { readFileSync } from 'fs'
import { resolve } from 'path'
import { BButton, BFormGroup, BFormInput, BFormSelect, BFormCheckbox, BAlert, BFormDatepicker } from 'cib-common-components'

// Mock dependencies
vi.mock('@/services.js', () => ({
  ProcessService: {
    putLocalExecutionVariable: vi.fn(() => Promise.resolve())
  }
}))

describe('AddVariableModal.vue UI interactions', () => {
  let wrapper

  const translations = {...JSON.parse(
      // eslint-disable-next-line no-undef
      readFileSync(resolve(__dirname, '../../assets/translations_en.json'), 'utf-8')
    ),
    ...{
      bcomponents: {
        ariaLabelClose: 'Close',
      },
    }
  }

  beforeEach(() => {

    i18n.global.locale = 'en'
    i18n.global.setLocaleMessage('en', translations)

    wrapper = mount(AddVariableModal, {
      props: {
          selectedInstance: { id: 100 }
      },
      global: {
        provide: {
          // Provide any necessary global properties or mocks
        },
        mocks: {
          $t: (msg) => msg,
          $te: () => true,
        },
        plugins: [i18n],
        stubs: {
          'b-modal': {
            template: '<div><slot></slot></div>',
            methods: {
              show: vi.fn(),
              hide: vi.fn(),
            }
          },
          'b-form-group': BFormGroup,
          'b-form-input': BFormInput,
          'b-form-select': BFormSelect,
          'b-form-checkbox': BFormCheckbox,
          'b-form-datepicker': BFormDatepicker,
          'b-button': BButton,
          'b-alert': BAlert,
        }
      }
    })
  })

  async function changeType(newType) {
    const select = wrapper.find('select')
    await select.setValue(newType)    
  }

  async function setValue(newValue) {
    await wrapper.setData({ value: newValue })
  }

  describe('change type', () => {
    it('resets form on hide', () => {
      wrapper.setData({ name: 'test', type: 'Object', value: 'val', objectTypeName: 'obj', serializationDataFormat: 'format' })
      wrapper.vm.reset()
      expect(wrapper.vm.name).toBe('')
      expect(wrapper.vm.type).toBe('String')
      expect(wrapper.vm.value).toBe('')
      expect(wrapper.vm.objectTypeName).toBe('')
      expect(wrapper.vm.serializationDataFormat).toBe('')
    })

    it('changing type to Object shows objectTypeName and serializationDataFormat inputs', async () => {
      expect(wrapper.findAll('input').length).toBe(1)
      expect(wrapper.findAll('textarea').length).toBe(1)

      // Select Object type
      await changeType('Object')
      expect(wrapper.findAll('input').length).toBe(3)
      expect(wrapper.findAll('textarea').length).toBe(1)
    })

    it('changing type to Boolean updates value and displays switch', async () => {
      await changeType('Boolean')
      expect(wrapper.vm.value).toBe(true)

      // Simulate toggling checkbox
      await setValue(false)
      expect(wrapper.vm.value).toBe(false)

      await changeType('Long')
      expect(wrapper.vm.value).toBe(0)

      await changeType('Boolean')
      expect(wrapper.vm.value).toBe(true)
      await setValue(true)
      await changeType('Long')
      expect(wrapper.vm.value).toBe(1)
    })

    it('changing type to Json sets default value', async () => {
      await changeType('Json')
      expect(wrapper.vm.value).toBe('{}')
    })

    it('changing type to String does not reset value to empty string', async () => {
      await setValue('some text')
      await changeType('String')
      expect(wrapper.vm.value).toBe('some text')
    })

    it('selects different types and updates value accordingly', async () => {
      const typesToTest = ['Boolean', 'Object', 'Json', 'String']
      for (const t of typesToTest) {
        await wrapper.setData({ type: t, value: t === 'Boolean' ? false : 'test' })
        await wrapper.vm.$nextTick()
      }
      expect(wrapper.vm.type).toBe('String') // default reset
    })

    it('chnage type from String to Long preservs number value', async () => {
      await changeType('String')
      await setValue('100')
      expect(wrapper.vm.value).toBe('100')
      await changeType('Long')
      expect(wrapper.vm.value).toBe(100)
      await changeType('String')
      expect(wrapper.vm.value).toBe('100')
    })
  })

  describe('type selection', () => {
    it('selects different options from the type select', async () => {
      await changeType('Object')
      expect(wrapper.vm.type).toBe('Object')
    })
  })

  describe('validate value', () => {
    it('validates number input and shows validation error', async () => {
      await wrapper.setData({ type: 'Integer' })
      await wrapper.setData({ value: 'notANumber' })
      expect(wrapper.vm.valueValidationError).toBe('isNan')
      await wrapper.setData({ value: 5000000000 }) // out of range
      expect(wrapper.vm.valueValidationError).toBe('Out of range: -2147483648 ... 2147483647')
      wrapper.vm.type = 'Long'
      expect(wrapper.vm.valueValidationError).toBeNull()
    })

    it('validates JSON input and shows error for invalid JSON', async () => {
      await wrapper.setData({ type: 'Json' })
      await wrapper.setData({ value: '{ invalid json' })
      expect(wrapper.vm.valueValidationError).toBeTruthy()

      await wrapper.setData({ value: '{ "a": "b"}' })
      expect(wrapper.vm.valueValidationError).toBeNull()
    })

    it('validates XML input and shows error for invalid XML', async () => {
      await wrapper.setData({ type: 'Xml', value: '<invalid></xml>' })
      expect(wrapper.vm.valueValidationError).toBeTruthy()
    })
  })

  describe('buttons', () => {
    it.each([
      [{ name: '', type: 'String', value: '' }],
      [{ name: '', type: 'Json', value: '' }],
      [{ name: '', type: 'Xml', value: '' }],
      [{ name: '', type: 'Object', value: '' }],

      [{ name: 'name', type: 'Xml', value: '' }],
      [{ name: 'name', type: 'Object', value: '' }],

      [{ name: 'name', type: 'Short', value: -32768-1 }],
      [{ name: 'name', type: 'Integer', value: -2147483648-1 }],
      [{ name: 'name', type: 'Long', value: -Number.MAX_SAFE_INTEGER-1 }],

      [{ name: 'name', type: 'Short', value: 32767+1 }],
      [{ name: 'name', type: 'Integer', value: 2147483647+1 }],
      [{ name: 'name', type: 'Long', value: Number.MAX_SAFE_INTEGER+1 }],

    ])('isValid=false: %s', async (data) => {
      await wrapper.setData(data)
      expect(wrapper.vm.isValid).toBe(false)
    })

    it.each([
      [{ name: 'name', type: 'String', value: '' }],
      [{ name: 'name', type: 'Json', value: '' }],
      [{ name: 'name', type: 'Object', value: '', objectTypeName: 'x', serializationDataFormat: 'x' }],

      [{ name: 'name', type: 'String', value: 'text' }],
      [{ name: 'name', type: 'Json', value: '' }],
      [{ name: 'name', type: 'Json', value: '{}' }],
      //[{ name: 'name', type: 'Xml', value: '<b>vvv</b>' }],
      [{ name: 'name', type: 'Object', value: 'some data', objectTypeName: 'x', serializationDataFormat: 'x' }],

      [{ name: 'name', type: 'Short', value: 0 }],
      [{ name: 'name', type: 'Integer', value: 0 }],
      [{ name: 'name', type: 'Long', value: 0 }],
      [{ name: 'name', type: 'Double', value: 0 }],

      [{ name: 'name', type: 'Short', value: -100 }],
      [{ name: 'name', type: 'Integer', value: -100 }],
      [{ name: 'name', type: 'Long', value: -100 }],
      [{ name: 'name', type: 'Double', value: -100 }],

      [{ name: 'name', type: 'Short', value: 100 }],
      [{ name: 'name', type: 'Integer', value: 100 }],
      [{ name: 'name', type: 'Long', value: 100 }],
      [{ name: 'name', type: 'Double', value: 100 }],

      [{ name: 'name', type: 'Short', value: -32768 }],
      [{ name: 'name', type: 'Integer', value: -2147483648 }],
      [{ name: 'name', type: 'Long', value: -Number.MAX_SAFE_INTEGER }],
      [{ name: 'name', type: 'Double', value: -Number.MAX_SAFE_INTEGER - 100 }],

      [{ name: 'name', type: 'Short', value: 32767 }],
      [{ name: 'name', type: 'Integer', value: 2147483647 }],
      [{ name: 'name', type: 'Long', value: Number.MAX_SAFE_INTEGER }],
      [{ name: 'name', type: 'Double', value: Number.MAX_SAFE_INTEGER + 100 }],

    ])('isValid=true: %s', async (data) => {
      await wrapper.setData(data)
      expect(wrapper.vm.valueValidationError).toBeNull()
      expect(wrapper.vm.isValid).toBe(true)
    })

  })
})
