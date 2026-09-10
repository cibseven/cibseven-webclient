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

/**
 * Exercise BpmnViewer's own logic without mounting it.
 *
 * The component owns a bpmn-js NavigatedViewer, a canvas and an overlay registry, none of
 * which exist meaningfully in jsdom. Its statistics merging and badge placement are pure
 * functions of props though, so calling the options object's methods against a hand-built
 * `this` keeps the unit under test to that logic.
 */
function viewerContext(overrides = {}) {
  const vm = {
    $t: (key) => key,
    selectedInstance: null,
    activityInstance: null,
    statistics: null,
    badgeOptions: {},
    overlayList: [],
    historicActivityStatistics: [],
    getStaticCalledProcessDefinitions: [],
    setHtmlOnDiagram: vi.fn(),
    ...overrides
  }
  for (const [name, method] of Object.entries(BpmnViewer.methods)) {
    if (!(name in vm)) vm[name] = method.bind(vm)
    else if (typeof vm[name] === 'function' && vm[name].mock) continue
  }
  // Bind the real implementations for anything the caller did not stub.
  for (const [name, method] of Object.entries(BpmnViewer.methods)) {
    if (overrides[name] === undefined) vm[name] = method.bind(vm)
  }
  return vm
}

describe('BpmnViewer - getMergedStatistics dispatch', () => {
  it('should use the instance merge when an instance is selected', () => {
    const vm = viewerContext({
      selectedInstance: { id: 'pi1' },
      getInstanceMergedStatistics: vi.fn(() => 'instance'),
      getProcessMergedStatistics: vi.fn(() => 'process'),
      getProcessMergedStatisticsFullHistory: vi.fn(() => 'full')
    })

    expect(BpmnViewer.methods.getMergedStatistics.call(vm, [], 'full')).toBe('instance')
  })

  it('should use the runtime process merge when history is not full', () => {
    const vm = viewerContext({
      getInstanceMergedStatistics: vi.fn(() => 'instance'),
      getProcessMergedStatistics: vi.fn(() => 'process'),
      getProcessMergedStatisticsFullHistory: vi.fn(() => 'full')
    })

    expect(BpmnViewer.methods.getMergedStatistics.call(vm, [], 'activity')).toBe('process')
  })

  it('should use the full-history process merge otherwise', () => {
    const vm = viewerContext({
      getInstanceMergedStatistics: vi.fn(() => 'instance'),
      getProcessMergedStatistics: vi.fn(() => 'process'),
      getProcessMergedStatisticsFullHistory: vi.fn(() => 'full')
    })

    expect(BpmnViewer.methods.getMergedStatistics.call(vm, [], 'full')).toBe('full')
  })
})

describe('BpmnViewer - getInstanceMergedStatistics', () => {
  const call = (vm, history, level) => BpmnViewer.methods.getInstanceMergedStatistics.call(vm, history, level)

  it('should return the history untouched when there is no activity tree yet', () => {
    const history = [{ id: 'task_1', instances: 3 }]

    expect(call(viewerContext(), history, 'full')).toBe(history)
  })

  // Transition instances are activities caught between sequence flows; they only exist in
  // the runtime tree, so their count has to be layered onto the history entry.
  it('should override the history instance count from transition instances', () => {
    const vm = viewerContext({
      activityInstance: { childTransitionInstances: [{ activityId: 'task_1' }, { activityId: 'task_1' }] }
    })

    const merged = call(vm, [{ id: 'task_1', instances: 99, finished: 5 }], 'full')

    expect(merged).toEqual([{ id: 'task_1', instances: 2, finished: 5 }])
  })

  it('should leave history entries with no runtime counterpart alone', () => {
    const vm = viewerContext({ activityInstance: { childTransitionInstances: [] } })
    const stat = { id: 'task_1', instances: 3 }

    expect(call(vm, [stat], 'full')[0]).toBe(stat)
  })

  it('should append runtime-only activities that history does not know', () => {
    const vm = viewerContext({
      activityInstance: { childTransitionInstances: [{ activityId: 'task_new' }] }
    })

    const merged = call(vm, [], 'full')

    expect(merged).toEqual([{
      id: 'task_new',
      instances: 1,
      finished: 0,
      canceled: 0,
      openIncidents: 0,
      resolvedIncidents: 0,
      deletedIncidents: 0
    }])
  })

  // Below the 'full' history level the history API reports no incidents at all, so both
  // the counts and the incidents are taken from the runtime tree instead.
  it('should read incidents from the runtime tree when history is not full', () => {
    const vm = viewerContext({
      activityInstance: {
        childActivityInstances: [{ activityId: 'task_1', incidents: [{}, {}] }],
        childTransitionInstances: []
      }
    })

    const merged = call(vm, [{ id: 'task_1', instances: 0, openIncidents: 0 }], 'activity')

    expect(merged[0]).toMatchObject({ id: 'task_1', instances: 1, openIncidents: 2 })
  })

  it('should ignore child activity instances when history is full', () => {
    const vm = viewerContext({
      activityInstance: {
        childActivityInstances: [{ activityId: 'task_1', incidents: [{}] }],
        childTransitionInstances: []
      }
    })

    const stat = { id: 'task_1', instances: 7 }
    expect(call(vm, [stat], 'full')[0]).toBe(stat)
  })

  it('should sum transition and activity instances for the same activity', () => {
    const vm = viewerContext({
      activityInstance: {
        childTransitionInstances: [{ activityId: 'task_1' }],
        childActivityInstances: [{ activityId: 'task_1' }]
      }
    })

    const merged = call(vm, [], 'activity')

    expect(merged[0]).toMatchObject({ id: 'task_1', instances: 2 })
  })

  it('should default a missing incident list to zero incidents', () => {
    const vm = viewerContext({
      activityInstance: { childActivityInstances: [{ activityId: 'task_1' }], childTransitionInstances: [] }
    })

    expect(call(vm, [], 'activity')[0]).toMatchObject({ openIncidents: 0 })
  })

  it('should tolerate an activity tree with neither child collection', () => {
    const vm = viewerContext({ activityInstance: {} })

    expect(call(vm, [{ id: 'task_1', instances: 1 }], 'full')).toEqual([{ id: 'task_1', instances: 1 }])
  })
})

describe('BpmnViewer - getProcessMergedStatistics', () => {
  const call = (vm, history) => BpmnViewer.methods.getProcessMergedStatistics.call(vm, history)

  it.each([[null], [[]]])('should fall back to the history when runtime statistics are %j', (statistics) => {
    const vm = viewerContext({ statistics })
    const history = [{ id: 'task_1', instances: 2 }]

    expect(call(vm, history)).toBe(history)
  })

  it('should return an empty array when there is nothing at all', () => {
    expect(call(viewerContext({ statistics: [] }), null)).toEqual([])
  })

  // The history instance count wins when present, because it also covers activities that
  // have since finished.
  it('should prefer the history instance count over the runtime one', () => {
    const vm = viewerContext({ statistics: [{ id: 'task_1', instances: 1 }] })

    expect(call(vm, [{ id: 'task_1', instances: 9, finished: 4 }])[0]).toMatchObject({
      instances: 9, finished: 4
    })
  })

  it('should use the runtime instance count when history has none', () => {
    const vm = viewerContext({ statistics: [{ id: 'task_1', instances: 3 }] })

    expect(call(vm, [])[0]).toMatchObject({ id: 'task_1', instances: 3 })
  })

  it('should default a missing runtime instance count to zero', () => {
    const vm = viewerContext({ statistics: [{ id: 'task_1' }] })

    expect(call(vm, [])[0]).toMatchObject({ instances: 0 })
  })

  // Open incidents come only from runtime and arrive grouped by type, so they are summed.
  it('should sum runtime incident counts across incident types', () => {
    const vm = viewerContext({
      statistics: [{ id: 'task_1', instances: 1, incidents: [{ incidentCount: 2 }, { incidentCount: 3 }] }]
    })

    expect(call(vm, [])[0]).toMatchObject({ openIncidents: 5 })
  })

  it('should tolerate incident entries without a count', () => {
    const vm = viewerContext({ statistics: [{ id: 'task_1', incidents: [{}] }] })

    expect(call(vm, [])[0]).toMatchObject({ openIncidents: 0 })
  })

  it('should default the history-only counters to zero', () => {
    const vm = viewerContext({ statistics: [{ id: 'task_1' }] })

    expect(call(vm, [])[0]).toMatchObject({
      finished: 0, canceled: 0, resolvedIncidents: 0, deletedIncidents: 0
    })
  })

  it('should append history entries with no runtime counterpart', () => {
    const vm = viewerContext({ statistics: [{ id: 'task_1' }] })

    const merged = call(vm, [{ id: 'task_gone', finished: 4 }])

    expect(merged.map(s => s.id)).toEqual(['task_1', 'task_gone'])
  })

  it('should tolerate a null history', () => {
    const vm = viewerContext({ statistics: [{ id: 'task_1' }] })

    expect(call(vm, null)).toHaveLength(1)
  })
})

describe('BpmnViewer - getProcessMergedStatisticsFullHistory', () => {
  const call = (vm, history) => BpmnViewer.methods.getProcessMergedStatisticsFullHistory.call(vm, history)

  it('should keep the history instance count when it has one', () => {
    const vm = viewerContext({ statistics: [{ id: 'task_1', instances: 1 }] })

    expect(call(vm, [{ id: 'task_1', instances: 9 }])).toEqual([{ id: 'task_1', instances: 9 }])
  })

  // A finished activity has no history instance count, so the runtime figure fills in.
  it('should fill a missing history instance count from runtime', () => {
    const vm = viewerContext({ statistics: [{ id: 'task_1', instances: 4 }] })

    expect(call(vm, [{ id: 'task_1', instances: null }])).toEqual([{ id: 'task_1', instances: 4 }])
  })

  it('should pass through history entries with no runtime counterpart', () => {
    const vm = viewerContext({ statistics: [] })
    const stat = { id: 'task_1', instances: 2 }

    expect(call(vm, [stat])[0]).toBe(stat)
  })

  it('should tolerate absent runtime statistics', () => {
    const vm = viewerContext({ statistics: null })
    const stat = { id: 'task_1' }

    expect(call(vm, [stat])[0]).toBe(stat)
  })
})

describe('BpmnViewer - badge placement', () => {
  const stat = (overrides = {}) => ({
    id: 'task_1', instances: 0, finished: 0, canceled: 0,
    openIncidents: 0, resolvedIncidents: 0, deletedIncidents: 0, ...overrides
  })

  const placements = (vm) => vm.setHtmlOnDiagram.mock.calls.map(([, , position]) => position)

  describe('drawActivityBadges', () => {
    it('should draw nothing for an activity absent from the diagram', () => {
      const vm = viewerContext({ setHtmlOnDiagram: vi.fn() })
      const elementRegistry = { get: vi.fn(() => null) }

      BpmnViewer.methods.drawActivityBadges.call(vm, stat({ instances: 5 }), elementRegistry)

      expect(vm.setHtmlOnDiagram).not.toHaveBeenCalled()
    })

    it('should draw every enabled badge type for an activity on the diagram', () => {
      const vm = viewerContext({
        setHtmlOnDiagram: vi.fn(),
        selectedInstance: { id: 'pi1' },
        badgeOptions: {
          showCanceled: true, showHistory: true, showClosedIncidents: true,
          showRunning: true, showIncidents: true
        }
      })
      const elementRegistry = { get: vi.fn(() => ({ id: 'task_1' })) }

      BpmnViewer.methods.drawActivityBadges.call(
        vm,
        stat({ canceled: 1, finished: 3, resolvedIncidents: 1, instances: 2, openIncidents: 4 }),
        elementRegistry
      )

      expect(vm.setHtmlOnDiagram).toHaveBeenCalledTimes(5)
    })

    it('should fall back to no badge options at all', () => {
      const vm = viewerContext({ setHtmlOnDiagram: vi.fn(), badgeOptions: null })
      const elementRegistry = { get: vi.fn(() => ({ id: 'task_1' })) }

      BpmnViewer.methods.drawActivityBadges.call(vm, stat({ instances: 5 }), elementRegistry)

      expect(vm.setHtmlOnDiagram).not.toHaveBeenCalled()
    })
  })

  describe('drawCanceledBadge', () => {
    it('should place a canceled badge bottom-right', () => {
      const vm = viewerContext({ setHtmlOnDiagram: vi.fn() })

      BpmnViewer.methods.drawCanceledBadge.call(vm, stat({ canceled: 2 }), { showCanceled: true })

      expect(vm.setHtmlOnDiagram).toHaveBeenCalledWith('task_1', expect.stringContaining('bg-warning'), { bottom: 15, right: 13 })
    })

    it.each([
      ['the option is off', stat({ canceled: 2 }), {}],
      ['there are none', stat({ canceled: 0 }), { showCanceled: true }]
    ])('should draw nothing when %s', (_label, s, options) => {
      const vm = viewerContext({ setHtmlOnDiagram: vi.fn() })

      BpmnViewer.methods.drawCanceledBadge.call(vm, s, options)

      expect(vm.setHtmlOnDiagram).not.toHaveBeenCalled()
    })
  })

  describe('drawFinishedBadge', () => {
    // Canceled instances are also counted as finished, so they are subtracted to avoid
    // showing them twice.
    it('should exclude canceled instances from the finished count', () => {
      const vm = viewerContext({ setHtmlOnDiagram: vi.fn() })

      BpmnViewer.methods.drawFinishedBadge.call(vm, stat({ finished: 5, canceled: 2 }), { showHistory: true })

      expect(vm.setHtmlOnDiagram).toHaveBeenCalledWith('task_1', expect.stringContaining('>3<'), expect.any(Object))
    })

    it('should stack above the canceled badge when one is present', () => {
      const vm = viewerContext({ setHtmlOnDiagram: vi.fn() })

      BpmnViewer.methods.drawFinishedBadge.call(vm, stat({ finished: 5, canceled: 2 }), { showHistory: true })

      expect(placements(vm)[0]).toEqual({ bottom: 35, right: 13 })
    })

    it('should sit at the bottom when there is no canceled badge', () => {
      const vm = viewerContext({ setHtmlOnDiagram: vi.fn() })

      BpmnViewer.methods.drawFinishedBadge.call(vm, stat({ finished: 5 }), { showHistory: true })

      expect(placements(vm)[0]).toEqual({ bottom: 15, right: 13 })
    })

    it('should draw nothing when every finished instance was canceled', () => {
      const vm = viewerContext({ setHtmlOnDiagram: vi.fn() })

      BpmnViewer.methods.drawFinishedBadge.call(vm, stat({ finished: 2, canceled: 2 }), { showHistory: true })

      expect(vm.setHtmlOnDiagram).not.toHaveBeenCalled()
    })

    it('should draw nothing when history badges are off', () => {
      const vm = viewerContext({ setHtmlOnDiagram: vi.fn() })

      BpmnViewer.methods.drawFinishedBadge.call(vm, stat({ finished: 5 }), {})

      expect(vm.setHtmlOnDiagram).not.toHaveBeenCalled()
    })
  })

  describe('drawRunningInstancesBadge', () => {
    it('should place the running badge bottom-left', () => {
      const vm = viewerContext({ setHtmlOnDiagram: vi.fn() })

      BpmnViewer.methods.drawRunningInstancesBadge.call(vm, stat({ instances: 3 }), { showRunning: true })

      expect(vm.setHtmlOnDiagram).toHaveBeenCalledWith('task_1', expect.stringContaining('bg-info'), { bottom: 15, left: -7 })
    })

    it.each([
      ['the option is off', stat({ instances: 3 }), {}],
      ['there are none', stat({ instances: 0 }), { showRunning: true }]
    ])('should draw nothing when %s', (_label, s, options) => {
      const vm = viewerContext({ setHtmlOnDiagram: vi.fn() })

      BpmnViewer.methods.drawRunningInstancesBadge.call(vm, s, options)

      expect(vm.setHtmlOnDiagram).not.toHaveBeenCalled()
    })
  })

  describe('drawOpenIncidentsBadge', () => {
    // The incident badge shifts right so it does not cover the running-instances badge.
    it('should shift right of the running badge when instances are shown', () => {
      const vm = viewerContext({ setHtmlOnDiagram: vi.fn() })

      BpmnViewer.methods.drawOpenIncidentsBadge.call(vm, stat({ instances: 2, openIncidents: 1 }), { showIncidents: true })

      expect(placements(vm)[0]).toEqual({ bottom: 15, left: 18 })
    })

    it('should take the bottom-left slot when there are no running instances', () => {
      const vm = viewerContext({ setHtmlOnDiagram: vi.fn() })

      BpmnViewer.methods.drawOpenIncidentsBadge.call(vm, stat({ openIncidents: 1 }), { showIncidents: true })

      expect(placements(vm)[0]).toEqual({ bottom: 15, left: -7 })
    })

    it.each([
      ['the option is off', stat({ openIncidents: 1 }), {}],
      ['there are none', stat({ openIncidents: 0 }), { showIncidents: true }]
    ])('should draw nothing when %s', (_label, s, options) => {
      const vm = viewerContext({ setHtmlOnDiagram: vi.fn() })

      BpmnViewer.methods.drawOpenIncidentsBadge.call(vm, s, options)

      expect(vm.setHtmlOnDiagram).not.toHaveBeenCalled()
    })
  })

  describe('drawClosedIncidentsBadge', () => {
    const inInstanceView = () => viewerContext({ setHtmlOnDiagram: vi.fn(), selectedInstance: { id: 'pi1' } })

    it('should render a "!" marker for resolved incidents', () => {
      const vm = inInstanceView()

      BpmnViewer.methods.drawClosedIncidentsBadge.call(vm, stat({ resolvedIncidents: 1 }), { showClosedIncidents: true })

      expect(vm.setHtmlOnDiagram).toHaveBeenCalledWith('task_1', expect.stringContaining('>!<'), expect.any(Object))
    })

    it('should render for deleted incidents too', () => {
      const vm = inInstanceView()

      BpmnViewer.methods.drawClosedIncidentsBadge.call(vm, stat({ deletedIncidents: 2 }), { showClosedIncidents: true })

      expect(vm.setHtmlOnDiagram).toHaveBeenCalled()
    })

    // Closed incidents are only meaningful for one concrete instance.
    it('should draw nothing outside the instance view', () => {
      const vm = viewerContext({ setHtmlOnDiagram: vi.fn() })

      BpmnViewer.methods.drawClosedIncidentsBadge.call(vm, stat({ resolvedIncidents: 1 }), { showClosedIncidents: true })

      expect(vm.setHtmlOnDiagram).not.toHaveBeenCalled()
    })

    it('should draw nothing when the option is off', () => {
      const vm = inInstanceView()

      BpmnViewer.methods.drawClosedIncidentsBadge.call(vm, stat({ resolvedIncidents: 1 }), {})

      expect(vm.setHtmlOnDiagram).not.toHaveBeenCalled()
    })

    it('should draw nothing when no incident was ever closed', () => {
      const vm = inInstanceView()

      BpmnViewer.methods.drawClosedIncidentsBadge.call(vm, stat(), { showClosedIncidents: true })

      expect(vm.setHtmlOnDiagram).not.toHaveBeenCalled()
    })

    it('should shift left of an occupying canceled badge', () => {
      const vm = inInstanceView()

      BpmnViewer.methods.drawClosedIncidentsBadge.call(
        vm, stat({ resolvedIncidents: 1, canceled: 2 }), { showClosedIncidents: true, showCanceled: true }
      )

      expect(placements(vm)[0]).toEqual({ bottom: 15, right: 36 })
    })

    it('should shift left of an occupying finished badge', () => {
      const vm = inInstanceView()

      BpmnViewer.methods.drawClosedIncidentsBadge.call(
        vm, stat({ resolvedIncidents: 1, finished: 3 }), { showClosedIncidents: true, showHistory: true }
      )

      expect(placements(vm)[0]).toEqual({ bottom: 15, right: 36 })
    })

    it('should take the bottom-right slot when nothing else occupies it', () => {
      const vm = inInstanceView()

      BpmnViewer.methods.drawClosedIncidentsBadge.call(
        vm, stat({ resolvedIncidents: 1 }), { showClosedIncidents: true }
      )

      expect(placements(vm)[0]).toEqual({ bottom: 15, right: 13 })
    })
  })
})

describe('BpmnViewer - getTypeAllowed', () => {
  it('should accept a type matching one of the allowed fragments', () => {
    expect(BpmnViewer.methods.getTypeAllowed('bpmn:UserTask', ['UserTask', 'ServiceTask'])).toBe(true)
  })

  it('should reject a type matching none of them', () => {
    expect(BpmnViewer.methods.getTypeAllowed('bpmn:Gateway', ['UserTask'])).toBe(false)
  })

  it('should reject when no types are allowed', () => {
    expect(BpmnViewer.methods.getTypeAllowed('bpmn:UserTask', [])).toBe(false)
  })

  // The check is a substring match, which is what lets 'Task' cover every task flavour.
  it('should match on a substring', () => {
    expect(BpmnViewer.methods.getTypeAllowed('bpmn:ServiceTask', ['Task'])).toBe(true)
  })
})

describe('BpmnViewer - overlay bookkeeping', () => {
  it('setHtmlOnDiagram should register the overlay and remember its id', () => {
    const add = vi.fn(() => 'overlay-1')
    const vm = { viewer: { get: () => ({ add }) }, overlayList: [] }

    const id = BpmnViewer.methods.setHtmlOnDiagram.call(vm, 'task_1', '<b>x</b>', { bottom: 1 })

    expect(add).toHaveBeenCalledWith('task_1', { position: { bottom: 1 }, html: '<b>x</b>' })
    expect(id).toBe('overlay-1')
    expect(vm.overlayList).toEqual(['overlay-1'])
  })

  // Overlays outlive a diagram redraw unless they are explicitly removed, so the tracked
  // list must be both drained and emptied.
  it('cleanDiagramState should remove every tracked overlay and clear the list', () => {
    const remove = vi.fn()
    const overlayList = ['a', 'b']
    const vm = { viewer: { get: () => ({ remove }) }, overlayList }

    BpmnViewer.methods.cleanDiagramState.call(vm)

    expect(remove).toHaveBeenCalledWith('a')
    expect(remove).toHaveBeenCalledWith('b')
    expect(overlayList).toEqual([])
  })

  it('cleanDiagramState should accept an explicit list', () => {
    const remove = vi.fn()
    const vm = { viewer: { get: () => ({ remove }) }, overlayList: ['tracked'] }
    const explicit = ['x']

    BpmnViewer.methods.cleanDiagramState.call(vm, explicit)

    expect(remove).toHaveBeenCalledWith('x')
    expect(remove).not.toHaveBeenCalledWith('tracked')
    expect(explicit).toEqual([])
    expect(vm.overlayList).toEqual(['tracked'])
  })
})

/**
 * A stubbed bpmn-js viewer. `get(name)` is how the component reaches every service it
 * uses, so one lookup table stands in for canvas, elementRegistry, overlays and zoomScroll.
 */
function viewerStub({ elements = [], graphics = () => null, overlays = {} } = {}) {
  const byId = new Map(elements.map(el => [el.id, el]))
  const services = {
    canvas: { zoom: vi.fn(), viewbox: vi.fn(), scrollToElement: vi.fn() },
    zoomScroll: { stepZoom: vi.fn(), reset: vi.fn() },
    elementRegistry: {
      get: vi.fn(id => byId.get(id) || null),
      getAll: vi.fn(() => elements),
      getGraphics: vi.fn(graphics)
    },
    overlays: { add: vi.fn(() => 'overlay-1'), remove: vi.fn(), ...overlays }
  }
  return { get: vi.fn(name => services[name]), services }
}

describe('BpmnViewer - zoom controls', () => {
  it('should step the zoom in', () => {
    const viewer = viewerStub()
    BpmnViewer.methods.zoomIn.call({ viewer })
    expect(viewer.services.zoomScroll.stepZoom).toHaveBeenCalledWith(1)
  })

  it('should step the zoom out', () => {
    const viewer = viewerStub()
    BpmnViewer.methods.zoomOut.call({ viewer })
    expect(viewer.services.zoomScroll.stepZoom).toHaveBeenCalledWith(-1)
  })

  it('should reset the zoom', () => {
    const viewer = viewerStub()
    BpmnViewer.methods.resetZoom.call({ viewer })
    expect(viewer.services.zoomScroll.reset).toHaveBeenCalled()
  })

  it('setViewbox should apply the viewbox to the canvas', () => {
    const viewer = viewerStub()
    const viewbox = { x: 1, y: 2, width: 3, height: 4 }

    BpmnViewer.methods.setViewbox.call({ viewer }, viewbox)

    expect(viewer.services.canvas.viewbox).toHaveBeenCalledWith(viewbox)
  })

  // The viewer is torn down on unmount while a restore may still be pending.
  it('setViewbox should do nothing without a viewer', () => {
    expect(() => BpmnViewer.methods.setViewbox.call({ viewer: null }, {})).not.toThrow()
  })
})

describe('BpmnViewer - highlightElement', () => {
  const shape = () => {
    const classList = { add: vi.fn(), remove: vi.fn(), contains: vi.fn(() => false) }
    return { classList }
  }
  const gfxFor = (target) => ({ querySelector: vi.fn(() => target) })

  it('should highlight the element named by a plain id', () => {
    const target = shape()
    const viewer = viewerStub({ elements: [{ id: 'task_1' }], graphics: () => gfxFor(target) })
    const vm = { viewer, currentHighlight: null }

    BpmnViewer.methods.highlightElement.call(vm, 'task_1')

    expect(target.classList.add).toHaveBeenCalledWith('bpmn-highlight')
    expect(vm.currentHighlight).toEqual({ id: 'task_1', shape: target })
    expect(viewer.services.canvas.scrollToElement).toHaveBeenCalledWith('task_1')
  })

  it('should accept an object carrying an activityId', () => {
    const target = shape()
    const viewer = viewerStub({ elements: [{ id: 'task_1' }], graphics: () => gfxFor(target) })
    const vm = { viewer, currentHighlight: null }

    BpmnViewer.methods.highlightElement.call(vm, { activityId: 'task_1' })

    expect(target.classList.add).toHaveBeenCalled()
  })

  it.each([[null], [undefined], [{}], [42]])('should ignore the selection %j', (item) => {
    const viewer = viewerStub()
    expect(() => BpmnViewer.methods.highlightElement.call({ viewer }, item)).not.toThrow()
    expect(viewer.get).not.toHaveBeenCalled()
  })

  // Selecting nothing clears the previous highlight rather than leaving it stuck on.
  it('should clear the previous highlight for an empty id', () => {
    const previous = shape()
    const vm = { viewer: viewerStub(), currentHighlight: { id: 'old', shape: previous } }

    BpmnViewer.methods.highlightElement.call(vm, '')

    expect(previous.classList.remove).toHaveBeenCalledWith('bpmn-highlight')
    expect(vm.currentHighlight).toBeNull()
  })

  it('should tolerate an empty id with nothing highlighted', () => {
    const vm = { viewer: viewerStub(), currentHighlight: null }

    expect(() => BpmnViewer.methods.highlightElement.call(vm, '')).not.toThrow()
  })

  it('should move the highlight off the previously selected element', () => {
    const previous = shape()
    const target = shape()
    const viewer = viewerStub({ elements: [{ id: 'task_2' }], graphics: () => gfxFor(target) })
    const vm = { viewer, currentHighlight: { id: 'task_1', shape: previous } }

    BpmnViewer.methods.highlightElement.call(vm, 'task_2')

    expect(previous.classList.remove).toHaveBeenCalledWith('bpmn-highlight')
    expect(target.classList.add).toHaveBeenCalled()
  })

  it('should keep the highlight when the same element is selected again', () => {
    const target = shape()
    const viewer = viewerStub({ elements: [{ id: 'task_1' }], graphics: () => gfxFor(target) })
    const vm = { viewer, currentHighlight: { id: 'task_1', shape: target } }

    BpmnViewer.methods.highlightElement.call(vm, 'task_1')

    expect(target.classList.remove).not.toHaveBeenCalled()
  })

  it('should do nothing when the element is not on the diagram', () => {
    const viewer = viewerStub({ elements: [] })
    const vm = { viewer, currentHighlight: null }

    BpmnViewer.methods.highlightElement.call(vm, 'missing')

    expect(vm.currentHighlight).toBeNull()
  })

  it('should do nothing when the element has no graphics yet', () => {
    const viewer = viewerStub({ elements: [{ id: 'task_1' }], graphics: () => null })
    const vm = { viewer, currentHighlight: null }

    BpmnViewer.methods.highlightElement.call(vm, 'task_1')

    expect(vm.currentHighlight).toBeNull()
  })

  // Some shapes (e.g. groups) have no rect/path child, so the group node is highlighted.
  it('should fall back to the graphics node when it has no rect or path', () => {
    const gfx = { querySelector: vi.fn(() => null), classList: { add: vi.fn(), remove: vi.fn() } }
    const viewer = viewerStub({ elements: [{ id: 'task_1' }], graphics: () => gfx })
    const vm = { viewer, currentHighlight: null }

    BpmnViewer.methods.highlightElement.call(vm, 'task_1')

    expect(gfx.classList.add).toHaveBeenCalledWith('bpmn-highlight')
  })
})

describe('BpmnViewer - buildActivityMap', () => {
  it('should publish an id-to-name map of the named elements', () => {
    const commit = vi.fn()
    const elementRegistry = {
      getAll: () => [
        { businessObject: { id: 'task_1', name: 'Approve' } },
        { businessObject: { id: 'task_2', name: 'Reject' } }
      ]
    }

    BpmnViewer.methods.buildActivityMap.call({ $store: { commit } }, elementRegistry)

    expect(commit).toHaveBeenCalledWith('setProcessActivities', { task_1: 'Approve', task_2: 'Reject' })
  })

  // Sequence flows and unlabelled shapes have no name and would pollute the map.
  it('should skip elements without an id or a name', () => {
    const commit = vi.fn()
    const elementRegistry = {
      getAll: () => [
        { businessObject: { id: 'task_1', name: 'Approve' } },
        { businessObject: { id: 'flow_1' } },
        { businessObject: { name: 'nameless' } },
        {}
      ]
    }

    BpmnViewer.methods.buildActivityMap.call({ $store: { commit } }, elementRegistry)

    expect(commit).toHaveBeenCalledWith('setProcessActivities', { task_1: 'Approve' })
  })
})

describe('BpmnViewer - setSelectableOnAllowedElements', () => {
  const gfxStub = (contains = false) => ({
    classList: { add: vi.fn(), contains: vi.fn(() => contains) }
  })

  it('should mark an interactive element as selectable', () => {
    const gfx = gfxStub()
    const viewer = viewerStub({ elements: [{ id: 'task_1', type: 'bpmn:UserTask' }], graphics: () => gfx })

    BpmnViewer.methods.setSelectableOnAllowedElements.call({
      viewer, getTypeAllowed: () => true
    })

    expect(gfx.classList.add).toHaveBeenCalledWith('selectable')
  })

  it('should leave non-interactive element types alone', () => {
    const gfx = gfxStub()
    const viewer = viewerStub({ elements: [{ id: 'flow_1', type: 'bpmn:SequenceFlow' }], graphics: () => gfx })

    BpmnViewer.methods.setSelectableOnAllowedElements.call({
      viewer, getTypeAllowed: () => false
    })

    expect(gfx.classList.add).not.toHaveBeenCalled()
  })

  it('should not add the class twice', () => {
    const gfx = gfxStub(true)
    const viewer = viewerStub({ elements: [{ id: 'task_1', type: 'bpmn:UserTask' }], graphics: () => gfx })

    BpmnViewer.methods.setSelectableOnAllowedElements.call({
      viewer, getTypeAllowed: () => true
    })

    expect(gfx.classList.add).not.toHaveBeenCalled()
  })

  it('should tolerate an element with no graphics', () => {
    const viewer = viewerStub({ elements: [{ id: 'task_1', type: 'bpmn:UserTask' }], graphics: () => null })

    expect(() => BpmnViewer.methods.setSelectableOnAllowedElements.call({
      viewer, getTypeAllowed: () => true
    })).not.toThrow()
  })

  // The real allow-list lives in the module, so this pins which types actually respond to
  // a click on the diagram.
  it('should use the component\'s own type allow-list', () => {
    const marked = []
    const elements = [
      { id: 'a', type: 'bpmn:UserTask' },
      { id: 'b', type: 'bpmn:SequenceFlow' },
      { id: 'c', type: 'bpmn:CallActivity' }
    ]
    const viewer = viewerStub({
      elements,
      graphics: (el) => ({ classList: { add: () => marked.push(el.id), contains: () => false } })
    })
    const vm = { viewer }
    vm.getTypeAllowed = BpmnViewer.methods.getTypeAllowed.bind(vm)

    BpmnViewer.methods.setSelectableOnAllowedElements.call(vm)

    expect(marked).toContain('a')
    expect(marked).toContain('c')
    expect(marked).not.toContain('b')
  })
})

describe('BpmnViewer - drawActivitiesBadges', () => {
  it('should draw badges for every merged statistic', () => {
    const drawActivityBadges = vi.fn()
    const elementRegistry = {}
    const vm = {
      historicActivityStatistics: [{ id: 'task_1' }],
      $root: { config: { camundaHistoryLevel: 'full' } },
      getMergedStatistics: vi.fn(() => [{ id: 'task_1' }, { id: 'task_2' }]),
      drawActivityBadges
    }

    BpmnViewer.methods.drawActivitiesBadges.call(vm, elementRegistry)

    expect(vm.getMergedStatistics).toHaveBeenCalledWith([{ id: 'task_1' }], 'full')
    expect(drawActivityBadges).toHaveBeenCalledTimes(2)
    expect(drawActivityBadges).toHaveBeenCalledWith({ id: 'task_1' }, elementRegistry)
  })
})

describe('BpmnViewer - drawJobDefinitionBadges', () => {
  const vmWith = (overrides = {}) => ({
    $t: (key) => key,
    badgeOptions: {},
    suspendedOverlayMap: {},
    jobDefinitions: [],
    viewer: viewerStub({ elements: [{ id: 'task_1' }] }),
    ...overrides
  })

  it('should mark a suspended job definition with a pause badge', () => {
    const vm = vmWith({ jobDefinitions: [{ activityId: 'task_1', suspended: true }] })

    BpmnViewer.methods.drawJobDefinitionBadges.call(vm)

    expect(vm.viewer.services.overlays.add).toHaveBeenCalledWith('task_1', {
      position: { top: -10, right: 15 },
      html: expect.stringContaining('mdi-pause')
    })
    expect(vm.suspendedOverlayMap).toEqual({ task_1: 'overlay-1' })
  })

  it('should not badge an active job definition', () => {
    const vm = vmWith({ jobDefinitions: [{ activityId: 'task_1', suspended: false }] })

    BpmnViewer.methods.drawJobDefinitionBadges.call(vm)

    expect(vm.viewer.services.overlays.add).not.toHaveBeenCalled()
  })

  it('should not badge a job definition whose activity is off-diagram', () => {
    const vm = vmWith({ jobDefinitions: [{ activityId: 'missing', suspended: true }] })

    BpmnViewer.methods.drawJobDefinitionBadges.call(vm)

    expect(vm.viewer.services.overlays.add).not.toHaveBeenCalled()
  })

  // Redrawing must clear the previous pause badges, or they accumulate on every refresh.
  it('should remove the badges from the previous draw', () => {
    const vm = vmWith({
      suspendedOverlayMap: { task_1: 'stale-1', task_2: 'stale-2' },
      jobDefinitions: []
    })

    BpmnViewer.methods.drawJobDefinitionBadges.call(vm)

    expect(vm.viewer.services.overlays.remove).toHaveBeenCalledWith('stale-1')
    expect(vm.viewer.services.overlays.remove).toHaveBeenCalledWith('stale-2')
    expect(vm.suspendedOverlayMap).toEqual({})
  })

  it('should do nothing when job definition badges are switched off', () => {
    const vm = vmWith({
      badgeOptions: { showJobDefinitions: false },
      suspendedOverlayMap: { task_1: 'stale' },
      jobDefinitions: [{ activityId: 'task_1', suspended: true }]
    })

    BpmnViewer.methods.drawJobDefinitionBadges.call(vm)

    expect(vm.viewer.services.overlays.remove).not.toHaveBeenCalled()
  })

  it('should do nothing before the viewer exists', () => {
    const vm = vmWith({ viewer: null })

    expect(() => BpmnViewer.methods.drawJobDefinitionBadges.call(vm)).not.toThrow()
  })

  it.each([[null], [undefined], ['not-an-array']])('should do nothing for jobDefinitions %j', (jobDefinitions) => {
    const vm = vmWith({ jobDefinitions, suspendedOverlayMap: { task_1: 'stale' } })

    BpmnViewer.methods.drawJobDefinitionBadges.call(vm)

    expect(vm.viewer.services.overlays.remove).not.toHaveBeenCalled()
  })
})

describe('BpmnViewer - drawDiagramState', () => {
  const vmWith = (overrides = {}) => ({
    viewer: viewerStub(),
    cleanDiagramState: vi.fn(),
    drawActivitiesBadges: vi.fn(),
    drawSubprocessLinks: vi.fn(),
    drawJobDefinitionBadges: vi.fn(),
    setSelectableOnAllowedElements: vi.fn(),
    buildActivityMap: vi.fn(),
    jobDefinitions: null,
    ...overrides
  })

  it('should clear the old overlays before drawing the new ones', () => {
    const order = []
    const vm = vmWith({
      cleanDiagramState: vi.fn(() => order.push('clean')),
      drawActivitiesBadges: vi.fn(() => order.push('badges'))
    })

    BpmnViewer.methods.drawDiagramState.call(vm)

    expect(order).toEqual(['clean', 'badges'])
  })

  it('should draw activity badges, subprocess links, selectability and the activity map', () => {
    const vm = vmWith()

    BpmnViewer.methods.drawDiagramState.call(vm)

    expect(vm.drawActivitiesBadges).toHaveBeenCalled()
    expect(vm.drawSubprocessLinks).toHaveBeenCalled()
    expect(vm.setSelectableOnAllowedElements).toHaveBeenCalled()
    expect(vm.buildActivityMap).toHaveBeenCalled()
  })

  it('should draw job definition badges only when job definitions are loaded', () => {
    const withJobs = vmWith({ jobDefinitions: [] })
    BpmnViewer.methods.drawDiagramState.call(withJobs)
    expect(withJobs.drawJobDefinitionBadges).toHaveBeenCalled()

    const withoutJobs = vmWith({ jobDefinitions: null })
    BpmnViewer.methods.drawDiagramState.call(withoutJobs)
    expect(withoutJobs.drawJobDefinitionBadges).not.toHaveBeenCalled()
  })
})

describe('BpmnViewer - navigateToSubprocess', () => {
  it('should route to the called instance when one is known', async () => {
    const push = vi.fn()
    const vm = { $router: { push }, processDefinitionId: 'pd-parent' }

    await BpmnViewer.methods.navigateToSubprocess.call(vm, 'sub', 2, 'pi-child')

    expect(push).toHaveBeenCalledWith({
      name: 'process-instance-id',
      params: { instanceId: 'pi-child' },
      query: { parentProcessDefinitionId: 'pd-parent', tab: 'variables' }
    })
  })

  // Without a concrete instance the definition's instance list is the useful destination.
  it('should route to the called definition when no instance is known', async () => {
    const push = vi.fn()
    const vm = { $router: { push }, processDefinitionId: 'pd-parent' }

    await BpmnViewer.methods.navigateToSubprocess.call(vm, 'sub', 2, null)

    expect(push).toHaveBeenCalledWith({
      name: 'process',
      params: { processKey: 'sub', versionIndex: 2 },
      query: { parentProcessDefinitionId: 'pd-parent', tab: 'instances' }
    })
  })
})

describe('BpmnViewer - drawSubprocessLinks', () => {
  const callActivity = (id, calledElement) => ({
    id, type: 'bpmn:CallActivity', businessObject: { calledElement }
  })

  const vmWith = (overrides = {}) => ({
    $t: (key) => key,
    badgeOptions: {},
    selectedInstance: null,
    historicActivityStatistics: [],
    getStaticCalledProcessDefinitions: [],
    setHtmlOnDiagram: vi.fn(),
    openSubprocess: vi.fn(),
    ...overrides
  })

  const buttonFrom = (vm) => vm.setHtmlOnDiagram.mock.calls[0][1].querySelector('button')

  it('should place an enabled link button on a statically called activity', () => {
    const vm = vmWith({
      getStaticCalledProcessDefinitions: [{ id: 'sub-1', key: 'sub', calledFromActivityIds: ['call_1'] }]
    })
    const registry = { getAll: () => [callActivity('call_1', 'sub')], get: (id) => callActivity(id, 'sub') }

    BpmnViewer.methods.drawSubprocessLinks.call(vm, registry)

    expect(vm.setHtmlOnDiagram).toHaveBeenCalledWith('call_1', expect.any(HTMLElement), { bottom: -7, right: 15 })
    expect(buttonFrom(vm).disabled) .toBe(false)
  })

  it('should invoke openSubprocess when the button is clicked', () => {
    const vm = vmWith({
      getStaticCalledProcessDefinitions: [{ id: 'sub-1', key: 'sub', calledFromActivityIds: ['call_1'] }]
    })
    const registry = { getAll: () => [callActivity('call_1', 'sub')], get: (id) => callActivity(id, 'sub') }

    BpmnViewer.methods.drawSubprocessLinks.call(vm, registry)
    buttonFrom(vm).click()

    expect(vm.openSubprocess).toHaveBeenCalledWith('call_1', expect.objectContaining({ key: 'sub' }), false)
  })

  // A dynamic calledElement is an expression, so the target is unknown until something has
  // actually run — the link stays disabled until then.
  it('should disable the link on a dynamic call activity that never ran', () => {
    const vm = vmWith()
    const registry = {
      getAll: () => [callActivity('call_1', '${subKey}')],
      get: (id) => callActivity(id, '${subKey}')
    }

    BpmnViewer.methods.drawSubprocessLinks.call(vm, registry)

    expect(buttonFrom(vm).disabled).toBe(true)
    expect(vm.setHtmlOnDiagram.mock.calls[0][1].title).toBe('bpmn-viewer.legend.disabledSubprocess')
  })

  it('should enable a dynamic call activity that has running instances', () => {
    const vm = vmWith({ historicActivityStatistics: [{ id: 'call_1', instances: 1 }] })
    const registry = {
      getAll: () => [callActivity('call_1', '${subKey}')],
      get: (id) => callActivity(id, '${subKey}')
    }

    BpmnViewer.methods.drawSubprocessLinks.call(vm, registry)

    expect(buttonFrom(vm).disabled).toBe(false)
  })

  it('should enable a dynamic call activity that already executed in this instance', () => {
    const vm = vmWith({
      selectedInstance: { id: 'pi1', processDefinitionId: 'pd-other' },
      historicActivityStatistics: [{ id: 'call_1', finished: 2 }]
    })
    const registry = {
      getAll: () => [callActivity('call_1', '${subKey}')],
      get: (id) => callActivity(id, '${subKey}')
    }

    BpmnViewer.methods.drawSubprocessLinks.call(vm, registry)

    expect(buttonFrom(vm).disabled).toBe(false)
  })

  it('should count a canceled activity as executed', () => {
    const vm = vmWith({
      selectedInstance: { id: 'pi1', processDefinitionId: 'pd-parent' },
      historicActivityStatistics: [{ id: 'call_1', canceled: 1 }]
    })
    const registry = {
      getAll: () => [callActivity('call_1', '${subKey}')],
      get: (id) => callActivity(id, '${subKey}')
    }

    BpmnViewer.methods.drawSubprocessLinks.call(vm, registry)

    expect(buttonFrom(vm).disabled).toBe(false)
  })

  // Quirk worth pinning: the "already showing this definition" check compares
  // `calledProcess?.id` with `selectedInstance.processDefinitionId`. For a dynamic call
  // there is no calledProcess, so if the selected instance also carries no
  // processDefinitionId the comparison is `undefined === undefined` and the link is
  // disabled even though the activity did run. Real instances always carry the id, so
  // this only bites on a partially populated instance object.
  it('should disable a dynamic link when neither the call nor the instance has a definition id', () => {
    const vm = vmWith({
      selectedInstance: { id: 'pi1' },
      historicActivityStatistics: [{ id: 'call_1', canceled: 1 }]
    })
    const registry = {
      getAll: () => [callActivity('call_1', '${subKey}')],
      get: (id) => callActivity(id, '${subKey}')
    }

    BpmnViewer.methods.drawSubprocessLinks.call(vm, registry)

    expect(buttonFrom(vm).disabled).toBe(true)
  })

  // Following the link would just reload the diagram already on screen.
  it('should disable the link when it points at the definition already shown', () => {
    const vm = vmWith({
      selectedInstance: { id: 'pi1', processDefinitionId: 'sub-1' },
      getStaticCalledProcessDefinitions: [{ id: 'sub-1', key: 'sub', calledFromActivityIds: ['call_1'] }]
    })
    const registry = { getAll: () => [callActivity('call_1', 'sub')], get: (id) => callActivity(id, 'sub') }

    BpmnViewer.methods.drawSubprocessLinks.call(vm, registry)

    expect(buttonFrom(vm).disabled).toBe(true)
  })

  it('should mark a disabled wrapper with a not-allowed cursor', () => {
    const vm = vmWith()
    const registry = {
      getAll: () => [callActivity('call_1', '${subKey}')],
      get: (id) => callActivity(id, '${subKey}')
    }

    BpmnViewer.methods.drawSubprocessLinks.call(vm, registry)

    expect(vm.setHtmlOnDiagram.mock.calls[0][1].style.cursor).toBe('not-allowed')
  })

  it('should draw nothing when subprocess links are switched off', () => {
    const vm = vmWith({ badgeOptions: { showCalledProcesses: false } })
    const registry = { getAll: () => [callActivity('call_1', 'sub')], get: (id) => callActivity(id, 'sub') }

    BpmnViewer.methods.drawSubprocessLinks.call(vm, registry)

    expect(vm.setHtmlOnDiagram).not.toHaveBeenCalled()
  })

  it('should ignore elements that are not call activities', () => {
    const vm = vmWith()
    const registry = { getAll: () => [{ id: 'task_1', type: 'bpmn:UserTask' }], get: () => null }

    BpmnViewer.methods.drawSubprocessLinks.call(vm, registry)

    expect(vm.setHtmlOnDiagram).not.toHaveBeenCalled()
  })

  it('should skip a call activity that is not on the diagram', () => {
    const vm = vmWith()
    const registry = { getAll: () => [callActivity('call_1', 'sub')], get: () => null }

    BpmnViewer.methods.drawSubprocessLinks.call(vm, registry)

    expect(vm.setHtmlOnDiagram).not.toHaveBeenCalled()
  })

  it('should tolerate a call activity with no businessObject', () => {
    const vm = vmWith()
    const element = { id: 'call_1', type: 'bpmn:CallActivity' }
    const registry = { getAll: () => [element], get: () => element }

    BpmnViewer.methods.drawSubprocessLinks.call(vm, registry)

    expect(buttonFrom(vm).disabled).toBe(true)
  })
})
