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

import org.junit.jupiter.api.Test;

/**
 * The webclient has two exception families and they behave very differently.
 * <p>
 * {@link SystemException} extends {@link RuntimeException} properly, so its message and cause
 * survive. {@link ApplicationException} does not - see
 * {@link #applicationExceptionSubclassesLoseTheirMessageAndCause()} - and everything derived from
 * it is affected. These tests pin both, because the difference decides what a caller can log.
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

	@Test
	void applicationExceptionSubclassesLoseTheirMessageAndCause() {
		SystemException cause = new SystemException("variable 'amount' does not exist");

		NoObjectFoundException fromCause = new NoObjectFoundException(cause);
		NoObjectFoundException fromMessage = new NoObjectFoundException("no such variable");

		// KNOWN BUG (pinned, not fixed): ApplicationException declares only
		// `ApplicationException(Object... data)` and never calls super(message), so every
		// constructor of every subclass resolves to that varargs constructor. The arguments are
		// stashed in `data` and RuntimeException's own message and cause are left unset. Anything
		// that logs or serialises one of these exceptions - which is what the REST error handlers
		// do - sees nothing at all, and the cause chain is broken for stack traces.
		assertThat(fromCause.getMessage()).isNull();
		assertThat(fromCause.getCause()).isNull();
		assertThat(fromMessage.getMessage()).isNull();

		// the information is reachable only through getData()
		assertThat(fromCause.getData()).containsExactly("The object could not be found!", cause);
		assertThat(fromMessage.getData()).containsExactly("no such variable");
	}

	@Test
	void everyApplicationExceptionSubclassBehavesTheSameWay() {
		// the same defect, reached through each of the four subclasses
		assertThat(new OptimisticLockingException(new IllegalStateException("clash")).getMessage()).isNull();
		assertThat(new UnknownResourceTypeException(99).getMessage()).isNull();
		assertThat(new ExistingElementTemplateException("dup").getMessage()).isNull();
		assertThat(new NoObjectFoundException("gone").getMessage()).isNull();

		// and the arguments they were built with are only in data
		assertThat(new UnknownResourceTypeException(99).getData()).containsExactly(99);
		assertThat(new ExistingElementTemplateException("dup").getData()).containsExactly("dup");
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
