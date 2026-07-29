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
package org.cibseven.webapp.auth.assertion;

/**
 * Supported JWT client assertion types.
 */
public enum AssertionType {

    /**
     * Signs assertions via JJWT, auto-detecting the algorithm from the key type
     * (RS256 for RSA, ES256 for EC P-256).
     * Requires {@code key-location} to point to a PEM-encoded private key.
     * Requires {@code io.jsonwebtoken:jjwt-api} at compile time and
     * {@code io.jsonwebtoken:jjwt-impl} at runtime.
     */
    JJWT,

    /**
     * Uses the Azure Workload Identity federated token (reads the file referenced by
     * the {@code AZURE_FEDERATED_TOKEN_FILE} environment variable, injected by the
     * Azure Workload Identity mutating webhook).
     * No key configuration required.
     */
    AZURE_WORKLOAD_IDENTITY
}