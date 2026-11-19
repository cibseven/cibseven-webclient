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
import { leadingIcon, trailingIcon } from '@/components/common-components/navigation/navItemIcons.js'

describe('navItemIcons', () => {
  it('puts an icon in front of the label by default', () => {
    const item = { label: 'Settings', icon: 'mdi-cog' }
    expect(leadingIcon(item)).toBe('mdi-cog')
    expect(trailingIcon(item)).toBeNull()
  })

  it('puts the icon of an iconEnd entry after the label', () => {
    const item = { label: 'Logout', icon: 'mdi-logout', iconEnd: true }
    expect(leadingIcon(item)).toBeNull()
    expect(trailingIcon(item)).toBe('mdi-logout')
  })

  it('has neither for an entry without an icon', () => {
    for (const item of [{ label: 'Plain' }, { label: 'Plain', iconEnd: true }]) {
      expect(leadingIcon(item)).toBeNull()
      expect(trailingIcon(item)).toBeNull()
    }
  })
})
