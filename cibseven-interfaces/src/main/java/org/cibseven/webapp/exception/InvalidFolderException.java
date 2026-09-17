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
 * A folder operation was refused before it reached the database: an empty or duplicate name, a
 * move into the folder's own subtree, or a model placed where models cannot live. Carries the
 * field and the reason, so the frontend can name the input that has to change.
 */
public class InvalidFolderException extends ApplicationException {

	private static final long serialVersionUID = 1L;

	public InvalidFolderException(String field, String reason) {
		super(field, reason);
	}

	public String getField() {
		return (String) getData()[0];
	}

	public String getReason() {
		return (String) getData()[1];
	}
}
