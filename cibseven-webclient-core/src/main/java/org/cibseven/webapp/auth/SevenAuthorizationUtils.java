package org.cibseven.webapp.auth;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.cibseven.webapp.auth.exception.AuthenticationException;
import org.cibseven.webapp.rest.model.Authorization;
import org.cibseven.webapp.rest.model.Authorizations;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class SevenAuthorizationUtils {
	
	@Getter @AllArgsConstructor
	private enum SevenAuthorizationType {
		AUTH_TYPE_GLOBAL(0),
		AUTH_TYPE_GRANT(1),
		AUTH_TYPE_REVOKE(2);
		
		private final int type;
	}
	
	/*public static final String USER = SevenResourceType.USER.name();
	public static final String GROUP = SevenResourceType.GROUP.name();
	public static final String AUTHORIZATION = SevenResourceType.AUTHORIZATION.name();
	public static final String PROCESS_DEFINITION = SevenResourceType.PROCESS_DEFINITION.name();
	public static final String PROCESS_INSTANCE = SevenAuthorizationUtils.SevenResourceType.PROCESS_INSTANCE.name();
	public static final String DECISION_DEFINITION = SevenAuthorizationUtils.SevenResourceType.DECISION_DEFINITION.name();
	public static final String DECISION_REQUIREMENTS_DEFINITION = SevenAuthorizationUtils.SevenResourceType.DECISION_REQUIREMENTS_DEFINITION.name();
	public static final String TASK = SevenAuthorizationUtils.SevenResourceType.TASK.name();
	public static final String FILTER = SevenAuthorizationUtils.SevenResourceType.FILTER.name();
	public static final String HISTORY = SevenAuthorizationUtils.SevenResourceType.HISTORY.name();
	public static final String DEPLOYMENT = SevenAuthorizationUtils.SevenResourceType.DEPLOYMENT.name();
	public static final String CASE_DEFINITION = SevenAuthorizationUtils.SevenResourceType.CASE_DEFINITION.name();
	public static final String CASE_INSTANCE = SevenAuthorizationUtils.SevenResourceType.CASE_INSTANCE.name();
	public static final String APPLICATION = SevenAuthorizationUtils.SevenResourceType.APPLICATION.name();
	public static final String JOB_DEFINITION = SevenAuthorizationUtils.SevenResourceType.JOB_DEFINITION.name();
	public static final String BATCH = SevenAuthorizationUtils.SevenResourceType.BATCH.name();
	public static final String GROUP_MEMBERSHIP = SevenAuthorizationUtils.SevenResourceType.GROUP_MEMBERSHIP.name();
	public static final String HISTORIC_TASK = SevenAuthorizationUtils.SevenResourceType.HISTORIC_TASK.name();
	public static final String HISTORIC_PROCESS_INSTANCE = SevenAuthorizationUtils.SevenResourceType.HISTORIC_PROCESS_INSTANCE.name();
	public static final String TENANT = SevenAuthorizationUtils.SevenResourceType.TENANT.name();
	public static final String TENANT_MEMBERSHIP = SevenAuthorizationUtils.SevenResourceType.TENANT_MEMBERSHIP.name();
	public static final String REPORT = SevenAuthorizationUtils.SevenResourceType.REPORT.name();
	public static final String DASHBOARD = SevenAuthorizationUtils.SevenResourceType.DASHBOARD.name();
	public static final String USER_OPERATION_LOG_CATEGORY = SevenAuthorizationUtils.SevenResourceType.USER_OPERATION_LOG_CATEGORY.name();
	public static final String SYSTEM = SevenAuthorizationUtils.SevenResourceType.SYSTEM.name();
	public static final String MESSAGE = SevenAuthorizationUtils.SevenResourceType.MESSAGE.name();
	public static final String EVENT_SUBSCRIPTION = SevenAuthorizationUtils.SevenResourceType.EVENT_SUBSCRIPTION.name();
	*/
	public static final String AUTH_TYPE_GLOBAL = SevenAuthorizationUtils.SevenAuthorizationType.AUTH_TYPE_GLOBAL.name();
	public static final String AUTH_TYPE_GRANT = SevenAuthorizationUtils.SevenAuthorizationType.AUTH_TYPE_GRANT.name();
	public static final String AUTH_TYPE_REVOKE = SevenAuthorizationUtils.SevenAuthorizationType.AUTH_TYPE_REVOKE.name();
	
	public static int resourceType(SevenResourceType type) {
	    return type.getType();
	}
	
	public static int authorizationType(String name) {
	    return SevenAuthorizationType.valueOf(name).getType();
	}
	
    public static boolean hasCockpitRights(Authorizations authorizations) {
        return authorizations.getApplication().stream().anyMatch(auth -> {
            if (auth.getType() == authorizationType(AUTH_TYPE_GRANT) && auth.getPermissions().length > 0 &&
                    ("ALL".equals(auth.getPermissions()[0]) || "ACCESS".equals(auth.getPermissions()[0])) &&
                    ("cockpit".equals(auth.getResourceId()) || "*".equals(auth.getResourceId()))) {
                return true;
            } else if (auth.getType() == authorizationType(AUTH_TYPE_GLOBAL) && Arrays.asList(auth.getPermissions()).contains("ALL") &&
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
                auth.getType() == authorizationType(AUTH_TYPE_REVOKE) && auth.getPermissions().length > 0 &&
                        permissions.contains(auth.getPermissions()[0])
        );

        if (hasDeny) return false;

        return authList.stream().anyMatch(auth ->
                (auth.getType() == authorizationType(AUTH_TYPE_GRANT) || auth.getType() == authorizationType(AUTH_TYPE_GLOBAL)) &&
                        auth.getPermissions().length > 0 && permissions.contains(auth.getPermissions()[0])
        );
    }

    public static boolean hasSpecificProcessRights(Authorizations authorizations, String processKey) {
        return authorizations.getProcessDefinition().stream().anyMatch(auth ->
                auth.getType() == authorizationType(AUTH_TYPE_GRANT) && auth.getPermissions().length > 0 &&
                        ("ALL".equals(auth.getPermissions()[0]) || "CREATE_INSTANCE".equals(auth.getPermissions()[0])) &&
                        (processKey.equals(auth.getResourceId()))
        );
    }
}
