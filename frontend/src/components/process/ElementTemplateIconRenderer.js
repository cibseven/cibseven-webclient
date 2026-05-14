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

// Custom bpmn-js renderer that draws element-template icons on top of
// activities/events in a read-only viewer (cockpit). It overrides the default
// BpmnRenderer with HIGH_PRIORITY for elements that have a known template id,
// reading the icon data from a runtime registry populated by BpmnViewer.vue
// (so that NavigatedViewer does not have to mutate businessObjects after
// XML import).

import inherits from 'inherits-browser'
import BaseRenderer from 'diagram-js/lib/draw/BaseRenderer'
import { getBusinessObject, is, isAny } from 'bpmn-js/lib/util/ModelUtil'
import { isLabel } from 'bpmn-js/lib/util/LabelUtil'
import { append as svgAppend, attr as svgAttr, create as svgCreate } from 'tiny-svg'

const HIGH_PRIORITY = 1250
const ICON_SIZE = 18

function ElementTemplateIconRenderer(eventBus, bpmnRenderer, elementTemplateIcons) {
  this._bpmnRenderer = bpmnRenderer
  this._elementTemplateIcons = elementTemplateIcons
  BaseRenderer.call(this, eventBus, HIGH_PRIORITY)
}

inherits(ElementTemplateIconRenderer, BaseRenderer)

ElementTemplateIconRenderer.prototype._getIcon = function(element) {
  const bo = getBusinessObject(element)
  const templateId = bo && bo.get && bo.get('camunda:modelerTemplate')
  if (!templateId) return null
  return this._elementTemplateIcons.getIcon(templateId)
}

ElementTemplateIconRenderer.prototype.canRender = function(element) {
  if (isLabel(element)) return false
  return isAny(element, ['bpmn:Activity', 'bpmn:Event']) && !!this._getIcon(element)
}

ElementTemplateIconRenderer.prototype.drawShape = function(parentGfx, element, attrs = {}) {
  const handlerType = [
    'bpmn:BoundaryEvent',
    'bpmn:CallActivity',
    'bpmn:EndEvent',
    'bpmn:IntermediateCatchEvent',
    'bpmn:IntermediateThrowEvent',
    'bpmn:StartEvent',
    'bpmn:Task',
    'bpmn:AdHocSubProcess',
    'bpmn:Transaction',
    'bpmn:SubProcess'
  ].find(t => is(element, t))

  const renderer = handlerType && this._bpmnRenderer.handlers[handlerType]
  if (!renderer) return this._bpmnRenderer.drawShape(parentGfx, element)

  const gfx = renderer(parentGfx, element, { ...attrs, renderIcon: false })

  const icon = this._getIcon(element)
  const padding = is(element, 'bpmn:Activity')
    ? { x: 5, y: 5 }
    : { x: (element.width - ICON_SIZE) / 2, y: (element.height - ICON_SIZE) / 2 }

  const img = svgCreate('image')
  svgAttr(img, { href: icon, width: ICON_SIZE, height: ICON_SIZE, ...padding })
  svgAppend(parentGfx, img)

  return gfx
}

ElementTemplateIconRenderer.$inject = ['eventBus', 'bpmnRenderer', 'elementTemplateIcons']

// Registry service: holds the templateId → icon dataURL map. BpmnViewer.vue
// updates this after fetching templates and then fires `elements.changed`
// to trigger a re-render of affected shapes.
function ElementTemplateIcons(eventBus, elementRegistry) {
  this._eventBus = eventBus
  this._elementRegistry = elementRegistry
  this._icons = new Map()
}

ElementTemplateIcons.prototype.getIcon = function(templateId) {
  return this._icons.get(templateId) || null
}

ElementTemplateIcons.prototype.setIcons = function(iconsMap) {
  this._icons = iconsMap instanceof Map ? iconsMap : new Map(Object.entries(iconsMap || {}))
  // Re-render every element that references a template id, so the renderer
  // re-evaluates canRender/drawShape with the new icons available.
  const affected = this._elementRegistry.filter(el => {
    const bo = el.businessObject
    return bo && bo.get && bo.get('camunda:modelerTemplate')
  })
  if (affected.length) {
    this._eventBus.fire('elements.changed', { elements: affected })
  }
}

ElementTemplateIcons.$inject = ['eventBus', 'elementRegistry']

export default {
  __init__: ['elementTemplateIconRenderer'],
  elementTemplateIconRenderer: ['type', ElementTemplateIconRenderer],
  elementTemplateIcons: ['type', ElementTemplateIcons]
}
