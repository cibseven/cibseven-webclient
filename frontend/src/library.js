// Import the CSS to ensure it is bundled with the package
import './assets/main.css';

import CibSeven from '@/components/CibSeven.vue'
import FlowTable from '@/components/common-components/FlowTable.vue'
import ErrorDialog from '@/components/common-components/ErrorDialog.vue'
import ConfirmDialog from '@/components/common-components/ConfirmDialog.vue'
import ProblemReport from '@/components/common-components/ProblemReport.vue'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'
import TaskPopper from '@/components/common-components/TaskPopper.vue'
import Clipboard from '@/components/common-components/Clipboard.vue'
import TaskList from '@/components/common-components/TaskList.vue'
import CIBHeaderFlow from '@/components/common-components/CIBHeaderFlow.vue'
import Sidebars from '@/components/common-components/Sidebars.vue'
import ResetDialog from '@/components/login/ResetDialog.vue'
import OtpDialog from '@/components/login/OtpDialog.vue'
import CIBForm from '@/components/common-components/CIBForm.vue'
import SecureInput from '@/components/login/SecureInput.vue'
import FilterableSelect from '@/components/task/filter/FilterableSelect.vue'
import IconButton from '@/components/render-template/IconButton.vue'
import MultisortModal from '@/components/process/modals/MultisortModal.vue'
import SidebarsFlow from '@/components/common-components/SidebarsFlow.vue'
import SmartSearch from '@/components/task/SmartSearch.vue'
import SupportModal from '@/components/common-components/SupportModal.vue'
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
import BpmnViewer from '@/components/process/BpmnViewer.vue'
import InstancesTable from '@/components/process/InstancesTable.vue'
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

const registerComponents = function(app) {
  app.component('cib-seven', CibSeven)
  app.component('flow-table', FlowTable)
  app.component('error-dialog', ErrorDialog)
  app.component('confirm-dialog', ConfirmDialog)
  app.component('problem-report', ProblemReport)
  app.component('success-alert', SuccessAlert)
  app.component('task-popper', TaskPopper)
  app.component('clipboard', Clipboard)
  app.component('task-list', TaskList)
  app.component('cib-header-flow', CIBHeaderFlow)
  app.component('sidebars', Sidebars)
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
}

export {
  registerComponents,

  CibSeven,
  FlowTable,
  ErrorDialog,
  ConfirmDialog,
  ProblemReport,
  SuccessAlert,
  TaskPopper,
  Clipboard,
  TaskList,
  CIBHeaderFlow,
  Sidebars,
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
  HighlightedText
}
