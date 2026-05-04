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
import { describe, it, expect, afterEach, vi, beforeAll, beforeEach } from 'vitest'
import BpmnViewer from '@/components/process/BpmnViewer.vue'

// Mock bpmn-js NavigatedViewer - it requires a DOM canvas and is not relevant here
vi.mock('bpmn-js/lib/NavigatedViewer', () => {
  return {
    default: vi.fn().mockImplementation(() => ({
      on: vi.fn(),
      destroy: vi.fn(),
      importXML: vi.fn(() => Promise.resolve()),
      get: vi.fn()
    }))
  }
})

const getBadgeOverlayHtml = (number, classes, type, activityId) => {
  return BpmnViewer.methods.getBadgeOverlayHtml.call(
    { $t: (key) => key },
    number,
    classes,
    type,
    activityId
  )
}

describe('BpmnViewer - getBadgeOverlayHtml number formatting', () => {
  beforeAll(() => {
    localStorage.removeItem('cibseven:preferences:shortenBadgeNumbers')
  })

  afterEach(() => {
    localStorage.setItem('cibseven:preferences:shortenBadgeNumbers', 'true')
  })

  it('abbreviates large numbers when shortenBadgeNumbers is not set (default true)', () => {
    const html = getBadgeOverlayHtml(1500, 'bg-info', 'runningInstances', 'act1')
    expect(html).toContain('2K')
    expect(html).not.toContain('1500')
  })

  it('abbreviates large numbers when shortenBadgeNumbers is "true"', () => {
    const html = getBadgeOverlayHtml(1500, 'bg-info', 'runningInstances', 'act1')
    expect(html).toContain('2K')
    expect(html).not.toContain('1500')
  })

  it('abbreviates large numbers when shortenBadgeNumbers is "true"', () => {
    const html = getBadgeOverlayHtml(1499, 'bg-info', 'runningInstances', 'act1')
    expect(html).toContain('1K')
    expect(html).not.toContain('1499')
  })

  it('abbreviates large numbers when shortenBadgeNumbers is "true"', () => {
    const html = getBadgeOverlayHtml(1500000, 'bg-info', 'runningInstances', 'act1')
    expect(html).toContain('2M')
    expect(html).not.toContain('1500000')
  })

  it('shows full numbers below 1000 regardless of preference', () => {
    const html = getBadgeOverlayHtml(42, 'bg-info', 'runningInstances', 'act1')
    expect(html).toContain('42')
  })
})

describe('BpmnViewer - getBadgeOverlayHtml numbers not formatting', () => {
  beforeEach(() => {
    localStorage.setItem('cibseven:preferences:shortenBadgeNumbers', 'false') // Ensure full number for easier testing
  })

  it('shows full numbers below 1000 regardless of preference', () => {
    const html = getBadgeOverlayHtml(42, 'bg-info', 'runningInstances', 'act1')
    expect(html).toContain('42')
  })
  
  it('shows full numbers when shortenBadgeNumbers is "false"', () => {
    const html = getBadgeOverlayHtml(1500, 'bg-info', 'runningInstances', 'act1')
    expect(html).toContain('1500')
    expect(html).not.toContain('1.5K')
  })
  
  it('shows full numbers when shortenBadgeNumbers is "false"', () => {
    const html = getBadgeOverlayHtml(1500000, 'bg-info', 'runningInstances', 'act1')
    expect(html).toContain('1500000')
    expect(html).not.toContain('2M')
  })

  it('shows full numbers when shortenBadgeNumbers is "false"', () => {
    const html = getBadgeOverlayHtml(1499, 'bg-info', 'runningInstances', 'act1')
    expect(html).toContain('1499')
    expect(html).not.toContain('1K')
  })
})
