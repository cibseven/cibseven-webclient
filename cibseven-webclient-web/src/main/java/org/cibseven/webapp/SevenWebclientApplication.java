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
package org.cibseven.webapp;

import org.cibseven.webapp.config.StandaloneDefaults;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class SevenWebclientApplication extends SpringBootServletInitializer {
	
	public static void main(String[] args) {
		new SpringApplicationBuilder(SevenWebclientApplication.class)
			.properties(StandaloneDefaults.properties())
			.run(args);
	}

	@Override // deployment as WAR to an application server
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder.properties(StandaloneDefaults.properties());
	}
}
