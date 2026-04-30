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
import { describe, it, expect, afterEach, vi } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import { createStore } from 'vuex'
import { i18n } from '@/i18n'
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

const createMinimalStore = () => createStore({
  getters: {
    highlightedElement: () => null,
    getHistoricActivityStatistics: () => () => [],
  },
  actions: {
    selectActivity: vi.fn(),
    clearActivitySelection: vi.fn(),
    setHighlightedElement: vi.fn(),
    loadActivitiesInstanceHistory: vi.fn(),
    getProcessById: vi.fn()
  },
  modules: {
    calledProcessDefinitions: {
      namespaced: true,
      getters: { getStaticCalledProcessDefinitions: () => [] }
    },
    job: {
      namespaced: true,
      getters: { jobDefinitions: () => [] }
    },
    diagram: {
      namespaced: true,
      actions: { setDiagramReady: vi.fn() }
    }
  }
})

const createWrapper = () => {
  return shallowMount(BpmnViewer, {
    global: {
      plugins: [i18n, createMinimalStore()],
      config: {
        warnHandler: () => {} // suppress $root warnings
      }
    }
  })
}

describe('BpmnViewer - getBadgeOverlayHtml number formatting', () => {
  afterEach(() => {
    localStorage.removeItem('cibseven:preferences:shortenBadgeNumbers')
  })

  it('abbreviates large numbers when shortenBadgeNumbers is not set (default true)', () => {
    // No localStorage entry → defaults to true (abbreviate)
    // abbreviateNumber(1500, 0) rounds to 2K (0 decimal places)
    localStorage.removeItem('cibseven:preferences:shortenBadgeNumbers')
    const wrapper = createWrapper()
    const html = wrapper.vm.getBadgeOverlayHtml(1500, 'bg-info', 'runningInstances', 'act1')
    expect(html).toContain('2K')
    expect(html).not.toContain('1500')
  })

  it('abbreviates large numbers when shortenBadgeNumbers is "true"', () => {
    localStorage.setItem('cibseven:preferences:shortenBadgeNumbers', 'true')
    const wrapper = createWrapper()
    const html = wrapper.vm.getBadgeOverlayHtml(1500, 'bg-info', 'runningInstances', 'act1')
    expect(html).toContain('2K')
    expect(html).not.toContain('1500')
  })

  it('shows full numbers when shortenBadgeNumbers is "false"', () => {
    localStorage.setItem('cibseven:preferences:shortenBadgeNumbers', 'false')
    const wrapper = createWrapper()
    const html = wrapper.vm.getBadgeOverlayHtml(1500, 'bg-info', 'runningInstances', 'act1')
    expect(html).toContain('1500')
    expect(html).not.toContain('1.5K')
  })

  it('shows full numbers below 1000 regardless of preference', () => {
    localStorage.setItem('cibseven:preferences:shortenBadgeNumbers', 'true')
    const wrapper = createWrapper()
    const html = wrapper.vm.getBadgeOverlayHtml(42, 'bg-info', 'runningInstances', 'act1')
    expect(html).toContain('42')
  })
})
