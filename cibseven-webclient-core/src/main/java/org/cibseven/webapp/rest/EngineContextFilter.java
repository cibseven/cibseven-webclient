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
package org.cibseven.webapp.rest;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.cibseven.webapp.providers.SevenProviderBase;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Filter that extracts the engine name from the HTTP request header and sets it in ThreadLocal
 * for the duration of the request. This ensures that all provider calls during the request
 * use the correct engine context.
 * 
 * The filter runs early in the filter chain (Order 1) to ensure the engine context is available
 * for all subsequent processing.
 */
@Slf4j
@Component
@Order(1)
public class EngineContextFilter implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		
		if (request instanceof HttpServletRequest) {
			HttpServletRequest httpRequest = (HttpServletRequest) request;
			String engineName = httpRequest.getHeader(SevenProviderBase.ENGINE_NAME_HEADER);
			
			if (engineName != null && !engineName.isEmpty()) {
				SevenProviderBase.setCurrentEngineName(engineName);
			}
		}
		
		try {
			// Continue with the filter chain
			chain.doFilter(request, response);
		} finally {
			// Always clear the ThreadLocal to prevent memory leaks
			SevenProviderBase.clearCurrentEngineName();
		}
	}
}
