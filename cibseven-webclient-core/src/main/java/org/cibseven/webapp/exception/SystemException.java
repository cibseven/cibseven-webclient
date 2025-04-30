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

import lombok.NonNull;

public class SystemException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;

	public SystemException(@NonNull Throwable cause) {
		super("Some unexpected technical problem occured", cause);
	}
	
	public SystemException(@NonNull String msg) {
		super("Some unexpected technical problem occured: " + msg);
	}

	public SystemException(@NonNull String msg, @NonNull Throwable cause) {
		super("Some unexpected technical problem occured: " + msg, cause);
	}
	
}
