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
import { parseXMLDocumentation } from '@/utils/parser'

function parse(tasksXml) {
  return parseXMLDocumentation(`<?xml version="1.0" encoding="UTF-8"?>
    <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL">
      ${tasksXml}
    </bpmn:definitions>`)
}

describe('parseXMLDocumentation', () => {
  it('should return empty array for empty XML', () => {
    expect(parseXMLDocumentation('')).toEqual([])
  })
  it('should return empty array for null or undefined input', () => {
    expect(parseXMLDocumentation(null)).toEqual([])
    expect(parseXMLDocumentation(undefined)).toEqual([])
  })

  it('should handle malformed XML gracefully', () => {
    // parseXMLDocumentation should handle this gracefully and return empty array
    const result = parseXMLDocumentation('invalid xml')
    expect(Array.isArray(result)).toBe(true)
  })

  it('should parse single task with documentation', () => {
    const result = parse(`<bpmn:task id="task1" name="Test Task">
          <bpmn:documentation>This is a test task documentation</bpmn:documentation>
        </bpmn:task>`)

    expect(result).toHaveLength(1)
    expect(result[0]).toEqual({
      id: 'task1',
      element: 'Test Task',
      documentation: 'This is a test task documentation'
    })
  })

  it('should parse multiple tasks with documentation', () => {
    const result = parse(
        `<bpmn:userTask id="userTask1" name="User Task">
          <bpmn:documentation>User task documentation</bpmn:documentation>
        </bpmn:userTask>
        <bpmn:serviceTask id="serviceTask1" name="Service Task">
          <bpmn:documentation>Service task documentation</bpmn:documentation>
        </bpmn:serviceTask>`)

    expect(result).toHaveLength(2)
    expect(result[0]).toEqual({
      id: 'userTask1',
      element: 'User Task',
      documentation: 'User task documentation'
    })
    expect(result[1]).toEqual({
      id: 'serviceTask1',
      element: 'Service Task',
      documentation: 'Service task documentation'
    })
  })

  it('should handle tasks without names (fallback to id)', () => {
    const result = parse(`<bpmn:scriptTask id="script1">
          <bpmn:documentation>Script task without name</bpmn:documentation>
        </bpmn:scriptTask>`)

    expect(result).toHaveLength(1)
    expect(result[0]).toEqual({
      id: 'script1',
      element: 'script1',
      documentation: 'Script task without name'
    })
  })

  it('should handle tasks without IDs', () => {
    const result = parse(`<bpmn:manualTask name="Manual Task">
          <bpmn:documentation>Manual task without ID</bpmn:documentation>
        </bpmn:manualTask>`)

    expect(result).toHaveLength(1)
    expect(result[0]).toEqual({
      id: null,
      element: 'Manual Task',
      documentation: 'Manual task without ID'
    })
  })

  it('should ignore tasks without documentation', () => {
    const result = parse(`<bpmn:task id="task1" name="Task Without Doc" />
        <bpmn:userTask id="task2" name="Task With Doc">
          <bpmn:documentation>This has documentation</bpmn:documentation>
        </bpmn:userTask>`)

    expect(result).toHaveLength(1)
    expect(result[0]).toEqual({
      id: 'task2',
      element: 'Task With Doc',
      documentation: 'This has documentation'
    })
  })

  it('should ignore tasks with empty documentation', () => {
    const result = parse(`<bpmn:task id="task1" name="Task With Empty Doc">
          <bpmn:documentation></bpmn:documentation>
        </bpmn:task>
        <bpmn:task id="task2" name="Task With Whitespace Doc">
          <bpmn:documentation>   </bpmn:documentation>
        </bpmn:task>
        <bpmn:userTask id="task3" name="Task With Valid Doc">
          <bpmn:documentation>Valid documentation</bpmn:documentation>
        </bpmn:userTask>`)

    expect(result).toHaveLength(1)
    expect(result[0]).toEqual({
      id: 'task3',
      element: 'Task With Valid Doc',
      documentation: 'Valid documentation'
    })
  })

  it('should handle all supported activity types', () => {
    const result = parse(`<bpmn:task id="task1" name="Task">
          <bpmn:documentation>Task doc</bpmn:documentation>
        </bpmn:task>
        <bpmn:userTask id="userTask1" name="User Task">
          <bpmn:documentation>User task doc</bpmn:documentation>
        </bpmn:userTask>
        <bpmn:serviceTask id="serviceTask1" name="Service Task">
          <bpmn:documentation>Service task doc</bpmn:documentation>
        </bpmn:serviceTask>
        <bpmn:scriptTask id="scriptTask1" name="Script Task">
          <bpmn:documentation>Script task doc</bpmn:documentation>
        </bpmn:scriptTask>
        <bpmn:manualTask id="manualTask1" name="Manual Task">
          <bpmn:documentation>Manual task doc</bpmn:documentation>
        </bpmn:manualTask>
        <bpmn:businessRuleTask id="businessRuleTask1" name="Business Rule Task">
          <bpmn:documentation>Business rule task doc</bpmn:documentation>
        </bpmn:businessRuleTask>
        <bpmn:callActivity id="callActivity1" name="Call Activity">
          <bpmn:documentation>Call activity doc</bpmn:documentation>
        </bpmn:callActivity>
        <bpmn:subProcess id="subProcess1" name="Sub Process">
          <bpmn:documentation>Sub process doc</bpmn:documentation>
        </bpmn:subProcess>`)

    expect(result).toHaveLength(8)

    const taskTypes = result.map(r => r.element)
    expect(taskTypes).toContain('Task')
    expect(taskTypes).toContain('User Task')
    expect(taskTypes).toContain('Service Task')
    expect(taskTypes).toContain('Script Task')
    expect(taskTypes).toContain('Manual Task')
    expect(taskTypes).toContain('Business Rule Task')
    expect(taskTypes).toContain('Call Activity')
    expect(taskTypes).toContain('Sub Process')
  })

  it('should handle non-namespaced BPMN elements', () => {
    const result = parseXMLDocumentation(`<?xml version="1.0" encoding="UTF-8"?>
      <definitions>
        <task id="task1" name="Non-namespaced Task">
          <documentation>Non-namespaced documentation</documentation>
        </task>
        <userTask id="userTask1" name="Non-namespaced User Task">
          <documentation>Non-namespaced user task doc</documentation>
        </userTask>
      </definitions>`)

    expect(result).toHaveLength(2)
    expect(result[0]).toEqual({
      id: 'task1',
      element: 'Non-namespaced Task',
      documentation: 'Non-namespaced documentation'
    })
    expect(result[1]).toEqual({
      id: 'userTask1',
      element: 'Non-namespaced User Task',
      documentation: 'Non-namespaced user task doc'
    })
  })

  it('should handle mixed namespaced and non-namespaced elements', () => {
    const result = parse(`<bpmn:task id="task1" name="Namespaced Task">
          <bpmn:documentation>Namespaced documentation</bpmn:documentation>
        </bpmn:task>
        <userTask id="userTask1" name="Non-namespaced User Task">
          <documentation>Non-namespaced documentation</documentation>
        </userTask>`)

    expect(result).toHaveLength(2)
    expect(result[0]).toEqual({
      id: 'task1',
      element: 'Namespaced Task',
      documentation: 'Namespaced documentation'
    })
    expect(result[1]).toEqual({
      id: 'userTask1',
      element: 'Non-namespaced User Task',
      documentation: 'Non-namespaced documentation'
    })
  })

  it('should trim whitespace from documentation', () => {
    const result = parse(`<bpmn:task id="task1" name="Task">
          <bpmn:documentation>
            
            This documentation has whitespace
            
          </bpmn:documentation>
        </bpmn:task>`)

    expect(result).toHaveLength(1)
    expect(result[0]).toEqual({
      id: 'task1',
      element: 'Task',
      documentation: 'This documentation has whitespace'
    })
  })

  it('should handle duplicate elements correctly', () => {
    const result = parse(`<bpmn:task id="task1" name="Task 1">
          <bpmn:documentation>First task documentation</bpmn:documentation>
        </bpmn:task>
        <bpmn:task id="task2" name="Task 2">
          <bpmn:documentation>Second task documentation</bpmn:documentation>
        </bpmn:task>`)

    expect(result).toHaveLength(2)
    expect(result[0]).toEqual({
      id: 'task1',
      element: 'Task 1',
      documentation: 'First task documentation'
    })
    expect(result[1]).toEqual({
      id: 'task2',
      element: 'Task 2',
      documentation: 'Second task documentation'
    })
  })

  it('should handle complex nested BPMN structure', () => {
    const result = parse(`<bpmn:process id="process1">
          <bpmn:subProcess id="subprocess1" name="Main Subprocess">
            <bpmn:documentation>Subprocess documentation</bpmn:documentation>
            <bpmn:task id="nestedTask1" name="Nested Task">
              <bpmn:documentation>Nested task documentation</bpmn:documentation>
            </bpmn:task>
          </bpmn:subProcess>
          <bpmn:userTask id="mainTask1" name="Main Task">
            <bpmn:documentation>Main task documentation</bpmn:documentation>
          </bpmn:userTask>
        </bpmn:process>`)

    expect(result).toHaveLength(3)
    const taskNames = result.map(r => r.element)
    expect(taskNames).toContain('Main Subprocess')
    expect(taskNames).toContain('Nested Task')
    expect(taskNames).toContain('Main Task')
  })
})
