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

import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Safety net for controllers that return a ResponseEntity received from another server:
 * connection-level headers (RFC 9110, 7.6.1) describe that other connection, and the servlet
 * container frames our response itself. Forwarding them sends e.g. Transfer-Encoding twice.
 */
@ControllerAdvice
public class HopByHopHeadersAdvice implements ResponseBodyAdvice<Object> {

	static final List<String> HOP_BY_HOP_HEADERS = List.of(
			HttpHeaders.CONNECTION,
			"Keep-Alive",
			HttpHeaders.PROXY_AUTHENTICATE,
			HttpHeaders.PROXY_AUTHORIZATION,
			HttpHeaders.TE,
			HttpHeaders.TRAILER,
			HttpHeaders.TRANSFER_ENCODING,
			HttpHeaders.UPGRADE);

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return true;
	}

	@Override
	public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
			Class<? extends HttpMessageConverter<?>> selectedConverterType,
			ServerHttpRequest request, ServerHttpResponse response) {
		HttpHeaders headers = response.getHeaders();
		HOP_BY_HOP_HEADERS.forEach(headers::remove);
		return body;
	}
}
