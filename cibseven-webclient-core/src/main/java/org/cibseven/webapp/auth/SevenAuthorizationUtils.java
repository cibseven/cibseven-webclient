package org.cibseven.webapp.auth;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.cibseven.webapp.auth.exception.AuthenticationException;
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
	
    public static boolean hasCockpitRights(Authorizations authorizations) {
        return authorizations.getApplication().stream().anyMatch(auth -> {
            if (auth.getType() == authorizationType(SevenAuthorizationType.AUTH_TYPE_GRANT) && auth.getPermissions().length > 0 &&
                    ("ALL".equals(auth.getPermissions()[0]) || "ACCESS".equals(auth.getPermissions()[0])) &&
                    ("cockpit".equals(auth.getResourceId()) || "*".equals(auth.getResourceId()))) {
                return true;
            } else if (auth.getType() == authorizationType(SevenAuthorizationType.AUTH_TYPE_GLOBAL) && Arrays.asList(auth.getPermissions()).contains("ALL") &&
                    "*".equals(auth.getResourceId())) {
                return true;
            }
            return false;
        });
    }

    public static boolean hasAdminManagementPermissions(Authorizations authorizations, String action, SevenResourceType type, List<String> permissions) {
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
            case HISTORY: authList = authorizations.getHistory(); break;
            case DEPLOYMENT: authList = authorizations.getDeployment(); break;
            case CASE_DEFINITION: authList = authorizations.getCaseDefinition(); break;
            case CASE_INSTANCE: authList = authorizations.getCaseInstance(); break;
            case APPLICATION: authList = authorizations.getApplication(); break;
            case JOB_DEFINITION: authList = authorizations.getJobDefinition(); break;
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
            case MESSAGE: authList = authorizations.getMessage(); break;
            case EVENT_SUBSCRIPTION: authList = authorizations.getEventSubscription(); break;
            default:
                throw new AuthenticationException("You are not authorized to do this");
        }

        boolean hasDeny = authList.stream().anyMatch(auth ->
                auth.getType() == authorizationType(SevenAuthorizationType.AUTH_TYPE_REVOKE) && auth.getPermissions().length > 0 &&
                        permissions.contains(auth.getPermissions()[0])
        );

        if (hasDeny) return false;

        return authList.stream().anyMatch(auth ->
                (auth.getType() == authorizationType(SevenAuthorizationType.AUTH_TYPE_GRANT) || auth.getType() == authorizationType(SevenAuthorizationType.AUTH_TYPE_GLOBAL)) &&
                        auth.getPermissions().length > 0 && permissions.contains(auth.getPermissions()[0])
        );
    }

    public static boolean hasSpecificProcessRights(Authorizations authorizations, String processKey) {
        return authorizations.getProcessDefinition().stream().anyMatch(auth ->
                auth.getType() == authorizationType(SevenAuthorizationType.AUTH_TYPE_GRANT) && auth.getPermissions().length > 0 &&
                        ("ALL".equals(auth.getPermissions()[0]) || "CREATE_INSTANCE".equals(auth.getPermissions()[0])) &&
                        (processKey.equals(auth.getResourceId()))
        );
    }
}
