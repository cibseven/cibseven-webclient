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
 * Splits a search string into an array of words or phrases.
 *
 * Handles:
 * - Unquoted words separated by whitespace.
 * - Quoted phrases (single or double quotes) as single units.
 * - Escaped characters within quotes (e.g., \" or \').
 * - Ignores empty strings and trims leading/trailing whitespace.
 *
 * Examples:
 *   splitToWords('one two') => ['one', 'two']
 *   splitToWords('"one two" three') => ['one two', 'three']
 *   splitToWords('"a\\"b" c') => ['a\\"b', 'c']
 *
 * @param {string} searchMe - The search query string to split.
 * @returns {string[]} An array of parsed words and phrases.
 */
function splitToWords(searchMe) {
  if (!searchMe || !searchMe.trim()) {
    return []
  }

  const regex = /"([^"]*)"|'([^']*)'|[^\s"']+/g
  const result = []
  let match

  while ((match = regex.exec(searchMe)) !== null) {
    // match[1] is a double-quoted phrase
    // match[2] is a single-quoted phrase
    // match[0] is a unquoted word
    const word = match[1] ?? match[2] ?? match[0]
    if (word !== '') {
      result.push(word)
    }
  }

  return result
}

export {
  splitToWords
}
