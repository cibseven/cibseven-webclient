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
 * Central registry of permission requirements for every frontend action.
 *
 * This is the single source of truth for what the engine's
 * AuthorizationCommandChecker enforces for each operation.
 *
 * FORMAT
 * ------
 * Each entry is either:
 *
 *   • A plain object  { resourceType: ['PERM', …] }
 *     ALL resource types listed must grant the permission (conjunctive / AND).
 *     Example: createProcessInstance requires BOTH processDefinition AND processInstance.
 *
 *   • An array of objects  [{ resourceType: ['PERM'] }, …]
 *     At least ONE of the listed alternatives must grant the permission
 *     (disjunctive / OR).
 *     Example: deleteProcessInstance is satisfied by DELETE on processInstance
 *     OR DELETE_INSTANCE on processDefinition.
 *
 * RESOURCE TYPES correspond to the names used in the engine and in
 * config.admin.resourcesTypes (application, task, filter, processDefinition,
 * processInstance, historicProcessInstance, deployment, decisionDefinition,
 * decisionRequirementsDefinition, batch, user, group, authorization, tenant,
 * system).
 */
const ACTION_PERMISSIONS = {

	// ── Application ────────────────────────────────────────────────────────────
	// Engine: checkAuthorization(ACCESS, APPLICATION, 'tasklist')
	tasklist: { application: ['ACCESS'] },
	// Engine: checkAuthorization(ACCESS, APPLICATION, 'cockpit')
	cockpit: { application: ['ACCESS'] },
	// Engine: checkAuthorization(ALL, APPLICATION, *)
	userProfile: { application: ['ALL'] },

	// ── Filters ────────────────────────────────────────────────────────────────
	displayFilter: { filter: ['READ'] },
	createFilter:  { filter: ['CREATE'] },
	editFilter:    { filter: ['UPDATE'] },
	deleteFilter:  { filter: ['DELETE'] },

	// ── Tasks ──────────────────────────────────────────────────────────────────
	// Engine: checkReadTask → READ_TASK on PROCESS_DEFINITION OR READ on TASK
	readTask: [
		{ processDefinition: ['READ_TASK'] },
		{ task: ['READ'] },
	],
	// Used for displaying tasks in the tasklist (simplified AND-based check)
	displayTasks: { task: ['READ', 'UPDATE'] },
	// Engine: checkTaskAssign →
	//   (TASK_ASSIGN on TASK OR UPDATE on TASK)
	//   OR (TASK_ASSIGN on PROCESS_DEFINITION OR UPDATE_TASK on PROCESS_DEFINITION)
	taskAssign: [
		{ task: ['TASK_ASSIGN'] },
		{ processDefinition: ['TASK_ASSIGN'] },
		{ task: ['UPDATE'] },
		{ processDefinition: ['UPDATE_TASK'] },
	],
	// Engine: checkTaskWork →
	//   (TASK_WORK on TASK OR UPDATE on TASK)
	//   OR (TASK_WORK on PROCESS_DEFINITION OR UPDATE_TASK on PROCESS_DEFINITION)
	taskWork: [
		{ task: ['TASK_WORK'] },
		{ processDefinition: ['TASK_WORK'] },
		{ task: ['UPDATE'] },
		{ processDefinition: ['UPDATE_TASK'] },
	],
	// Engine: checkUpdateTaskVariable →
	//   UPDATE_VARIABLE on TASK OR UPDATE_TASK_VARIABLE on PROCESS_DEFINITION
	//   OR UPDATE on TASK OR UPDATE_TASK on PROCESS_DEFINITION
	updateTaskVariable: [
		{ task: ['UPDATE_VARIABLE'] },
		{ processDefinition: ['UPDATE_TASK_VARIABLE'] },
		{ task: ['UPDATE'] },
		{ processDefinition: ['UPDATE_TASK'] },
	],

	// ── Process Definitions ────────────────────────────────────────────────────
	// Engine: checkReadProcessDefinition → READ on PROCESS_DEFINITION
	displayProcess:    { processDefinition: ['READ'] },
	readProcessDefinition: { processDefinition: ['READ'] },
	// Engine: read for cockpit / management views
	managementProcess: { processDefinition: ['READ'] },
	// Engine: checkReadHistoryProcessDefinition → READ_HISTORY on PROCESS_DEFINITION
	historyProcess: { processDefinition: ['READ', 'READ_HISTORY'] },
	readProcessDefinitionHistory: { processDefinition: ['READ_HISTORY'] },
	// Engine: checkUpdateProcessDefinitionByKey → UPDATE on PROCESS_DEFINITION
	updateProcessDefinition: { processDefinition: ['UPDATE'] },
	// Engine: checkUpdateProcessDefinitionSuspensionStateByKey →
	//   SUSPEND on PROCESS_DEFINITION OR UPDATE on PROCESS_DEFINITION
	suspendProcessDefinition: [
		{ processDefinition: ['SUSPEND'] },
		{ processDefinition: ['UPDATE'] },
	],
	// Engine: checkDeleteProcessDefinitionByKey → DELETE on PROCESS_DEFINITION
	deleteProcessDefinition: { processDefinition: ['DELETE'] },
	// Engine: for job-definition overrides on process definitions
	updateInstanceProcessDefinition: { processDefinition: ['UPDATE_INSTANCE'] },

	// ── Process Instances ──────────────────────────────────────────────────────
	// Engine: checkCreateProcessInstance →
	//   CREATE on PROCESS_INSTANCE AND CREATE_INSTANCE on PROCESS_DEFINITION
	startProcess: {
		processDefinition: ['READ', 'CREATE_INSTANCE'],
		processInstance:   ['CREATE'],
	},
	createProcessInstance: {
		processDefinition: ['CREATE_INSTANCE'],
		processInstance:   ['CREATE'],
	},
	// Engine: checkReadProcessInstance →
	//   READ on PROCESS_INSTANCE OR READ_INSTANCE on PROCESS_DEFINITION
	readProcessInstance: [
		{ processInstance:   ['READ'] },
		{ processDefinition: ['READ_INSTANCE'] },
	],
	// Engine: checkUpdateProcessInstance →
	//   UPDATE on PROCESS_INSTANCE OR UPDATE_INSTANCE on PROCESS_DEFINITION
	updateProcessInstance: [
		{ processInstance:   ['UPDATE'] },
		{ processDefinition: ['UPDATE_INSTANCE'] },
	],
	// Engine: checkUpdateProcessInstanceSuspensionState →
	//   SUSPEND on PROCESS_INSTANCE OR SUSPEND_INSTANCE on PROCESS_DEFINITION
	//   OR UPDATE on PROCESS_INSTANCE OR UPDATE_INSTANCE on PROCESS_DEFINITION
	suspendProcessInstance: [
		{ processInstance:   ['SUSPEND'] },
		{ processDefinition: ['SUSPEND_INSTANCE'] },
		{ processInstance:   ['UPDATE'] },
		{ processDefinition: ['UPDATE_INSTANCE'] },
	],
	// Engine: checkDeleteProcessInstance →
	//   DELETE on PROCESS_INSTANCE OR DELETE_INSTANCE on PROCESS_DEFINITION
	deleteProcessInstance: [
		{ processInstance:   ['DELETE'] },
		{ processDefinition: ['DELETE_INSTANCE'] },
	],
	// Engine: checkUpdateProcessInstanceVariables →
	//   UPDATE_VARIABLE on PROCESS_INSTANCE
	//   OR UPDATE_INSTANCE_VARIABLE on PROCESS_DEFINITION
	//   OR UPDATE on PROCESS_INSTANCE
	//   OR UPDATE_INSTANCE on PROCESS_DEFINITION
	updateProcessInstanceVariable: [
		{ processInstance:   ['UPDATE_VARIABLE'] },
		{ processDefinition: ['UPDATE_INSTANCE_VARIABLE'] },
		{ processInstance:   ['UPDATE'] },
		{ processDefinition: ['UPDATE_INSTANCE'] },
	],
	// Engine: checkReadProcessInstanceVariable →
	//   READ_INSTANCE_VARIABLE on PROCESS_DEFINITION
	//   OR READ on PROCESS_INSTANCE OR READ_INSTANCE on PROCESS_DEFINITION
	readProcessInstanceVariable: [
		{ processDefinition: ['READ_INSTANCE_VARIABLE'] },
		{ processInstance:   ['READ'] },
		{ processDefinition: ['READ_INSTANCE'] },
	],
	// Engine: checkUpdateRetriesJob (job on process instance) →
	//   RETRY_JOB on PROCESS_INSTANCE OR RETRY_JOB on PROCESS_DEFINITION
	//   OR UPDATE on PROCESS_INSTANCE OR UPDATE_INSTANCE on PROCESS_DEFINITION
	retryJobProcessInstance: [
		{ processInstance:   ['RETRY_JOB'] },
		{ processDefinition: ['RETRY_JOB'] },
		{ processInstance:   ['UPDATE'] },
		{ processDefinition: ['UPDATE_INSTANCE'] },
	],

	// ── Historic Process Instances ─────────────────────────────────────────────
	// Engine: checkDeleteHistoricProcessInstance →
	//   DELETE_HISTORY on PROCESS_DEFINITION
	deleteHistoricProcessInstance: { processDefinition: ['DELETE_HISTORY'] },

	// ── Deployments ────────────────────────────────────────────────────────────
	// Engine: checkCreateDeployment → CREATE on DEPLOYMENT
	createDeployment: { deployment: ['CREATE'] },
	// Engine: checkReadDeployment → READ on DEPLOYMENT
	readDeployment:   { deployment: ['READ'] },
	// Engine: checkDeleteDeployment → DELETE on DEPLOYMENT
	deleteDeployment: { deployment: ['DELETE'] },

	// ── Decision Definitions ───────────────────────────────────────────────────
	// Engine: checkReadDecisionDefinition → READ on DECISION_DEFINITION
	readDecisionDefinition: { decisionDefinition: ['READ'] },
	// Engine: checkUpdateDecisionDefinition → UPDATE on DECISION_DEFINITION
	updateDecisionDefinition: { decisionDefinition: ['UPDATE'] },
	// Engine: checkEvaluateDecision → CREATE_INSTANCE on DECISION_DEFINITION
	evaluateDecision: { decisionDefinition: ['CREATE_INSTANCE'] },
	// Engine: checkReadDecisionRequirementsDefinition → READ on DECISION_REQUIREMENTS_DEFINITION
	readDecisionRequirementsDefinition: { decisionRequirementsDefinition: ['READ'] },

	// ── Batches ────────────────────────────────────────────────────────────────
	// Engine: checkCreateBatch → CREATE on BATCH
	createBatch:          { batch: ['CREATE'] },
	// Engine: checkDeleteBatch → DELETE on BATCH
	deleteBatch:          { batch: ['DELETE'] },
	// Engine: checkSuspendBatch / checkActivateBatch → UPDATE on BATCH
	updateBatch:          { batch: ['UPDATE'] },
	// Engine: checkReadHistoricBatch → READ_HISTORY on BATCH
	readHistoricBatch:    { batch: ['READ_HISTORY'] },

	// ── Admin Management ───────────────────────────────────────────────────────
	usersManagement:          { user:          ['ALL'] },
	groupsManagement:         { group:         ['ALL'] },
	authorizationsManagement: { authorization: ['ALL'] },
	systemManagement:         { system:        ['ALL'] },
	tenantsManagement:        { tenant:        ['ALL'] },
}

/**
 * Convert ACTION_PERMISSIONS into the flat `{ actionName: permissionsObj }`
 * format expected by the legacy mixin methods (`processByPermissions`,
 * `filtersByPermissions`, etc.) and stored in `config.permissions`.
 *
 * For disjunctive (OR) entries (arrays), the FIRST alternative is used as the
 * representative single-resource-type check so that the existing AND-based mixin
 * methods continue to work.  Full OR evaluation is available through
 * `checkActionPermission()` in permissionsMixin.
 *
 * @param {Object} actionPerms  An ACTION_PERMISSIONS-shaped object.
 * @returns {Object}  A flat permissions object compatible with `config.permissions`.
 */
function toConfigPermissions(actionPerms) {
	const result = {}
	for (const [action, def] of Object.entries(actionPerms)) {
		if (Array.isArray(def)) {
			if (def.length === 0) continue // skip empty OR arrays — no usable alternative
			result[action] = def[0]
		} else {
			result[action] = def
		}
	}
	return result
}

export { ACTION_PERMISSIONS, toConfigPermissions }
