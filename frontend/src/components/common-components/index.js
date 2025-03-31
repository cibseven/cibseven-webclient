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
import { HoverStyle } from '@/components/common-components/directives.js'
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

import processesVariablesMixin from '@/components/process/mixins/processesVariablesMixin.js'
import processesMixin from '@/components/process/mixins/processesMixin.js'
import resizerMixin from '@/components/process/mixins/resizerMixin.js'
import AddVariableModal from '@/components/process/modals/AddVariableModal.vue'
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
import StartProcess from '@/components/start-process/StartProcess.vue'
import TaskAssignationModal from '@/components/process/modals/TaskAssignationModal.vue'
import VariablesTable from '@/components/process/tables/VariablesTable.vue'
import IncidentsTable from '@/components/process/tables/IncidentsTable.vue'
import UserTasksTable from '@/components/process/tables/UserTasksTable.vue'


import RenderTemplate from '@/components/render-template/RenderTemplate.vue'

import AdvancedSearchModal from '@/components/task/AdvancedSearchModal.vue'
import Task from '@/components/task/Task.vue'
import TaskContent from '@/components/task/TaskContent.vue'
import TasksContent from '@/components/task/TasksContent.vue'
import TasksNavBar from '@/components/task/TasksNavBar.vue'
import TasksView from '@/components/task/TasksView.vue'

import usersMixin from '@/mixins/usersMixin.js'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import { debounce } from '@/utils/debounce.js'

export {
    FlowTable, ErrorDialog, ConfirmDialog, ProblemReport,
    SuccessAlert, TaskPopper, Clipboard, TaskList, CIBHeaderFlow,
    Sidebars, ResetDialog, OtpDialog, HoverStyle, CIBForm,
    SecureInput, FilterableSelect, SmartSearch, SidebarsFlow,
    IconButton, MultisortModal, StartProcess, BpmnViewer,
    ProcessList, ProcessInstancesView, InstancesTable, StartProcessList,
    ProcessDetailsSidebar, ProcessCard, ProcessAdvanced,
    ProcessTable, VariablesTable, IncidentsTable, UserTasksTable, TaskAssignationModal,
    AddVariableModal, ProcessView, DeploymentsView, ResourcesNavBar,
    DeploymentList, TasksContent, TasksNavBar, Task, AdvancedSearchModal,
    FilterModal, FilterNavBar, FilterNavCollapsed,
    RenderTemplate, AdminUsers, AdminGroups, AdminAuthorizations,
    AdminAuthorizationsTable, AuthorizationsNavBar, CreateUser,
    CreateGroup, ProfileUser, ProfileGroup, SupportModal,
    UsersManagement, processesVariablesMixin, processesMixin,
    resizerMixin, StartProcessView, ProcessListView,
    ProcessInstanceView, ProcessDefinitionView, TaskContent, TasksView,
    usersMixin, copyToClipboardMixin, debounce
}
