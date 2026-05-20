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
 */
export default {
  displayValue(variable) {
    if (this.isFileValueDataSource(variable)) {
      return this.getFileVariableName(variable)
    }
    else if (variable.type === 'File') {
      return variable.valueInfo.filename
    }
    else if (variable.type === 'Json') {
      if (typeof variable.valueSerialized === 'string') {
        return variable.valueSerialized
      }
      else if (typeof variable.value === 'object') {
        try {
          return JSON.stringify(variable.value, null, 2)
        } catch {
          return '- Json Object -'
        }
      }
      return '- Json Object -'
    }
    else if (variable.type === 'Object') {
      if (variable.valueDeserialized && typeof variable.valueDeserialized === 'object') {
        return JSON.stringify(variable.valueDeserialized, null, 2)
      }
      else if (typeof variable.value === 'object') {
        try {
          return JSON.stringify(variable.value, null, 2)
        } catch {
          return '- Object -'
        }
      }
      else if (typeof variable.value === 'string') {
        return variable.value
      }
      return '- Object -'
    }
    else if (variable.type === 'Null') {
      return ''
    }
    else {
      return '' + variable.value
    }
  },

  isFile(variable) {
    return (variable.type === 'File') || this.isFileValueDataSource(variable)
  },

  isFileValueDataSource(variable) {
    if (variable.type === 'Object') {
      const objectTypeName =
        variable.value?.objectTypeName ||
        variable.valueInfo?.objectTypeName
      if (objectTypeName && this.fileObjects.includes(objectTypeName)) return true
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
}
