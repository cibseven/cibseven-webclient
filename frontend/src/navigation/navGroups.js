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
 * `buildNavGroups` is CE's shared catalog (Tasks/Cockpit/Builder/Admin); EE/Flow
 * add a `data` group via NavGroupsExtender (see StartView.vue). Surfaces
 * (`navbar`, `startHover`, hub pages) project this same catalog — use the
 * `surfaces` allowlist per item instead of parallel booleans. Admin system
 * leaves collapse into one startHover entry via `collapseGroup: 'system'`.
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

// startHover stand-in for collapsed navbar-only leaves, keyed by collapseGroup.
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
 * @param {object} ctx permission flags and startableProcesses boolean
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
    // Access Management is a hub page, not a leaf, so it can't use routeName — list it here.
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
      // Collapses to COLLAPSE_TARGETS.system on startHover — keep that entry in sync by hand.
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

/** Start-hover projection: startHover-visible items, plus collapsed navbar-only leaves. */
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

/** Collapses a group's options to its one destination (e.g. Builder → "Modeler"); null otherwise. */
export function singleOptionTile(options) {
  if (options?.length !== 1) return null
  const [only] = options
  return { to: only.to, title: only.title }
}

/** Where to send the user when Tasks is the only tile: its sole option, or the tasks hub. */
export function tasksOnlyRedirectTarget(tiles, tasksOptions) {
  if (tiles?.length !== 1 || tiles[0] !== 'tasks') return null
  const single = singleOptionTile(tasksOptions)
  return single ? single.to : { name: 'tasksHome' }
}

/** Builds + filters the catalog, returns one group by id (used by single-group hub pages). */
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

/** Reads navigationPermissionsMixin's flags off a vm into the plain object buildNavGroups expects. */
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
