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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import org.cibseven.webapp.exception.BatchOperationException;
import org.cibseven.webapp.exception.DmnTransformationException;
import org.cibseven.webapp.exception.ExistingGroupRequestException;
import org.cibseven.webapp.exception.ExistingUserRequestException;
import org.cibseven.webapp.exception.ExpressionEvaluationException;
import org.cibseven.webapp.exception.InvalidAttributeValueException;
import org.cibseven.webapp.exception.InvalidGroupIdException;
import org.cibseven.webapp.exception.InvalidUserIdException;
import org.cibseven.webapp.exception.MissingVariableException;
import org.cibseven.webapp.exception.NoObjectFoundException;
import org.cibseven.webapp.exception.NoRessourcesFoundException;
import org.cibseven.webapp.exception.OptimisticLockingException;
import org.cibseven.webapp.exception.PasswordPolicyException;
import org.cibseven.webapp.exception.SubmitDeniedException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.exception.UnsupportedTypeException;
import org.cibseven.webapp.exception.VariableModificationException;
import org.cibseven.webapp.exception.WrongDeploymenIdException;
import org.cibseven.webapp.rest.model.Authorization;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

/**
 * {@link SevenProviderBase#wrapException} turns the engine's error text into the exception the
 * frontend shows a specific message for. Since the webclient leaves permission checks to the
 * engine, this translation is the only place an engine refusal gets its meaning.
 */
public class SevenProviderBaseErrorMappingTest {

	static Stream<Arguments> engineMessages() {
		return Stream.of(
			Arguments.of("Cannot change tenantId of Task 't1'. Tenant id to set 'other'", InvalidAttributeValueException.class),
			Arguments.of("Valid values for property 'sortBy' are [a, b] but was 'c'", InvalidAttributeValueException.class),
			Arguments.of("Cannot set query parameter 'maxResults' to value 'x'", InvalidAttributeValueException.class),
			Arguments.of("Cannot instantiate process definition p:1: Unsupported value type 'Foo'", UnsupportedTypeException.class),
			Arguments.of("Cannot instantiate process definition p:1: Error while evaluating expression ${x}", ExpressionEvaluationException.class),
			Arguments.of("Cannot submit task form t1: Cannot find task with id t1", SubmitDeniedException.class),
			Arguments.of("The user already exists", ExistingUserRequestException.class),
			Arguments.of("The group already exists", ExistingGroupRequestException.class),
			Arguments.of("Group has an invalid id", InvalidGroupIdException.class),
			Arguments.of("The given authenticated user password is not valid", SystemException.class),
			Arguments.of("Cannot modify variables for execution e1: execution e1 doesn't exist: execution is null", VariableModificationException.class),
			Arguments.of("No matching task with id t1", NoObjectFoundException.class),
			Arguments.of("Process instance with id p1 does not exist", NoObjectFoundException.class),
			Arguments.of("Cannot find task with id t1", NoObjectFoundException.class),
			Arguments.of("Historic process instance with id p1 does not exist", NoObjectFoundException.class),
			Arguments.of("No matching definition with id d1", NoObjectFoundException.class),
			Arguments.of("Cannot get start form data for process definition d1", NoObjectFoundException.class),
			Arguments.of("Cannot get process instance variable v: execution e1 doesn't exist", NoObjectFoundException.class),
			Arguments.of("process instance variable with name v does not exist", NoObjectFoundException.class),
			Arguments.of("Filter with id f1 does not exist", NoObjectFoundException.class),
			Arguments.of("Variable instance with Id v1 does not exist", NoObjectFoundException.class),
			Arguments.of("Cannot delete execution variable v: execution e1 doesn't exist: execution is null", NoObjectFoundException.class),
			Arguments.of("OptimisticLockingException: entity was updated by another transaction", OptimisticLockingException.class),
			Arguments.of("task variable with name v does not exist", MissingVariableException.class),
			Arguments.of("Password does not match policy", PasswordPolicyException.class),
			Arguments.of("User has an invalid id", InvalidUserIdException.class),
			Arguments.of("processInstanceIds is empty", BatchOperationException.class),
			Arguments.of("Deployment with id d1 does not exist", WrongDeploymenIdException.class),
			Arguments.of("Deployment resources for deployment id d1 do not exist", NoRessourcesFoundException.class),
			Arguments.of("Unable to transform DMN resource d.dmn", DmnTransformationException.class));
	}

	@ParameterizedTest
	@MethodSource("engineMessages")
	public void mapsTheEngineErrorToItsException(String engineMessage, Class<? extends RuntimeException> expected) {
		RuntimeException wrapped = SevenProviderBase.wrapException(clientError(engineMessage), null);

		assertEquals(expected, wrapped.getClass());
	}

	@Test
	public void carriesTheRejectedValueIntoTheMessage() {
		RuntimeException wrapped = SevenProviderBase.wrapException(
			clientError("Valid values for property 'sortBy' are [a, b] but was 'c'"), null);

		assertEquals("The value \"c\" is not valid!", wrapped.getMessage());
	}

	@Test
	public void filterResourcesKeepsOnlyTheRequestedResourceType() {
		Authorization onTasks = authorization(7);
		Authorization onProcesses = authorization(6);

		assertEquals(List.of(onTasks), List.copyOf(SevenProviderBase.filterResources(List.of(onTasks, onProcesses), 7)));
	}

	private static Authorization authorization(int resourceType) {
		Authorization authorization = new Authorization();
		authorization.setResourceType(resourceType);
		return authorization;
	}

	private static HttpClientErrorException clientError(String body) {
		return HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request",
			HttpHeaders.EMPTY, body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
	}
}
