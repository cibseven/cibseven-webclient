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

import { axios } from '@/globals.js'
import appConfig from '@/appConfig.js'
import { permissionsMixin } from '@/permissions.js'
import processesVariablesMixin from '@/components/process/mixins/processesVariablesMixin.js'
import processesMixin from '@/components/process/mixins/processesMixin.js'
import resizerMixin from '@/components/process/mixins/resizerMixin.js'
import store from '@/store'
import usersMixin from '@/mixins/usersMixin.js'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import { debounce } from '@/utils/debounce.js'
import { HoverStyle } from '@/components/common-components/directives.js'
import CibSeven from '@/components/CibSeven.vue'
import FlowTable from '@/components/common-components/FlowTable.vue'
import ContentBlock from '@/components/common-components/ContentBlock.vue'
import ErrorDialog from '@/components/common-components/ErrorDialog.vue'
import AboutModal from '@/components/modals/AboutModal.vue'
import ConfirmDialog from '@/components/common-components/ConfirmDialog.vue'
import FeedbackModal from '@/components/modals/FeedbackModal.vue'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'
import TaskPopper from '@/components/common-components/TaskPopper.vue'
import FeedbackScreenshot from '@/components/modals/FeedbackScreenshot.vue'
import TaskList from '@/components/common-components/TaskList.vue'
import CIBHeaderFlow from '@/components/common-components/CIBHeaderFlow.vue'
import ResetDialog from '@/components/login/ResetDialog.vue'
import OtpDialog from '@/components/login/OtpDialog.vue'
import CIBForm from '@/components/common-components/CIBForm.vue'
import SecureInput from '@/components/login/SecureInput.vue'
import FilterableSelect from '@/components/task/filter/FilterableSelect.vue'
import IconButton from '@/components/render-template/IconButton.vue'
import MultisortModal from '@/components/process/modals/MultisortModal.vue'
import SidebarsFlow from '@/components/common-components/SidebarsFlow.vue'
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
import RenderTemplate from '@/components/render-template/RenderTemplate.vue'
import AdvancedSearchModal from '@/components/task/AdvancedSearchModal.vue'
import TaskContent from '@/components/task/TaskContent.vue'
import TaskView from '@/components/task/TaskView.vue'
import TasksContent from '@/components/task/TasksContent.vue'
import TasksNavBar from '@/components/task/TasksNavBar.vue'
import TasksView from '@/components/task/TasksView.vue'
import HighlightedText from '@/components/common-components/HighlightedText.vue'
import HumanTasksView from '@/components/task/HumanTasksView.vue'
import DecisionView from '@/components/decision/DecisionView.vue'
import DecisionList from '@/components/decisions/list/DecisionList.vue'
import DecisionListView from '@/components/decisions/list/DecisionListView.vue'
import DecisionDefinitionVersion from '@/components/decision/DecisionDefinitionVersion.vue'
import TenantsView from '@/components/tenants/TenantsView.vue'
import EditTenant from './components/tenants/EditTenant.vue';
import CreateTenant from './components/tenants/CreateTenant.vue';
import BatchesView from '@/components/batches/BatchesView.vue'
import RuntimeBatches from '@/components/batches/tables/RuntimeBatches.vue'
import HistoricBatches from '@/components/batches/tables/HistoricBatches.vue'
import BatchDetails from '@/components/batches/tables/BatchDetails.vue'
import SystemView from '@/components/system/SystemView.vue'
import SystemDiagnostics from '@/components/system/SystemDiagnostics.vue'
import ExecutionMetrics from '@/components/system/ExecutionMetrics.vue'
import ShortcutsModal from '@/components/modals/ShortcutsModal.vue'
import ShortcutsTable from '@/components/modals/ShortcutsTable.vue'
import { TaskService, HistoryService, ProcessService } from '@/services.js';
import DeployedForm from '@/components/forms/DeployedForm.vue'
import StartDeployedForm from '@/components/forms/StartDeployedForm.vue'
import DecisionDefinitionDetails from '@/components/decision/DecisionDefinitionDetails.vue'
import DecisionInstance from '@/components/decision/DecisionInstance.vue'
import DecisionVersionListSidebar from '@/components/decision/DecisionVersionListSidebar.vue'
import DmnViewer from '@/components/decision/DmnViewer.vue'
import TemplateBase from '@/components/forms/TemplateBase.vue'
import StartView from '@/components/start/StartView.vue'
import LoginView from '@/components/login/LoginView.vue'

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
  app.component('profile-user', ProfileUser)
  app.component('users-management', UsersManagement)
  app.component('deployment-list', DeploymentList)
  app.component('deployments-view', DeploymentsView)
  app.component('resources-nav-bar', ResourcesNavBar)
  app.component('filter-modal', FilterModal)
  app.component('filter-nav-bar', FilterNavBar)
  app.component('filter-nav-collapsed', FilterNavCollapsed)
  app.component('process-view', ProcessView)
  app.component('add-variable-modal', AddVariableModal)
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
  app.component('decision-', DecisionDefinitionVersion)
  app.component('tenants-view', TenantsView)
  app.component('edit-tenant', EditTenant)
  app.component('create-tenant', CreateTenant)
  app.component('batches-view', BatchesView)
  app.component('system-view', SystemView)
  app.component('deployed-form', DeployedForm)
  app.component('decision-definition-details', DecisionDefinitionDetails)
  app.component('decision-instance', DecisionInstance)
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
  axios,
  appConfig,
  permissionsMixin,
  store,
  usersMixin,
  processesVariablesMixin,
  processesMixin,
  resizerMixin,
  copyToClipboardMixin,
  debounce,
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
  ProfileUser,
  UsersManagement,
  DeploymentList,
  DeploymentsView,
  ResourcesNavBar,
  FilterModal,
  FilterNavBar,
  FilterNavCollapsed,
  ProcessView,
  AddVariableModal,
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
  HumanTasksView,
  DecisionView,
  DecisionList,
  DecisionListView,
  DecisionDefinitionVersion,
  StartDeployedForm,
  DeployedForm,
  SystemDiagnostics,
  ExecutionMetrics,
  RuntimeBatches,
  HistoricBatches,
  BatchDetails,
  DecisionDefinitionDetails,
  DecisionInstance,
  DecisionVersionListSidebar,
  DmnViewer,
  AboutModal,
  TemplateBase,
  StartView,
  LoginView
}
