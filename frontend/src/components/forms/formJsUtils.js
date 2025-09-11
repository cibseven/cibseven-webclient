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
 * Find all documentPreview components in form schema
 * @param {Object} formContent - Form schema object
 * @returns {Array} Array of documentPreview components
 */
export function findDocumentPreviewComponents(formContent) {
  return formContent.components?.filter(component => component.type === 'documentPreview') || []
}

/**
 * Generate document reference variable name by adding postfix.
 * The postfix is needed to avoid conflicts when file picker and document preview use the same variable name in the form.
 * Without the postfix, their data would overwrite each other in form data.
 * 
 * @param {string} variableName - Original variable name
 * @returns {string} Variable name with document reference postfix
 */
export function getDocumentReferenceVariableName(variableName) {
  return variableName + '_documentReference'
}
