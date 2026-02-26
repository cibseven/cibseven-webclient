package org.cibseven.webapp.providers;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.PasswordPolicyRequest;

public interface IIdentityProvider {
    public Object validatePasswordPolicy(PasswordPolicyRequest request, CIBUser user) throws SystemException;

}
