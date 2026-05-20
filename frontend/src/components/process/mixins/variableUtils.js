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

  getFileObjects() {
    return [
      'de.cib.cibflow.api.files.FileValueDataFlowSource',
      'de.cib.cibflow.api.files.FileValueDataSource',
    ]
  },
}
