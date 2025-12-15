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
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { i18n } from '@/i18n'
import AddVariableModalUI from '@/components/process/modals/AddVariableModalUI.vue'
import { readFileSync } from 'fs'
import { resolve } from 'path'
import { BButton, BFormGroup, BFormInput, BFormSelect, BFormCheckbox, BAlert } from '@cib/common-frontend'

// Mock dependencies
vi.mock('@/services.js', () => ({
  ProcessService: {
    putLocalExecutionVariable: vi.fn(() => Promise.resolve())
  }
}))

describe('AddVariableModal.vue UI interactions', () => {
  let wrapper

  function createWrapper(props = {}) {
    return mount(AddVariableModalUI, {
      props: props,
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
          'b-form-datepicker': true,
          'b-button': BButton,
          'b-alert': BAlert,
          'b-tab': { template: '<div><slot></slot></div>' },
          'b-tabs': { template: '<div><slot></slot></div>' },
        }
      }
    })
  }

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

    wrapper = createWrapper({})
  })

  async function setData(data) {
    await wrapper.vm.setData(data)
  }

  async function changeType(newType) {
    const select = wrapper.find('select')
    await select.setValue(newType)
  }

  async function setValue(newValue) {
    await wrapper.setData({ value: newValue })
  }

  describe('editMode', () => {
    describe('editMode=true', () => {
      it('allowEditName=undefined', async () => {
        wrapper = createWrapper({ editMode: true })
        expect(wrapper.vm.editMode).toBe(true)
        expect(wrapper.vm.computedAllowEditName).toBe(false)
        expect(wrapper.find('input').attributes('disabled')).toBeDefined()
      })

      it('allowEditName=false', async () => {
        wrapper = createWrapper({ editMode: true, allowEditName: false })
        expect(wrapper.vm.editMode).toBe(true)
        expect(wrapper.vm.computedAllowEditName).toBe(false)
        expect(wrapper.find('input').attributes('disabled')).toBeDefined()
      })

      it('allowEditName=true', async () => {
        wrapper = createWrapper({ editMode: true, allowEditName: true })
        expect(wrapper.vm.editMode).toBe(true)
        expect(wrapper.vm.computedAllowEditName).toBe(true)
        expect(wrapper.find('input').attributes('disabled')).toBeUndefined()
      })
    })

    describe('editMode=false', () => {
      it('allowEditName=undefined', async () => {
        wrapper = createWrapper({ editMode: false })
        expect(wrapper.vm.editMode).toBe(false)
        expect(wrapper.vm.computedAllowEditName).toBe(true)
        expect(wrapper.find('input').attributes('disabled')).toBeUndefined()
      })

      it('allowEditName=false', async () => {
        wrapper = createWrapper({ editMode: false, allowEditName: false })
        expect(wrapper.vm.editMode).toBe(false)
        expect(wrapper.vm.computedAllowEditName).toBe(true)
        expect(wrapper.find('input').attributes('disabled')).toBeUndefined()
      })

      it('allowEditName=true', async () => {
        wrapper = createWrapper({ editMode: false, allowEditName: true })
        expect(wrapper.vm.editMode).toBe(false)
        expect(wrapper.vm.computedAllowEditName).toBe(true)
        expect(wrapper.find('input').attributes('disabled')).toBeUndefined()
      })
    })
  })

  describe('change type', () => {
    it('resets form on hide', () => {
      setData({ name: 'test', type: 'Object', value: 'val', objectTypeName: 'obj', serializationDataFormat: 'format' })
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

    it('compress Json value', async () => {
      await changeType('Json')
      expect(wrapper.vm.value).toBe('{}')
      await setValue('{  "a":   "b"\n  }')

      await wrapper.vm.onSubmit()

      expect(wrapper.emitted('add-variable')).toBeTruthy()
      const args = wrapper.emitted('add-variable')[0][0]
      expect(args.value).toContain('{"a":"b"}')
    })

    it('changing type to String does not reset value to empty string', async () => {
      await setValue('some text')
      await changeType('String')
      expect(wrapper.vm.value).toBe('some text')
    })

    it('selects different types and updates value accordingly', async () => {
      const typesToTest = ['Boolean', 'Object', 'Json', 'String']
      for (const t of typesToTest) {
        await setData({ type: t, value: t === 'Boolean' ? false : 'test' })
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
      await setData({ type: 'Integer', value: 'notANumber' })
      expect(wrapper.vm.valueValidationError).toBe('Invalid number')
      await setValue(5000000000) // out of range
      expect(wrapper.vm.valueValidationError).toBe('Out of range: -2147483648 ... 2147483647')
      wrapper.vm.type = 'Long'
      expect(wrapper.vm.valueValidationError).toBeNull()
    })

    it('validates JSON input and shows error for invalid JSON', async () => {
      await setData({ type: 'Json', value: '{ invalid json' })
      expect(wrapper.vm.valueValidationError).toBeTruthy()

      await setValue('{ "a": "b"}')
      expect(wrapper.vm.valueValidationError).toBeNull()
    })

    it('validates XML input and shows error for invalid XML', async () => {
      await setData({ type: 'Xml', value: '<invalid></xml>' })
      expect(wrapper.vm.valueValidationError).not.toBeNull()
    })

    it('Null', async () => {
      await changeType('Null')
      expect(wrapper.vm.value).toBe(null)
    })

    it('Boolean', async () => {
      await changeType('Boolean')
      expect(wrapper.vm.value).toBe(true)
    })

  })

  describe('isSubmitDisabled', () => {
    it.each([
      [{ name: '', type: 'String', value: 'text' }, true],
      [{ name: 'name', type: 'String', value: 'text' }, false],
    ])('isSubmitDisabled(%s)', async (data, result) => {
      await setData(data)
      expect(wrapper.vm.isSubmitDisabled).toBe(result)
    })
  })

  describe('valueValidationError', () => {

    it('debug', async () => {
      await setData({ type: 'Date', value: '2025-09-25T00:00:01.000+0200' })
      expect(wrapper.vm.valueValidationError).toBeNull()
    })

    it.each([
      // Boolean with invalid values
      [{ type: 'Boolean', value: 0 }, false],
      [{ type: 'Boolean', value: 100 }, false],
      [{ type: 'Boolean', value: '' }, false],
      [{ type: 'Boolean', value: 'text' }, false],
      [{ type: 'Boolean', value: 'true' }, false],
      [{ type: 'Boolean', value: 'false' }, false],
      // Boolean with valid values
      [{ type: 'Boolean', value: null }, true],
      [{ type: 'Boolean', value: undefined }, true],
      [{ type: 'Boolean', value: true }, true],
      [{ type: 'Boolean', value: false }, true],

      // String with invalid values
      [{ type: 'String', value: 0 }, false],
      [{ type: 'String', value: true }, false],
      [{ type: 'String', value: false }, false],
      // String with valid values
      [{ type: 'String', value: null }, true],
      [{ type: 'String', value: undefined }, true],
      [{ type: 'String', value: '' }, true],
      [{ type: 'String', value: 'text' }, true],
      [{ type: 'String', value: 'true' }, true],
      [{ type: 'String', value: 'false' }, true],

      // Date with invalid values
      [{ type: 'Date', value: true }, false],
      [{ type: 'Date', value: false }, false],
      [{ type: 'Date', value: 'invalid-date' }, false],
      [{ type: 'Date', value: '2021-13-01' }, false],
      // Date with valid values
      [{ type: 'Date', value: null }, true],
      [{ type: 'Date', value: undefined }, true],
      [{ type: 'Date', value: '2021-12-31' }, true],
      [{ type: 'Date', value: '2025-09-25T00:00:01.000+0200' }, true],

      // Short with invalid values
      [{ type: 'Short', value: 'notANumber' }, false],
      [{ type: 'Short', value: 3.14 }, false],
      [{ type: 'Short', value: -32768-1 }, false],
      [{ type: 'Short', value: 32767+1 }, false],
      // Short with valid values
      [{ type: 'Short', value: null }, true],
      [{ type: 'Short', value: undefined }, true],
      [{ type: 'Short', value: -32768 }, true],
      [{ type: 'Short', value: -100 }, true],
      [{ type: 'Short', value: 0 }, true],
      [{ type: 'Short', value: 100 }, true],
      [{ type: 'Short', value: 32767 }, true],

      // Integer with invalid values
      [{ type: 'Integer', value: 'notANumber' }, false],
      [{ type: 'Integer', value: 3.14 }, false],
      [{ type: 'Integer', value: -2147483648-1 }, false],
      [{ type: 'Integer', value: 2147483647+1 }, false],
      // Integer with valid values
      [{ type: 'Integer', value: null }, true],
      [{ type: 'Integer', value: undefined }, true],
      [{ type: 'Integer', value: -2147483648 }, true],
      [{ type: 'Integer', value: -100 }, true],
      [{ type: 'Integer', value: 0 }, true],
      [{ type: 'Integer', value: 100 }, true],
      [{ type: 'Integer', value: 2147483647 }, true],

      // Long with invalid values
      [{ type: 'Long', value: 'notANumber' }, false],
      [{ type: 'Long', value: 3.14 }, false],
      [{ type: 'Long', value: -Number.MAX_SAFE_INTEGER-1 }, false],
      [{ type: 'Long', value: Number.MAX_SAFE_INTEGER+1 }, false],
      // Long with valid values
      [{ type: 'Long', value: null }, true],
      [{ type: 'Long', value: undefined }, true],
      [{ type: 'Long', value: -Number.MAX_SAFE_INTEGER }, true],
      [{ type: 'Long', value: -100 }, true],
      [{ type: 'Long', value: 0 }, true],
      [{ type: 'Long', value: 100 }, true],
      [{ type: 'Long', value: Number.MAX_SAFE_INTEGER }, true],

      // Double with invalid values
      [{ type: 'Double', value: 'notANumber' }, false],
      // Double with valid values
      [{ type: 'Double', value: null }, true],
      [{ type: 'Double', value: undefined }, true],
      [{ type: 'Double', value: -Number.MAX_SAFE_INTEGER-100 }, true],
      [{ type: 'Double', value: -10.99 }, true],
      [{ type: 'Double', value: 0 }, true],
      [{ type: 'Double', value: 10.99 }, true],
      [{ type: 'Double', value: Number.MAX_SAFE_INTEGER+100 }, true],

      // Json with invalid values
      [{ type: 'Json', value: '{ invalid json' }, false],
      [{ type: 'Json', value: 123 }, false],
      [{ type: 'Json', value: 123.45 }, false],
      [{ type: 'Json', value: true }, false],
      [{ type: 'Json', value: '0000' }, false], // invalid number
      [{ type: 'Json', value: 'some text' }, false], // invalid JSON
      [{ type: 'Json', value: '{ "a": "b" ' }, false], // invalid JSON
      [{ type: 'Json', value: '[1,2,3' }, false], // invalid JSON
      [{ type: 'Json', value: '{"a":1,,}' }, false], // invalid JSON
      [{ type: 'Json', value: '{"a":1 "b":2}' }, false], // invalid JSON
      [{ type: 'Json', value: '{"a":1,"b":2,,}' }, false], // invalid JSON
      [{ type: 'Json', value: '{"a":1,"b":2,' }, false], // invalid JSON
      [{ type: 'Json', value: 'nulla' }, false], // invalid JSON
      [{ type: 'Json', value: 'truee' }, false], // invalid JSON
      [{ type: 'Json', value: 'falsee' }, false], // invalid JSON
      [{ type: 'Json', value: '123a' }, false], // invalid JSON
      [{ type: 'Json', value: '12.34a' }, false], // invalid JSON
      [{ type: 'Json', value: '"unclosed string' }, false], // invalid JSON
      [{ type: 'Json', value: "'single quotes'" }, false], // invalid JSON
      // Json with valid values
      [{ type: 'Json', value: null }, true],
      [{ type: 'Json', value: undefined }, true],
      [{ type: 'Json', value: '' }, true],
      [{ type: 'Json', value: '{}' }, true],
      [{ type: 'Json', value: '[]' }, true],
      [{ type: 'Json', value: '123' }, true],
      [{ type: 'Json', value: '"a string"' }, true],
      [{ type: 'Json', value: 'true' }, true],
      [{ type: 'Json', value: 'false' }, true],
      [{ type: 'Json', value: '{"a":1,"b":2}' }, true],

      // Xml with invalid values
      [{ type: 'Xml', value: '<invalid></xml>' }, false],
      [{ type: 'Xml', value: 'Just a string' }, false],
      // Xml with valid values
      [{ type: 'Xml', value: null }, true],
      [{ type: 'Xml', value: undefined }, true],
      [{ type: 'Xml', value: '' }, true],
      [{ type: 'Xml', value: '<root></root>' }, true],
      [{ type: 'Xml', value: '<a><b>text</b></a>' }, true],

      // Object
      [{ type: 'Object', value: undefined }, true, false],
      [{ type: 'Object', value: null }, true, false],
      [{ type: 'Object', value: 'text' }, true, false],
      [{ type: 'Object', value: '' }, true, false],
      [{ type: 'Object', value: null, valueInfo: { objectTypeName: 'any', serializationDataFormat: 'any' } }, true, true],
      [{ type: 'Object', value: undefined, valueInfo: { objectTypeName: 'any', serializationDataFormat: 'any' } }, true, true],
      [{ type: 'Object', value: '', valueInfo: { objectTypeName: 'any', serializationDataFormat: 'any' } }, true, true],
      [{ type: 'Object', value: '{"json":"value"}', valueInfo: { objectTypeName: 'any', serializationDataFormat: 'any' } }, true, true],
      [{ type: 'Object', value: '{"json":"value"}', valueInfo: { objectTypeName: 'any', serializationDataFormat: 'application/json' } }, true, true],
      [{ type: 'Object', value: 'invalide json', valueInfo: { objectTypeName: 'any', serializationDataFormat: 'application/json' } }, false, false],
      [{ type: 'Object', value: 'some data', valueInfo: { objectTypeName: 'any', serializationDataFormat: 'any' } }, true, true],

    ])('valueValidationError(%s) = %s', async (data, result, validObjectTypes) => {
      await setData({
        ...data,
        name: 'name',
      })
      if (result) {
        expect(wrapper.vm.valueValidationError).toBeNull()
      }
      else {
        expect(wrapper.vm.valueValidationError).not.toBeNull()
      }

      if (validObjectTypes !== undefined) {
        expect(wrapper.vm.isSubmitDisabled).not.toEqual(validObjectTypes)
      }
    })
  })

  describe('change type', () => {

    it('chain', async () => {
      await setData({ type: 'String', value: 'text' })
      expect(wrapper.vm.value).toBe('text')

      await changeType('Boolean')
      expect(wrapper.vm.value).toBe(true)
      await changeType('Long')
      expect(wrapper.vm.value).toBe(1)
      await changeType('Double')
      expect(wrapper.vm.value).toBe(1.0)
      await changeType('Date')
      expect(wrapper.vm.value).toBe(wrapper.vm.currentDate())
      await changeType('Json')
      expect(wrapper.vm.value).toBe('{}')
      await changeType('Xml')
      expect(wrapper.vm.value).toBe('')
      await changeType('Object')
      expect(wrapper.vm.value).toBe('')
    })

    describe('from String', () => {
      describe('from String "text"', () => {
        it.each([
          ['Boolean', true],
          ['Long', 0],
          ['Double', 0.],
          ['Date', null], // will use currentDate()
          ['Json', '{}'],
          ['Xml', ''],
          ['Object', 'text'],
        ])('to %s', async (targetType, expectedValue) => {
          await setData({ type: 'String', value: 'text' })
          expect(wrapper.vm.value).toBe('text')
          await changeType(targetType)
          if (targetType === 'Date') {
            expect(wrapper.vm.value).toBe(wrapper.vm.currentDate())
          } else {
            expect(wrapper.vm.value).toBe(expectedValue)
          }
        })
      })

      describe('from String ""', () => {
        it.each([
          ['Boolean', true],
          ['Long', 0],
          ['Double', 0.],
          ['Date', null], // will use currentDate()
          ['Json', '{}'],
          ['Xml', ''],
          ['Object', ''],
        ])('to %s', async (targetType, expectedValue) => {
          await setData({ type: 'String', value: '' })
          expect(wrapper.vm.value).toBe('')
          await changeType(targetType)
          if (targetType === 'Date') {
            expect(wrapper.vm.value).toBe(wrapper.vm.currentDate())
          } else {
            expect(wrapper.vm.value).toBe(expectedValue)
          }
        })
      })

      describe('from String "10.50"', () => {
        it.each([
          ['Boolean', true],
          ['Long', 10],
          ['Double', 10.5],
          ['Date', null], // will use currentDate()
          ['Json', '10.5'],
          ['Xml', ''],
          ['Object', '10.5'],
        ])('to %s', async (targetType, expectedValue) => {
          await setData({ type: 'String', value: '10.50' })
          expect(wrapper.vm.value).toBe('10.50')
          await changeType(targetType)
          if (targetType === 'Date') {
            expect(wrapper.vm.value).toBe(wrapper.vm.currentDate())
          } else {
            expect(wrapper.vm.value).toBe(expectedValue)
          }
        })
      })

      describe('from String "{"a":"b"}"', () => {
        it.each([
          ['Boolean', true],
          ['Long', 0],
          ['Double', 0.0],
          ['Date', null], // will use currentDate()
          ['Json', '{\n  "a": "b"\n}'],
          ['Xml', ''],
          ['Object', '{\n  "a": "b"\n}'],
        ])('to %s', async (targetType, expectedValue) => {
          await setData({ type: 'String', value: '{"a":"b"}' })
          expect(wrapper.vm.value).toBe('{"a":"b"}')
          await changeType(targetType)
          if (targetType === 'Date') {
            expect(wrapper.vm.value).toBe(wrapper.vm.currentDate())
          } else {
            expect(wrapper.vm.value).toBe(expectedValue)
          }
        })
      })
    })

    describe('from Boolean', () => {

      describe('true', () => {
        beforeEach(async () => {
          await setData({ type: 'Boolean', value: true })
          expect(wrapper.vm.value).toBe(true)
        })

        it.each([
          ['Long', 1],
          ['Double', 1.0],
          ['String', ''],
          ['Date', null],
          ['Json', '{}'],
          ['Xml', ''],
          ['Object', '']
        ])('true to %s', async (targetType, expectedValue) => {
          await changeType(targetType)
          expect(wrapper.vm.value).toBe(expectedValue === null ? wrapper.vm.currentDate() : expectedValue)
        })
      })

      describe('false', () => {
        beforeEach(async () => {
          await setData({ type: 'Boolean', value: false })
          expect(wrapper.vm.value).toBe(false)
        })

        it.each([
          ['Long', 0],
          ['Double', 0.0],
          ['String', ''],
          ['Date', null],
          ['Json', '{}'],
          ['Xml', ''],
          ['Object', '']
        ])('false to %s', async (targetType, expectedValue) => {
          await changeType(targetType)
          expect(wrapper.vm.value).toBe(expectedValue === null ? wrapper.vm.currentDate() : expectedValue)
        })
      })

      describe('null', () => {
        beforeEach(async () => {
          await setData({ type: 'Boolean', value: null })
          expect(wrapper.vm.value).toBe(null)
        })

        it.each([
          ['Long', 0],
          ['Double', 0.0],
          ['String', ''],
          ['Date', null],
          ['Json', '{}'],
          ['Xml', ''],
          ['Object', '']
        ])('null to %s', async (targetType, expectedValue) => {
          await changeType(targetType)
          expect(wrapper.vm.value).toBe(expectedValue === null ? wrapper.vm.currentDate() : expectedValue)
        })
      })
    })

    describe('from Long', () => {

      describe('0', () => {
        beforeEach(async () => {
          await setData({ type: 'Long', value: 0 })
          expect(wrapper.vm.value).toBe(0)
        })

        it.each([
          ['Boolean', true],
          ['Double', 0.0],
          ['String', ''],
          ['Json', '{}'],
          ['Date', null],
          ['Xml', ''],
          ['Object', '']
        ])('0 to %s', async (targetType, expectedValue) => {
          await changeType(targetType)
          expect(wrapper.vm.value).toBe(expectedValue === null ? wrapper.vm.currentDate() : expectedValue)
        })
      })

      describe('100', () => {
        beforeEach(async () => {
          await setData({ type: 'Long', value: 100 })
          expect(wrapper.vm.value).toBe(100)
        })

        it.each([
          ['Boolean', true],
          ['Double', 100.0],
          ['String', '100'],
          ['Json', '{}'],
          ['Date', null],
          ['Xml', ''],
          ['Object', '100']
        ])('100 to %s', async (targetType, expectedValue) => {
          await changeType(targetType)
          expect(wrapper.vm.value).toBe(expectedValue === null ? wrapper.vm.currentDate() : expectedValue)
        })
      })
    })

    describe('from Double', () => {

      describe('0.0', () => {
        beforeEach(async () => {
          await setData({ type: 'Double', value: 0.0 })
          expect(wrapper.vm.value).toBe(0.0)
        })

        it.each([
          ['Boolean', true],
          ['Double', 0.0],
          ['String', ''],
          ['Json', '{}']
        ])('0 to %s', async (targetType, expectedValue) => {
          await changeType(targetType)
          expect(wrapper.vm.value).toBe(expectedValue)
        })
      })

      describe('100.50', () => {
        beforeEach(async () => {
          await setData({ type: 'Double', value: 100.50 })
          expect(wrapper.vm.value).toBe(100.50)
        })

        it.each([
          ['Boolean', true],
          ['Double', 100.50],
          ['String', '100.5'],
          ['Json', '{}'],
          ['Date', null],
          ['Long', 100],
          ['Xml', ''],
          ['Object', '100.5']
        ])('100.50 to %s', async (targetType, expectedValue) => {
          await changeType(targetType)
          expect(wrapper.vm.value).toBe(expectedValue === null ? wrapper.vm.currentDate() : expectedValue)
        })
      })

      describe('100.50', () => {
        beforeEach(async () => {
          await setData({ type: 'Double', value: 100.50 })
          expect(wrapper.vm.value).toBe(100.50)
        })

        it.each([
          ['Boolean', true],
          ['Double', 100.50],
          ['String', '100.5'],
          ['Json', '{}'],
          ['Date', null],
          ['Long', 100],
          ['Xml', ''],
          ['Object', '100.5']
        ])('100.50 to %s', async (targetType, expectedValue) => {
          await changeType(targetType)
          expect(wrapper.vm.value).toBe(expectedValue === null ? wrapper.vm.currentDate() : expectedValue)
        })
      })
    })

    describe('from Date', () => {

      describe('valid date', () => {
        beforeEach(async () => {
          await setData({ type: 'Date', value: '2025-09-25' })
          expect(wrapper.vm.value).toBe('2025-09-25')
        })

        it.each([
          ['Boolean', true],
          ['Long', 0],
          ['Double', 0.0],
          ['String', '2025-09-25'],
          ['Json', '{}'],
          ['Xml', ''],
          ['Object', '2025-09-25']
        ])('date to %s', async (targetType, expectedValue) => {
          await changeType(targetType)
          expect(wrapper.vm.value).toBe(expectedValue)
        })
      })

      describe('null date', () => {
        beforeEach(async () => {
          await setData({ type: 'Date', value: null })
          expect(wrapper.vm.value).toBe(null)
        })

        it.each([
          ['Boolean', true],
          ['Long', 0],
          ['Double', 0.0],
          ['String', ''],
          ['Json', '{}'],
          ['Xml', ''],
          ['Object', '']
        ])('null date to %s', async (targetType, expectedValue) => {
          await changeType(targetType)
          expect(wrapper.vm.value).toBe(expectedValue)
        })
      })
    })

    describe('from Short', () => {

      describe('0', () => {
        beforeEach(async () => {
          await setData({ type: 'Short', value: 0 })
          expect(wrapper.vm.value).toBe(0)
        })

        it.each([
          ['Boolean', true],
          ['Long', 0],
          ['Double', 0.0],
          ['String', ''],
          ['Date', null],
          ['Json', '{}'],
          ['Xml', ''],
          ['Object', '']
        ])('0 to %s', async (targetType, expectedValue) => {
          await changeType(targetType)
          expect(wrapper.vm.value).toBe(expectedValue === null ? wrapper.vm.currentDate() : expectedValue)
        })
      })

      describe('100', () => {
        beforeEach(async () => {
          await setData({ type: 'Short', value: 100 })
          expect(wrapper.vm.value).toBe(100)
        })

        it.each([
          ['Boolean', true],
          ['Long', 100],
          ['Double', 100.0],
          ['String', '100'],
          ['Date', null],
          ['Json', '{}'],
          ['Xml', ''],
          ['Object', '100']
        ])('100 to %s', async (targetType, expectedValue) => {
          await changeType(targetType)
          expect(wrapper.vm.value).toBe(expectedValue === null ? wrapper.vm.currentDate() : expectedValue)
        })
      })
    })

    describe('from Integer', () => {

      describe('0', () => {
        beforeEach(async () => {
          await setData({ type: 'Integer', value: 0 })
          expect(wrapper.vm.value).toBe(0)
        })

        it.each([
          ['Boolean', true],
          ['Long', 0],
          ['Double', 0.0],
          ['String', ''],
          ['Date', null],
          ['Json', '{}'],
          ['Xml', ''],
          ['Object', '']
        ])('0 to %s', async (targetType, expectedValue) => {
          await changeType(targetType)
          expect(wrapper.vm.value).toBe(expectedValue === null ? wrapper.vm.currentDate() : expectedValue)
        })
      })

      describe('100', () => {
        beforeEach(async () => {
          await setData({ type: 'Integer', value: 100 })
          expect(wrapper.vm.value).toBe(100)
        })

        it.each([
          ['Boolean', true],
          ['Long', 100],
          ['Double', 100.0],
          ['String', '100'],
          ['Date', null],
          ['Json', '{}'],
          ['Xml', ''],
          ['Object', '100']
        ])('100 to %s', async (targetType, expectedValue) => {
          await changeType(targetType)
          expect(wrapper.vm.value).toBe(expectedValue === null ? wrapper.vm.currentDate() : expectedValue)
        })
      })
    })

    describe('from Json', () => {

      describe('empty object', () => {
        beforeEach(async () => {
          await setData({ type: 'Json', value: '{}' })
          expect(wrapper.vm.value).toBe('{}')
        })

        it.each([
          ['Boolean', true],
          ['Long', 0],
          ['Double', 0.0],
          ['String', '{}'],
          ['Date', null],
          ['Xml', ''],
          ['Object', '{}']
        ])('"{" to %s', async (targetType, expectedValue) => {
          await changeType(targetType)
          expect(wrapper.vm.value).toBe(expectedValue === null ? wrapper.vm.currentDate() : expectedValue)
        })
      })

      describe('number json', () => {
        beforeEach(async () => {
          await setData({ type: 'Json', value: '123' })
          expect(wrapper.vm.value).toBe('123')
        })

        it.each([
          ['Boolean', true],
          ['Long', 123],
          ['Double', 123.0],
          ['String', '123'],
          ['Date', null],
          ['Xml', '']
        ])('"123" to %s', async (targetType, expectedValue) => {
          await changeType(targetType)
          expect(wrapper.vm.value).toBe(expectedValue === null ? wrapper.vm.currentDate() : expectedValue)
        })

        it('"123" to Object', async () => {
          await changeType('Object')
          expect(wrapper.vm.value).toBe('123')
          expect(wrapper.vm.objectTypeName).toBe('')
          expect(wrapper.vm.serializationDataFormat).toBe('')
        })
      })

      describe('json object {"a":"b"}', () => {
        beforeEach(async () => {
          await setData({ type: 'Json', value: '{"a":"b"}' })
          expect(wrapper.vm.value).toBe('{"a":"b"}')
        })

        it.each([
          ['Boolean', true],
          ['Long', 0],
          ['Double', 0.0],
          ['String', '{"a":"b"}'],
          ['Date', null],
          ['Xml', '']
        ])('"{"a":"b"}" to %s', async (targetType, expectedValue) => {
          await changeType(targetType)
          expect(wrapper.vm.value).toBe(expectedValue === null ? wrapper.vm.currentDate() : expectedValue)
        })

        it('"{"a":"b"}" to Object', async () => {
          await changeType('Object')
          expect(wrapper.vm.value).toBe('{\n  "a": "b"\n}')
          expect(wrapper.vm.objectTypeName).toBe('java.util.Map')
          expect(wrapper.vm.serializationDataFormat).toBe('application/json')
        })
      })
    })

    describe('from Xml', () => {

      describe('empty xml', () => {
        beforeEach(async () => {
          await setData({ type: 'Xml', value: '' })
          expect(wrapper.vm.value).toBe('')
        })

        it.each([
          ['Boolean', true],
          ['Long', 0],
          ['Double', 0.0],
          ['String', ''],
          ['Date', null],
          ['Json', '{}'],
          ['Object', '']
        ])('"" to %s', async (targetType, expectedValue) => {
          await changeType(targetType)
          expect(wrapper.vm.value).toBe(expectedValue === null ? wrapper.vm.currentDate() : expectedValue)
        })
      })

      describe('valid xml', () => {
        beforeEach(async () => {
          await setData({ type: 'Xml', value: '<root>test</root>' })
          expect(wrapper.vm.value).toBe('<root>test</root>')
        })

        it.each([
          ['Boolean', true],
          ['Long', 0],
          ['Double', 0.0],
          ['String', '<root>test</root>'],
          ['Date', null],
          ['Json', '{}'],
          ['Object', '<root>test</root>']
        ])('"<root>test</root>" to %s', async (targetType, expectedValue) => {
          await changeType(targetType)
          expect(wrapper.vm.value).toBe(expectedValue === null ? wrapper.vm.currentDate() : expectedValue)
        })
      })
    })

    describe('from Object', () => {

      describe('empty object', () => {
        beforeEach(async () => {
          await setData({ type: 'Object', value: '', objectTypeName: 'TestClass', serializationDataFormat: 'application/json' })
          expect(wrapper.vm.value).toBe('')
        })

        it.each([
          ['Boolean', true],
          ['Long', 0],
          ['Double', 0.0],
          ['String', ''],
          ['Date', null],
          ['Json', '{}'],
          ['Xml', '']
        ])('"" to %s', async (targetType, expectedValue) => {
          await changeType(targetType)
          expect(wrapper.vm.value).toBe(expectedValue === null ? wrapper.vm.currentDate() : expectedValue)
        })
      })

      describe('object with data', () => {
        beforeEach(async () => {
          await setData({ type: 'Object', value: '{"name":"test"}', objectTypeName: 'TestClass', serializationDataFormat: 'application/json' })
          expect(wrapper.vm.value).toBe('{"name":"test"}')
        })

        it.each([
          ['Boolean', true],
          ['Long', 0],
          ['Double', 0.0],
          ['String', '{"name":"test"}'],
          ['Date', null],
          ['Json', '{\n  "name": "test"\n}'],
          ['Xml', '']
        ])('"{"name":"test"}" to %s', async (targetType, expectedValue) => {
          await changeType(targetType)
          expect(wrapper.vm.value).toBe(expectedValue === null ? wrapper.vm.currentDate() : expectedValue)
        })
      })
    })

    describe('from Null', () => {

      describe('null value', () => {
        beforeEach(async () => {
          await setData({ type: 'Null', value: null })
          expect(wrapper.vm.value).toBe(null)
        })

        it('null to Boolean', async () => {
          await changeType('Boolean')
          expect(wrapper.vm.value).toBe(true)
        })

        it('null to Long', async () => {
          await changeType('Long')
          expect(wrapper.vm.value).toBe(0)
        })

        it('null to Double', async () => {
          await changeType('Double')
          expect(wrapper.vm.value).toBe(0.0)
        })

        it('null to String', async () => {
          await changeType('String')
          expect(wrapper.vm.value).toBe('')
        })

        it('null to Date', async () => {
          await changeType('Date')
          expect(wrapper.vm.value).toBe(wrapper.vm.currentDate())
        })

        it('null to Json', async () => {
          await changeType('Json')
          expect(wrapper.vm.value).toBe('{}')
        })

        it('null to Xml', async () => {
          await changeType('Xml')
          expect(wrapper.vm.value).toBe('')
        })

        it('null to Object', async () => {
          await changeType('Object')
          expect(wrapper.vm.value).toBe('')
        })
      })
    })
  })
})
