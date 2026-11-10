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
import { mount } from '@vue/test-utils'
import NavIcon from '@/components/common-components/navigation/NavIcon.vue'

describe('NavIcon.vue', () => {
  it('draws the font icon at 24px by default', () => {
    const icon = mount(NavIcon, { props: { icon: 'mdi-speedometer' } })

    expect(icon.classes()).toEqual(['mdi', 'mdi-24px', 'mdi-speedometer'])
    expect(icon.attributes('style')).toBe('width: 24px; height: 24px;')
    expect(icon.attributes('aria-hidden')).toBe('true')
  })

  // Small files are inlined as data URIs, so the URL is quoted in the mask
  it('draws an image instead of the font icon, as a mask in the text colour', () => {
    const image = "data:image/svg+xml,%3csvg width='24'%3e"
    const icon = mount(NavIcon, { props: { icon: 'mdi-speedometer', image } })

    expect(icon.classes()).toEqual(['cib-nav-icon-image'])
    expect(icon.attributes('style')).toContain(`--cib-nav-icon: url("${image}")`)
    expect(icon.attributes('style')).toContain('width: 24px; height: 24px;')
  })

  it('keeps its box without an icon, so a column of labels stays lined up', () => {
    const icon = mount(NavIcon, { props: { size: 18 } })

    expect(icon.classes()).toEqual(['mdi', 'mdi-18px'])
    expect(icon.attributes('style')).toBe('width: 18px; height: 18px;')
  })
})
