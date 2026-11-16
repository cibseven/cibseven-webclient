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
package org.cibseven.webapp.config;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.regex.Pattern;

import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Lets a browser tell a changed file from an unchanged one by its content, for the files the webclient serves
 * under a fixed name: everything in a theme folder (logo, favicon, login image, translations), the public
 * translation files, {@code config.json}, {@code favicon.ico} and {@code plugin-runtime.js}.
 * <p>
 * The bundles carry a content hash in their names, these files do not, and a theme or a customer replaces them
 * under the same name. Spring's resource handler answers the browser's revalidation with the file's
 * modification time only. A replaced logo whose time did not move forward (a reproducible build, a copy that
 * keeps the time, a file older than the cached one) was answered with "304 Not Modified", and the browser kept
 * showing the old logo until the user cleared the cache.
 * <p>
 * For these files this filter therefore
 * <ul>
 * <li>adds an ETag computed from the content ({@link ShallowEtagHeaderFilter}) and answers "304 Not Modified"
 * only when the browser's ETag matches it, and</li>
 * <li>hides the browser's {@code If-Modified-Since} from the resource handler, so the modification time no
 * longer decides whether the file is sent.</li>
 * </ul>
 * The files stay small, so buffering them to compute the ETag costs little. Every other request passes
 * through untouched.
 */
public class StaticFileEtagFilter extends ShallowEtagHeaderFilter {

	/**
	 * The path, relative to the application, of a file served under a fixed name. The webclient may be served
	 * below a prefix, so the names are matched at the end of the path.
	 */
	private static final Pattern FIXED_NAME_FILE = Pattern.compile(
		"(?:^|.*/)(?:themes/[^?#]+\\.[A-Za-z0-9]+|translations_[A-Za-z_-]+\\.json|config\\.json|favicon\\.ico|plugin-runtime\\.js)");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (!isFixedNameFile(request)) {
			filterChain.doFilter(request, response);
			return;
		}
		super.doFilterInternal(new WithoutIfModifiedSince(request), response, filterChain);
	}

	/**
	 * @return whether the request asks for a file the webclient serves under a fixed name
	 */
	static boolean isFixedNameFile(HttpServletRequest request) {
		String path = request.getRequestURI().substring(request.getContextPath().length());
		return FIXED_NAME_FILE.matcher(path).matches();
	}

	/**
	 * The request without {@code If-Modified-Since}, so the resource handler cannot answer "304 Not Modified"
	 * on the modification time alone; the ETag of the content decides instead.
	 */
	static final class WithoutIfModifiedSince extends HttpServletRequestWrapper {

		WithoutIfModifiedSince(HttpServletRequest request) {
			super(request);
		}

		private static boolean isIfModifiedSince(String name) {
			return HttpHeaders.IF_MODIFIED_SINCE.equalsIgnoreCase(name);
		}

		@Override
		public String getHeader(String name) {
			return isIfModifiedSince(name) ? null : super.getHeader(name);
		}

		@Override
		public Enumeration<String> getHeaders(String name) {
			return isIfModifiedSince(name) ? Collections.emptyEnumeration() : super.getHeaders(name);
		}

		@Override
		public Enumeration<String> getHeaderNames() {
			return Collections.enumeration(Collections.list(super.getHeaderNames()).stream()
				.filter(name -> !isIfModifiedSince(name))
				.toList());
		}

		@Override
		public long getDateHeader(String name) {
			return isIfModifiedSince(name) ? -1 : super.getDateHeader(name);
		}
	}
}
