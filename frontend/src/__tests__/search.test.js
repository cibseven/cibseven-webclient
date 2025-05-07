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
import { splitToWords } from '@/utils/search'

describe('TasksContent', () => {
  it.each([
    [null,  []], // null input → empty result
    [undefined,  []], // undefined input → empty result
    ['',  []], // empty string → empty result
    [' ',  []], // whitespace only → empty result

    ['value', ['value']], // single word
    ['"value"', ['value']], // quoted single word
    ["'value'", ['value']], // single-quoted word

    ['one two', ['one', 'two']], // two words, space-separated
    ['"one" "two"', ['one', 'two']], // two quoted words
    ['"one two"', ['one two']], // quoted phrase as one word

    ['"one two" three', ['one two', 'three']], // quoted phrase + unquoted word
    ['"one two" "three four"', ['one two', 'three four']], // two quoted phrases
    ['one "two three" four', ['one', 'two three', 'four']], // mix of quoted and unquoted

    ['" spaced "  text ', [' spaced ', 'text']], // quoted word with internal spaces
    ['"a""b""c"', ['a', 'b', 'c']], // back-to-back quoted single letters

    ['"a b"c d', ['a b', 'c', 'd']], // quoted + adjacent unquoted words
    ['a    b    c', ['a', 'b', 'c']], // multiple spaces between unquoted words
    ['   "one"    two   ', ['one', 'two']], // leading/trailing/multiple spaces

    ['""', []], // empty quoted string
    ['" "', [' ']], // quoted space (preserved)

    ['"multi word"single', ['multi word', 'single']], // quoted phrase + unquoted word glued on

    ['"tricky\\"quote"', ['tricky\\', 'quote']], // escaped quote inside string (no parsing here)
    ['"quote with inner \'single\' quotes"', ['quote with inner \'single\' quotes']], // nested quotes
    ['\'quote with inner "single" quotes\'', ['quote with inner "single" quotes']], // nested quotes
    ['"nested "quotes"', ['nested ', 'quotes']] // malformed nested quotes; depends on parser

  ])('splitToWords("%s") → %j', (query, expectedWords) => {
    expect(splitToWords(query)).toEqual(expectedWords)
  })
})
