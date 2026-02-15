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

// Error message pattern constants
const ERROR_PATTERNS = {
    FORM_ELEMENT: 'Form must provide exactly one element',
    DEPLOYMENT_NOT_FOUND: 'cannot be found in deployment with id',
    CAMUNDA_FORM_NOT_FOUND: 'No Camunda Form Definition was found'
};

/**
 * Extract server error details from error messages that contain embedded JSON.
 * Engine errors often include stringified JSON like: "... occurred: {\"type\":\"...\",\"message\":\"...\"}"
 * @param {string} message - Error message that may contain embedded JSON
 * @returns {Object|null} Parsed error object with type and message, or null if not found
 */
function extractServerErrorFromMessage(message) {
    if (!message || typeof message !== 'string') {
        return null;
    }

    const lastBraceIndex = message.lastIndexOf('{');
    if (lastBraceIndex === -1) {
        return null;
    }

    // Try to extract JSON substring - attempt from last '{' to end first
    let jsonSubstring = message.substring(lastBraceIndex);

    // Handle escaped quotes that might be present in the error message
    // Check if the string appears to have escaped JSON (contains \" but parsing would fail)
    const hasEscapedQuotes = jsonSubstring.includes('\\"');
    if (hasEscapedQuotes) {
        // Try to determine if we need to unescape by checking if JSON.parse would fail
        try {
            JSON.parse(jsonSubstring);
            // If it parses successfully, don't unescape
        } catch {
            // If parsing fails, try unescaping
            jsonSubstring = jsonSubstring.replace(/\\"/g, '"');
        }
    }

    // Try to parse and find the first valid JSON object
    let depth = 0;
    let endIndex = -1;

    for (let i = 0; i < jsonSubstring.length; i++) {
        if (jsonSubstring[i] === '{') {
            depth++;
        } else if (jsonSubstring[i] === '}') {
            depth--;
            if (depth === 0) {
                endIndex = i + 1;
                break;
            }
        }
    }

    if (endIndex > 0) {
        jsonSubstring = jsonSubstring.substring(0, endIndex);
    }

    try {
        const parsedError = JSON.parse(jsonSubstring);
        if (parsedError && typeof parsedError === 'object' && parsedError.type && parsedError.message) {
            return parsedError;
        }
    } catch {
        // Not valid JSON, ignore
    }

    return null;
}

/**
 * Extract error message from a response data object (works for both Axios and BPM SDK).
 * @param {Object} data - Response data object (error.response.data or error.response.body)
 * @returns {string|null} Extracted error message or null if not found
 */
function extractFromResponseData(data) {
    if (!data || typeof data !== 'object') {
        return null;
    }

    // Check for params array first (most specific error messages)
    if (data.params && Array.isArray(data.params) && data.params.length > 0) {
        const message = data.params[0];
        if (typeof message === 'string') {
            const serverError = extractServerErrorFromMessage(message);
            if (serverError) {
                return serverError.message;
            }
            return message;
        }
    }

    // Fall back to message property
    if (data.message && typeof data.message === 'string') {
        return data.message;
    }

    return null;
}

/**
 * Extract a human-readable error message from various error formats.
 * Handles Axios errors, BPM SDK errors, Error objects, and plain strings.
 * Attempts to extract the most specific message from nested engine error responses.
 * @param {*} error - Error in any format (Axios error, BPM SDK error, Error, string, etc.)
 * @param {string} [fallback] - Fallback message if no meaningful message can be extracted
 * @returns {string} Human-readable error message
 */
export function extractErrorMessage(error, fallback) {
    if (!error) {
        return fallback || 'An unexpected error occurred';
    }

    // Plain string - use as-is
    if (typeof error === 'string') {
        return error;
    }

    // Axios errors - error.response.data contains the server response body
    if (typeof error === 'object' && error.response && error.response.data) {
        const extracted = extractFromResponseData(error.response.data);
        if (extracted) {
            return extracted;
        }
    }

    // BPM SDK errors - error.response.body contains the server response
    if (typeof error === 'object' && error.response && error.response.body) {
        const extracted = extractFromResponseData(error.response.body);
        if (extracted) {
            return extracted;
        }
    }

    // Error objects or objects with message property
    if (error.message) {
        const serverError = extractServerErrorFromMessage(error.message);
        if (serverError) {
            return serverError.message;
        }
        return error.message;
    }

    // Fallback
    return fallback || 'An unexpected error occurred';
}

/**
 * Check if the error message is the BPM SDK "Form must provide exactly one element <form ..>" error.
 * This typically means the form HTML is empty or the form resource was not deployed.
 * @param {string} message - Already extracted error message string
 * @returns {boolean} True if this is a form-element SDK error
 */
export function isFormElementError(message) {
    if (!message || typeof message !== 'string') return false;
    return message.includes(ERROR_PATTERNS.FORM_ELEMENT);
}

/**
 * Check if the error message indicates a deployed form resource was not found.
 * Matches engine errors like:
 * - "The form with the resource name '...' cannot be found in deployment with id ..."
 * - "No Camunda Form Definition was found for Camunda Form Ref: CamundaFormRefImpl [key=..., ...]"
 * @param {string} message - Already extracted error message string
 * @returns {boolean} True if this is a deployed-form-not-found error
 */
export function isDeployedFormNotFoundError(message) {
    if (!message || typeof message !== 'string') return false;
    return message.includes(ERROR_PATTERNS.DEPLOYMENT_NOT_FOUND)
        || message.includes(ERROR_PATTERNS.CAMUNDA_FORM_NOT_FOUND);
}

/**
 * Extract the form name from a deployed form not-found error message.
 * Matches engine errors like:
 * - "The form with the resource name 'X' cannot be found in deployment with id ..."
 * - "No Camunda Form Definition was found for Camunda Form Ref: CamundaFormRefImpl [key=X, ...]"
 * @param {string} message - Already extracted error message string
 * @returns {string|null} The extracted form name, or null if no match
 */
export function extractDeployedFormName(message) {
    if (!message || typeof message !== 'string') return null;

    // First pattern: resource name 'formName'
    const resourceNameMatch = message.match(/resource name '([^']+)'/);
    if (resourceNameMatch) {
        return resourceNameMatch[1];
    }

    // Second pattern: [key=formName, ...]
    const keyMatch = message.match(/\[key=([^,\]]+)/);
    if (keyMatch) {
        return keyMatch[1];
    }

    return null;
}
