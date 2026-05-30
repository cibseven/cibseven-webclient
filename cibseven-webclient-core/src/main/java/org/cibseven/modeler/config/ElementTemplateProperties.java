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
package org.cibseven.modeler.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cibseven.webclient.modeler.templates")
@Getter
@Setter
public class ElementTemplateProperties {
    private List<String> paths;

    /**
     * Number of attempts to verify that the {@code MOD_ELEMENT_TEMPLATES} table exists
     * before loading element templates. The table is created by the process engine
     * (MyBatis), which has no Spring context and cannot be ordered against the webclient
     * deployment, so on external databases (MySQL/PostgreSQL) and slow environments
     * (e.g. arm64 under emulation) the table may not be present yet when this loader runs.
     * Default 30 attempts.
     */
    private int schemaReadyMaxAttempts = 30;

    /**
     * Delay between schema-readiness attempts, in milliseconds. Default 2000ms,
     * giving a default total wait of 60s ({@link #schemaReadyMaxAttempts} * this).
     */
    private long schemaReadyRetryDelayMs = 2000;
}
