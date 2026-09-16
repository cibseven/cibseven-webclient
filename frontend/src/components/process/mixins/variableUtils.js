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

/**
 * Utility functions for handling process variables DTOs
 * 
 * `variable` objects are expected to have the following structure:
 * {
 *   name: string,
 *   value: any,
 *   type: string, // e.g. 'String', 'Object', 'File', 'Json', 'Null', etc.
 *   valueInfo: {
 *     objectTypeName?: string, // for Object variables only
 *     serializationDataFormat?: string, // for Object variables only
 * 
 *     filename?: string, // for File variables only
 *     mimeType?: string, // for File variables only
 *   },
 *   valueDeserialized?: any,
 *   valueSerialized?: string
 * }
 */
export default {
  displayValue(variable) {
    if (this.isFileValueDataSource(variable)) {
      return this.getFileVariableName(variable)
    }

    switch (variable.type) {
      case 'File':
        return variable.valueInfo.filename

      case 'Json':
        return this.displayValueJson(variable)

      case 'Object':
        return this.displayValueObject(variable)

      case 'Null':
        return ''

      default:
        return '' + variable.value
    }
  },

  displayValueJson(variable) {
    if (typeof variable.valueSerialized === 'string') {
      return variable.valueSerialized
    }

    if (typeof variable.value === 'object') {
      try {
        return JSON.stringify(variable.value, null, 2)
      }
      catch {
        return '- Json Object -'
      }
    }

    return '- Json Object -'
  },

  displayValueObject(variable) {
    if (variable.valueDeserialized &&
      typeof variable.valueDeserialized === 'object'
    ) {
      try {
        return JSON.stringify(variable.valueDeserialized, null, 2)
      }
      catch {
        return '- Object -'
      }
    }

    if (typeof variable.value === 'object') {
      try {
        return JSON.stringify(variable.value, null, 2)
      }
      catch {
        return '- Object -'
      }
    }

    if (typeof variable.value === 'string') {
      return variable.value
    }

    return '- Object -'
  },

  isFile(variable) {
    return (variable.type === 'File') || this.isFileValueDataSource(variable)
  },

  isFileValueDataSource(variable) {
    if (variable.type === 'Object') {
      const objectTypeName =
        variable.value?.objectTypeName ||
        variable.valueInfo?.objectTypeName
      if (objectTypeName && this.getFileObjects().includes(objectTypeName)) return true
    }
    return false
  },

  getFileVariableName(variable) {
    // Prioritize valueDeserialized over value
    const targetValue = variable.valueDeserialized || variable.value
    if (targetValue && typeof targetValue === 'object' && targetValue.name) {
      return targetValue.name
    }
    if (targetValue && typeof targetValue === 'string') {
      try {
        const parsed = JSON.parse(targetValue)
        if (parsed?.name) return parsed.name
      } catch { return '' }
    }
    return ''
  },

  shortValue(value) {
    const str = '' + value
    const dot = str.lastIndexOf('.')
    return dot >= 0 && dot < str.length - 1 && /^[a-z]/.test(str) ? str.substring(dot + 1) : str
  },

  getFileObjects() {
    return [
      'de.cib.cibflow.api.files.FileValueDataFlowSource',
      'de.cib.cibflow.api.files.FileValueDataSource',
    ]
  },

  /**
   * Client-side equivalent of one 'variableValues' condition of the runtime variable-instance
   * query ({ name, operator, value }, operators as offered by the variable search box).
   * Needed because the historic variable-instance query does not support 'variableValues';
   * conditions are combined with AND, so a variable of another name never matches.
   * Unknown operators do not filter anything out.
   */
  matchesValueCondition(variable, condition, { namesIgnoreCase = false, valuesIgnoreCase = false } = {}) {
    if (!condition || !condition.name) return true
    if (this.asString(variable.name, namesIgnoreCase) !== this.asString(condition.name, namesIgnoreCase)) return false

    const actual = variable.value
    const expected = condition.value
    const ignoreCase = valuesIgnoreCase
    switch (condition.operator) {
      case 'eq': return this.valuesEqual(actual, expected, ignoreCase)
      case 'neq': return !this.valuesEqual(actual, expected, ignoreCase)
      case 'gt': return this.compareValues(actual, expected, ignoreCase) > 0
      case 'gteq': return this.compareValues(actual, expected, ignoreCase) >= 0
      case 'lt': return this.compareValues(actual, expected, ignoreCase) < 0
      case 'lteq': return this.compareValues(actual, expected, ignoreCase) <= 0
      case 'like': return this.valueLike(actual, expected, ignoreCase)
      default: return true
    }
  },

  valuesEqual(actual, expected, ignoreCase = false) {
    const numbers = this.asNumbers(actual, expected)
    if (numbers) return numbers[0] === numbers[1]
    return this.asString(actual, ignoreCase) === this.asString(expected, ignoreCase)
  },

  compareValues(actual, expected, ignoreCase = false) {
    const numbers = this.asNumbers(actual, expected)
    if (numbers) return numbers[0] - numbers[1]
    const a = this.asString(actual, ignoreCase)
    const b = this.asString(expected, ignoreCase)
    return a < b ? -1 : (a > b ? 1 : 0)
  },

  valueLike(actual, expected, ignoreCase = false) {
    const pattern = this.asString(expected, ignoreCase)
      .replace(/[.*+?^${}()|[\]\\]/g, '\\$&') // escape regex metacharacters, keep SQL wildcards
      .replace(/%/g, '.*')
      .replace(/_/g, '.')
    try {
      return new RegExp(`^${pattern}$`).test(this.asString(actual, ignoreCase))
    } catch { return true }
  },

  asNumbers(actual, expected) {
    if (actual === null || actual === undefined || expected === null || expected === undefined) return null
    if (typeof actual === 'boolean' || typeof expected === 'boolean') return null
    const a = Number(actual)
    const b = Number(expected)
    if (Number.isNaN(a) || Number.isNaN(b) || ('' + actual).trim() === '' || ('' + expected).trim() === '') return null
    return [a, b]
  },

  asString(value, ignoreCase = false) {
    const str = value === null || value === undefined ? '' : '' + value
    return ignoreCase ? str.toLowerCase() : str
  },
}
