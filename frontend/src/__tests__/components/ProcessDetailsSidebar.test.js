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
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import { mountWithDefaults } from '../support/mountWithDefaults.js'

vi.mock('@/services.js', () => ({
  ProcessService: { findDeployment: vi.fn() }
}))

import { ProcessService } from '@/services.js'
import ProcessDetailsSidebar from '@/components/process/ProcessDetailsSidebar.vue'

const DEPLOYMENT_TIME = '2025-05-16T12:44:00.000+0200'

function version(overrides = {}) {
  return {
    id: 'invoice:37:abc', key: 'invoice', version: '37', versionTag: '3.0.1', deploymentId: 'dep-37',
    runningInstances: 20, allInstances: 32, completedInstances: 12, suspended: 'false',
    ...overrides
  }
}

// a lazy loaded version that is not selected: the list leaves its counts as '-'
function lazyVersion(overrides = {}) {
  return version({ id: 'invoice:36:def', version: '36', deploymentId: 'dep-36',
    runningInstances: '-', allInstances: '-', completedInstances: '-', ...overrides })
}

function statistics(...incidentCounts) {
  return incidentCounts.map((counts, i) => ({
    id: `task${i}`, instances: 1, incidents: counts.map(incidentCount => ({ incidentType: 'failedJob', incidentCount }))
  }))
}

// the view hands over the selected definition, with its activity statistics once loaded
function selected(processDefinitions, versionIndex, statistics) {
  const process = processDefinitions.find(v => v.version === versionIndex)
  return process ? { ...process, statistics } : null
}

function mountSidebar({ processDefinitions = [version()], versionIndex = '37', selectedStatistics, config = {}, component = ProcessDetailsSidebar } = {}) {
  return mountWithDefaults(component, {
    props: { processDefinitions, processKey: 'invoice', versionIndex, selectedProcess: selected(processDefinitions, versionIndex, selectedStatistics) },
    i18nPlugin: false,
    global: {
      stubs: {
        ProcessDefinitionDetails: { name: 'ProcessDefinitionDetails', template: '<div></div>', props: ['version', 'versionIndex', 'selectedDeployment', 'shown', 'selectedInstance'] },
        DeleteProcessDefinitionModal: true,
        SuccessAlert: true
      },
      mocks: {
        $route: { query: {} },
        user: { permissions: { deployment: [], processDefinition: [] } },
        config: { camundaHistoryLevel: 'full', permissions: { deleteProcessDefinition: {} }, ...config }
      }
    }
  })
}

describe('ProcessDetailsSidebar', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    ProcessService.findDeployment.mockResolvedValue({ id: 'dep-37', deploymentTime: DEPLOYMENT_TIME })
  })

  describe('version card', () => {
    it('shows the version number, the version tag and the running and completed counts', async () => {
      const wrapper = mountSidebar()
      await flushPromises()

      expect(wrapper.find('.version-number').text()).toBe('37.')
      expect(wrapper.text()).toContain('process.details.versionTag: 3.0.1')
      expect(wrapper.find('.running-instances').text()).toContain('20')
      expect(wrapper.find('.finished-instances').text()).toContain('12')
    })

    it('shows a dash as version tag when the version has none', async () => {
      const wrapper = mountSidebar({ processDefinitions: [version({ versionTag: null })] })
      await flushPromises()

      expect(wrapper.text()).toContain('process.details.versionTag: -')
    })

    it('hides the completed count when the engine records no history', async () => {
      const wrapper = mountSidebar({ config: { camundaHistoryLevel: 'none' } })
      await flushPromises()

      expect(wrapper.find('.running-instances').exists()).toBe(true)
      expect(wrapper.find('.finished-instances').exists()).toBe(false)
    })

    it('marks a suspended version', async () => {
      const wrapper = mountSidebar({ processDefinitions: [version({ suspended: 'true' })] })
      await flushPromises()

      expect(wrapper.text()).toContain('process-instance.jobDefinitions.suspended')
    })

    // the absolutely positioned screen reader labels must stay inside their card, or they stretch the sidebar
    it('positions the card so its screen reader labels stay inside it', async () => {
      const wrapper = mountSidebar()
      await flushPromises()

      const label = wrapper.find('.running-instances .visually-hidden')
      expect(label.text()).toBe('process.details.unfinishedInstances:')
      expect(label.element.closest('.list-group-item-action').classList).toContain('position-relative')
    })
  })

  describe('counts only loaded for the selected version', () => {
    it('shows a dash for every count and the deployment time of a lazy loaded version that is not selected', async () => {
      const wrapper = mountSidebar({ processDefinitions: [version(), lazyVersion()], selectedStatistics: statistics([1]) })
      await flushPromises()

      const card = wrapper.findAll('.list-group-item-action')[1]
      expect(card.find('.running-instances').text()).toBe('process.details.unfinishedInstances: -')
      expect(card.find('.open-incidents').text()).toBe('bpmn-viewer.legend.openIncidents: -')
      expect(card.find('.finished-instances').text()).toBe('process.details.finishedInstances: -')
      expect(card.find('.deployment-time').text()).toBe('deployment.deploymentTime: -')
    })

    it('shows the counts of every version once all of them are loaded', async () => {
      const wrapper = mountSidebar({ processDefinitions: [version(), lazyVersion({ runningInstances: 3, completedInstances: 7 })] })
      await flushPromises()

      const cards = wrapper.findAll('.list-group-item-action')
      expect(cards[1].find('.running-instances').text()).toContain('3')
      expect(cards[1].find('.finished-instances').text()).toContain('7')
    })

    it('adds up the open incidents of the selected version from its activity statistics', async () => {
      const wrapper = mountSidebar({ selectedStatistics: statistics([2, 1], [], [4]) })
      await flushPromises()

      expect(wrapper.find('.open-incidents').text()).toContain('7')
    })

    it('shows zero open incidents when no activity of the selected version has any', async () => {
      const wrapper = mountSidebar({ selectedStatistics: [{ id: 'task0', instances: 1 }] })
      await flushPromises()

      expect(wrapper.find('.open-incidents').text()).toContain('0')
    })

    it('shows a dash for the incidents until the statistics of the selected version are loaded', async () => {
      const wrapper = mountSidebar()
      await flushPromises()

      expect(wrapper.find('.open-incidents').text()).toBe('bpmn-viewer.legend.openIncidents: -')
    })

    it('shows the incidents and deployment time on the selected version only', async () => {
      const wrapper = mountSidebar({ processDefinitions: [version(), lazyVersion({ runningInstances: 3 })], selectedStatistics: statistics([1]) })
      await flushPromises()

      const cards = wrapper.findAll('.list-group-item-action')
      expect(cards[0].find('.open-incidents').text()).toContain('1')
      expect(cards[0].find('.deployment-time').text()).not.toBe('deployment.deploymentTime: -')
      expect(cards[0].find('.deployment-time').attributes('title')).toBeTruthy()
      expect(cards[1].find('.open-incidents').text()).toContain('-')
      expect(cards[1].find('.deployment-time').text()).toBe('deployment.deploymentTime: -')
    })

    it('shows a dash as deployment time when the deployment has none', async () => {
      ProcessService.findDeployment.mockResolvedValue({ id: 'dep-37' })
      const wrapper = mountSidebar()
      await flushPromises()

      expect(wrapper.find('.deployment-time').text()).toBe('deployment.deploymentTime: -')
    })
  })

  describe('versions selected before', () => {
    const versions = () => [version(), lazyVersion()]
    const select36 = async (wrapper, processDefinitions, statistics36) => {
      ProcessService.findDeployment.mockResolvedValue({ id: 'dep-36', deploymentTime: '2025-04-02T09:15:00.000+0200' })
      await wrapper.setProps({ versionIndex: '36', selectedProcess: selected(processDefinitions, '36', statistics36) })
      await flushPromises()
    }

    it('keep the incidents and deployment time loaded while they were selected', async () => {
      const processDefinitions = versions()
      const wrapper = mountSidebar({ processDefinitions, selectedStatistics: statistics([2]) })
      await flushPromises()

      await select36(wrapper, processDefinitions, statistics([5]))

      const cards = wrapper.findAll('.list-group-item-action')
      expect(cards[0].find('.open-incidents').text()).toContain('2')
      expect(cards[0].find('.deployment-time').text()).not.toBe('deployment.deploymentTime: -')
      expect(cards[1].find('.open-incidents').text()).toContain('5')
      expect(cards[1].find('.deployment-time').text()).not.toBe('deployment.deploymentTime: -')
    })

    it('keep the incidents of the statistics already loaded when the version is selected', async () => {
      const processDefinitions = versions()
      const wrapper = mountSidebar({ processDefinitions, selectedStatistics: statistics([3]) })
      await flushPromises()

      await select36(wrapper, processDefinitions, undefined)

      expect(wrapper.findAll('.list-group-item-action')[0].find('.open-incidents').text()).toContain('3')
    })

    it('do not ask their deployment again when selected once more', async () => {
      const processDefinitions = versions()
      const wrapper = mountSidebar({ processDefinitions })
      await flushPromises()
      await select36(wrapper, processDefinitions)

      await wrapper.setProps({ versionIndex: '37', selectedProcess: selected(processDefinitions, '37') })
      await flushPromises()

      expect(ProcessService.findDeployment).toHaveBeenCalledTimes(2)
      expect(wrapper.findAll('.deployment-time')[0].text()).not.toBe('deployment.deploymentTime: -')
    })

    it('lose their incidents when the versions are reloaded, like their counts', async () => {
      const processDefinitions = versions()
      const wrapper = mountSidebar({ processDefinitions, selectedStatistics: statistics([2]) })
      await flushPromises()
      await select36(wrapper, processDefinitions, statistics([5]))

      await wrapper.setProps({ processDefinitions: versions() })

      const cards = wrapper.findAll('.list-group-item-action')
      expect(cards[0].find('.open-incidents').text()).toContain('-')
      expect(cards[1].find('.open-incidents').text()).toContain('5')
    })
  })

  describe('deployment of the selected version', () => {
    const deploymentTimes = wrapper => wrapper.findAll('.deployment-time').map(d => d.text())

    it('loads the deployment of the selected version only', async () => {
      mountSidebar({ processDefinitions: [version(), lazyVersion()] })
      await flushPromises()

      expect(ProcessService.findDeployment).toHaveBeenCalledTimes(1)
      expect(ProcessService.findDeployment).toHaveBeenCalledWith('dep-37')
    })

    it('hands the details panel the deployment of its version once it was loaded', async () => {
      const wrapper = mountSidebar({ processDefinitions: [version(), lazyVersion()] })
      await flushPromises()
      const toggles = wrapper.findAll('.details-toggle')

      await toggles[0].trigger('click')
      expect(wrapper.findComponent({ name: 'ProcessDefinitionDetails' }).props('selectedDeployment'))
        .toEqual({ id: 'dep-37', deploymentTime: DEPLOYMENT_TIME })

      await toggles[1].trigger('click')
      expect(wrapper.findComponent({ name: 'ProcessDefinitionDetails' }).props('selectedDeployment')).toBeNull()
    })

    it('loads the new deployment when another version is selected', async () => {
      const wrapper = mountSidebar({ processDefinitions: [version(), lazyVersion()] })
      await flushPromises()

      ProcessService.findDeployment.mockResolvedValue({ id: 'dep-36', deploymentTime: DEPLOYMENT_TIME })
      await wrapper.setProps({ versionIndex: '36' })
      await flushPromises()

      expect(ProcessService.findDeployment).toHaveBeenLastCalledWith('dep-36')
      expect(deploymentTimes(wrapper)[1]).not.toBe('deployment.deploymentTime: -')
    })

    it('keeps a deployment that arrives after the selection changed under its own version', async () => {
      let deliverFirst
      ProcessService.findDeployment.mockReturnValueOnce(new Promise(resolve => { deliverFirst = resolve }))
      const wrapper = mountSidebar({ processDefinitions: [version(), lazyVersion()] })

      ProcessService.findDeployment.mockResolvedValue({ id: 'dep-36', deploymentTime: '2025-04-02T09:15:00.000+0200' })
      await wrapper.setProps({ versionIndex: '36' })
      await flushPromises()
      deliverFirst({ id: 'dep-37', deploymentTime: DEPLOYMENT_TIME })
      await flushPromises()

      expect(wrapper.vm.deployments['dep-37'].deploymentTime).toBe(DEPLOYMENT_TIME)
      expect(wrapper.vm.deployments['dep-36'].deploymentTime).toBe('2025-04-02T09:15:00.000+0200')
    })

    it('does not load the deployment without read permission on it', async () => {
      const component = { ...ProcessDetailsSidebar, methods: { ...ProcessDetailsSidebar.methods, canReadDeployment: () => false } }
      const wrapper = mountSidebar({ component })
      await flushPromises()

      expect(ProcessService.findDeployment).not.toHaveBeenCalled()
      expect(deploymentTimes(wrapper)).toEqual(['deployment.deploymentTime: -'])
    })

    it('does not load anything while no version is selected', async () => {
      mountSidebar({ versionIndex: '' })
      await flushPromises()

      expect(ProcessService.findDeployment).not.toHaveBeenCalled()
    })
  })

  describe('details panel', () => {
    it('opens below the card from its arrow, without leaving for the version', async () => {
      const wrapper = mountSidebar({ processDefinitions: [version(), lazyVersion()] })
      await flushPromises()
      const toggle = wrapper.findAll('.details-toggle')[1]

      expect(wrapper.find('.version-details').exists()).toBe(false)
      expect(toggle.attributes('aria-expanded')).toBe('false')

      const click = new MouseEvent('click', { bubbles: true, cancelable: true })
      toggle.element.dispatchEvent(click)
      await flushPromises()

      expect(click.defaultPrevented).toBe(true)
      expect(toggle.attributes('aria-expanded')).toBe('true')
      expect(toggle.attributes('aria-controls')).toBe('version-details-invoice:36:def')
      expect(wrapper.find('.version-details').attributes('id')).toBe('version-details-invoice:36:def')
      expect(toggle.find('.mdi-chevron-up').exists()).toBe(true)
      const details = wrapper.findComponent({ name: 'ProcessDefinitionDetails' })
      expect(details.props('version').id).toBe('invoice:36:def')
      expect(details.props('shown')).toBe(true)
    })

    it('casts the shadow of the selected version around its card and details, not between them', async () => {
      const wrapper = mountSidebar({ processDefinitions: [version(), lazyVersion()] })
      await flushPromises()

      await wrapper.find('.details-toggle').trigger('click')

      const items = wrapper.findAll('.version-item')
      expect(items[0].classes()).toContain('shadow')
      expect(items[0].find('.list-group-item-action').classes()).not.toContain('shadow')
      expect(items[0].find('.version-details').exists()).toBe(true)
      expect(items[1].classes()).not.toContain('shadow')
      // rounded as one block: the open card only on top, its details only at the bottom
      expect(items[0].find('.list-group-item-action').classes()).toEqual(expect.arrayContaining(['rounded-top', 'rounded-bottom-0']))
      expect(items[0].find('.version-details').classes()).toContain('rounded-bottom')
      expect(items[1].find('.list-group-item-action').classes()).toContain('rounded')
    })

    it('closes from the same arrow', async () => {
      const wrapper = mountSidebar()
      await flushPromises()

      await wrapper.find('.details-toggle').trigger('click')
      await wrapper.find('.details-toggle').trigger('click')

      expect(wrapper.find('.version-details').exists()).toBe(false)
      expect(wrapper.find('.details-toggle .mdi-chevron-down').exists()).toBe(true)
    })

    it('keeps one version open at a time', async () => {
      const wrapper = mountSidebar({ processDefinitions: [version(), lazyVersion()] })
      await flushPromises()
      const toggles = wrapper.findAll('.details-toggle')

      await toggles[0].trigger('click')
      await toggles[1].trigger('click')

      expect(wrapper.findAll('.version-details')).toHaveLength(1)
      expect(toggles[0].attributes('aria-expanded')).toBe('false')
    })

    it('keeps the left half of the actions free for the enterprise edition', async () => {
      const wrapper = mountSidebar()
      await flushPromises()

      await wrapper.find('.details-toggle').trigger('click')

      const reserved = wrapper.find('.version-details .version-actions-plugin')
      expect(reserved.classes()).toContain('col-6')
      expect(reserved.element.children).toHaveLength(0)
      const remove = wrapper.find('.version-details button[title="process.deleteProcessDefinition.tooltip"]')
      expect(remove.element.closest('.col-6')).not.toBe(reserved.element)
      // alone, Delete keeps its own width at the right
      expect(remove.classes()).toContain('ms-auto')
      // styled with the darker danger color
      expect(remove.classes()).toContain('delete-version')
      expect(remove.classes()).not.toContain('w-100')
    })

    it('renders the actions the enterprise edition adds, for the open version', async () => {
      const plugin = { name: 'ProcessDefinitionVersionActionsPlugin', props: ['process', 'processKey'], template: '<button class="migrate">Migrate</button>' }
      const component = { ...ProcessDetailsSidebar, components: { ...ProcessDetailsSidebar.components, ProcessDefinitionVersionActionsPlugin: plugin } }
      const wrapper = mountSidebar({ component, processDefinitions: [version(), lazyVersion()] })
      await flushPromises()

      await wrapper.findAll('.details-toggle')[1].trigger('click')

      const rendered = wrapper.findComponent({ name: 'ProcessDefinitionVersionActionsPlugin' })
      expect(wrapper.find('.version-actions-plugin .migrate').exists()).toBe(true)
      expect(rendered.props('process').id).toBe('invoice:36:def')
      expect(rendered.props('processKey')).toBe('invoice')
      // beside it, Delete fills its half
      expect(wrapper.find('.version-details button[title="process.deleteProcessDefinition.tooltip"]').classes()).toContain('w-100')
    })

    it('has no delete button without the permission to delete the version', async () => {
      const component = { ...ProcessDetailsSidebar, methods: { ...ProcessDetailsSidebar.methods, processByPermissions: () => false } }
      const wrapper = mountSidebar({ component })
      await flushPromises()

      await wrapper.find('.details-toggle').trigger('click')

      expect(wrapper.find('.version-details').exists()).toBe(true)
      expect(wrapper.find('button[title="process.deleteProcessDefinition.tooltip"]').exists()).toBe(false)
    })
  })

  describe('load data button', () => {
    // a SidebarsFlow-like panel: the header button with the caption, then the content holding the sidebar
    function mountInPanel(captionEnd) {
      const panel = document.createElement('div')
      // test-utils mounts into a new element inside attachTo, which stands for the content div
      panel.innerHTML = '<button><h2>History versions</h2></button><div class="content"></div>'
      document.body.appendChild(panel)
      vi.spyOn(document, 'createRange').mockReturnValue({ selectNodeContents: vi.fn(), getBoundingClientRect: () => ({ right: captionEnd }) })
      const wrapper = mountWithDefaults(ProcessDetailsSidebar, {
        props: { processDefinitions: [version()], processKey: 'invoice', versionIndex: '37' },
        i18nPlugin: false,
        attachTo: panel.querySelector('.content'),
        global: {
          stubs: { ProcessDefinitionDetails: true, DeleteProcessDefinitionModal: true, SuccessAlert: true },
          mocks: { $route: { query: {} }, user: { permissions: { deployment: [], processDefinition: [] } }, config: { camundaHistoryLevel: 'full', permissions: { deleteProcessDefinition: {} } } }
        }
      })
      const button = wrapper.find('.load-data').element
      Object.defineProperty(button, 'offsetWidth', { value: 95 })
      return wrapper
    }
    const sidebarWidth = (wrapper, right) => vi.spyOn(wrapper.vm.$el, 'getBoundingClientRect').mockReturnValue({ left: 0, right })

    afterEach(() => {
      vi.restoreAllMocks()
      document.body.innerHTML = ''
    })

    it('shows its label with the statistics tooltip while it fits beside the caption', async () => {
      const wrapper = mountInPanel(130)
      sidebarWidth(wrapper, 360)

      wrapper.vm.fitLoadData()
      await flushPromises()

      const button = wrapper.find('.load-data')
      expect(button.text()).toBe('process.loadData')
      expect(button.attributes('title')).toBe('process.loadVersions')
      expect(button.attributes('aria-label')).toBeUndefined()
      expect(button.classes()).not.toContain('border-0')
    })

    it('shrinks to a flat icon, named for screen readers, when the label would reach the caption', async () => {
      const wrapper = mountInPanel(130)
      sidebarWidth(wrapper, 275)

      wrapper.vm.fitLoadData()
      await flushPromises()

      const button = wrapper.find('.load-data')
      expect(button.text()).toBe('')
      expect(button.attributes('aria-label')).toBe('process.loadData')
      expect(button.classes()).toContain('border-0')
      expect(button.attributes('style')).toContain('right: 60px')
    })

    it('gets its label back once the sidebar is wide enough again', async () => {
      const wrapper = mountInPanel(130)
      const width = sidebarWidth(wrapper, 275)
      wrapper.vm.fitLoadData()
      await flushPromises()

      width.mockReturnValue({ left: 0, right: 360 })
      wrapper.vm.fitLoadData()
      await flushPromises()

      expect(wrapper.find('.load-data').text()).toBe('process.loadData')
    })

    it('fits itself whenever the sidebar is resized, and stops when it goes away', async () => {
      const observers = []
      vi.stubGlobal('ResizeObserver', class {
        constructor(callback) { this.callback = callback; this.observe = vi.fn(); this.disconnect = vi.fn(); observers.push(this) }
      })
      const wrapper = mountSidebar()
      await flushPromises()
      const fit = vi.spyOn(wrapper.vm, 'fitLoadData')

      observers[0].callback()
      wrapper.unmount()

      expect(observers[0].observe).toHaveBeenCalledWith(wrapper.vm.$el)
      expect(fit).toHaveBeenCalled()
      expect(observers[0].disconnect).toHaveBeenCalled()
      vi.unstubAllGlobals()
    })
  })

  describe('actions', () => {
    it('asks to load the counts of every version from the load button', async () => {
      const wrapper = mountSidebar({ config: { lazyLoadHistory: true } })
      await flushPromises()

      await wrapper.find('button[title="process.loadVersions"]').trigger('click')

      expect(wrapper.emitted('onRefreshProcessDefinitions')).toEqual([[false]])
    })

    it('asks to refresh the versions honouring the lazy load setting from the refresh button', async () => {
      const wrapper = mountSidebar({ config: { lazyLoadHistory: true } })
      await flushPromises()

      await wrapper.find('button[title="process.refreshVersions"]').trigger('click')

      expect(wrapper.emitted('onRefreshProcessDefinitions')).toEqual([[true]])
    })

    it('confirms before deleting a version and then asks the parent to delete it', async () => {
      const wrapper = mountSidebar()
      await flushPromises()
      const show = vi.fn()
      wrapper.vm.$refs.confirm.show = show

      await wrapper.find('.details-toggle').trigger('click')
      await wrapper.find('.version-details button[title="process.deleteProcessDefinition.tooltip"]').trigger('click')
      const { ok, processDefinition } = show.mock.calls[0][0]
      ok(processDefinition)

      expect(processDefinition.id).toBe('invoice:37:abc')
      expect(wrapper.emitted('onDeleteProcessDefinition')).toEqual([[{ processDefinition }]])
    })

    it('stores the new history time to live on the matching version and reports success', async () => {
      const processDefinitions = [version(), lazyVersion({ historyTimeToLive: 5 })]
      const wrapper = mountSidebar({ processDefinitions })
      await flushPromises()
      const show = vi.fn()
      wrapper.vm.$refs.successOperation.show = show

      wrapper.vm.onUpdateHistoryTimeToLive('invoice:37:abc', 30)

      expect(processDefinitions[0].historyTimeToLive).toBe(30)
      expect(processDefinitions[1].historyTimeToLive).toBe(5)
      expect(show).toHaveBeenCalledTimes(1)
    })
  })
})
