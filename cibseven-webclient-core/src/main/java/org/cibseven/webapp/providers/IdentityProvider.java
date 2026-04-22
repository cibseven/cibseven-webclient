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
package org.cibseven.webapp.providers;

import java.util.ArrayList;
import java.util.Map;

import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.PasswordPolicyRequest;
import org.cibseven.webapp.rest.model.PasswordPolicyResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.cibseven.webapp.auth.CIBUser;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class IdentityProvider extends SevenProviderBase implements IIdentityProvider {

    public PasswordPolicyResponse validatePasswordPolicy(PasswordPolicyRequest request)
            throws SystemException {
        String url = getEngineRestUrl() + "/identity/password-policy";
        Map<String, Object> body = Map.of(
                "password", request.getPassword(),
                "profile", request.getProfile());
        try {
            ResponseEntity<PasswordPolicyResponse> response = (ResponseEntity<PasswordPolicyResponse>) doPost(url, body,
                    PasswordPolicyResponse.class, null);
            PasswordPolicyResponse result = response.getBody();
            return result;
            // when enable-password-policy is switched of the endpoint will return 404, so
            // we catch this and return a default response
        } catch (SystemException e) {
            Throwable cause = e.getCause();
            if (cause instanceof HttpClientErrorException.NotFound) {
                return new PasswordPolicyResponse(true, new ArrayList<>());
            }
            throw e;
        }
    }
}
