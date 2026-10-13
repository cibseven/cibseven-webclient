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
import { describe, it, expect, vi, afterEach } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import ResourcesNavBar from '@/components/deployment/ResourcesNavBar.vue'
import { ProcessService, DeploymentService } from '@/services.js'
import { mountWithDefaults } from '../support/mountWithDefaults.js'

vi.mock('@/services.js', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    ProcessService: {
      findDeployment: vi.fn(() => Promise.resolve({
        id: 'd1', name: 'Dep1', tenantId: 't1', source: 'src', deploymentTime: '2026-01-01T00:00:00Z'
      })),
      findProcessesWithFilters: vi.fn(() => Promise.resolve([{ id: 'pd1' }])),
      fetchDiagram: vi.fn()
    },
    DeploymentService: {
      ...actual.DeploymentService,
      fetchDataFromDeploymentResource: vi.fn()
    }
  }
})

const m = ResourcesNavBar.methods

/**
 * A `this` for ResourcesNavBar's own methods, without mounting. `openModeler` is the only
 * method under test here; the diagram viewer/download paths are exercised by mounting below.
 */
function context(overrides = {}) {
  const vm = {
    deployment: { id: 'd1' },
    getDecisionList: vi.fn(() => Promise.resolve([{ id: 'dd1' }])),
    $router: { push: vi.fn() },
    ...overrides
  }
  for (const [name, method] of Object.entries(m)) {
    if (!(name in vm)) vm[name] = method.bind(vm)
  }
  return vm
}

describe('ResourcesNavBar.vue', () => {
  describe('openModeler (method)', () => {
    afterEach(() => vi.clearAllMocks())

    it('opens a bpmn resource by looking up its process definition id', async () => {
      const vm = context()
      await vm.openModeler({ name: 'invoice.bpmn' })
      expect(ProcessService.findProcessesWithFilters).toHaveBeenCalledWith('deploymentId=d1&resourceName=invoice.bpmn')
      expect(vm.$router.push).toHaveBeenCalledWith({ name: 'modeler', query: { processId: 'pd1', type: 'bpmn' } })
    })

    it('opens a dmn resource by looking up its decision definition id', async () => {
      const vm = context()
      await vm.openModeler({ name: 'rules.dmn' })
      expect(vm.getDecisionList).toHaveBeenCalledWith({ deploymentId: 'd1', resourceName: 'rules.dmn' })
      expect(vm.$router.push).toHaveBeenCalledWith({ name: 'modeler', query: { processId: 'dd1', type: 'dmn' } })
    })

    it('does not navigate when no matching definition is found', async () => {
      ProcessService.findProcessesWithFilters.mockResolvedValueOnce([])
      const vm = context()
      await vm.openModeler({ name: 'invoice.bpmn' })
      expect(vm.$router.push).not.toHaveBeenCalled()
    })
  })

  describe('openModelerTooltip (method)', () => {
    // Regression test: the template used to wrap this method's already-translated return
    // value in another $t() call, translating a sentence instead of a key. A stub that
    // echoes a distinct wrapper per key (rather than the identity `key => key` used
    // elsewhere) would catch that regression, since $t($t(key)) would then produce
    // `t:t:<key>` instead of `t:<key>`.
    it('translates the bpmn-specific key for a bpmn resource', () => {
      const vm = context({ $t: vi.fn(key => `t:${key}`) })
      expect(vm.openModelerTooltip({ name: 'invoice.bpmn' })).toBe('t:process.openModeler')
    })

    it('translates the dmn-specific key for a dmn resource', () => {
      const vm = context({ $t: vi.fn(key => `t:${key}`) })
      expect(vm.openModelerTooltip({ name: 'rules.dmn' })).toBe('t:process.openModelerDmn')
    })
  })

  describe('open-in-modeler action (mounted, permission-gated)', () => {
    const resources = [{ id: 'r1', name: 'invoice.bpmn' }, { id: 'r2', name: 'rules.dmn' }]

    function createWrapper(configOverrides = {}) {
      return mountWithDefaults(ResourcesNavBar, {
        props: { resources, deploymentId: 'd1' },
        global: {
          stubs: { BpmnViewer: true, DmnViewer: true, TaskPopper: true },
          mocks: {
            $router: { push: vi.fn() },
            $store: { dispatch: vi.fn(() => Promise.resolve([{ id: 'dd1' }])) },
            // ResourcesNavBar reads `this.$root.*`; mounted standalone (no parent), $root
            // resolves to this very instance, so mocking these directly is equivalent.
            user: { id: 'u1' },
            config: { authorizationEnabled: false, modelerEnabled: true, permissions: {}, ...configOverrides }
          }
        }
      })
    }

    it('is shown for each bpmn/dmn resource, with a type-specific tooltip, when the user has modeler permission', async () => {
      const wrapper = createWrapper()
      await flushPromises()

      const bpmnButton = wrapper.find('[title="process.openModeler"]')
      const dmnButton = wrapper.find('[title="process.openModelerDmn"]')
      expect(bpmnButton.exists()).toBe(true)
      expect(dmnButton.exists()).toBe(true)

      await bpmnButton.trigger('click')
      await flushPromises()
      expect(wrapper.vm.$router.push).toHaveBeenCalledWith({ name: 'modeler', query: { processId: 'pd1', type: 'bpmn' } })

      await dmnButton.trigger('click')
      await flushPromises()
      expect(wrapper.vm.$router.push).toHaveBeenCalledWith({ name: 'modeler', query: { processId: 'dd1', type: 'dmn' } })
    })

    it('is hidden when the user lacks modeler permission', async () => {
      const wrapper = createWrapper({ authorizationEnabled: true, permissions: { modeler: undefined } })
      await flushPromises()

      expect(wrapper.find('[title="process.openModeler"]').exists()).toBe(false)
      expect(wrapper.find('[title="process.openModelerDmn"]').exists()).toBe(false)
    })
  })

  describe('resource types (methods)', () => {
    const vm = context()

    it('recognises an extension in any case', () => {
      expect(vm.hasExtension({ name: 'Invoice.FORM' }, '.form')).toBe(true)
      expect(vm.hasExtension({ name: 'invoice.form' }, '.html', '.htm')).toBe(false)
    })

    it('has no extension for a resource without a name', () => {
      expect(vm.hasExtension({}, '.form')).toBe(false)
      expect(vm.hasExtension(null, '.form')).toBe(false)
    })

    it('takes .html and .htm as HTML forms', () => {
      expect(vm.isHtml({ name: 'start.html' })).toBe(true)
      expect(vm.isHtml({ name: 'start.htm' })).toBe(true)
      expect(vm.isForm({ name: 'start.form' })).toBe(true)
    })

    it('downloads diagrams and forms, and opens only diagrams in the modeler', () => {
      expect(['a.bpmn', 'a.dmn', 'a.form', 'a.html'].map(name => vm.canDownload({ name }))).toEqual([true, true, true, true])
      expect(vm.canDownload({ name: 'logo.png' })).toBe(false)
      expect(['a.bpmn', 'a.dmn', 'a.form', 'a.html'].map(name => vm.canOpenModeler({ name }))).toEqual([true, true, false, false])
    })
  })

  describe('form resources (methods)', () => {
    afterEach(() => vi.clearAllMocks())

    const FORM_FILE = '{\n    "type": "default",\n    "components": []\n}'
    const formResource = { id: 'r1', deploymentId: 'd1', name: 'invoice.form' }

    it('reads a form from the deployment resource data, with the user\'s token', async () => {
      DeploymentService.fetchDataFromDeploymentResource.mockResolvedValueOnce(FORM_FILE)
      const vm = context({ $root: { user: { authToken: 'tok' } } })

      const content = await vm.getContent(formResource)

      expect(DeploymentService.fetchDataFromDeploymentResource).toHaveBeenCalledWith('d1', 'r1', 'invoice.form', 'tok')
      expect(content).toBe(FORM_FILE)
    })

    it('reads an HTML form the same way', async () => {
      DeploymentService.fetchDataFromDeploymentResource.mockResolvedValueOnce('<form></form>')
      const vm = context({ $root: { user: { authToken: 'tok' } } })

      expect(await vm.getContent({ id: 'r2', deploymentId: 'd1', name: 'start.html' })).toBe('<form></form>')
    })

    const downloaded = async (resource, content) => {
      DeploymentService.fetchDataFromDeploymentResource.mockResolvedValueOnce(content)
      const triggerDownload = vi.fn()
      const vm = context({ $root: { user: { authToken: 'tok' } }, $refs: { downloadPopper: { triggerDownload } } })
      await vm.download(resource)
      const [blob, name] = triggerDownload.mock.calls[0]
      return { blob, name, text: await blob.text() }
    }

    // A downloaded file has to be the deployed one, not a re-serialised copy
    it('downloads a form exactly as it was deployed', async () => {
      const { blob, name, text } = await downloaded(formResource, FORM_FILE)

      expect(text).toBe(FORM_FILE)
      expect(blob.type).toBe('application/json')
      expect(name).toBe('invoice.form')
    })

    it('downloads an HTML form as HTML', async () => {
      const { blob, text } = await downloaded({ id: 'r2', deploymentId: 'd1', name: 'start.html' }, '<form></form>')

      expect(text).toBe('<form></form>')
      expect(blob.type).toBe('text/html')
    })

    describe('showResource', () => {
      const viewers = () => ({
        diagramModal: { show: vi.fn() },
        formPreview: { showForm: vi.fn() },
        htmlFormPreview: { showForm: vi.fn() },
        diagram: { cleanDiagramState: vi.fn(), drawDiagramState: vi.fn(), showDiagram: vi.fn() },
        dmnDiagram: { cleanDiagramState: vi.fn(), showDiagram: vi.fn() }
      })
      const show = async (resource, type, content) => {
        vi.useFakeTimers()
        DeploymentService.fetchDataFromDeploymentResource.mockResolvedValueOnce(content)
        const $refs = viewers()
        const vm = context({
          $root: { user: { authToken: 'tok' } }, $refs,
          isFormResource: type === 'form', isHtmlResource: type === 'html', isDmnResource: false
        })
        await vm.showResource(resource)
        vi.advanceTimersByTime(500)
        vi.useRealTimers()
        return { vm, $refs }
      }

      it('opens a form in the form preview, leaving the diagram viewers alone', async () => {
        const { vm, $refs } = await show(formResource, 'form', FORM_FILE)

        expect($refs.diagramModal.show).toHaveBeenCalled()
        expect($refs.formPreview.showForm).toHaveBeenCalledWith(FORM_FILE)
        expect($refs.diagram.cleanDiagramState).not.toHaveBeenCalled()
        expect($refs.dmnDiagram.cleanDiagramState).not.toHaveBeenCalled()
        expect(vm.diagramLoading).toBe(false)
      })

      it('opens an HTML form in the HTML preview', async () => {
        const { $refs } = await show({ id: 'r2', deploymentId: 'd1', name: 'start.html' }, 'html', '<form></form>')

        expect($refs.htmlFormPreview.showForm).toHaveBeenCalledWith('<form></form>')
        expect($refs.formPreview.showForm).not.toHaveBeenCalled()
      })

      // The diagram paths share the method with the forms, so they are checked alongside
      it('opens a bpmn diagram in the diagram viewer after resetting it', async () => {
        ProcessService.fetchDiagram.mockResolvedValueOnce({ bpmn20Xml: '<bpmn/>' })
        const { $refs } = await show({ id: 'r3', deploymentId: 'd1', name: 'invoice.bpmn' }, 'bpmn')

        expect($refs.diagram.cleanDiagramState).toHaveBeenCalled()
        expect($refs.diagram.drawDiagramState).toHaveBeenCalled()
        expect($refs.diagram.showDiagram).toHaveBeenCalledWith('<bpmn/>')
      })

      it('opens a dmn diagram in the dmn viewer after resetting it', async () => {
        vi.useFakeTimers()
        const $refs = viewers()
        const vm = context({
          $refs, isFormResource: false, isHtmlResource: false, isDmnResource: true,
          getXmlById: vi.fn(() => Promise.resolve({ dmnXml: '<dmn/>' }))
        })
        await vm.showResource({ id: 'r4', deploymentId: 'd1', name: 'rules.dmn' })
        vi.advanceTimersByTime(500)
        vi.useRealTimers()

        expect(vm.getDecisionList).toHaveBeenCalledWith({ deploymentId: 'd1', resourceName: 'rules.dmn' })
        expect($refs.dmnDiagram.cleanDiagramState).toHaveBeenCalled()
        expect($refs.dmnDiagram.showDiagram).toHaveBeenCalledWith('<dmn/>')
      })

      it('reports an error when the form has no content', async () => {
        const { vm, $refs } = await show(formResource, 'form', '')

        expect(vm.error).toBe(true)
        expect(vm.diagramLoading).toBe(false)
        expect($refs.formPreview.showForm).not.toHaveBeenCalled()
      })
    })

    it('downloads nothing when the resource has no content', async () => {
      DeploymentService.fetchDataFromDeploymentResource.mockResolvedValueOnce('')
      const triggerDownload = vi.fn()
      const vm = context({ $root: { user: { authToken: 'tok' } }, $refs: { downloadPopper: { triggerDownload } } })

      await vm.download(formResource)

      expect(triggerDownload).not.toHaveBeenCalled()
    })
  })
})
