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
import ProcessInstancesView from '@/components/process/ProcessInstancesView.vue'

describe('ProcessInstancesView', () => {
  describe('collapseButtons', () => {
    const collapseButtons = context => ProcessInstancesView.computed.collapseButtons.call(context)

    /**
     * The value is handed to components declaring a Boolean prop, while both operands are
     * values rather than flags: a component definition, and an activity id.
     */
    it('reports a boolean when a plugin takes the space of the search box', () => {
      const collapsed = collapseButtons({
        ProcessInstancesSearchBoxPlugin: { name: 'ProcessInstancesSearchBoxPlugin' },
        selectedActivityId: ''
      })

      expect(collapsed).toBe(true)
    })

    it('reports a boolean when an activity is selected', () => {
      const collapsed = collapseButtons({
        ProcessInstancesSearchBoxPlugin: null,
        selectedActivityId: 'Activity_1'
      })

      expect(collapsed).toBe(true)
    })

    it('keeps the labels when neither takes the space', () => {
      const collapsed = collapseButtons({
        ProcessInstancesSearchBoxPlugin: null,
        selectedActivityId: ''
      })

      expect(collapsed).toBe(false)
    })
  })
})
