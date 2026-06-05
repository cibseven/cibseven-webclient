/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
 * Version 2.0; may not use this file except in compliance with the License.
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
        it('should return fallback for null/undefined', () => {
            expect(extractErrorMessage(null)).toBe('An unexpected error occurred')
            expect(extractErrorMessage(undefined)).toBe('An unexpected error occurred')
            expect(extractErrorMessage(null, 'Custom error')).toBe('Custom error')
        })

        it('should return string as-is', () => {
            expect(extractErrorMessage('Simple error message')).toBe('Simple error message')
        })

        it('should extract message from Axios error.response.data.message', () => {
            const error = {
                response: {
                    data: {
                        message: 'Axios error message'
                    }
                }
            }
            expect(extractErrorMessage(error)).toBe('Axios error message')
        })

        it('should extract message from Axios error.response.data.params array', () => {
            const error = {
                response: {
                    data: {
                        params: ['Param error message']
                    }
                }
            }
            expect(extractErrorMessage(error)).toBe('Param error message')
        })

        it('should extract nested server error from params array', () => {
            const error = {
                response: {
                    data: {
                        type: 'SystemException',
                        params: [
                            'Some unexpected technical problem occured: Error getting deployed form: Some unexpected technical problem occured: {"type":"InvalidRequestException","message":"No Camunda Form Definition was found for Camunda Form Ref: CamundaFormRefImpl [key=Form_deployed_with_process, binding=latest, version=null]","code":null}'
                        ]
                    }
                }
            }
            const result = extractErrorMessage(error)
            expect(result).toBe('No Camunda Form Definition was found for Camunda Form Ref: CamundaFormRefImpl [key=Form_deployed_with_process, binding=latest, version=null]')
        })

        it('should prioritize params over message', () => {
            const error = {
                response: {
                    data: {
                        message: 'Message property',
                        params: ['Params message']
                    }
                }
            }
            expect(extractErrorMessage(error)).toBe('Params message')
        })

        it('should extract message from BPM SDK error.response.body', () => {
            const error = {
                response: {
                    body: {
                        message: 'SDK error message'
                    }
                }
            }
            expect(extractErrorMessage(error)).toBe('SDK error message')
        })

        it('should extract nested server error from body.params', () => {
            const error = {
                response: {
                    body: {
                        type: 'SystemException',
                        params: [
                            'Error: {"type":"InvalidRequestException","message":"Form not found"}'
                        ]
                    }
                }
            }
            expect(extractErrorMessage(error)).toBe('Form not found')
        })

        it('should extract nested server error from body.params with resource name', () => {
            const error = {
                response: {
                    body: {
                        type: 'SystemException',
                        params: [
                            'Some unexpected technical problem occured: Error getting deployed form: Some unexpected technical problem occured: {"type":"InvalidRequestException","message":"The form with the resource name \'prepare-bank-transfer.html\' cannot be found in deployment with id 2f474185-0b1a-11f1-a358-c2c2b7244840","code":null}'
                        ]
                    }
                }
            }
            const result = extractErrorMessage(error)
            expect(result).toBe("The form with the resource name 'prepare-bank-transfer.html' cannot be found in deployment with id 2f474185-0b1a-11f1-a358-c2c2b7244840")
            expect(isDeployedFormNotFoundError(result)).toBe(true)
            expect(extractDeployedFormName(result)).toBe('prepare-bank-transfer.html')
        })

        it('should return error.message directly', () => {
            const error = new Error('Standard error message')
            expect(extractErrorMessage(error)).toBe('Standard error message')
        })

        it('should extract nested server error from error.message', () => {
            const error = {
                message: 'Some error occurred: {"type":"CustomException","message":"Inner error message"}'
            }
            expect(extractErrorMessage(error)).toBe('Inner error message')
        })

        it('should handle BPM SDK form element error', () => {
            const error = {
                message: 'Form must provide exactly one element <form class="camunda-form">'
            }
            const result = extractErrorMessage(error)
            expect(result).toBe('Form must provide exactly one element <form class="camunda-form">')
            expect(isFormElementError(result)).toBe(true)
        })

        it('should handle validation error message', () => {
            const error = {
                message: "Value 'sdasdasd' is not of type Long"
            }
            const result = extractErrorMessage(error)
            expect(result).toBe("Value 'sdasdasd' is not of type Long")
        })
    })

    describe('isFormElementError', () => {
        it('should return false for null/undefined', () => {
            expect(isFormElementError(null)).toBe(false)
            expect(isFormElementError(undefined)).toBe(false)
        })

        it('should return false for non-string', () => {
            expect(isFormElementError(123)).toBe(false)
            expect(isFormElementError({})).toBe(false)
        })

        it('should return true for form element error', () => {
            expect(isFormElementError('Form must provide exactly one element <form>')).toBe(true)
        })

        it('should return false for other errors', () => {
            expect(isFormElementError('Some other error')).toBe(false)
            expect(isFormElementError('Deployment not found')).toBe(false)
        })
    })

    describe('isDeployedFormNotFoundError', () => {
        it('should return false for null/undefined', () => {
            expect(isDeployedFormNotFoundError(null)).toBe(false)
            expect(isDeployedFormNotFoundError(undefined)).toBe(false)
        })

        it('should return false for non-string', () => {
            expect(isDeployedFormNotFoundError(123)).toBe(false)
            expect(isDeployedFormNotFoundError({})).toBe(false)
        })

        it('should return true for deployment not found error', () => {
            expect(isDeployedFormNotFoundError("The form with the resource name 'myForm' cannot be found in deployment with id 123")).toBe(true)
        })

        it('should return true for Camunda form not found error', () => {
            expect(isDeployedFormNotFoundError('No Camunda Form Definition was found for Camunda Form Ref: CamundaFormRefImpl')).toBe(true)
        })

        it('should return false for other errors', () => {
            expect(isDeployedFormNotFoundError('Some other error')).toBe(false)
            expect(isDeployedFormNotFoundError('Form must provide exactly one element')).toBe(false)
        })
    })

    describe('extractDeployedFormName', () => {
        it('should return null for null/undefined', () => {
            expect(extractDeployedFormName(null)).toBeNull()
            expect(extractDeployedFormName(undefined)).toBeNull()
        })

        it('should return null for non-string', () => {
            expect(extractDeployedFormName(123)).toBeNull()
            expect(extractDeployedFormName({})).toBeNull()
        })

        it('should extract form name from resource name pattern', () => {
            const message = "The form with the resource name 'myForm' cannot be found in deployment with id 123"
            expect(extractDeployedFormName(message)).toBe('myForm')
        })

        it('should extract form name from key pattern', () => {
            const message = 'No Camunda Form Definition was found for Camunda Form Ref: CamundaFormRefImpl [key=myFormKey, version=1]'
            expect(extractDeployedFormName(message)).toBe('myFormKey')
        })

        it('should return null when no pattern matches', () => {
            expect(extractDeployedFormName('Some error without form name')).toBeNull()
        })
    })
})
