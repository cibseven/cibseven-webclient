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
import static org.assertj.core.api.Assertions.entry;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * The webclient has two exception families, {@link SystemException} and {@link ApplicationException}.
 * Both pass their message and cause on to {@link RuntimeException}, so a caller can log them;
 * an {@link ApplicationException} additionally names the values it is about in {@code getData()}.
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

	// ---------- ApplicationException ----------

	@Test
	void applicationExceptionSubclassesShouldKeepTheirMessageAndCause() {
		SystemException cause = new SystemException("variable 'amount' does not exist");

		NoObjectFoundException fromCause = new NoObjectFoundException(cause);
		NoObjectFoundException fromMessage = new NoObjectFoundException("no such variable");

		assertThat(fromCause.getMessage()).isEqualTo("The object could not be found!");
		assertThat(fromCause.getCause()).isSameAs(cause);
		assertThat(fromMessage.getMessage()).isEqualTo("no such variable");
	}

	@Test
	void applicationExceptionToleratesANullMessageAndNullData() {
		NoObjectFoundException fromNull = new NoObjectFoundException((String) null);
		ApplicationException fromNothing = new ApplicationException(null, (Map<String, Object>) null) {
			private static final long serialVersionUID = 1L;
		};

		assertThat(fromNull.getMessage()).isEqualTo("An application error occurred");
		assertThat(fromNull.getData()).isEmpty();
		assertThat(fromNothing.getMessage()).isEqualTo("An application error occurred");
		assertThat(fromNothing.getData()).isNotNull().isEmpty();
	}

	@Test
	void everyConstructorLeavesDataSet() {
		IllegalStateException cause = new IllegalStateException("clash");

		// the (message, cause) constructor has no data of its own, and still leaves no null behind
		assertThat(new OptimisticLockingException(cause).getData()).isNotNull().isEmpty();
		assertThat(new NoObjectFoundException(cause).getData()).isNotNull().isEmpty();
		assertThat(new ApplicationException("with data", cause, Map.of("id", "7")) {
			private static final long serialVersionUID = 1L;
		}.getData()).containsExactly(entry("id", "7"));
	}

	@Test
	void everyApplicationExceptionSubclassNamesTheValuesItIsAbout() {
		assertThat(new UnknownResourceTypeException(99).getData())
			.containsExactly(entry(UnknownResourceTypeException.RESOURCE_TYPE, 99));
		assertThat(new ExistingElementTemplateException("dup").getData())
			.containsExactly(entry(ExistingElementTemplateException.TEMPLATE_ID, "dup"));
		assertThat(new ExistingFormIdException("invoice-form").getData())
			.containsExactly(entry(ExistingFormIdException.FORM_ID, "invoice-form"));
		assertThat(new ExistingProcessKeyException("invoice").getData())
			.containsExactly(entry(ExistingProcessKeyException.PROCESS_KEY, "invoice"));
		assertThat(new InvalidFolderException("name", "empty").getData())
			.containsExactly(entry(InvalidFolderException.FIELD, "name"), entry(InvalidFolderException.REASON, "empty"));
		assertThat(new ValueTooLongException("description", 255).getData())
			.containsExactly(entry(ValueTooLongException.FIELD, "description"), entry(ValueTooLongException.LIMIT, 255));
	}

	@Test
	void anIdThatIsNotThereYetIsStillReported() {
		// e.g. new ExistingFormIdException(entity.getFormId()) with a form that has no id yet
		assertThat(new ExistingFormIdException(null).getData()).containsEntry(ExistingFormIdException.FORM_ID, null);
		assertThat(new ExistingProcessKeyException(null).getData()).containsEntry(ExistingProcessKeyException.PROCESS_KEY, null);
		assertThat(new ExistingElementTemplateException(null).getData()).containsEntry(ExistingElementTemplateException.TEMPLATE_ID, null);
	}

	@Test
	void exceptionsThatCarryATextKeepItInTheirMessage() {
		InvalidPathValueException invalidPath = new InvalidPathValueException("Tenant ID", "a/b");

		assertThat(invalidPath.getMessage()).isEqualTo("Tenant ID \"a/b\" cannot be part of the engine URL");
		assertThat(invalidPath.getData()).isEmpty();
		assertThat(new FeatureDisabledException("modeler").getMessage()).isEqualTo("modeler");
		assertThat(new UnknownEngineException().getMessage()).isEqualTo("The requested process engine is not configured");
		assertThat(new NoObjectFoundException("gone").getData()).isEmpty();
	}

	@Test
	void applicationExceptionsAreStillRuntimeExceptions() {
		// they propagate without being declared, which is what the REST layer relies on
		assertThat(new NoObjectFoundException("gone")).isInstanceOf(RuntimeException.class);
		assertThat(new SystemException("boom")).isInstanceOf(RuntimeException.class);
	}

	@Test
	void applicationExceptionKeepsMessageAndCauseOfTheMessageAndCauseConstructor() {
		IllegalStateException cause = new IllegalStateException("b");

		NoObjectFoundException exception = new NoObjectFoundException("a", cause);

		assertThat(exception.getMessage()).isEqualTo("a");
		assertThat(exception.getCause()).isSameAs(cause);
	}

	@Test
	void valuesInDataAreNotTakenForACause() {
		InvalidFolderException invalidFolder = new InvalidFolderException("name", "empty");
		ValueTooLongException tooLong = new ValueTooLongException("description", 255);

		assertThat(invalidFolder.getCause()).isNull();
		assertThat(invalidFolder.getField()).isEqualTo("name");
		assertThat(invalidFolder.getReason()).isEqualTo("empty");
		assertThat(tooLong.getCause()).isNull();
		assertThat(tooLong.getField()).isEqualTo("description");
		assertThat(tooLong.getLimit()).isEqualTo(255);
	}
}
