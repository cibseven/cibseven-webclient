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
export function sortDeployments(a, b, sorting, order) {
  if (!a[sorting] && b[sorting]) return -1
  else if (a[sorting] && !b[sorting]) return 1
  a = a[sorting].toLowerCase()
  b = b[sorting].toLowerCase()
  if (order === 'asc') return a < b ? -1 : a > b ? 1 : 0
  else return a < b ? 1 : a > b ? -1 : 0
}
