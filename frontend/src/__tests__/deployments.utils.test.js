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
import { sortDeployments } from '@/components/deployment/utils'

const deployments = [
  {
    id: "b21d793d-29c4-11f0-8d84-00059a3c7a00",
    name: "InvoiceProcessApplication",
    deploymentTime: "2025-05-05T17:22:02.694+0200"
  },
  {
    id: "b1d26673-29c4-11f0-8d84-00059a3c7a00",
    name: null,
    deploymentTime: "2025-05-05T17:22:02.217+0200"
  }
]

describe('sortDeployments', () => {

  describe('name', () => {
    it('should sort by name ascending', () => {
      const arr = [...deployments].sort((a, b) => sortDeployments(a, b, 'name', 'asc'))
      expect(arr[0].name).toBeNull()
      expect(arr[1].name).toBe('InvoiceProcessApplication')
    })

    it('should sort by name descending', () => {
      const arr = [...deployments].sort((a, b) => sortDeployments(a, b, 'name', 'desc'))
      expect(arr[0].name).toBe('InvoiceProcessApplication')
      expect(arr[1].name).toBeNull()
    })

    it('sort 4 items, asc', () => {
      const arr = [
        {name: 'a2'},
        {name: 'a1'},
        {},
        {name: 'a4'},
        {name: 'a3'},
      ].sort((a, b) => sortDeployments(a, b, 'name', 'asc'))
      expect(arr[0].name).toBeUndefined()
      expect(arr[1].name).toBe('a1')
      expect(arr[2].name).toBe('a2')
      expect(arr[3].name).toBe('a3')
      expect(arr[4].name).toBe('a4')
    })

    it('sort 4 items, desc', () => {
      const arr = [
        {name: 'a2'},
        {name: 'a1'},
        {},
        {name: 'a4'},
        {name: 'a3'},
      ].sort((a, b) => sortDeployments(a, b, 'name', 'desc'))
      expect(arr[0].name).toBe('a4')
      expect(arr[1].name).toBe('a3')
      expect(arr[2].name).toBe('a2')
      expect(arr[3].name).toBe('a1')
      expect(arr[4].name).toBeUndefined()
    })

    it('both nulls', () => {
      const arr = deployments.map((d) => { return {...d, name: null} }).sort((a, b) => sortDeployments(a, b, 'name', 'desc'))
      expect(arr[0].name).toBeNull()
      expect(arr[1].name).toBeNull()
    })

    it('both undefined', () => {
      const arr = deployments.map((d) => { return {...d, name: undefined} }).sort((a, b) => sortDeployments(a, b, 'name', 'desc'))
      expect(arr[0].name).toBeUndefined()
      expect(arr[1].name).toBeUndefined()
    })
  })

  describe('deploymentTime', () => {
    it('should sort by deploymentTime ascending', () => {
      const arr = [...deployments].sort((a, b) => sortDeployments(a, b, 'deploymentTime', 'asc'))
      expect(arr[0].deploymentTime).toBe("2025-05-05T17:22:02.217+0200")
      expect(arr[1].deploymentTime).toBe("2025-05-05T17:22:02.694+0200")
    })

    it('should sort by deploymentTime descending', () => {
      const arr = [...deployments].sort((a, b) => sortDeployments(a, b, 'deploymentTime', 'desc'))
      expect(arr[0].deploymentTime).toBe("2025-05-05T17:22:02.694+0200")
      expect(arr[1].deploymentTime).toBe("2025-05-05T17:22:02.217+0200")
    })
  })
})
