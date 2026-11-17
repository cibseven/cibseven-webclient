/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Where a menu entry's icon goes, the same in the desktop menus (NavCluster) and the mobile menu (NavMobileMenu):
// in front of the label, or after it when the entry sets iconEnd (Logout)

/** @returns {string|null} The icon in front of the entry's label, if any */
export function leadingIcon(item) {
  return item.icon && !item.iconEnd ? item.icon : null
}

/** @returns {string|null} The icon after the entry's label, if any */
export function trailingIcon(item) {
  return item.icon && item.iconEnd ? item.icon : null
}
