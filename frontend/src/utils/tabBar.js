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
 * How a tab bar is built, for every bar that renders one.
 *
 * A bar owns its tabs, their order and which of them a configuration leaves out. What it does
 * not own is what surrounds them: the ids it takes are reserved against plugins, the deep links
 * a configuration adds are appended, and so are the tabs plugins contribute. Those three are
 * the same wherever a bar is rendered, and were copied into each of them, which is how the
 * enterprise bars ended up with the plugin part and without the deep link part.
 *
 * @param {string} section the deep link section of this bar, e.g. 'processInstance'
 * @param {string} slot the plugin slot of this bar, e.g. 'process-instance-tab'
 * @param {Array} builtin the bar's own tabs, as { id, text }
 * @param {Array} reserve the ids plugins may not take, the built-in ones by default. A bar
 *        whose tabs depend on the configuration reserves every id it can render, including
 *        the ones it leaves out today
 * @returns a function building the full list, called with the current configuration
 */
export function defineTabBar({ section, slot, builtin = [], reserve = builtin.map(tab => tab.id) }) {
  // At import time, because a plugin can register before this bar is ever rendered
  reserveSlotIds(slot, reserve)

  return ({ builtin: current = builtin, config = null, t = key => key } = {}) => {
    const deepLinkTabs = getDeepLinkEntries(config, section, reserve)
      .filter(entry => entry.type === 'tab')
      .map(entry => ({ id: entry.id, text: resolveDeepLinkLabel(t, entry) }))

    // Contributed tabs are appended, so the built-in ones keep their order whatever is deployed.
    // Their content is rendered by the PluginSlot of the view this bar belongs to.
    const contributed = getPlugin(slot).value
      .filter(contribution => contribution.id && contribution.text)
      .map(({ id, text }) => ({ id, text }))

    return [...current, ...deepLinkTabs, ...contributed]
  }
}
