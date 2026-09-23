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
import { getDeepLinkEntries, resolveDeepLinkLabel } from '@/utils/deepLinks.js'
import { getPlugin, reserveSlotIds } from '@/plugins/pluginsConfig.js'

/**
 * How a tab bar is built. A bar owns its tabs and their order; what surrounds them is the same
 * everywhere, and copying it into each bar is how the enterprise ones ended up with the plugin
 * part and without the deep links.
 *
 * @param {string} deepLinkSection section of the deepLinks configuration, e.g. 'processInstance'
 * @param {string} pluginSlot slot plugins contribute tabs to, e.g. 'process-instance-tab'
 * @param {Array} builtin the bar's own tabs, as { id, text }
 * @param {Array} reservedIds ids a plugin may not take, the built-in ones by default. A bar whose
 *        tabs depend on the configuration reserves every id it can render, today's included
 * @returns a function building the full list, called with the current configuration
 */
export function defineTabBar({ deepLinkSection, pluginSlot, builtin = [], reservedIds = builtin.map(tab => tab.id) }) {
  // At import time, because a plugin can register before this bar is ever rendered
  reserveSlotIds(pluginSlot, reservedIds)

  return ({ builtin: current = builtin, config = null, t = key => key } = {}) => {
    const deepLinkTabs = getDeepLinkEntries(config, deepLinkSection, reservedIds)
      .filter(entry => entry.type === 'tab')
      .map(entry => ({ id: entry.id, text: resolveDeepLinkLabel(t, entry) }))

    // Appended, so the built-in order never depends on what is configured or deployed
    const contributed = getPlugin(pluginSlot).value
      .filter(contribution => contribution.id && contribution.text)
      .map(({ id, text }) => ({ id, text }))

    return [...current, ...deepLinkTabs, ...contributed]
  }
}
