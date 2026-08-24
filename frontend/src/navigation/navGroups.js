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

/**
 * Navigation catalog vs surface projections
 * ----------------------------------------
 * `buildNavGroups` is the shared IA catalog for CE's four groups (Tasks /
 * Cockpit / Builder / Admin). There is no CE "Data" group — EE (Ins7ght) and
 * Flow (dataflow) inject their own `data` group entirely, via the same
 * NavGroupsExtender hook StartView.vue already calls; see that component's
 * `navGroups` computed. Surfaces project the catalog — they are not
 * independent menus:
 *
 * - navbar     — dense power-user dropdown (all secondary destinations)
 * - startHover — short orientation list on start tiles (tile click covers group default)
 * - hub pages  — same subset as startHover (or identity-only for Access management)
 *
 * Item membership uses a single `surfaces` allowlist (default: both).
 * Do not add parallel booleans (navbarOnly / omitFromStartHover).
 * Admin system leaves use `collapseGroup: 'system'` so startHover shows one System entry
 * while the navbar keeps the leaf destinations.
 */

import taskImage from '@/assets/images/start/task.svg'
import processImage from '@/assets/images/start/process.svg'
import modelerImage from '@/assets/images/start/modeler.svg'
import adminUsersImage from '@/assets/images/admin/users_admin.svg'
import groupsAdminImage from '@/assets/images/admin/groups_admin.svg'
import tenantsAdminImage from '@/assets/images/admin/tenants_admin.svg'
import authorizationsAdminImage from '@/assets/images/admin/authorizations_admin.svg'

export const SURFACES = {
  NAVBAR: 'navbar',
  START_HOVER: 'startHover'
}

const DEFAULT_SURFACES = [SURFACES.NAVBAR, SURFACES.START_HOVER]

/**
 * Collapsed startHover entries for navbar-only leaf groups, keyed by
 * `collapseGroup`. Each entry stands in for its whole leaf group on the
 * startHover surface, so its to/icon/title are deliberately the group-level
 * "System" identity, not any single leaf's — kept in sync by hand with the
 * `collapseGroup: 'system'` items inside buildNavGroups's admin group below.
 */
export const COLLAPSE_TARGETS = {
  system: {
    to: '/seven/auth/admin/system',
    icon: 'mdi-cog-outline',
    title: 'admin.system.title',
    tooltip: 'admin.system.tooltip'
  }
}

/**
 * Builds CE's four navigation groups (Tasks / Cockpit / Builder / Admin).
 * EE/Flow inject their own `data` group on top of this via NavGroupsExtender.
 *
 * @param {object} ctx permission flags and startableProcesses boolean
 * @returns {Array<object>}
 */
export function buildNavGroups(ctx) {
  return [{
    id: 'tasks',
    icon: 'mdi-clipboard-text-outline',
    title: 'start.tasks.title',
    defaultTo: '/seven/auth/tasks',
    startTo: { name: 'tasksHome' },
    show: !!ctx.permissionsTaskList,
    items: [{
      to: '/seven/auth/tasks',
      active: ['seven/auth/tasks'],
      icon: 'mdi-clipboard-text-outline',
      tileImage: taskImage,
      tooltip: 'start.taskList.tooltip',
      title: 'start.taskList.title'
    }, {
      show: !!ctx.startableProcesses,
      to: '/seven/auth/start-process',
      active: ['seven/auth/start-process'],
      icon: 'mdi-play-circle-outline',
      tileImage: processImage,
      tooltip: 'start.startProcess.tooltip',
      title: 'start.startProcess.title'
    }]
  }, {
    id: 'cockpit',
    icon: 'mdi-chart-donut-variant',
    title: 'start.cockpit.title',
    defaultTo: '/seven/auth/processes',
    startTo: { name: 'cockpit' },
    show: !!ctx.permissionsCockpit,
    items: [{
      // Tile click already goes to dashboard via defaultTo / cockpit redirect
      surfaces: [SURFACES.NAVBAR],
      to: '/seven/auth/processes',
      active: ['seven/auth/processes/dashboard'],
      icon: 'mdi-view-dashboard-outline',
      tooltip: 'start.cockpit.tooltip',
      title: 'start.cockpit.dashboard.title'
    }, {
      to: '/seven/auth/processes/list',
      active: ['seven/auth/process/', 'seven/auth/processes/list'],
      icon: 'mdi-map-legend',
      tooltip: 'start.cockpit.processes.tooltip',
      title: 'start.cockpit.processes.title'
    }, {
      divider: true
    }, {
      to: '/seven/auth/decisions/list',
      active: ['seven/auth/decision/', 'seven/auth/decisions/list'],
      icon: 'mdi-wall-sconce-flat-outline',
      tooltip: 'start.cockpit.decisions.tooltip',
      title: 'start.cockpit.decisions.title'
    }, {
      divider: true
    }, {
      to: '/seven/auth/human-tasks',
      active: ['seven/auth/human-tasks'],
      icon: 'mdi-account-file-text-outline',
      tooltip: 'start.cockpit.humanTasks.tooltip',
      title: 'start.cockpit.humanTasks.title'
    }, {
      divider: true
    }, {
      to: '/seven/auth/deployments',
      active: ['seven/auth/deployments'],
      icon: 'mdi-upload-box-outline',
      tooltip: 'start.cockpit.deployments.tooltip',
      title: 'start.cockpit.deployments.title'
    }, {
      divider: true
    }, {
      to: '/seven/auth/batches',
      active: ['seven/auth/batches'],
      icon: 'mdi-repeat',
      tooltip: 'start.cockpit.batches.tooltip',
      title: 'start.cockpit.batches.title',
      activeExact: true
    }]
  }, {
    id: 'builder',
    icon: 'mdi-hammer-wrench',
    title: 'start.builder.title',
    defaultTo: '/seven/auth/modeler',
    startTo: { name: 'builderHome' },
    show: !!ctx.permissionsModeler,
    items: [{
      to: '/seven/auth/modeler',
      active: ['seven/auth/modeler'],
      icon: 'mdi-drawing-box',
      tileImage: modelerImage,
      tooltip: 'start.modeler.tooltip',
      title: 'start.modeler.title'
    }]
  }, {
    id: 'admin',
    icon: 'mdi-shield-account-variant-outline',
    title: 'start.admin.title',
    defaultTo: '/seven/auth/admin',
    startTo: { name: 'usersManagement' },
    // Access Management (a separate identity-only hub page, not a navbar
    // leaf) doesn't correspond to any single item below, so it can't be
    // covered by an item's routeName/activeRouteNames — list it here instead.
    hubRouteNames: ['accessManagement'],
    show: !!ctx.permissionsUsers,
    items: [{
      show: !!ctx.permissionsUsersManagement,
      to: '/seven/auth/admin/users',
      routeName: 'adminUsers',
      activeRouteNames: ['adminUser', 'createUser'],
      icon: 'mdi-account-search-outline',
      tileImage: adminUsersImage,
      tooltip: 'admin.users.tooltip',
      title: 'admin.users.title'
    }, {
      show: !!ctx.permissionsGroupsManagement,
      to: '/seven/auth/admin/groups',
      routeName: 'adminGroups',
      activeRouteNames: ['adminGroup', 'createGroup'],
      icon: 'mdi-account-group-outline',
      tileImage: groupsAdminImage,
      tooltip: 'admin.groups.tooltip',
      title: 'admin.groups.title'
    }, {
      show: !!ctx.permissionsTenantsManagement,
      to: '/seven/auth/admin/tenants',
      routeName: 'adminTenants',
      activeRouteNames: ['adminTenant', 'createTenant'],
      icon: 'mdi-domain',
      tileImage: tenantsAdminImage,
      tooltip: 'admin.tenants.tooltip',
      title: 'admin.tenants.title'
    }, {
      show: !!ctx.permissionsAuthorizationsManagement,
      to: '/seven/auth/admin/authorizations',
      routeName: 'authorizations',
      activeRouteNames: ['authorizationType'],
      icon: 'mdi-account-key-outline',
      tileImage: authorizationsAdminImage,
      tooltip: 'admin.authorizations.tooltip',
      title: 'admin.authorizations.title'
    }, {
      show: !!ctx.permissionsSystemManagement,
      divider: true
    }, {
      // Both 'system' leaves below collapse to the single COLLAPSE_TARGETS.system
      // entry on startHover — keep that entry's to/icon/title in sync by hand
      // if this leaf's identity changes.
      show: !!ctx.permissionsSystemManagement,
      surfaces: [SURFACES.NAVBAR],
      collapseGroup: 'system',
      to: '/seven/auth/admin/system/system-diagnostics',
      active: ['seven/auth/admin/system/system-diagnostics'],
      icon: 'mdi-cog-outline',
      tooltip: 'admin.system.system-diagnostics.title',
      title: 'admin.system.system-diagnostics.title'
    }, {
      show: !!ctx.permissionsSystemManagement,
      surfaces: [SURFACES.NAVBAR],
      collapseGroup: 'system',
      to: '/seven/auth/admin/system/execution-metrics',
      active: ['seven/auth/admin/system/execution-metrics'],
      icon: 'mdi-chart-timeline-variant',
      tooltip: 'admin.system.execution-metrics.title',
      title: 'admin.system.execution-metrics.title'
    }]
  }]
}

export function itemSurfaces(item) {
  return item.surfaces ?? DEFAULT_SURFACES
}

export function itemVisibleOnSurface(item, surface) {
  if (item.divider) return true
  return itemSurfaces(item).includes(surface)
}

/**
 * Filter groups/items by show flags and collapse consecutive dividers.
 * Optionally restrict items to a surface allowlist.
 */
export function filterVisibleNavGroups(groups, surface = null) {
  return groups
    .filter(tool => tool.show)
    .map(tool => {
      const filtered = (tool.items || []).filter(item => {
        if (item.show === false) return false
        if (surface && !itemVisibleOnSurface(item, surface)) return false
        return true
      })
      const cleaned = []
      for (const item of filtered) {
        if (item.divider) {
          if (cleaned.length === 0 || cleaned[cleaned.length - 1].divider) continue
          cleaned.push(item)
        } else {
          cleaned.push(item)
        }
      }
      while (cleaned.length && cleaned[cleaned.length - 1].divider) cleaned.pop()
      if (cleaned.length === 0) return null
      return { ...tool, items: cleaned }
    })
    .filter(Boolean)
}

/** Navbar projection of the catalog. */
export function projectGroupsForNavbar(groups) {
  return filterVisibleNavGroups(groups, SURFACES.NAVBAR)
}

/**
 * Map nav-group items to start-tile hover options (skip dividers; resolve i18n via $t).
 */
export function navItemsToTileOptions(items, t) {
  return (items || [])
    .filter(item => !item.divider && item.to)
    .map(item => ({
      to: item.to,
      icon: item.icon,
      title: t(item.title),
      tooltip: t(item.tooltip || item.title)
    }))
}

/**
 * Start-hover projection: surfaces includes startHover, plus collapsed groups
 * for navbar-only leaves that declare collapseGroup.
 */
export function projectStartHoverOptions(items, t) {
  const options = []
  const collapsed = new Set()

  for (const item of items || []) {
    if (item.divider || item.show === false) continue

    if (item.collapseGroup) {
      if (item.show === false) continue
      if (!collapsed.has(item.collapseGroup)) {
        const target = COLLAPSE_TARGETS[item.collapseGroup]
        if (target) {
          options.push({
            to: target.to,
            icon: target.icon,
            title: t(target.title),
            tooltip: t(target.tooltip || target.title)
          })
          collapsed.add(item.collapseGroup)
        }
      }
      continue
    }

    if (!itemVisibleOnSurface(item, SURFACES.START_HOVER)) continue
    if (!item.to) continue

    options.push({
      to: item.to,
      icon: item.icon,
      src: item.tileImage,
      title: t(item.title),
      tooltip: t(item.tooltip || item.title)
    })
  }

  return options
}

/**
 * When a group's start-hover options resolve to exactly one destination, the
 * start tile can represent that destination directly instead of showing a
 * generic hub tile — e.g. Builder's tile becomes "Modeler" when Modeler is
 * the only builder option; a Data tile becomes "Ins7ght" once that's its
 * only option. Returns null when the tile should keep its own hub identity
 * (zero, or more than one, option).
 */
export function singleOptionTile(options) {
  if (options?.length !== 1) return null
  const [only] = options
  return { to: only.to, title: only.title }
}

/**
 * When Tasks is the only tile a user has, the start page itself has nothing
 * left to offer — go straight into Tasks instead. Resolves to the tasklist
 * directly if that's the only tasks option, otherwise to the tasks hub
 * (tasklist + start process). Returns null when Tasks isn't the sole tile.
 */
export function tasksOnlyRedirectTarget(tiles, tasksOptions) {
  if (tiles?.length !== 1 || tiles[0] !== 'tasks') return null
  const single = singleOptionTile(tasksOptions)
  return single ? single.to : { name: 'tasksHome' }
}

/**
 * Resolves a single visible group from a Vue component instance — the
 * shared "build the catalog, filter by permission, pick one group" chain
 * used by single-group hub pages (Tasks/Builder/Access-management hubs).
 */
export function getVisibleGroup(vm, id) {
  const groups = filterVisibleNavGroups(buildNavGroups(permissionFlagsFromVm(vm)))
  return groups.find(g => g.id === id)
}

/**
 * Access-management hub: identity catalog items only (no system / collapse groups).
 */
export function accessManagementCatalogItems(adminItems) {
  return (adminItems || []).filter(item => item.show !== false && item.routeName)
}

/**
 * Reads the permission flags navigationPermissionsMixin computes on a Vue
 * component instance into the plain object buildNavGroups expects. Used
 * across the navbar, the start-page tiles, and the admin pages — it's a
 * dumb bridge, not a second source of permission logic.
 */
export function permissionFlagsFromVm(vm) {
  return {
    permissionsTaskList: !!vm.permissionsTaskList,
    startableProcesses: !!vm.startableProcesses,
    permissionsCockpit: !!vm.permissionsCockpit,
    permissionsModeler: !!vm.permissionsModeler,
    permissionsUsers: !!vm.permissionsUsers,
    permissionsUsersManagement: !!vm.permissionsUsersManagement,
    permissionsGroupsManagement: !!vm.permissionsGroupsManagement,
    permissionsTenantsManagement: !!vm.permissionsTenantsManagement,
    permissionsAuthorizationsManagement: !!vm.permissionsAuthorizationsManagement,
    permissionsSystemManagement: !!vm.permissionsSystemManagement
  }
}
