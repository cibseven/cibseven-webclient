/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { describe, it, expect } from 'vitest'
import { buildVersion, versionedUrl } from '@/utils/assetVersion.js'

// A logo, favicon or translation file replaced under its own name must not come from the cache after an update:
// its URL carries the build, so a new build is a new URL
describe('versionedUrl', () => {
  it('takes the build version that vite.config.js defines (vitest.config.js fixes it to "test")', () => {
    expect(buildVersion).toBe('test')
    expect(versionedUrl('themes/cib/logo.svg')).toBe('themes/cib/logo.svg?v=test')
  })

  it('gives the file a new URL when the version changes', () => {
    expect(versionedUrl('themes/cib/logo.svg', '1')).not.toBe(versionedUrl('themes/cib/logo.svg', '2'))
  })

  it('appends the version to a query the URL already has', () => {
    expect(versionedUrl('config.json?tenant=a', '1')).toBe('config.json?tenant=a&v=1')
  })

  it('keeps a fragment at the end', () => {
    expect(versionedUrl('themes/cib/icons.svg#home', '1')).toBe('themes/cib/icons.svg?v=1#home')
  })

  it('encodes a version that is not URL-safe', () => {
    expect(versionedUrl('logo.svg', '3.0.0 dev&1')).toBe('logo.svg?v=3.0.0%20dev%261')
  })

  it('leaves the URL as it is without a version', () => {
    expect(versionedUrl('themes/cib/logo.svg', '')).toBe('themes/cib/logo.svg')
  })
})
