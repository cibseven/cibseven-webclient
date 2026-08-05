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
import { describe, it, expect, beforeEach } from 'vitest'
import { registerPlugin, getPlugin, resetPlugins, PLUGIN_API_VERSION } from '@/plugins/pluginsConfig.js'

describe('pluginsConfig', () => {
  beforeEach(() => {
    resetPlugins()
  })

  it('yields an empty array for a slot nothing contributed to', () => {
    expect(getPlugin('unused-slot').value).toEqual([])
  })

  it('returns the same ref for repeated reads of a slot', () => {
    expect(getPlugin('some-slot')).toBe(getPlugin('some-slot'))
  })

  it('registers a contribution with its metadata', () => {
    const component = { name: 'Contribution' }
    registerPlugin('demo', component, { pluginId: 'demo-plugin' })

    expect(getPlugin('demo').value).toEqual([{ component, pluginId: 'demo-plugin' }])
  })

  it('keeps contributions of several plugins to one slot in registration order', () => {
    const first = { name: 'First' }
    const second = { name: 'Second' }
    registerPlugin('demo', first, { pluginId: 'a' })
    registerPlugin('demo', second, { pluginId: 'b' })

    expect(getPlugin('demo').value.map(c => c.pluginId)).toEqual(['a', 'b'])
  })

  it('replaces the array on registration so shallowRef consumers re-render', () => {
    const slot = getPlugin('demo')
    const before = slot.value
    registerPlugin('demo', { name: 'Contribution' })

    expect(slot.value).not.toBe(before)
  })

  it('keeps slots isolated from each other', () => {
    registerPlugin('slot-a', { name: 'A' })

    expect(getPlugin('slot-a').value).toHaveLength(1)
    expect(getPlugin('slot-b').value).toHaveLength(0)
  })

  it('exposes the API version plugins are validated against', () => {
    expect(PLUGIN_API_VERSION).toBe('1')
  })

  it('clears all slots on reset', () => {
    registerPlugin('slot-a', { name: 'A' })
    registerPlugin('slot-b', { name: 'B' })
    resetPlugins()

    expect(getPlugin('slot-a').value).toEqual([])
    expect(getPlugin('slot-b').value).toEqual([])
  })
})
