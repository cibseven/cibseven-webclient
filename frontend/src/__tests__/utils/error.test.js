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

import { describe, it, expect } from 'vitest';
import {
    extractErrorMessage,
    isFormElementError,
    isDeployedFormNotFoundError,
    extractDeployedFormName
} from '../../utils/error.js';

describe('extractErrorMessage', () => {
    it('should extract message from Axios error with response data', () => {
        const error = {
            response: {
                data: {
                    message: 'Test error message'
                }
            }
        };
        expect(extractErrorMessage(error)).toBe('Test error message');
    });

    it('should extract message from Axios error with params array', () => {
        const error = {
            response: {
                data: {
                    params: ['Specific error from params'],
                    message: 'Generic message'
                }
            }
        };
        expect(extractErrorMessage(error)).toBe('Specific error from params');
    });

    it('should extract message from BPM SDK error with response body', () => {
        const error = {
            response: {
                body: {
                    message: 'BPM SDK error message'
                }
            }
        };
        expect(extractErrorMessage(error)).toBe('BPM SDK error message');
    });

    it('should extract nested JSON from error message with escaped quotes', () => {
        const error = {
            message: 'An error occurred: {"type":"ServerError","message":"Database connection failed"}'
        };
        expect(extractErrorMessage(error)).toBe('Database connection failed');
    });

    it('should extract nested JSON from error message with doubly-escaped quotes', () => {
        const error = {
            response: {
                data: {
                    params: ['An error occurred: {\\"type\\":\\"ServerError\\",\\"message\\":\\"Database connection failed\\"}']
                }
            }
        };
        expect(extractErrorMessage(error)).toBe('Database connection failed');
    });

    it('should handle error message with braces inside quoted strings', () => {
        const error = {
            message: 'Error occurred: {"type":"ValidationError","message":"Field {name} is required"}'
        };
        expect(extractErrorMessage(error)).toBe('Field {name} is required');
    });

    it('should handle error message with nested JSON objects', () => {
        const error = {
            message: 'Error: {"type":"ComplexError","message":"Nested error","details":{"code":500}}'
        };
        expect(extractErrorMessage(error)).toBe('Nested error');
    });

    it('should handle plain string errors', () => {
        expect(extractErrorMessage('Plain error string')).toBe('Plain error string');
    });

    it('should handle Error objects with plain messages', () => {
        const error = new Error('Standard error message');
        expect(extractErrorMessage(error)).toBe('Standard error message');
    });

    it('should use fallback for null or undefined', () => {
        expect(extractErrorMessage(null, 'Fallback message')).toBe('Fallback message');
        expect(extractErrorMessage(undefined, 'Fallback message')).toBe('Fallback message');
    });

    it('should use default fallback when no fallback provided', () => {
        expect(extractErrorMessage(null)).toBe('An unexpected error occurred');
    });

    it('should handle error message with escaped backslashes and quotes', () => {
        const error = {
            message: 'Error: {"type":"PathError","message":"Path C:\\\\"Program Files\\\\" not found"}'
        };
        expect(extractErrorMessage(error)).toBe('Path C:\\"Program Files\\" not found');
    });

    it('should handle legitimate backslash-quote in message content', () => {
        // This is a case where \" is actual content, not an escape sequence
        const error = {
            message: 'Error: {"type":"InfoError","message":"Use backslash-quote like this: \\\\\\"hello\\\\\\""}'
        };
        const result = extractErrorMessage(error);
        expect(result).toContain('backslash-quote');
    });

    it('should handle incomplete JSON gracefully', () => {
        const error = {
            message: 'Error occurred: {"type":"Incomplete"'
        };
        expect(extractErrorMessage(error)).toBe('Error occurred: {"type":"Incomplete"');
    });

    it('should handle message with multiple JSON-like structures', () => {
        const error = {
            message: 'First {"invalid":} then {"type":"RealError","message":"Real message"}'
        };
        expect(extractErrorMessage(error)).toBe('Real message');
    });
});

describe('isFormElementError', () => {
    it('should detect form element errors', () => {
        const message = 'Form must provide exactly one element <form camunda:formKey="embedded:deployment:forms/test.html">';
        expect(isFormElementError(message)).toBe(true);
    });

    it('should return false for non-form-element errors', () => {
        expect(isFormElementError('Some other error')).toBe(false);
    });

    it('should handle null or undefined', () => {
        expect(isFormElementError(null)).toBe(false);
        expect(isFormElementError(undefined)).toBe(false);
    });
});

describe('isDeployedFormNotFoundError', () => {
    it('should detect deployment not found error', () => {
        const message = "The form with the resource name 'myForm.html' cannot be found in deployment with id 123";
        expect(isDeployedFormNotFoundError(message)).toBe(true);
    });

    it('should detect Camunda form not found error', () => {
        const message = 'No Camunda Form Definition was found for Camunda Form Ref: CamundaFormRefImpl [key=myForm, ...]';
        expect(isDeployedFormNotFoundError(message)).toBe(true);
    });

    it('should return false for other errors', () => {
        expect(isDeployedFormNotFoundError('Some other error')).toBe(false);
    });

    it('should handle null or undefined', () => {
        expect(isDeployedFormNotFoundError(null)).toBe(false);
        expect(isDeployedFormNotFoundError(undefined)).toBe(false);
    });
});

describe('extractDeployedFormName', () => {
    it('should extract form name from resource name pattern', () => {
        const message = "The form with the resource name 'myForm.html' cannot be found in deployment";
        expect(extractDeployedFormName(message)).toBe('myForm.html');
    });

    it('should extract form name from key pattern', () => {
        const message = 'No Camunda Form Definition was found for Camunda Form Ref: CamundaFormRefImpl [key=myFormKey, binding=latest]';
        expect(extractDeployedFormName(message)).toBe('myFormKey');
    });

    it('should return null for messages without form name', () => {
        expect(extractDeployedFormName('Some other error')).toBe(null);
    });

    it('should handle null or undefined', () => {
        expect(extractDeployedFormName(null)).toBe(null);
        expect(extractDeployedFormName(undefined)).toBe(null);
    });
});
