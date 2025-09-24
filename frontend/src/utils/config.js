/*
 * Configuration utility with default values
 * Provides fallback configuration for inherited projects
 */

// Default configuration values
const DEFAULT_CONFIG = {
  supportedLanguages: ["en"],
  theme: "generic",
  taskListTime: "30000",
  warnOnDueExpirationIn: 48,
  authorizationEnabled: false,
  permissions: {
    tasklist: { application: ["ALL"] },
    cockpit: { application: ["ALL"] },
    displayTasks: { task: ["READ", "UPDATE"] },
    displayFilter: { filter: ["READ"] },
    editFilter: { filter: ["UPDATE"] },
    deleteFilter: { filter: ["DELETE"] },
    createFilter: { filter: ["CREATE"] },
    startProcess: {
      processDefinition: ["READ", "CREATE_INSTANCE"],
      processInstance: ["CREATE"]
    },
    displayProcess: { processDefinition: ["READ"] },
    managementProcess: { processDefinition: ["READ"] },
    historyProcess: { processDefinition: ["READ", "READ_HISTORY"] },
    usersManagement: { user: ["ALL"] },
    groupsManagement: { group: ["ALL"] },
    authorizationsManagement: { authorization: ["ALL"] },
    systemManagement: { system: ["ALL"] },
    tenantsManagement: { tenant: ["ALL"] },
    userProfile: { application: ["ALL"] },
    udpateInstanceProcessDefinition: { processDefinition: ["UPDATE_INSTANCE"] },
    updateProcessDefinition : { processDefinition: ["UPDATE"] },
    deleteProcessDefinition: { processDefinition: ["DELETE"] },
    suspendProcessInstance: {processInstance: ["SUSPEND"] },
	  updateProcessInstance: { processInstance: ["UPDATE"] },
    deleteProcessInstance: { processInstance: ["DELETE"] },
    deleteHistoricProcessInstance: { historicProcessInstance: ["DELETE"] },
  },
  taskSorting: {
    fields: ["created", "dueDate", "name", "assignee", "priority"],
    default: {
      sortOrder: "desc",
      sortBy: "created"
    }
  },
  subProcessFolder: "activities/",
  taskFilter: {
    smartSearch: { options: [] },
    advancedSearch: {
      modalEnabled: false,
      filterEnabled: false,
      criteriaKeys: [],
      processVariables: []
    },
    tasksNumber: {
      enabled: false,
      interval: 0
    }
  },
  dashboard: {
    autoUpdate: {
      enabled: true,
      interval: 60000
    }
  },
  processes: {
    autoUpdate: {
      enabled: false,
      interval: 0
    }
  },
  layout: {
    showPopoverHowToAssign: true,
    showBusinessKey: true,
    showTaskDetailsSidebar: false,
    showFilterDueDate: true,
    showFilterReminderDate: true,
    showFeedbackButton: false,
    showTaskListManualRefresh: true,
    disableCandidateUsers: false,
    showProcessName: true,
    showSupportInfo: false,
    showInfoAndHelp: true,
    showChat: false,
    showUserSettings: true,
    showStatusBar: true
  },
  admin: {
    resourcesTypes: {
      "0": { "id": "0", "key": "application", "permissions": ["ACCESS"] },
      "4": { "id": "4", "key": "authorization", "permissions": ["READ","UPDATE","CREATE","DELETE"] },
      "13": { "id": "13", "key": "batch", "permissions": [
        "READ","UDPATE","CREATE","DELETE","READ_HISTORY","DELETE_HISTORY","CREATE_BATCH_MIGRATE_PROCESS_INSTANCES",
        "CREATE_BATCH_MODIFY_PROCESS_INSTANCES","CREATE_BATCH_RESTART_PROCESS_INSTANCES","CREATE_BATCH_DELETE_RUNNING_PROCESS_INSTANCES",
        "CREATE_BATCH_DELETE_FINISHED_PROCESS_INSTANCES","CREATE_BATCH_DELETE_DECISION_INSTANCES","CREATE_BATCH_SET_JOB_RETRIES",
        "CREATE_BATCH_SET_REMOVAL_TIME","CREATE_BATCH_SET_EXTERNAL_TASK_RETRIES",
        "CREATE_BATCH_UPDATE_PROCESS_INSTANCES_SUSPEND","CREATE_BATCH_SET_VARIABLES"
      ] },
      "10": { "id": "10", "key": "decisionDefinition", "permissions": ["READ","UPDATE","CREATE_INSTANCE","READ_HISTORY","DELETE_HISTORY"] },
      "14": { "id": "14", "key": "decisionRequirements", "permissions": ["READ"] },
      "9": { "id": "9", "key": "deployment", "permissions": ["CREATE","READ","DELETE"] },
      "5": { "id": "5", "key": "filter", "permissions": ["READ","UPDATE","CREATE","DELETE"] },
      "2": { "id": "2", "key": "group", "permissions": ["READ","UPDATE","CREATE","DELETE"] },
      "3": { "id": "3", "key": "groupMembership", "permissions": ["CREATE","DELETE"] },
      "20": { "id": "20", "key": "historicProcessInstance", "permissions": ["READ"] },
      "19": { "id": "19", "key": "historicTask", "permissions": ["READ","READ_VARIABLE"] },
      "17": { "id": "17", "key": "userOperationLogCategory", "permissions": ["READ","DELETE","UPDATE"] },
      "6": { "id": "6", "key": "processDefinition", "permissions": [
        "READ","UPDATE","DELETE","SUSPEND","CREATE_INSTANCE","READ_INSTANCE","UPDATE_INSTANCE",
        "RETRY_JOB","SUSPEND_INSTANCE","DELETE_INSTANCE","MIGRATE_INSTANCE","READ_TASK",
        "UPDATE_TASK","TASK_ASSIGN","TASK_WORK","READ_TASK_VARIABLE","READ_HISTORY","READ_HISTORY_VARIABLE",
        "DELETE_HISTORY","READ_INSTANCE_VARIABLE","UPDATE_INSTANCE_VARIABLE","UPDATE_TASK_VARIABLE","UPDATE_HISTORY"
      ] },
      "7": { "id": "7", "key": "processInstance", "permissions": [
        "READ","UPDATE","DELETE","SUSPEND","RETRY_JOB","CREATE","READ_TASK","UPDATE_TASK",
        "TASK_ASSIGN","TASK_WORK","READ_TASK_VARIABLE","UPDATE_TASK_VARIABLE","READ_VARIABLE",
        "UPDATE_VARIABLE","READ_HISTORY","READ_HISTORY_VARIABLE","DELETE_HISTORY"
      ] },
      "11": { "id": "11", "key": "report", "permissions": ["READ"] },
      "8": { "id": "8", "key": "task", "permissions": [
        "READ","UPDATE","DELETE","CREATE","TASK_ASSIGN","TASK_WORK","READ_VARIABLE","UPDATE_VARIABLE"
      ] },
      "15": { "id": "15", "key": "optimize", "permissions": ["READ"] },
      "16": { "id": "16", "key": "operationLogCategory", "permissions": ["READ","DELETE"] },
      "12": { "id": "12", "key": "system", "permissions": ["READ","UPDATE","DELETE"] },
      "18": { "id": "18", "key": "property", "permissions": ["READ","UPDATE","DELETE"] },
      "1": { "id": "1", "key": "user", "permissions": ["READ","UPDATE","CREATE","DELETE"] }
    }
  },
  notifications: {
    tasks: {
      enabled: false,
      interval: 30000
    }
  },
  filters: [
    { "key": "processInstanceId", "text": "processInstanceId", "type": "text", "group": "processInstanceGroup" },
    { "key": "processInstanceBusinessKey", "text": "processInstanceBusinessKey", "type": "text", "group": "processInstanceGroup" },
    { "key": "processDefinitionId", "text": "processDefinitionId", "type": "text", "group": "processDefinitionGroup" },
    { "key": "processDefinitionKey", "text": "processDefinitionKey", "type": "text", "group": "processDefinitionGroup" },
    { "key": "assignee", "text": "assignee", "type": "text", "group": "usersGroup" },
    { "key": "candidateGroup", "text": "candidateGroup", "type": "text", "group": "usersGroup" },
    { "key": "candidateUser", "text": "candidateUser", "type": "text", "group": "usersGroup" },
    { "key": "taskDefinitionKey", "text": "taskDefinitionKey", "type": "text", "group": "taskGroup" },
    { "key": "name", "text": "name", "type": "text", "group": "taskGroup" },
    { "key": "priority", "text": "priority", "type": "text", "group": "taskGroup" },
    { "key": "createdBefore", "text": "createdBefore", "type": "text", "group": "datesGroup" },
    { "key": "dueBefore", "text": "dueBefore", "type": "text", "group": "datesGroup" },
    { "key": "dueAfter", "text": "dueAfter", "type": "text", "group": "datesGroup" }
  ]
}

/**
 * Deep merge two objects, with source taking precedence
 * @param {Object} target - The target object
 * @param {Object} source - The source object
 * @returns {Object} - Merged object
 */
function deepMerge(target, source) {
  const result = { ...target }

  for (const key in source) {
    if (source[key] && typeof source[key] === 'object' && !Array.isArray(source[key])) {
      result[key] = deepMerge(target[key] || {}, source[key])
    } else {
      result[key] = source[key]
    }
  }

  return result
}

/**
 * Applies default configuration values to the provided config
 * @param {Object} config - The configuration object from config.json
 * @returns {Object} - Configuration with defaults applied
 */
export function applyConfigDefaults(config) {
  return deepMerge(DEFAULT_CONFIG, config || {})
}

