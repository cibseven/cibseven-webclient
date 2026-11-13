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

/**
 * A request value that would become part of an engine REST URL, but carries a character that would
 * change that URL (a path separator, a query, a fragment, a matrix parameter, an escape) or is a
 * relative segment. Reported as a request error, with one readable sentence as its only parameter.
 */
public class InvalidPathValueException extends ApplicationException {

	private static final long serialVersionUID = 1L;

	public InvalidPathValueException(String what, String value) {
		super(String.format("%s \"%s\" cannot be part of the engine URL", what, value));
	}
}
