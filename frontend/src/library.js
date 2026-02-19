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
import '@cib/common-frontend/dist/style.css';

export {  axios, moment, createUUID } from '@/globals.js'
export { permissionsMixin } from '@/permissions.js'
export { default as registerComponents } from './register.js'
export { default as store, modules as storeModules } from '@/store'
export { debounce } from '@/utils/debounce.js'
export { formatDate, formatDateForTooltips, formatDuration } from '@/utils/dates.js'
export { initEmbeddedForm } from './embedded-form/embedded-form.js'
export { i18n, setLanguage, loadTranslations, translationSources } from './i18n'
export { appRoutes,
  createAppRouter,
  authGuard,
  setupGuard,
  permissionsGuard,
  permissionsDeniedGuard,
  permissionsGuardUserAdmin } from './router.js'
export { updateAppTitle, checkExternalReturn, isMobile, hasHeader, getTheme, loadTheme, applyTheme, handleAxiosError, fetchAndStoreProcesses, fetchDecisionsIfEmpty, setupTaskNotifications } from './utils/init'
export { parseXMLDocumentation } from './utils/parser.js'
export { applyConfigDefaults } from './utils/config.js'
export { ENGINE_STORAGE_KEY, ENGINE_TOKENS_STORAGE_KEY } from './constants.js'
export { 
  getEngineTokens, 
  getTokenForEngine, 
  storeTokenForEngine, 
  removeTokenForEngine, 
  clearAllEngineTokens, 
  hasTokenForEngine, 
  restoreTokenForEngine 
} from './utils/engineTokens.js'
export { TaskService, FilterService, ProcessService, VariableInstanceService, HistoricVariableInstanceService, AdminService, JobService, JobDefinitionService, SystemService,
  HistoryService, IncidentService, AuthService, InfoService, FormsService, TemplateService, DecisionService,
  AnalyticsService, BatchService, TenantService, ExternalTaskService, DeploymentService, EngineService, SetupService, getServicesBasePath, setServicesBasePath, createDocumentEndpointUrl } from '@/services.js';
export { getEnabledShortcuts, getShortcutsForModal,
  getGlobalNavigationShortcuts, getTaskEventShortcuts, checkKeyMatch } from './utils/shortcuts.js'
export { createProvideObject } from '@/utils/provide.js'

// mixins
export { default as processesVariablesMixin } from '@/components/process/mixins/processesVariablesMixin.js'
export { default as resizerMixin } from '@/components/process/mixins/resizerMixin.js'
export { default as tabUrlMixin } from '@/components/process/mixins/tabUrlMixin.js'
export { default as usersMixin } from '@/mixins/usersMixin.js'
export { default as copyToClipboardMixin } from '@/mixins/copyToClipboardMixin.js'
export { default as assigneeMixin } from '@/mixins/assigneeMixin.js'
export { default as decisionInstanceMixin } from '@/mixins/decisionInstanceMixin.js'

// components
export { default as CibSeven } from '@/components/CibSeven.vue'
export { default as CellActionButton } from '@/components/common-components/CellActionButton.vue'
export { default as AboutModal } from '@/components/modals/AboutModal.vue'
export { default as FeedbackModal } from '@/components/modals/FeedbackModal.vue'
export { default as FeedbackScreenshot } from '@/components/modals/FeedbackScreenshot.vue'
export { default as TaskList } from '@/components/common-components/TaskList.vue'
export { default as CIBHeaderFlow } from '@/components/common-components/CIBHeaderFlow.vue'
export { default as ResetDialog } from '@/components/login/ResetDialog.vue'
export { default as OtpDialog } from '@/components/login/OtpDialog.vue'
export { default as SecureInput } from '@/components/login/SecureInput.vue'
export { default as FilterableSelect } from '@/components/task/filter/FilterableSelect.vue'
export { default as IconButton } from '@/components/forms/IconButton.vue'
export { default as MultisortModal } from '@/components/process/modals/MultisortModal.vue'
export { default as SmartSearch } from '@/components/task/SmartSearch.vue'
export { default as SupportModal } from '@/components/modals/SupportModal.vue'
export { default as AdminAuthorizations } from '@/components/admin/AdminAuthorizations.vue'
export { default as AdminAuthorizationsTable } from '@/components/admin/AdminAuthorizationsTable.vue'
export { default as AdminGroups } from '@/components/admin/AdminGroups.vue'
export { default as AdminUsers } from '@/components/admin/AdminUsers.vue'
export { default as AuthorizationsNavBar } from '@/components/admin/AuthorizationsNavBar.vue'
export { default as CreateGroup } from '@/components/admin/CreateGroup.vue'
export { default as CreateUser } from '@/components/admin/CreateUser.vue'
export { default as ProfileGroup } from '@/components/admin/ProfileGroup.vue'
export { default as ProfilePreferencesTab } from '@/components/admin/ProfilePreferencesTab.vue'
export { default as ProfileUser } from '@/components/admin/ProfileUser.vue'
export { default as UsersManagement } from '@/components/admin/UsersManagement.vue'
export { default as DeploymentList } from '@/components/deployment/DeploymentList.vue'
export { default as DeploymentsView } from '@/components/deployment/DeploymentsView.vue'
export { default as ResourcesNavBar } from '@/components/deployment/ResourcesNavBar.vue'
export { default as FilterModal } from '@/components/task/filter/FilterModal.vue'
export { default as FilterNavBar } from '@/components/task/filter/FilterNavBar.vue'
export { default as FilterNavCollapsed } from '@/components/task/filter/FilterNavCollapsed.vue'
export { default as ProcessView } from '@/components/process/ProcessView.vue'
export { default as AddVariableModal } from '@/components/process/modals/AddVariableModal.vue'
export { default as AddVariableModalUI } from '@/components/process/modals/AddVariableModalUI.vue'
export { default as EditVariableModal } from '@/components/process/modals/EditVariableModal.vue'
export { default as DeleteVariableModal } from '@/components/process/modals/DeleteVariableModal.vue'
export { default as BpmnViewer } from '@/components/process/BpmnViewer.vue'
export { default as InstancesTable } from '@/components/process/tables/InstancesTable.vue'
export { default as ProcessInstancesView } from '@/components/process/ProcessInstancesView.vue'
export { default as ProcessCard } from '@/components/start-process/ProcessCard.vue'
export { default as ProcessDetailsSidebar } from '@/components/process/ProcessDetailsSidebar.vue'
export { default as StartProcessList } from '@/components/start-process/StartProcessList.vue'
export { default as StartProcessView } from '@/components/start-process/StartProcessView.vue'
export { default as ProcessList } from '@/components/processes/list/ProcessList.vue'
export { default as ProcessListView } from '@/components/processes/list/ProcessListView.vue'
export { default as ProcessTable } from '@/components/start-process/ProcessTable.vue'
export { default as ProcessInstanceView } from '@/components/process/ProcessInstanceView.vue'
export { default as ProcessDefinitionView } from '@/components/process/ProcessDefinitionView.vue'
export { default as ProcessesDashboardView } from '@/components/processes/dashboard/ProcessesDashboardView.vue'
export { default as PieChart } from '@/components/processes/dashboard/PieChart.vue'
export { default as DeploymentItem } from '@/components/processes/dashboard/DeploymentItem.vue'
export { default as DeleteProcessDefinitionModal } from '@/components/process/modals/DeleteProcessDefinitionModal.vue'
export { default as ConfirmActionOnProcessInstanceModal } from '@/components/process/modals/ConfirmActionOnProcessInstanceModal.vue'
export { default as StartProcess } from '@/components/start-process/StartProcess.vue'
export { default as TaskAssignationModal } from '@/components/process/modals/TaskAssignationModal.vue'
export { default as VariablesTable } from '@/components/process/tables/VariablesTable.vue'
export { default as IncidentsTable } from '@/components/process/tables/IncidentsTable.vue'
export { default as UserTasksTable } from '@/components/process/tables/UserTasksTable.vue'
export { default as ExternalTasksTable } from '@/components/process/tables/ExternalTasksTable.vue'
export { default as RenderTemplate } from '@/components/render-template/RenderTemplate.vue'
export { default as AdvancedSearchModal } from '@/components/task/AdvancedSearchModal.vue'
export { default as TaskContent } from '@/components/task/TaskContent.vue'
export { default as TaskView } from '@/components/task/TaskView.vue'
export { default as TasksContent } from '@/components/task/TasksContent.vue'
export { default as TasksNavBar } from '@/components/task/TasksNavBar.vue'
export { default as TasksView } from '@/components/task/TasksView.vue'
export { default as HumanTasksView } from '@/components/task/HumanTasksView.vue'
export { default as DecisionView } from '@/components/decision/DecisionView.vue'
export { default as DecisionList } from '@/components/decisions/list/DecisionList.vue'
export { default as DecisionListView } from '@/components/decisions/list/DecisionListView.vue'
export { default as DecisionInstance } from './components/decision/DecisionInstance.vue'
export { default as DecisionDefinitionVersion } from '@/components/decision/DecisionDefinitionVersion.vue'
export { default as TenantsView } from '@/components/tenants/TenantsView.vue'
export { default as EditTenant } from './components/tenants/EditTenant.vue'
export { default as CreateTenant } from './components/tenants/CreateTenant.vue'
export { default as BatchesView } from '@/components/batches/BatchesView.vue'
export { default as RuntimeBatches } from '@/components/batches/tables/RuntimeBatches.vue'
export { default as HistoricBatches } from '@/components/batches/tables/HistoricBatches.vue'
export { default as BatchDetails } from '@/components/batches/tables/BatchDetails.vue'
export { default as SystemView, SystemSidebarItems } from '@/components/system/SystemView.vue'
export { default as SystemDiagnostics } from '@/components/system/SystemDiagnostics.vue'
export { default as ExecutionMetrics } from '@/components/system/ExecutionMetrics.vue'
export { default as ShortcutsModal } from '@/components/modals/ShortcutsModal.vue'
export { default as ShortcutsTable } from '@/components/modals/ShortcutsTable.vue'
export { default as DeployedForm } from '@/components/forms/DeployedForm.vue'
export { default as StartDeployedForm } from '@/components/forms/StartDeployedForm.vue'
export { default as DecisionDefinitionDetails } from '@/components/decision/DecisionDefinitionDetails.vue'
export { default as DecisionVersionListSidebar } from '@/components/decision/DecisionVersionListSidebar.vue'
export { default as DmnViewer } from '@/components/decision/DmnViewer.vue'
export { default as TemplateBase } from '@/components/forms/TemplateBase.vue'
export { default as StartView } from '@/components/start/StartView.vue'
export { default as StartViewItem } from '@/components/start/StartViewItem.vue'
export { default as LoginView } from '@/components/login/LoginView.vue'
export { default as InitialSetup } from '@/components/setup/InitialSetup.vue'
export { default as StackTraceModal } from '@/components/process/modals/StackTraceModal.vue'
export { default as RetryModal } from '@/components/process/modals/RetryModal.vue'
export { default as ScrollableTabsContainer } from '@/components/common-components/ScrollableTabsContainer.vue'
export { default as JobLogModal } from '@/components/batches/modals/JobLogModal.vue'
export { default as FailedJobs } from '@/components/batches/tables/FailedJobs.vue'
export { default as LoginForm } from '@/components/login/LoginForm.vue'
export { default as ProcessDefinitionDetails } from '@/components/process/ProcessDefinitionDetails.vue'
export { default as ProcessInstanceTabs } from '@/components/process/ProcessInstanceTabs.vue'
export { default as ProcessInstancesTabs } from '@/components/process/ProcessInstancesTabs.vue'
export { default as AnnotationModal } from '@/components/process/modals/AnnotationModal.vue'
export { default as JobDefinitionPriorityModal } from '@/components/process/modals/JobDefinitionPriorityModal.vue'
export { default as JobDefinitionStateModal } from '@/components/process/modals/JobDefinitionStateModal.vue'
export { default as CalledProcessDefinitionsTable } from '@/components/process/tables/CalledProcessDefinitionsTable.vue'
export { default as CalledProcessInstancesTable } from '@/components/process/tables/CalledProcessInstancesTable.vue'
export { default as JobDefinitionsTable } from '@/components/process/tables/JobDefinitionsTable.vue'
export { default as JobsTable } from '@/components/process/tables/JobsTable.vue'
export { default as JobDueDateModal } from '@/components/process/modals/JobDueDateModal.vue'
export { default as ViewerFrame } from '@/components/common-components/ViewerFrame.vue'
export { default as RemovableBadge } from '@/components/common-components/RemovableBadge.vue'

// re-export common frontend library
export * from '@cib/common-frontend'
