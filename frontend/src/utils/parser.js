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

export function parseXMLDocumentation(bpmnXml) {
  const parser = new window.DOMParser()
  const xmlDoc = parser.parseFromString(bpmnXml, 'text/xml')
  const activityTags = [
    'bpmn:task', 'bpmn:userTask', 'bpmn:serviceTask', 'bpmn:scriptTask',
    'bpmn:manualTask', 'bpmn:businessRuleTask', 'bpmn:callActivity',
    'bpmn:subProcess', 'bpmn:startEvent', 'bpmn:endEvent',
    'task', 'userTask', 'serviceTask', 'scriptTask',
    'manualTask', 'businessRuleTask', 'callActivity', 'subProcess',
    'startEvent', 'endEvent'
  ]
  const docs = []
  const seen = new Set()
  activityTags.forEach(tag => {
    const els = Array.from(xmlDoc.getElementsByTagName(tag))
    els.forEach(el => {
      if (seen.has(el)) return
      seen.add(el)
      const docNode = el.querySelector('documentation') || el.querySelector('bpmn\\:documentation')
      if (docNode && docNode.textContent.trim()) {
        docs.push({
          id: el.getAttribute('id') || null,
          element: el.getAttribute('name') || el.getAttribute('id'),
          documentation: docNode.textContent.trim()
        })
      }
    })
  })
  return docs
}