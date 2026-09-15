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
package org.cibseven.webapp.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * The webclient has two exception families and they behave very differently.
 * <p>
 * {@link SystemException} extends {@link RuntimeException} properly, so its message and cause
 * survive. {@link ApplicationException} does not - see
 * {@link #applicationExceptionSubclassesShouldKeepTheirMessageAndCause()}, which is disabled
 * because it describes the behaviour the family should have and fails today - and everything
 * derived from it is affected. The difference decides what a caller can log.
 */
public class ExceptionContractTest {

	// ---------- SystemException: works as expected ----------

	@Test
	void systemException_prefixesTheMessage() {
		SystemException exception = new SystemException("engine unreachable");

		assertThat(exception.getMessage())
			.isEqualTo("Some unexpected technical problem occured: engine unreachable");
	}

	@Test
	void systemException_keepsTheCause() {
		IllegalStateException cause = new IllegalStateException("socket closed");

		SystemException exception = new SystemException("engine unreachable", cause);

		assertThat(exception.getMessage()).contains("engine unreachable");
		assertThat(exception.getCause()).isSameAs(cause);
	}

	@Test
	void systemException_fromACauseAloneStillCarriesBoth() {
		IllegalStateException cause = new IllegalStateException("socket closed");

		SystemException exception = new SystemException(cause);

		assertThat(exception.getMessage()).isEqualTo("Some unexpected technical problem occured");
		assertThat(exception.getCause()).isSameAs(cause);
	}

	// ---------- ApplicationException: loses both ----------

	/**
	 * TODO KNOWN BUG (not fixed): {@link ApplicationException} declares only
	 * {@code ApplicationException(Object... data)} and never calls {@code super(message)}, so every
	 * constructor of every subclass resolves to that varargs constructor. The arguments are stashed
	 * in {@code data} and {@link RuntimeException}'s own message and cause are left unset. Anything
	 * that logs or serialises one of these exceptions - which is what the REST error handlers do -
	 * sees nothing at all, and the cause chain is broken for stack traces. This test states the
	 * contract the family should honour; it fails until {@code ApplicationException} forwards the
	 * message and cause to {@code RuntimeException}.
	 */
	@Test
	@Disabled("KNOWN BUG: ApplicationException never calls super(message), so getMessage()/getCause() are null for every subclass")
	void applicationExceptionSubclassesShouldKeepTheirMessageAndCause() {
		SystemException cause = new SystemException("variable 'amount' does not exist");

		NoObjectFoundException fromCause = new NoObjectFoundException(cause);
		NoObjectFoundException fromMessage = new NoObjectFoundException("no such variable");

		assertThat(fromCause.getMessage()).isEqualTo("The object could not be found!");
		assertThat(fromCause.getCause()).isSameAs(cause);
		assertThat(fromMessage.getMessage()).isEqualTo("no such variable");
	}

	@Test
	void everyApplicationExceptionSubclassCollectsItsArgumentsInData() {
		// what each subclass was built with is reachable through getData() - and today only there
		assertThat(new OptimisticLockingException(new IllegalStateException("clash")).getData())
			.hasSize(2).startsWith("Entity was updated by another transaction concurrently.");
		assertThat(new UnknownResourceTypeException(99).getData()).containsExactly(99);
		assertThat(new ExistingElementTemplateException("dup").getData()).containsExactly("dup");
		assertThat(new NoObjectFoundException("gone").getData()).containsExactly("gone");
		assertThat(new NoObjectFoundException(new IllegalStateException("boom")).getData())
			.hasSize(2).startsWith("The object could not be found!");
	}

	@Test
	void applicationExceptionsAreStillRuntimeExceptions() {
		// they propagate without being declared, which is what the REST layer relies on
		assertThat(new NoObjectFoundException("gone")).isInstanceOf(RuntimeException.class);
		assertThat(new SystemException("boom")).isInstanceOf(RuntimeException.class);
	}

	@Test
	void applicationExceptionKeepsEveryArgumentItWasGiven() {
		NoObjectFoundException exception = new NoObjectFoundException("a", new IllegalStateException("b"));

		// the (message, cause) constructor also funnels both into data
		assertThat(exception.getData()).hasSize(2);
	}
}
