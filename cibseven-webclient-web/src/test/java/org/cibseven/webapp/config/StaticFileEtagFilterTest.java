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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.cibseven.webapp.SevenWebclientContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A theme's logo replaced under the same name must reach the browser without clearing its cache. The files are
 * served by Spring's own resource handler, as in the application, from a folder whose logo is swapped between
 * two requests while its modification time stays the same - what a reproducible build or a copy that keeps the
 * time does.
 */
class StaticFileEtagFilterTest {

	private static final String LOGO = "themes/cib/logo.svg";
	private static final String OLD_LOGO = "<svg id=\"old\"/>";
	private static final String NEW_LOGO = "<svg id=\"new\"/>";
	private static final FileTime SAME_TIME = FileTime.from(Instant.parse("2026-01-01T00:00:00Z"));

	@TempDir
	Path webRoot;

	private ResourceHttpRequestHandler resourceHandler;

	@BeforeEach
	void serveTheFolder() throws Exception {
		resourceHandler = new ResourceHttpRequestHandler();
		resourceHandler.setLocations(List.of(new FileSystemResource(webRoot.toString() + "/")));
		resourceHandler.setServletContext(new MockServletContext());
		resourceHandler.afterPropertiesSet();
		writeLogo(OLD_LOGO);
	}

	private void writeLogo(String content) throws IOException {
		Path logo = webRoot.resolve(LOGO);
		Files.createDirectories(logo.getParent());
		Files.writeString(logo, content, StandardCharsets.UTF_8);
		Files.setLastModifiedTime(logo, SAME_TIME);
	}

	/** The resource handler, as Spring MVC calls it for the path below its mapping. */
	private FilterChain resourceHandlerChain(String path) {
		return (request, response) -> {
			request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, path);
			resourceHandler.handleRequest((HttpServletRequest) request, (HttpServletResponse) response);
		};
	}

	private MockHttpServletResponse get(String path, boolean throughFilter, String ifNoneMatch, String ifModifiedSince)
			throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/" + path);
		if (ifNoneMatch != null) request.addHeader(HttpHeaders.IF_NONE_MATCH, ifNoneMatch);
		if (ifModifiedSince != null) request.addHeader(HttpHeaders.IF_MODIFIED_SINCE, ifModifiedSince);
		MockHttpServletResponse response = new MockHttpServletResponse();
		if (throughFilter) {
			new StaticFileEtagFilter().doFilter(request, response, resourceHandlerChain(path));
		} else {
			resourceHandlerChain(path).doFilter(request, response);
		}
		return response;
	}

	/** The cause: without the filter the handler judges by the modification time and keeps the old logo. */
	@Test
	void withoutTheFilterTheSwappedLogoIsAnsweredNotModified() throws Exception {
		String lastModified = get(LOGO, false, null, null).getHeader(HttpHeaders.LAST_MODIFIED);

		writeLogo(NEW_LOGO);
		MockHttpServletResponse revalidation = get(LOGO, false, null, lastModified);

		assertThat(revalidation.getStatus()).isEqualTo(304);
	}

	@Test
	void sendsTheNewLogoWhenItsContentChangedButNotItsTime() throws Exception {
		MockHttpServletResponse first = get(LOGO, true, null, null);
		String etag = first.getHeader(HttpHeaders.ETAG);
		String lastModified = first.getHeader(HttpHeaders.LAST_MODIFIED);
		assertThat(first.getStatus()).isEqualTo(200);
		assertThat(first.getContentAsString()).isEqualTo(OLD_LOGO);
		assertThat(etag).isNotBlank();

		writeLogo(NEW_LOGO);
		// A browser revalidates with both the ETag and the time it was given
		MockHttpServletResponse revalidation = get(LOGO, true, etag, lastModified);

		assertThat(revalidation.getStatus()).isEqualTo(200);
		assertThat(revalidation.getContentAsString()).isEqualTo(NEW_LOGO);
		assertThat(revalidation.getHeader(HttpHeaders.ETAG)).isNotBlank().isNotEqualTo(etag);
	}

	@Test
	void answersNotModifiedWhileTheLogoIsUnchanged() throws Exception {
		MockHttpServletResponse first = get(LOGO, true, null, null);

		MockHttpServletResponse revalidation = get(LOGO, true,
			first.getHeader(HttpHeaders.ETAG), first.getHeader(HttpHeaders.LAST_MODIFIED));

		assertThat(revalidation.getStatus()).isEqualTo(304);
		assertThat(revalidation.getContentAsByteArray()).isEmpty();
	}

	/** A browser that cached the logo before this filter existed has no ETag, only the time */
	@Test
	void sendsTheNewLogoToABrowserThatOnlyKnowsTheModificationTime() throws Exception {
		String lastModified = get(LOGO, false, null, null).getHeader(HttpHeaders.LAST_MODIFIED);

		writeLogo(NEW_LOGO);
		MockHttpServletResponse revalidation = get(LOGO, true, null, lastModified);

		assertThat(revalidation.getStatus()).isEqualTo(200);
		assertThat(revalidation.getContentAsString()).isEqualTo(NEW_LOGO);
	}

	@Test
	void leavesOtherRequestsAlone() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/services/v1/process");
		request.addHeader(HttpHeaders.IF_MODIFIED_SINCE, "Thu, 01 Jan 2026 00:00:00 GMT");
		MockHttpServletResponse response = new MockHttpServletResponse();
		HttpServletRequest[] seen = new HttpServletRequest[1];

		new StaticFileEtagFilter().doFilter(request, response, (req, res) -> {
			seen[0] = (HttpServletRequest) req;
			res.getWriter().write("[]");
		});

		assertThat(seen[0]).isSameAs(request);
		assertThat(response.getHeader(HttpHeaders.ETAG)).isNull();
		assertThat(response.getContentAsString()).isEqualTo("[]");
	}

	@Nested
	class FilesServedUnderAFixedName {

		private boolean isFixedNameFile(String contextPath, String path) {
			MockHttpServletRequest request = new MockHttpServletRequest("GET", contextPath + path);
			request.setContextPath(contextPath);
			return StaticFileEtagFilter.isFixedNameFile(request);
		}

		@ParameterizedTest
		@ValueSource(strings = {
			"/themes/cib/logo.svg", "/themes/cib/favicon.ico", "/themes/cib/login-image.svg",
			"/themes/acme/translations_de.json", "/translations_en.json", "/translations_pt-BR.json",
			"/config.json", "/favicon.ico", "/plugin-runtime.js", "/webapp/themes/cib/logo.svg"
		})
		void coversTheFilesAThemeOrCustomerReplaces(String path) {
			assertThat(isFixedNameFile("", path)).isTrue();
			assertThat(isFixedNameFile("/cibseven", path)).isTrue();
		}

		@ParameterizedTest
		@ValueSource(strings = {
			"/assets/index-B1x2y3z4.js", "/assets/main-C5d6e7f8.css", "/index.html", "/",
			"/services/v1/process", "/services/v1/info/properties", "/themes", "/themes/cib/"
		})
		void leavesOutBundlesHtmlAndServices(String path) {
			assertThat(isFixedNameFile("", path)).isFalse();
		}
	}

	@Nested
	class RequestWithoutIfModifiedSince {

		private final MockHttpServletRequest original = new MockHttpServletRequest();
		private StaticFileEtagFilter.WithoutIfModifiedSince request;

		@BeforeEach
		void withConditionalHeaders() {
			original.addHeader(HttpHeaders.IF_MODIFIED_SINCE, "Thu, 01 Jan 2026 00:00:00 GMT");
			original.addHeader(HttpHeaders.IF_NONE_MATCH, "\"abc\"");
			request = new StaticFileEtagFilter.WithoutIfModifiedSince(original);
		}

		@Test
		void hidesIfModifiedSinceInEveryForm() {
			assertThat(request.getHeader("if-modified-since")).isNull();
			assertThat(Collections.list(request.getHeaders(HttpHeaders.IF_MODIFIED_SINCE))).isEmpty();
			assertThat(request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE)).isEqualTo(-1);
			assertThat(Collections.list(request.getHeaderNames())).doesNotContain(HttpHeaders.IF_MODIFIED_SINCE);
		}

		@Test
		void keepsTheOtherHeaders() {
			assertThat(request.getHeader(HttpHeaders.IF_NONE_MATCH)).isEqualTo("\"abc\"");
			assertThat(Collections.list(request.getHeaders(HttpHeaders.IF_NONE_MATCH))).containsExactly("\"abc\"");
			assertThat(Collections.list(request.getHeaderNames())).contains(HttpHeaders.IF_NONE_MATCH);
		}
	}

	@Test
	void isRegisteredForEveryRequestByTheWebclientContext() {
		FilterRegistrationBean<StaticFileEtagFilter> registration =
			new SevenWebclientContext(new SevenWebclientProperties()).staticFileEtagFilter();

		assertThat(registration.getFilter()).isInstanceOf(StaticFileEtagFilter.class);
		assertThat(registration.getFilterName()).isEqualTo("staticFileEtagFilter");
		// No URL patterns: the filter applies to all requests and picks the files itself
		assertThat(registration.getUrlPatterns()).isEmpty();
	}
}
