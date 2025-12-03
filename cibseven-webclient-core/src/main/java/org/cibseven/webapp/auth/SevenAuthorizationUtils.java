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
package org.cibseven.webapp.auth;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.cibseven.webapp.exception.AccessDeniedException;
import org.cibseven.webapp.rest.model.Authorization;
import org.cibseven.webapp.rest.model.Authorizations;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class SevenAuthorizationUtils{ 

	@Getter @AllArgsConstructor
	private enum SevenAuthorizationType {
		AUTH_TYPE_GLOBAL(0),
		AUTH_TYPE_GRANT(1),
		AUTH_TYPE_REVOKE(2);

		private final int type;
	}


	public static int resourceType(SevenResourceType type) {
	    return type.getType();
	}

	public static int authorizationType(SevenAuthorizationType type) {
	    return type.getType();
	}

    public static boolean hasAdminManagementPermissions(Authorizations authorizations, SevenResourceType type, List<String> permissions) {
        return checkPermission(authorizations, type, permissions);
    }

    public static boolean checkPermission(Authorizations authorizations, SevenResourceType type, List<String> permissions) {
        Collection<Authorization> authList;
        switch (type) {
            case USER: authList = authorizations.getUser(); break;
            case GROUP: authList = authorizations.getGroup(); break;
            case AUTHORIZATION: authList = authorizations.getAuthorization(); break;
            case PROCESS_DEFINITION: authList = authorizations.getProcessDefinition(); break;
            case PROCESS_INSTANCE: authList = authorizations.getProcessInstance(); break;
            case DECISION_DEFINITION: authList = authorizations.getDecisionDefinition(); break;
            case DECISION_REQUIREMENTS_DEFINITION: authList = authorizations.getDecisionRequirementsDefinition(); break;
            case TASK: authList = authorizations.getTask(); break;
            case FILTER: authList = authorizations.getFilter(); break;
            case DEPLOYMENT: authList = authorizations.getDeployment(); break;
            //case CASE_DEFINITION: authList = authorizations.getCaseDefinition(); break;
            //case CASE_INSTANCE: authList = authorizations.getCaseInstance(); break;
            case APPLICATION: authList = authorizations.getApplication(); break;
            //case JOB_DEFINITION: authList = authorizations.getJobDefinition(); break;
            case BATCH: authList = authorizations.getBatch(); break;
            case GROUP_MEMBERSHIP: authList = authorizations.getGroupMembership(); break;
            case HISTORIC_TASK: authList = authorizations.getHistoricTask(); break;
            case HISTORIC_PROCESS_INSTANCE: authList = authorizations.getHistoricProcessInstance(); break;
            case TENANT: authList = authorizations.getTenant(); break;
            case TENANT_MEMBERSHIP: authList = authorizations.getTenantMembership(); break;
            case REPORT: authList = authorizations.getReport(); break;
            case DASHBOARD: authList = authorizations.getDashboard(); break;
            case USER_OPERATION_LOG_CATEGORY: authList = authorizations.getUserOperationLogCategory(); break;
            case SYSTEM: authList = authorizations.getSystem(); break;
            //case MESSAGE: authList = authorizations.getMessage(); break;
            //case EVENT_SUBSCRIPTION: authList = authorizations.getEventSubscription(); break;
            default:
                throw new AccessDeniedException("You are not authorized to access resource type: " + type.name());
        }

        boolean hasDeny = authList.stream().anyMatch(auth ->
                auth.getType() == authorizationType(SevenAuthorizationType.AUTH_TYPE_REVOKE) && auth.getPermissions().length > 0 &&
                        Arrays.stream(auth.getPermissions()).anyMatch(permissions::contains)
        );

        if (hasDeny) {
            throw new AccessDeniedException("Access denied: Permission explicitly revoked for resource type: " + type.name() + ", permissions: " + String.join(", ", permissions));
        }

        boolean hasPermission = authList.stream().anyMatch(auth ->
                (auth.getType() == authorizationType(SevenAuthorizationType.AUTH_TYPE_GRANT) || auth.getType() == authorizationType(SevenAuthorizationType.AUTH_TYPE_GLOBAL)) &&
                        auth.getPermissions().length > 0 && Arrays.stream(auth.getPermissions()).anyMatch(permissions::contains)
        );

        if (!hasPermission) {
            throw new AccessDeniedException("Access denied: Missing required permissions for resource type: " + type.name() + ", permissions: " + String.join(", ", permissions));
        }

        return true;
    }

    public static boolean hasSpecificProcessRights(Authorizations authorizations, String processKey) {
        boolean hasRights = authorizations.getProcessDefinition().stream().anyMatch(auth ->
                auth.getType() == authorizationType(SevenAuthorizationType.AUTH_TYPE_GRANT) && auth.getPermissions().length > 0 &&
                        ("ALL".equals(auth.getPermissions()[0]) || "CREATE_INSTANCE".equals(auth.getPermissions()[0])) &&
                        (processKey.equals(auth.getResourceId()))
        );

        if (!hasRights) {
            throw new AccessDeniedException("Access denied: Missing required permissions for process: " + processKey + ", permissions: ALL or CREATE_INSTANCE");
        }

        return true;
    }
}
