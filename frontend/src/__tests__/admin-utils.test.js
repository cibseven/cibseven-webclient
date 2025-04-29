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
import { isValidEmail } from '@/components/admin/utils'

describe('isValidEmail', () => {
  it('null', () => {
    expect(isValidEmail(null)).toBeNull()
    expect(isValidEmail('')).toBeNull()
  })

  it.each([
    'example@example.com',
    'hi@test.de',
    'user@example.com',  // Valid email
    'first.last@example.com',  // Valid email with period in username
    'user.name123@example.co.uk',  // Valid email with alphanumeric username and country code TLD
    'user_name@example.org',  // Valid email with underscore in username
    'username+tag@example.com',  // Valid email with + sign in username (common for filtering)
    'user123@example.info',  // Valid email with alphanumeric username and .info TLD
    'user@subdomain.example.com',  // Valid email with subdomain in domain
    'user@subdomain.example.travel',  // Valid email with subdomain and .travel TLD
    'user@domain123.example',  // Valid email with numbers in domain name
    'name@domain.withdots.com',  // Valid email with domain containing dots
    'first_last123@example.company',  // Valid email with underscore and alphanumeric in username
    'user@domain.museum',  // Valid email with .museum TLD
    'x@example.com',  // Valid email with single character username
    'a@b.co',  // Valid email with short TLD
  ])('isValidEmail(\'%s\') => false', ( email ) => {
    expect(isValidEmail(email)).toBeTruthy()
  })

  it.each([
    'www.example.com',
    '@test.de',  // Invalid: missing username before "@"
    'test@@test.de',  // Invalid: double "@" in domain name
    'plainaddress',  // Invalid: no "@" symbol, no domain
    'user@com',  // Invalid: domain is too short (missing TLD)
    '@missingusername.com',  // Invalid: missing username before "@"
    'user@.com',  // Invalid: domain starts with a dot
    'user@com.',  // Invalid: domain ends with a dot
    'user@com..com',  // Invalid: double dot in the domain
    'user@domain,com',  // Invalid: comma in the domain
    'user@domain#com',  // Invalid: hash symbol in the domain
    'user@domain@domain.com',  // Invalid: multiple "@" symbols
    'user@.com',  // Invalid: dot before the domain
    'user@domain..com',  // Invalid: double dot in domain name
    'user@domain.com.',  // Invalid: trailing dot in the domain
    'user@domain..com',  // Invalid: consecutive dots in domain
    'user@domain',  // Invalid: missing TLD (e.g., .com)
    'user@domain,com',  // Invalid: comma in domain
  ])('isValidEmail(\'%s\') => false', ( email ) => {
    expect(isValidEmail(email)).toBeFalsy()
  })

  it('Edge cases', () => {
    // Valid: IP address as domain
    expect(isValidEmail('user@123.123.123.123')).toBeTruthy()
    // Valid: quotes around the username (though rare)
    expect(isValidEmail('"user"@example.com')).toBeFalsy()
    // Invalid: double dot in subdomain
    expect(isValidEmail('user@subdomain..example.com')).toBeFalsy()
  })
})
