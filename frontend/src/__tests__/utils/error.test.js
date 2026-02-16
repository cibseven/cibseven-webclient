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
import { describe, it, expect } from 'vitest'
import {
  extractErrorMessage,
  isFormElementError,
  isDeployedFormNotFoundError,
  extractDeployedFormName
} from '@/utils/error.js'

describe('error utility', () => {
  describe('extractErrorMessage', () => {
    describe('with string errors', () => {
      it('should return the string as-is', () => {
        expect(extractErrorMessage('Simple error message')).toBe('Simple error message')
      })

      it('should handle empty string', () => {
        expect(extractErrorMessage('')).toBe('')
      })
    })

    describe('with null/undefined errors', () => {
      it('should return default fallback for null', () => {
        expect(extractErrorMessage(null)).toBe('An unexpected error occurred')
      })

      it('should return default fallback for undefined', () => {
        expect(extractErrorMessage(undefined)).toBe('An unexpected error occurred')
      })

      it('should return custom fallback when provided', () => {
        expect(extractErrorMessage(null, 'Custom fallback')).toBe('Custom fallback')
      })
    })

    describe('with Axios error format', () => {
      it('should extract message from response.data.message', () => {
        const error = {
          response: {
            data: {
              message: 'Server error message'
            }
          }
        }
        expect(extractErrorMessage(error)).toBe('Server error message')
      })

      it('should extract message from response.data.params array', () => {
        const error = {
          response: {
            data: {
              params: ['Detailed error from params', 'another param'],
              message: 'Generic message'
            }
          }
        }
        expect(extractErrorMessage(error)).toBe('Detailed error from params')
      })

      it('should handle empty params array', () => {
        const error = {
          response: {
            data: {
              params: [],
              message: 'Fallback message'
            }
          }
        }
        expect(extractErrorMessage(error)).toBe('Fallback message')
      })

      it('should extract nested server error from params', () => {
        const error = {
          response: {
            data: {
              params: ['Error occurred: {"type":"ProcessEngineException","message":"Engine error details"}']
            }
          }
        }
        expect(extractErrorMessage(error)).toBe('Engine error details')
      })

      it('should handle non-string params', () => {
        const error = {
          response: {
            data: {
              params: [123, 'valid string'],
              message: 'Fallback message'
            }
          }
        }
        expect(extractErrorMessage(error)).toBe('Fallback message')
      })
    })

    describe('with BPM SDK error format', () => {
      it('should extract message from response.body.message', () => {
        const error = {
          response: {
            body: {
              message: 'BPM SDK error message'
            }
          }
        }
        expect(extractErrorMessage(error)).toBe('BPM SDK error message')
      })

      it('should extract message from response.body.params array', () => {
        const error = {
          response: {
            body: {
              params: ['Detailed BPM error', 'another param'],
              message: 'Generic message'
            }
          }
        }
        expect(extractErrorMessage(error)).toBe('Detailed BPM error')
      })

      it('should extract nested server error from body.params', () => {
        const error = {
          response: {
            body: {
              params: ['Error: {"type":"BadUserRequestException","message":"Invalid request"}']
            }
          }
        }
        expect(extractErrorMessage(error)).toBe('Invalid request')
      })
    })

    describe('with Error objects', () => {
      it('should extract message from Error object', () => {
        const error = new Error('Standard error message')
        expect(extractErrorMessage(error)).toBe('Standard error message')
      })

      it('should extract nested server error from Error message', () => {
        const error = new Error('Operation failed: {"type":"ValidationException","message":"Validation failed"}')
        expect(extractErrorMessage(error)).toBe('Validation failed')
      })

      it('should handle Error with no message', () => {
        const error = new Error()
        expect(extractErrorMessage(error)).toBe('')
      })
    })

    describe('with objects having message property', () => {
      it('should extract message from plain object', () => {
        const error = {
          message: 'Plain object error'
        }
        expect(extractErrorMessage(error)).toBe('Plain object error')
      })

      it('should extract nested server error from object message', () => {
        const error = {
          message: 'Failed: {"type":"RuntimeException","message":"Runtime error occurred"}'
        }
        expect(extractErrorMessage(error)).toBe('Runtime error occurred')
      })
    })

    describe('with complex nested scenarios', () => {
      it('should prioritize params over message in response.data', () => {
        const error = {
          response: {
            data: {
              params: ['Specific error from params'],
              message: 'Generic error message'
            }
          }
        }
        expect(extractErrorMessage(error)).toBe('Specific error from params')
      })

      it('should fall back to message when params is empty', () => {
        const error = {
          response: {
            data: {
              params: [],
              message: 'Fallback error message'
            }
          }
        }
        expect(extractErrorMessage(error)).toBe('Fallback error message')
      })

      it('should use fallback when response.data is not an object', () => {
        const error = {
          response: {
            data: 'string data'
          }
        }
        expect(extractErrorMessage(error, 'Custom fallback')).toBe('Custom fallback')
      })
    })

    describe('extractServerErrorFromMessage edge cases', () => {
      it('should handle message with escaped quotes', () => {
        const error = {
          message: 'Error: {\\"type\\":\\"Exception\\",\\"message\\":\\"Escaped error\\"}'
        }
        expect(extractErrorMessage(error)).toBe('Escaped error')
      })

      it('should handle message with nested JSON objects', () => {
        const error = {
          message: 'Failed: {"type":"Exception","message":"Nested error","details":{"code":500,"info":"Additional info"}}'
        }
        expect(extractErrorMessage(error)).toBe('Nested error')
      })

      it('should handle message with multiple JSON-like strings', () => {
        const error = {
          message: 'First {"invalid":"json"} then {"type":"Valid","message":"This should be extracted"}'
        }
        expect(extractErrorMessage(error)).toBe('This should be extracted')
      })

      it('should handle message with malformed JSON', () => {
        const error = {
          message: 'Error: {"type":"Exception", missing closing brace'
        }
        expect(extractErrorMessage(error)).toBe('Error: {"type":"Exception", missing closing brace')
      })

      it('should handle message with JSON missing required fields', () => {
        const error = {
          message: 'Error: {"type":"Exception"}'
        }
        expect(extractErrorMessage(error)).toBe('Error: {"type":"Exception"}')
      })

      it('should handle message with JSON missing type field', () => {
        const error = {
          message: 'Error: {"message":"Only message field"}'
        }
        expect(extractErrorMessage(error)).toBe('Error: {"message":"Only message field"}')
      })

      it('should handle message with no braces', () => {
        const error = {
          message: 'Plain error message without JSON'
        }
        expect(extractErrorMessage(error)).toBe('Plain error message without JSON')
      })

      it('should handle message with nested braces', () => {
        const error = {
          message: 'Error: {"type":"Exception","message":"Error with {nested} braces","details":{"level":1}}'
        }
        expect(extractErrorMessage(error)).toBe('Error with {nested} braces')
      })

      it('should handle message with unbalanced braces', () => {
        const error = {
          message: 'Error: {"type":"Exception","message":"Unbalanced"'
        }
        expect(extractErrorMessage(error)).toBe('Error: {"type":"Exception","message":"Unbalanced"')
      })
    })
  })

  describe('isFormElementError', () => {
    it('should return true for form element error', () => {
      const message = 'Form must provide exactly one element <form ...>'
      expect(isFormElementError(message)).toBe(true)
    })

    it('should return true for form element error in longer message', () => {
      const message = 'An error occurred: Form must provide exactly one element <form camunda:formKey="embedded:app:forms/start-form.html">'
      expect(isFormElementError(message)).toBe(true)
    })

    it('should return false for non-form-element errors', () => {
      expect(isFormElementError('Some other error')).toBe(false)
    })

    it('should return false for null', () => {
      expect(isFormElementError(null)).toBe(false)
    })

    it('should return false for undefined', () => {
      expect(isFormElementError(undefined)).toBe(false)
    })

    it('should return false for non-string input', () => {
      expect(isFormElementError(123)).toBe(false)
      expect(isFormElementError({})).toBe(false)
      expect(isFormElementError([])).toBe(false)
    })

    it('should return false for empty string', () => {
      expect(isFormElementError('')).toBe(false)
    })
  })

  describe('isDeployedFormNotFoundError', () => {
    it('should return true for deployment not found error', () => {
      const message = "The form with the resource name 'myform.html' cannot be found in deployment with id '123'"
      expect(isDeployedFormNotFoundError(message)).toBe(true)
    })

    it('should return true for Camunda form not found error', () => {
      const message = 'No Camunda Form Definition was found for Camunda Form Ref: CamundaFormRefImpl [key=myForm, ...]'
      expect(isDeployedFormNotFoundError(message)).toBe(true)
    })

    it('should return true for partial deployment message', () => {
      const message = 'Error: cannot be found in deployment with id abc123'
      expect(isDeployedFormNotFoundError(message)).toBe(true)
    })

    it('should return true for partial Camunda form message', () => {
      const message = 'Error: No Camunda Form Definition was found'
      expect(isDeployedFormNotFoundError(message)).toBe(true)
    })

    it('should return false for non-deployment errors', () => {
      expect(isDeployedFormNotFoundError('Some other error')).toBe(false)
    })

    it('should return false for null', () => {
      expect(isDeployedFormNotFoundError(null)).toBe(false)
    })

    it('should return false for undefined', () => {
      expect(isDeployedFormNotFoundError(undefined)).toBe(false)
    })

    it('should return false for non-string input', () => {
      expect(isDeployedFormNotFoundError(123)).toBe(false)
      expect(isDeployedFormNotFoundError({})).toBe(false)
    })

    it('should return false for empty string', () => {
      expect(isDeployedFormNotFoundError('')).toBe(false)
    })
  })

  describe('extractDeployedFormName', () => {
    it('should extract form name from resource name pattern', () => {
      const message = "The form with the resource name 'start-form.html' cannot be found in deployment with id '123'"
      expect(extractDeployedFormName(message)).toBe('start-form.html')
    })

    it('should extract form name from key pattern', () => {
      const message = 'No Camunda Form Definition was found for Camunda Form Ref: CamundaFormRefImpl [key=userTaskForm, binding=latest]'
      expect(extractDeployedFormName(message)).toBe('userTaskForm')
    })

    it('should prioritize resource name over key pattern', () => {
      const message = "The form with the resource name 'myform.html' [key=otherForm] cannot be found"
      expect(extractDeployedFormName(message)).toBe('myform.html')
    })

    it('should handle form name with special characters', () => {
      const message = "The form with the resource name 'my-complex_form.v2.html' cannot be found in deployment"
      expect(extractDeployedFormName(message)).toBe('my-complex_form.v2.html')
    })

    it('should handle key with special characters', () => {
      const message = 'No Camunda Form Definition was found for Camunda Form Ref: CamundaFormRefImpl [key=form_name-v2, binding=latest]'
      expect(extractDeployedFormName(message)).toBe('form_name-v2')
    })

    it('should return null for message without form name', () => {
      expect(extractDeployedFormName('Some other error')).toBeNull()
    })

    it('should return null for null', () => {
      expect(extractDeployedFormName(null)).toBeNull()
    })

    it('should return null for undefined', () => {
      expect(extractDeployedFormName(undefined)).toBeNull()
    })

    it('should return null for non-string input', () => {
      expect(extractDeployedFormName(123)).toBeNull()
      expect(extractDeployedFormName({})).toBeNull()
    })

    it('should return null for empty string', () => {
      expect(extractDeployedFormName('')).toBeNull()
    })

    it('should handle edge case with empty quotes', () => {
      const message = "The form with the resource name '' cannot be found"
      expect(extractDeployedFormName(message)).toBe('')
    })

    it('should handle edge case with empty key', () => {
      const message = 'Camunda Form Ref: CamundaFormRefImpl [key=, binding=latest]'
      expect(extractDeployedFormName(message)).toBe('')
    })
  })
})
