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
// Import the CSS to ensure it is bundled with the package
import './assets/main.css';

import { axios, moment } from '@/globals.js'
import { permissionsMixin } from '@/permissions.js'
import registerOwnComponents from './register.js'
import processesVariablesMixin from '@/components/process/mixins/processesVariablesMixin.js'
import processesMixin from '@/components/process/mixins/processesMixin.js'
import resizerMixin from '@/components/process/mixins/resizerMixin.js'
import tabUrlMixin from '@/components/process/mixins/tabUrlMixin.js'
import store, { modules } from '@/store'
import usersMixin from '@/mixins/usersMixin.js'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import { debounce } from '@/utils/debounce.js'
import { formatDate, formatDateForTooltips, formatDuration } from '@/utils/dates.js'
import { HoverStyle, FlowTable, ContentBlock, ErrorDialog, ConfirmDialog, SuccessAlert,
  TaskPopper, CIBForm, SidebarsFlow, PagedScrollableContent, HighlightedText,
  GenericTabs, CopyableActionButton, TranslationsDownload} from '@cib/common-frontend'
import { InfoService, AuthService, SystemService, SetupService } from './services.js'
import { initEmbeddedForm } from './embedded-form/embedded-form.js'
import { i18n, setLanguage, loadTranslations, translationSources } from './i18n'
import { appRoutes,
  createAppRouter,
  authGuard,
  setupGuard,
  permissionsGuard,
  permissionsDeniedGuard,
  permissionsGuardUserAdmin } from './router.js'
import { updateAppTitle, checkExternalReturn, isMobile, hasHeader, getTheme, loadTheme } from './utils/init.js'
import { applyTheme, handleAxiosError, fetchAndStoreProcesses, fetchDecisionsIfEmpty, setupTaskNotifications } from './utils/init'
import { parseXMLDocumentation } from './utils/parser.js'
import { applyConfigDefaults } from './utils/config.js'
import { ENGINE_STORAGE_KEY } from './constants.js'
import CibSeven from '@/components/CibSeven.vue'
import AboutModal from '@/components/modals/AboutModal.vue'
import FeedbackModal from '@/components/modals/FeedbackModal.vue'
import FeedbackScreenshot from '@/components/modals/FeedbackScreenshot.vue'
import TaskList from '@/components/common-components/TaskList.vue'
import CIBHeaderFlow from '@/components/common-components/CIBHeaderFlow.vue'
import ResetDialog from '@/components/login/ResetDialog.vue'
import OtpDialog from '@/components/login/OtpDialog.vue'
import SecureInput from '@/components/login/SecureInput.vue'
import FilterableSelect from '@/components/task/filter/FilterableSelect.vue'
import IconButton from '@/components/render-template/IconButton.vue'
import MultisortModal from '@/components/process/modals/MultisortModal.vue'
import SmartSearch from '@/components/task/SmartSearch.vue'
import SupportModal from '@/components/modals/SupportModal.vue'
import AdminAuthorizations from '@/components/admin/AdminAuthorizations.vue'
import AdminAuthorizationsTable from '@/components/admin/AdminAuthorizationsTable.vue'
import AdminGroups from '@/components/admin/AdminGroups.vue'
import AdminUsers from '@/components/admin/AdminUsers.vue'
import AuthorizationsNavBar from '@/components/admin/AuthorizationsNavBar.vue'
import CreateGroup from '@/components/admin/CreateGroup.vue'
import CreateUser from '@/components/admin/CreateUser.vue'
import ProfileGroup from '@/components/admin/ProfileGroup.vue'
import ProfilePreferencesTab from '@/components/admin/ProfilePreferencesTab.vue';
import ProfileUser from '@/components/admin/ProfileUser.vue'
import UsersManagement from '@/components/admin/UsersManagement.vue'
import DeploymentList from '@/components/deployment/DeploymentList.vue'
import DeploymentsView from '@/components/deployment/DeploymentsView.vue'
import ResourcesNavBar from '@/components/deployment/ResourcesNavBar.vue'
import FilterModal from '@/components/task/filter/FilterModal.vue'
import FilterNavBar from '@/components/task/filter/FilterNavBar.vue'
import FilterNavCollapsed from '@/components/task/filter/FilterNavCollapsed.vue'
import ProcessView from '@/components/process/ProcessView.vue'
import AddVariableModal from '@/components/process/modals/AddVariableModal.vue'
import AddVariableModalUI from '@/components/process/modals/AddVariableModalUI.vue'
import EditVariableModal from '@/components/process/modals/EditVariableModal.vue'
import DeleteVariableModal from '@/components/process/modals/DeleteVariableModal.vue'
import BpmnViewer from '@/components/process/BpmnViewer.vue'
import InstancesTable from '@/components/process/tables/InstancesTable.vue'
import ProcessInstancesView from '@/components/process/ProcessInstancesView.vue'
import ProcessAdvanced from '@/components/process/ProcessAdvanced.vue'
import ProcessCard from '@/components/process/ProcessCard.vue'
import ProcessDetailsSidebar from '@/components/process/ProcessDetailsSidebar.vue'
import StartProcessList from '@/components/start-process/StartProcessList.vue'
import StartProcessView from '@/components/start-process/StartProcessView.vue'
import ProcessList from '@/components/processes/list/ProcessList.vue'
import ProcessListView from '@/components/processes/list/ProcessListView.vue'
import ProcessTable from '@/components/start-process/ProcessTable.vue'
import ProcessInstanceView from '@/components/process/ProcessInstanceView.vue'
import ProcessDefinitionView from '@/components/process/ProcessDefinitionView.vue'
import ProcessesDashboardView from '@/components/processes/dashboard/ProcessesDashboardView.vue'
import PieChart from '@/components/processes/dashboard/PieChart.vue'
import DeploymentItem from '@/components/processes/dashboard/DeploymentItem.vue'
import DeleteProcessDefinitionModal from '@/components/process/modals/DeleteProcessDefinitionModal.vue'
import ConfirmActionOnProcessInstanceModal from '@/components/process/modals/ConfirmActionOnProcessInstanceModal.vue'
import StartProcess from '@/components/start-process/StartProcess.vue'
import TaskAssignationModal from '@/components/process/modals/TaskAssignationModal.vue'
import VariablesTable from '@/components/process/tables/VariablesTable.vue'
import IncidentsTable from '@/components/process/tables/IncidentsTable.vue'
import UserTasksTable from '@/components/process/tables/UserTasksTable.vue'
import ExternalTasksTable from '@/components/process/tables/ExternalTasksTable.vue'
import RenderTemplate from '@/components/render-template/RenderTemplate.vue'
import AdvancedSearchModal from '@/components/task/AdvancedSearchModal.vue'
import TaskContent from '@/components/task/TaskContent.vue'
import TaskView from '@/components/task/TaskView.vue'
import TasksContent from '@/components/task/TasksContent.vue'
import TasksNavBar from '@/components/task/TasksNavBar.vue'
import TasksView from '@/components/task/TasksView.vue'
import HumanTasksView from '@/components/task/HumanTasksView.vue'
import DecisionView from '@/components/decision/DecisionView.vue'
import DecisionList from '@/components/decisions/list/DecisionList.vue'
import DecisionListView from '@/components/decisions/list/DecisionListView.vue'
import DecisionInstance from './components/decision/DecisionInstance.vue';
import DecisionDefinitionVersion from '@/components/decision/DecisionDefinitionVersion.vue'
import TenantsView from '@/components/tenants/TenantsView.vue'
import EditTenant from './components/tenants/EditTenant.vue';
import CreateTenant from './components/tenants/CreateTenant.vue';
import BatchesView from '@/components/batches/BatchesView.vue'
import RuntimeBatches from '@/components/batches/tables/RuntimeBatches.vue'
import HistoricBatches from '@/components/batches/tables/HistoricBatches.vue'
import BatchDetails from '@/components/batches/tables/BatchDetails.vue'
import SystemView from '@/components/system/SystemView.vue'
import { SystemSidebarItems } from '@/components/system/SystemView.vue'
import SystemDiagnostics from '@/components/system/SystemDiagnostics.vue'
import ExecutionMetrics from '@/components/system/ExecutionMetrics.vue'
import ShortcutsModal from '@/components/modals/ShortcutsModal.vue'
import ShortcutsTable from '@/components/modals/ShortcutsTable.vue'
import { TaskService, HistoryService, ProcessService, getServicesBasePath,
  setServicesBasePath, IncidentService, DecisionService, BatchService, DeploymentService } from '@/services.js';
import DeployedForm from '@/components/forms/DeployedForm.vue'
import StartDeployedForm from '@/components/forms/StartDeployedForm.vue'
import DecisionDefinitionDetails from '@/components/decision/DecisionDefinitionDetails.vue'
import DecisionVersionListSidebar from '@/components/decision/DecisionVersionListSidebar.vue'
import DmnViewer from '@/components/decision/DmnViewer.vue'
import TemplateBase from '@/components/forms/TemplateBase.vue'
import StartView from '@/components/start/StartView.vue'
import LoginView from '@/components/login/LoginView.vue'
import InitialSetup from '@/components/setup/InitialSetup.vue'
import StackTraceModal from '@/components/process/modals/StackTraceModal.vue'
import RetryModal from '@/components/process/modals/RetryModal.vue'
import ScrollableTabsContainer from '@/components/common-components/ScrollableTabsContainer.vue'

import JobLogModal from '@/components/batches/modals/JobLogModal.vue'
import FailedJobs from '@/components/batches/tables/FailedJobs.vue'
import LoginForm from '@/components/login/LoginForm.vue'
import ProcessDefinitionDetails from '@/components/process/ProcessDefinitionDetails.vue'
import ProcessInstanceTabs from '@/components/process/ProcessInstanceTabs.vue'
import ProcessInstancesTabs from '@/components/process/ProcessInstancesTabs.vue'
import AnnotationModal from '@/components/process/modals/AnnotationModal.vue'
import JobDefinitionPriorityModal from '@/components/process/modals/JobDefinitionPriorityModal.vue'
import JobDefinitionStateModal from '@/components/process/modals/JobDefinitionStateModal.vue'
import CalledProcessDefinitionsTable from '@/components/process/tables/CalledProcessDefinitionsTable.vue'
import CalledProcessInstancesTable from '@/components/process/tables/CalledProcessInstancesTable.vue'
import JobDefinitionsTable from '@/components/process/tables/JobDefinitionsTable.vue'
import JobsTable from '@/components/process/tables/JobsTable.vue'
import JobDueDateModal from '@/components/process/modals/JobDueDateModal.vue'

// mixins
import assigneeMixin from '@/mixins/assigneeMixin.js'
import { getEnabledShortcuts, getShortcutsForModal,
  getGlobalNavigationShortcuts, getTaskEventShortcuts, checkKeyMatch } from './utils/shortcuts.js'

const registerComponents = function(app) {
  app.component('cib-seven', CibSeven)
  app.component('flow-table', FlowTable)
  app.component('content-block', ContentBlock)
  app.component('error-dialog', ErrorDialog)
  app.component('confirm-dialog', ConfirmDialog)
  app.component('about-modal', AboutModal)
  app.component('feedback-modal', FeedbackModal)
  app.component('shortcuts-modal', ShortcutsModal)
  app.component('shortcuts-table', ShortcutsTable)
  app.component('success-alert', SuccessAlert)
  app.component('task-popper', TaskPopper)
  app.component('feedback-screenshot', FeedbackScreenshot)
  app.component('task-list', TaskList)
  app.component('cib-header-flow', CIBHeaderFlow)
  app.component('reset-dialog', ResetDialog)
  app.component('otp-dialog', OtpDialog)
  app.component('cib-form', CIBForm)
  app.component('secure-input', SecureInput)
  app.component('filterable-select', FilterableSelect)
  app.component('icon-button', IconButton)
  app.component('multisort-modal', MultisortModal)
  app.component('sidebars-flow', SidebarsFlow)
  app.component('smart-search', SmartSearch)
  app.component('support-modal', SupportModal)
  app.component('admin-authorizations', AdminAuthorizations)
  app.component('admin-authorizations-table', AdminAuthorizationsTable)
  app.component('admin-groups', AdminGroups)
  app.component('admin-users', AdminUsers)
  app.component('authorizations-nav-bar', AuthorizationsNavBar)
  app.component('create-group', CreateGroup)
  app.component('create-user', CreateUser)
  app.component('profile-group', ProfileGroup)
  app.component('profile-preferences-tab', ProfilePreferencesTab)
  app.component('profile-user', ProfileUser)
  app.component('users-management', UsersManagement)
  app.component('deployment-list', DeploymentList)
  app.component('paged-scrollable-content', PagedScrollableContent)
  app.component('deployments-view', DeploymentsView)
  app.component('resources-nav-bar', ResourcesNavBar)
  app.component('filter-modal', FilterModal)
  app.component('filter-nav-bar', FilterNavBar)
  app.component('filter-nav-collapsed', FilterNavCollapsed)
  app.component('process-view', ProcessView)
  app.component('add-variable-modal', AddVariableModal)
  app.component('add-variable-modal-ui', AddVariableModalUI)
  app.component('edit-variable-modal', EditVariableModal)
  app.component('delete-variable-modal', DeleteVariableModal)
  app.component('bpmn-viewer', BpmnViewer)
  app.component('instances-table', InstancesTable)
  app.component('process-instances-view', ProcessInstancesView)
  app.component('process-advanced', ProcessAdvanced)
  app.component('process-card', ProcessCard)
  app.component('process-details-sidebar', ProcessDetailsSidebar)
  app.component('start-process-list', StartProcessList)
  app.component('start-process-view', StartProcessView)
  app.component('process-list', ProcessList)
  app.component('process-list-view', ProcessListView)
  app.component('process-table', ProcessTable)
  app.component('process-instance-view', ProcessInstanceView)
  app.component('process-definition-view', ProcessDefinitionView)
  app.component('delete-process-definition-modal', DeleteProcessDefinitionModal)
  app.component('confirm-action-on-process-instance-modal', ConfirmActionOnProcessInstanceModal)
  app.component('processes-dashboard-view', ProcessesDashboardView)
  app.component('pie-chart', PieChart)
  app.component('deployment-item', DeploymentItem)
  app.component('start-process', StartProcess)
  app.component('task-assignation-modal', TaskAssignationModal)
  app.component('variables-table', VariablesTable)
  app.component('incidents-table', IncidentsTable)
  app.component('user-tasks-table', UserTasksTable)
  app.component('render-template', RenderTemplate)
  app.component('advanced-search-modal', AdvancedSearchModal)
  app.component('task-content', TaskContent)
  app.component('task-view', TaskView)
  app.component('tasks-content', TasksContent)
  app.component('tasks-nav-bar', TasksNavBar)
  app.component('tasks-view', TasksView)
  app.component('highlighted-text', HighlightedText)
  app.component('human-tasks', HumanTasksView)
  app.component('decision-view', DecisionView)
  app.component('decision-list', DecisionList)
  app.component('decision-list-view', DecisionListView)
  app.component('tenants-view', TenantsView)
  app.component('edit-tenant', EditTenant)
  app.component('create-tenant', CreateTenant)
  app.component('batches-view', BatchesView)
  app.component('system-view', SystemView)
  app.component('deployed-form', DeployedForm)
  app.component('decision-definition-details', DecisionDefinitionDetails)
  app.component('decision-version-list-sidebar', DecisionVersionListSidebar)
  app.component('dmn-viewer', DmnViewer)
  app.component('template-base', TemplateBase)
  app.component('start-view', StartView)
  app.component('login-view', LoginView)
}

export {
  registerComponents,
  TenantsView,
  CreateTenant,
  EditTenant,
  BatchesView,
  SystemView,
  SystemSidebarItems,
  axios,
  moment,
  permissionsMixin,
  registerOwnComponents,
  store,
  modules as storeModules,
  usersMixin,
  processesVariablesMixin,
  processesMixin,
  resizerMixin,
  copyToClipboardMixin,
  debounce,
  formatDate,
  formatDateForTooltips,
  formatDuration,
  HoverStyle,
  CibSeven,
  FlowTable,
  ContentBlock,
  ErrorDialog,
  ConfirmDialog,
  FeedbackModal,
  ShortcutsModal,
  ShortcutsTable,
  SuccessAlert,
  TaskPopper,
  FeedbackScreenshot,
  TaskList,
  CIBHeaderFlow,
  ResetDialog,
  OtpDialog,
  CIBForm,
  SecureInput,
  FilterableSelect,
  IconButton,
  MultisortModal,
  SidebarsFlow,
  SmartSearch,
  SupportModal,
  AdminAuthorizations,
  AdminAuthorizationsTable,
  AdminGroups,
  AdminUsers,
  AuthorizationsNavBar,
  CreateGroup,
  CreateUser,
  ProfileGroup,
  ProfilePreferencesTab,
  ProfileUser,
  UsersManagement,
  DeploymentList,
  PagedScrollableContent,
  DeploymentsView,
  ResourcesNavBar,
  FilterModal,
  FilterNavBar,
  FilterNavCollapsed,
  ProcessView,
  AddVariableModal,
  AddVariableModalUI,
  EditVariableModal,
  DeleteVariableModal,
  BpmnViewer,
  InstancesTable,
  ProcessInstancesView,
  ProcessAdvanced,
  ProcessCard,
  ProcessDetailsSidebar,
  StartProcessList,
  StartProcessView,
  ProcessList,
  ProcessListView,
  ProcessTable,
  ProcessInstanceView,
  ProcessDefinitionView,
  DeleteProcessDefinitionModal,
  ConfirmActionOnProcessInstanceModal,
  ProcessesDashboardView,
  PieChart,
  DeploymentItem,
  StartProcess,
  TaskAssignationModal,
  VariablesTable,
  IncidentsTable,
  UserTasksTable,
  ExternalTasksTable,
  RenderTemplate,
  AdvancedSearchModal,
  TaskContent,
  TaskView,
  TasksContent,
  TasksNavBar,
  TasksView,
  HighlightedText,
  TaskService,
  HistoryService,
  ProcessService,
  BatchService,
  DecisionService,
  getServicesBasePath,
  setServicesBasePath,
  IncidentService,
  HumanTasksView,
  DecisionView,
  DecisionList,
  DecisionListView,
  DecisionInstance,
  DecisionDefinitionVersion,
  StartDeployedForm,
  DeployedForm,
  SystemDiagnostics,
  ExecutionMetrics,
  RuntimeBatches,
  HistoricBatches,
  BatchDetails,
  DecisionDefinitionDetails,
  DecisionVersionListSidebar,
  DmnViewer,
  AboutModal,
  TemplateBase,
  StartView,
  LoginView,
  InitialSetup,
  GenericTabs,
  CopyableActionButton,
  InfoService,
  AuthService,
  SystemService,
  SetupService,
  DeploymentService,
  i18n,
  setLanguage,
  loadTranslations,
  translationSources,
  TranslationsDownload,
  StackTraceModal,
  RetryModal,
  JobLogModal,
  FailedJobs,
  LoginForm,
  ProcessDefinitionDetails,
  ProcessInstanceTabs,
  ProcessInstancesTabs,
  AnnotationModal,
  JobDefinitionPriorityModal,
  JobDefinitionStateModal,
  CalledProcessDefinitionsTable,
  CalledProcessInstancesTable,
  JobDefinitionsTable,
  JobsTable,
  JobDueDateModal,

  // mixins
  assigneeMixin,

  // router
  appRoutes,
  createAppRouter,
  authGuard,
  setupGuard,
  permissionsGuard,
  permissionsDeniedGuard,
  permissionsGuardUserAdmin,

  updateAppTitle,
  checkExternalReturn,
  isMobile,
  hasHeader,
  getTheme,
  loadTheme,
  applyTheme,
  handleAxiosError,
  fetchAndStoreProcesses,
  fetchDecisionsIfEmpty,
  setupTaskNotifications,
  parseXMLDocumentation,
  getEnabledShortcuts,
  getShortcutsForModal,
  getGlobalNavigationShortcuts,
  getTaskEventShortcuts,
  checkKeyMatch,

  initEmbeddedForm,
  ScrollableTabsContainer,
  tabUrlMixin,

  // Configuration utilities
  applyConfigDefaults,

  // Constants
  ENGINE_STORAGE_KEY
}

export * from '@/cib/common-frontend'
