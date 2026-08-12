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
 * `buildNavGroups` is the shared IA catalog (Tasks / Cockpit / Builder / Data / Admin).
 * Surfaces project that catalog — they are not independent menus:
 *
 * - toolbar    — dense power-user dropdown (all secondary destinations)
 * - startHover — short orientation list on start tiles (tile click covers group default)
 * - hub pages  — same subset as startHover (or identity-only for Access management)
 *
 * Item membership uses a single `surfaces` allowlist (default: both).
 * Do not add parallel booleans (toolbarOnly / omitFromStartHover).
 * Admin system leaves use `collapseGroup: 'system'` so startHover shows one System entry
 * while the toolbar keeps the leaf destinations.
 */

export const SURFACES = {
  TOOLBAR: 'toolbar',
  START_HOVER: 'startHover'
}

const DEFAULT_SURFACES = [SURFACES.TOOLBAR, SURFACES.START_HOVER]

/** Collapsed startHover entries for toolbar-only leaf groups. */
export const COLLAPSE_TARGETS = {
  system: {
    to: '/seven/auth/admin/system',
    icon: 'mdi-cog-outline',
    title: 'admin.system.title',
    tooltip: 'admin.system.tooltip'
  }
}

/**
 * Builds the five CE navigation groups (Tasks / Cockpit / Builder / Data / Admin).
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
      tooltip: 'start.taskList.tooltip',
      title: 'start.taskList.title'
    }, {
      show: !!ctx.startableProcesses,
      to: '/seven/auth/start-process',
      active: ['seven/auth/start-process'],
      icon: 'mdi-play-circle-outline',
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
      surfaces: [SURFACES.TOOLBAR],
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
      tooltip: 'start.modeler.tooltip',
      title: 'start.modeler.title'
    }]
  }, {
    id: 'data',
    icon: 'mdi-database-outline',
    title: 'start.data.title',
    startTo: { name: 'dataHome' },
    show: false,
    items: []
  }, {
    id: 'admin',
    icon: 'mdi-shield-account-variant-outline',
    title: 'start.admin.title',
    defaultTo: '/seven/auth/admin',
    startTo: { name: 'usersManagement' },
    show: !!ctx.permissionsUsers,
    items: [{
      hub: 'users',
      show: !!ctx.permissionsUsersManagement,
      to: '/seven/auth/admin/users',
      routeName: 'adminUsers',
      active: ['seven/auth/admin/user', 'seven/auth/admin/create-user'],
      icon: 'mdi-account-search-outline',
      tooltip: 'admin.users.tooltip',
      title: 'admin.users.title'
    }, {
      hub: 'groups',
      show: !!ctx.permissionsGroupsManagement,
      to: '/seven/auth/admin/groups',
      routeName: 'adminGroups',
      active: ['seven/auth/admin/group', 'seven/auth/admin/create-group'],
      icon: 'mdi-account-group-outline',
      tooltip: 'admin.groups.tooltip',
      title: 'admin.groups.title'
    }, {
      hub: 'tenants',
      show: !!ctx.permissionsTenantsManagement,
      to: '/seven/auth/admin/tenants',
      routeName: 'adminTenants',
      active: ['seven/auth/admin/tenant', 'seven/auth/admin/create-tenant'],
      icon: 'mdi-domain',
      tooltip: 'admin.tenants.tooltip',
      title: 'admin.tenants.title'
    }, {
      hub: 'authorizations',
      show: !!ctx.permissionsAuthorizationsManagement,
      to: '/seven/auth/admin/authorizations',
      routeName: 'authorizations',
      active: ['seven/auth/admin/authorizations'],
      icon: 'mdi-account-key-outline',
      tooltip: 'admin.authorizations.tooltip',
      title: 'admin.authorizations.title'
    }, {
      show: !!ctx.permissionsSystemManagement,
      divider: true
    }, {
      show: !!ctx.permissionsSystemManagement,
      surfaces: [SURFACES.TOOLBAR],
      collapseGroup: 'system',
      to: '/seven/auth/admin/system/system-diagnostics',
      active: ['seven/auth/admin/system/system-diagnostics'],
      icon: 'mdi-cog-outline',
      tooltip: 'admin.system.system-diagnostics.title',
      title: 'admin.system.system-diagnostics.title'
    }, {
      show: !!ctx.permissionsSystemManagement,
      surfaces: [SURFACES.TOOLBAR],
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

/** Toolbar projection of the catalog. */
export function projectGroupsForToolbar(groups) {
  return filterVisibleNavGroups(groups, SURFACES.TOOLBAR)
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
 * for toolbar-only leaves that declare collapseGroup.
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
      title: t(item.title),
      tooltip: t(item.tooltip || item.title)
    })
  }

  return options
}

/** @deprecated Use projectStartHoverOptions */
export function cockpitItemsToTileOptions(items, t) {
  return projectStartHoverOptions(items, t)
}

/** @deprecated Use projectStartHoverOptions */
export function adminItemsToTileOptions(items, t) {
  return projectStartHoverOptions(items, t)
}

/**
 * Access-management hub: identity catalog items only (no system / collapse groups).
 */
export function accessManagementCatalogItems(adminItems) {
  return (adminItems || []).filter(item =>
    !item.divider
    && item.show !== false
    && item.hub
    && !item.collapseGroup
  )
}

/**
 * Permission context for buildNavGroups from a Vue component instance.
 */
export function navContextFromVm(vm) {
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
