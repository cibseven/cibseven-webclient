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
import { describe, it, expect, beforeEach, vi, beforeAll } from 'vitest'
import { mount } from '@vue/test-utils'
import { i18n } from '@/i18n'
import AddVariableModalUI from '@/components/process/modals/AddVariableModalUI.vue'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { BButton, BFormGroup, BFormInput, BFormSelect, BFormCheckbox, BAlert } from '@cib/common-frontend'

// Mock dependencies
vi.mock('@/services.js', () => ({
  ProcessService: {
    putLocalExecutionVariable: vi.fn(() => Promise.resolve())
  }
}))

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
        'b-form-file': true,
        'b-button': BButton,
        'b-alert': BAlert,
        'b-tab': { template: '<div><slot></slot></div>' },
        'b-tabs': { template: '<div><slot></slot></div>' },
      }
    }
  })
}

describe('AddVariableModal.vue UI interactions', () => {

  let wrapper

  beforeAll(() => {
    const translations = {
      ...JSON.parse(
        // eslint-disable-next-line no-undef
        readFileSync(resolve(__dirname, '../../assets/translations_en.json'), 'utf-8')
      ),
      bcomponents: {
        ariaLabelClose: 'Close',
      },
    }

    i18n.global.locale = 'en'
    i18n.global.setLocaleMessage('en', translations)
  })

  beforeEach(() => {
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

  describe.each([
    true,
    false,
  ])('editMode=%s', (editMode) => {
    it('allowEditName=undefined', async () => {
      wrapper = createWrapper({ editMode })
      expect(wrapper.vm.editMode).toBe(editMode)
      expect(wrapper.vm.computedAllowEditName).toBe(!editMode)
      expect(wrapper.find('input').attributes('disabled')).toBe(editMode ? '': undefined)
    })

    it('allowEditName=false', async () => {
      wrapper = createWrapper({ editMode: editMode, allowEditName: false })
      expect(wrapper.vm.editMode).toBe(editMode)
      expect(wrapper.vm.computedAllowEditName).toBe(!editMode)
      expect(wrapper.find('input').attributes('disabled')).toBe(editMode ? '': undefined)
    })

    it('allowEditName=true', async () => {
      wrapper = createWrapper({ editMode: editMode, allowEditName: true })
      expect(wrapper.vm.editMode).toBe(editMode)
      expect(wrapper.vm.computedAllowEditName).toBe(true)
      expect(wrapper.find('input').attributes('disabled')).toBeUndefined()
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
      [{ name: 'name', type: 'File', value: 'text' }, true],
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

      [{ type: 'file', value: null }, false],
      [{ type: 'file', value: 'Text' }, false],
      [{ type: 'file', value: 0 }, false],
      [{ type: 'file', value: 100 }, false],
      [{ type: 'file', value: true }, false],
      [{ type: 'file', value: false }, false],

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
      expect(wrapper.vm.value).toBe(1)
      await changeType('Date')
      expect(wrapper.vm.value).toBe(wrapper.vm.currentDate())
      await changeType('Json')
      expect(wrapper.vm.value).toBe('{}')
      await changeType('Xml')
      expect(wrapper.vm.value).toBe('')
      await changeType('Object')
      expect(wrapper.vm.value).toBe('')
    })

    // Helper function to reduce duplication
    const testTypeConversion = (sourceType, sourceValues, conversions, additionalSetup = {}) => {
      describe(`from ${sourceType}`, () => {
        const sourceValuesArray = Array.isArray(sourceValues) ? sourceValues : [sourceValues]
        describe.each(sourceValuesArray)(`${sourceType} with value: %s`, (sourceValue) => {
          beforeEach(async () => {
            await setData({ type: sourceType, value: sourceValue, ...additionalSetup })
            expect(wrapper.vm.value).toBe(sourceValue)
          })

          const conversionsNoSourceType = conversions.filter(([targetType]) => targetType !== sourceType)
          it.each(conversionsNoSourceType)('to %s', async (targetType, expectedValue) => {
            await changeType(targetType)
            expect(wrapper.vm.value).toBe(expectedValue === null ? wrapper.vm.currentDate() : expectedValue)
          })
        })
      })
    }

    const zeroValues = [
      ['Boolean', true],
      ['Long', 0],
      ['Short', 0],
      ['Double', 0],
      ['Integer', 0],
      ['String', ''],
      ['Date', null],
      ['Json', '{}'],
      ['Xml', ''],
      ['Object', '']
    ]

    // String conversions
    testTypeConversion('String', 'text', [
      ['Boolean', true],
      ['Long', 0],
      ['Double', 0],
      ['Date', null],
      ['Json', '{}'],
      ['Xml', ''],
      ['Object', 'text'],
    ])

    testTypeConversion('String', ['', null], zeroValues)

    testTypeConversion('String', '10.50', [
      ['Boolean', true],
      ['Long', 10],
      ['Double', 10.5],
      ['Date', null],
      ['Json', '10.5'],
      ['Xml', ''],
      ['Object', '10.5'],
    ])

    testTypeConversion('String', '{"a":"b"}', [
      ['Boolean', true],
      ['Long', 0],
      ['Double', 0],
      ['Date', null],
      ['Json', '{\n  "a": "b"\n}'],
      ['Xml', ''],
      ['Object', '{\n  "a": "b"\n}'],
    ])

    // Boolean conversions
    testTypeConversion('Boolean', true, [
      ['Long', 1],
      ['Double', 1],
      ['Short', 1],
      ['Integer', 1],
      ['String', ''],
      ['Date', null],
      ['Json', '{}'],
      ['Xml', ''],
      ['Object', '']
    ])

    testTypeConversion('Boolean', [false, null], zeroValues)

    // Long conversions
    testTypeConversion('Long', [0, null], zeroValues)

    testTypeConversion('Long', 100, [
      ['Boolean', true],
      ['Double', 100],
      ['String', '100'],
      ['Json', '{}'],
      ['Date', null],
      ['Xml', ''],
      ['Object', '100']
    ])

    // Double conversions
    testTypeConversion('Double', 0, [
      ['Boolean', true],
      ['Double', 0],
      ['String', ''],
      ['Json', '{}']
    ])

    testTypeConversion('Double', 100.5, [
      ['Boolean', true],
      ['Double', 100.5],
      ['String', '100.5'],
      ['Json', '{}'],
      ['Date', null],
      ['Long', 100],
      ['Xml', ''],
      ['Object', '100.5']
    ])

    // Date conversions
    testTypeConversion('Date', '2025-09-25', [
      ['Boolean', true],
      ['Long', 0],
      ['Double', 0],
      ['String', '2025-09-25'],
      ['Json', '{}'],
      ['Xml', ''],
      ['Object', '2025-09-25']
    ])

    testTypeConversion('Date', [null, ''], zeroValues)

    // Short conversions
    testTypeConversion('Short', [0, null], zeroValues)

    testTypeConversion('Short', 100, [
      ['Boolean', true],
      ['Long', 100],
      ['Double', 100],
      ['String', '100'],
      ['Date', null],
      ['Json', '{}'],
      ['Xml', ''],
      ['Object', '100']
    ])

    // Integer conversions
    testTypeConversion('Integer', [0, null], zeroValues)

    testTypeConversion('Integer', 100, [
      ['Boolean', true],
      ['Long', 100],
      ['Double', 100],
      ['String', '100'],
      ['Date', null],
      ['Json', '{}'],
      ['Xml', ''],
      ['Object', '100']
    ])

    // Json conversions
    testTypeConversion('Json', '{}', zeroValues)

    testTypeConversion('Json', '123', [
      ['Boolean', true],
      ['Long', 123],
      ['Double', 123],
      ['String', '123'],
      ['Date', null],
      ['Xml', '']
    ])

    describe('from Json', () => {
      it('json object "123" to Object', async () => {
        await setData({ type: 'Json', value: '123' })
        expect(wrapper.vm.value).toBe('123')

        await changeType('Object')
        expect(wrapper.vm.value).toBe('123')
        expect(wrapper.vm.objectTypeName).toBe('')
        expect(wrapper.vm.serializationDataFormat).toBe('')
      })

      it.each([
        ['Boolean', true],
        ['Long', 0],
        ['Double', 0],
        ['String', '{"a":"b"}'],
        ['Date', null],
        ['Xml', '']
      ])('json object "{"a":"b"}" to %s', async (targetType, expectedValue) => {
        await setData({ type: 'Json', value: '{"a":"b"}' })
        expect(wrapper.vm.value).toBe('{"a":"b"}')

        await changeType(targetType)
        expect(wrapper.vm.value).toBe(expectedValue === null ? wrapper.vm.currentDate() : expectedValue)
      })

      it('json object "{"a":"b"}" to Object', async () => {
        await setData({ type: 'Json', value: '{"a":"b"}' })
        expect(wrapper.vm.value).toBe('{"a":"b"}')

        await changeType('Object')
        expect(wrapper.vm.value).toBe('{\n  "a": "b"\n}')
        expect(wrapper.vm.objectTypeName).toBe('java.util.Map')
        expect(wrapper.vm.serializationDataFormat).toBe('application/json')
      })
    })

    // Xml conversions
    testTypeConversion('Xml', [ '', null ], zeroValues)

    testTypeConversion('Xml', '<root>test</root>', [
      ['Boolean', true],
      ['Long', 0],
      ['Double', 0],
      ['String', '<root>test</root>'],
      ['Date', null],
      ['Json', '{}'],
      ['Object', '<root>test</root>']
    ])

    // Object conversions
    testTypeConversion('Object', '', zeroValues, { objectTypeName: 'TestClass', serializationDataFormat: 'application/json' })

    testTypeConversion('Object', '{"name":"test"}', [
      ['Boolean', true],
      ['Long', 0],
      ['Double', 0],
      ['String', '{"name":"test"}'],
      ['Date', null],
      ['Json', '{\n  "name": "test"\n}'],
      ['Xml', '']
    ], { objectTypeName: 'TestClass', serializationDataFormat: 'application/json' })

    // Null conversions
    testTypeConversion('Null', [null, ''], zeroValues)
  })
})
