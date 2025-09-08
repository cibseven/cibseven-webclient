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
 * Convert form variables from service response format to form-js format
 * @param {Object} formData - Raw form data from service response
 * @returns {Object} Converted form data for form-js
 * @example
 * // Input (service response format):
 * {
 *   "availableOptions": {
 *     "name": null,
 *     "type": "Json", 
 *     "value": "[{\"label\":\"Option A\",\"value\":\"a\"},{\"label\":\"Option B\",\"value\":\"b\"}]",
 *     "valueInfo": {}
 *   },
 *   "userName": {
 *     "name": "user_name",
 *     "type": "String",
 *     "value": "John Doe",
 *     "valueInfo": {}
 *   }
 * }
 * 
 * // Output (form-js format):
 * {
 *   "availableOptions": [
 *     {"label": "Option A", "value": "a"},
 *     {"label": "Option B", "value": "b"}
 *   ],
 *   "user_name": "John Doe"
 * }
 */
export function convertFormDataForFormJs(formData) {
  const convertedFormData = {}
  Object.keys(formData).forEach(key => {
    // Use the original key if name is null, otherwise use the name
    const fieldName = formData[key].name === null ? key : formData[key].name
    
    // Handle JSON type values - parse them from string to object
    let value = formData[key].value
    if (formData[key].type === 'Json' && typeof value === 'string') {
      try {
        value = JSON.parse(value)
      } catch (e) {
        console.error(`Failed to parse JSON for ${fieldName}:`, value, e)
        // Keep original value if parsing fails
      }
    }
    
    convertedFormData[fieldName] = value
  })
  
  return convertedFormData
}

/**
 * Determine the value type based on the form field schema definition
 * @param {Object} schema - The form schema object
 * @param {string} fieldKey - The field key to look up
 * @param {*} value - The field value
 * @returns {string} The determined type (Integer, Double, Boolean, String)
 */
export function determineValueTypeFromSchema(schema, fieldKey, value) {
  // Find the field definition in the form schema
  const fieldDef = findFieldByKey(schema, fieldKey)
  
  if (fieldDef) {
    switch (fieldDef.type) {
      case 'number':
        // Check if it has decimal digits to determine Integer vs Double
        if (fieldDef.decimalDigits && fieldDef.decimalDigits > 0) {
          return 'Double'
        } else {
          return 'Integer'
        }
      case 'checkbox':
        return 'Boolean'
      default:
        return 'String'
    }
  }
  
  // Default to String if field not found in schema
  return 'String'
}

/**
 * Recursively find a field by key in the form schema
 * @param {Object} schema - The schema object to search in
 * @param {string} key - The field key to find
 * @returns {Object|null} The field definition or null if not found
 */
export function findFieldByKey(schema, key) {
  if (!schema || !schema.components) return null
  
  for (const component of schema.components) {
    if (component.key === key) {
      return component
    }
    // Recursively search in nested components if they exist
    if (component.components) {
      const found = findFieldByKey(component, key)
      if (found) return found
    }
  }
  return null
}