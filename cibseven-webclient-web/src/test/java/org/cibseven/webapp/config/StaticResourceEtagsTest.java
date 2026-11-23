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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.cibseven.webapp.SevenWebclientContext;
import org.cibseven.webapp.config.StaticResourceEtags.ContentEtag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.AbstractUrlHandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

/**
 * A theme's logo replaced under the same name must reach the browser without clearing its cache. The files are
 * served by Spring's own resource handler, as in the application, from a folder whose logo is swapped between
 * two requests while its modification time and its length stay the same - what a reproducible build, or a copy
 * that keeps the time, does to a logo whose colour changed.
 */
class StaticResourceEtagsTest {

	private static final String LOGO = "themes/cib/logo.svg";
	private static final String OLD_LOGO = "<svg fill=\"#ff0000\"/>";
	private static final String NEW_LOGO = "<svg fill=\"#00ff00\"/>";
	private static final FileTime SAME_TIME = FileTime.from(Instant.parse("2026-01-01T00:00:00Z"));

	@TempDir
	Path webRoot;

	private ResourceHttpRequestHandler handler(boolean configured) throws Exception {
		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		handler.setLocations(List.of(new FileSystemResource(webRoot.toString() + "/")));
		handler.setServletContext(new MockServletContext());
		handler.afterPropertiesSet();
		if (configured) {
			new StaticResourceEtags().configure(handler);
		}
		return handler;
	}

	private void writeLogo(String content) throws IOException {
		Path logo = webRoot.resolve(LOGO);
		Files.createDirectories(logo.getParent());
		Files.writeString(logo, content, StandardCharsets.UTF_8);
		Files.setLastModifiedTime(logo, SAME_TIME);
	}

	private static MockHttpServletResponse request(ResourceHttpRequestHandler handler, String method, String... headers)
			throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(method, "/" + LOGO);
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, LOGO);
		for (int i = 0; i < headers.length; i += 2) {
			request.addHeader(headers[i], headers[i + 1]);
		}
		MockHttpServletResponse response = new MockHttpServletResponse();
		handler.handleRequest(request, response);
		return response;
	}

	@BeforeEach
	void oldLogo() throws IOException {
		writeLogo(OLD_LOGO);
	}

	/** The cause: Spring's handler alone judges by the modification time and keeps the old logo. */
	@Test
	void withoutTheEtagTheSwappedLogoIsAnsweredNotModified() throws Exception {
		ResourceHttpRequestHandler plain = handler(false);
		String lastModified = request(plain, "GET").getHeader(HttpHeaders.LAST_MODIFIED);

		writeLogo(NEW_LOGO);

		assertThat(request(plain, "GET", HttpHeaders.IF_MODIFIED_SINCE, lastModified).getStatus()).isEqualTo(304);
	}

	@Test
	void sendsTheNewLogoWhenItsContentChangedButNotItsTimeOrLength() throws Exception {
		ResourceHttpRequestHandler handler = handler(true);
		MockHttpServletResponse first = request(handler, "GET");
		String etag = first.getHeader(HttpHeaders.ETAG);
		assertThat(first.getContentAsString()).isEqualTo(OLD_LOGO);
		assertThat(etag).isNotBlank();
		assertThat(first.getHeader(HttpHeaders.LAST_MODIFIED)).isNull();

		writeLogo(NEW_LOGO);
		MockHttpServletResponse revalidation = request(handler, "GET", HttpHeaders.IF_NONE_MATCH, etag);

		assertThat(revalidation.getStatus()).isEqualTo(200);
		assertThat(revalidation.getContentAsString()).isEqualTo(NEW_LOGO);
		assertThat(revalidation.getHeader(HttpHeaders.ETAG)).isNotBlank().isNotEqualTo(etag);
	}

	@Test
	void answersNotModifiedWhileTheLogoIsUnchanged() throws Exception {
		ResourceHttpRequestHandler handler = handler(true);
		String etag = request(handler, "GET").getHeader(HttpHeaders.ETAG);

		MockHttpServletResponse revalidation = request(handler, "GET", HttpHeaders.IF_NONE_MATCH, etag);

		assertThat(revalidation.getStatus()).isEqualTo(304);
		assertThat(revalidation.getContentAsByteArray()).isEmpty();
	}

	/** A browser that cached the logo before this change has no ETag, only the time */
	@Test
	void sendsTheNewLogoToABrowserThatOnlyKnowsTheModificationTime() throws Exception {
		String lastModified = request(handler(false), "GET").getHeader(HttpHeaders.LAST_MODIFIED);

		writeLogo(NEW_LOGO);
		MockHttpServletResponse revalidation = request(handler(true), "GET", HttpHeaders.IF_MODIFIED_SINCE, lastModified);

		assertThat(revalidation.getStatus()).isEqualTo(200);
		assertThat(revalidation.getContentAsString()).isEqualTo(NEW_LOGO);
	}

	/** The ETag describes the whole file, whatever part of it a request asks for */
	@Test
	void givesHeadAndRangeRequestsTheEtagOfTheWholeFile() throws Exception {
		ResourceHttpRequestHandler handler = handler(true);
		String etag = request(handler, "GET").getHeader(HttpHeaders.ETAG);

		MockHttpServletResponse head = request(handler, "HEAD");
		MockHttpServletResponse range = request(handler, "GET", HttpHeaders.RANGE, "bytes=0-3");

		assertThat(head.getHeader(HttpHeaders.ETAG)).isEqualTo(etag);
		assertThat(range.getStatus()).isEqualTo(206);
		assertThat(range.getContentAsString()).isEqualTo("<svg");
		assertThat(range.getHeader(HttpHeaders.ETAG)).isEqualTo(etag);
	}

	@Nested
	class ContentEtagOfAFile {

		/** A resource of the given content and time that counts how often it is read */
		private class CountingResource extends AbstractResource {
			private final byte[] content;
			private final long lastModified;
			int reads;

			CountingResource(byte[] content, long lastModified) {
				this.content = content;
				this.lastModified = lastModified;
			}

			@Override
			public InputStream getInputStream() {
				reads++;
				return new ByteArrayInputStream(content);
			}

			@Override
			public long contentLength() {
				return content.length;
			}

			@Override
			public long lastModified() {
				return lastModified;
			}

			@Override
			public URL getURL() throws IOException {
				return webRoot.resolve("bundle.js").toUri().toURL();
			}

			@Override
			public String getDescription() {
				return "bundle.js";
			}
		}

		private byte[] large(char fill) {
			byte[] content = new byte[(int) ContentEtag.HASH_EVERY_TIME_UP_TO + 1];
			Arrays.fill(content, (byte) fill);
			return content;
		}

		@Test
		void readsASmallFileOnEveryRequest() {
			ContentEtag etag = new ContentEtag();
			CountingResource small = new CountingResource("{}".getBytes(StandardCharsets.UTF_8), 1);

			assertThat(etag.apply(small)).isEqualTo(etag.apply(small));
			assertThat(small.reads).isEqualTo(2);
		}

		@Test
		void readsALargeFileOncePerVersion() {
			ContentEtag etag = new ContentEtag();
			CountingResource bundle = new CountingResource(large('a'), 1);

			String first = etag.apply(bundle);

			assertThat(etag.apply(bundle)).isEqualTo(first);
			assertThat(bundle.reads).isEqualTo(1);
		}

		@Test
		void readsALargeFileAgainWhenItsTimeOrLengthChanged() {
			ContentEtag etag = new ContentEtag();
			String first = etag.apply(new CountingResource(large('a'), 1));

			assertThat(etag.apply(new CountingResource(large('b'), 2))).isNotEqualTo(first);
			byte[] longer = Arrays.copyOf(large('c'), large('c').length + 1);
			assertThat(etag.apply(new CountingResource(longer, 1))).isNotEqualTo(first);
		}

		@Test
		void hasNoEtagForAFileThatCannotBeRead() {
			AbstractResource unreadable = new AbstractResource() {
				@Override
				public InputStream getInputStream() throws IOException {
					throw new IOException("gone");
				}

				@Override
				public long contentLength() {
					return 10;
				}

				@Override
				public String getDescription() {
					return "unreadable";
				}
			};

			assertThat(new ContentEtag().apply(unreadable)).isNull();
		}
	}

	/** Spring Boot's own handlers - the static files and the webjars - are the ones that get the ETag */
	@Test
	void configuresTheResourceHandlersOfSpringBoot() {
		new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(WebMvcAutoConfiguration.class))
			.withBean(StaticResourceEtags.class)
			.run(context -> {
				AbstractUrlHandlerMapping mapping = context.getBean("resourceHandlerMapping", AbstractUrlHandlerMapping.class);
				List<ResourceHttpRequestHandler> handlers = Stream
					.concat(mapping.getHandlerMap().values().stream(), mapping.getPathPatternHandlerMap().values().stream())
					.filter(ResourceHttpRequestHandler.class::isInstance)
					.map(ResourceHttpRequestHandler.class::cast)
					.toList();

				assertThat(handlers).hasSizeGreaterThanOrEqualTo(2);
				assertThat(handlers).allSatisfy(handler -> {
					assertThat(handler.getEtagGenerator()).isInstanceOf(ContentEtag.class);
					assertThat(handler.isUseLastModified()).isFalse();
				});
			});
	}

	@Test
	void leavesOtherBeansAlone() {
		Object bean = new Object();
		assertThat(new StaticResourceEtags().postProcessAfterInitialization(bean, "other")).isSameAs(bean);
	}

	@Test
	void isABeanOfTheWebclientContext() {
		assertThat(SevenWebclientContext.staticResourceEtags()).isInstanceOf(StaticResourceEtags.class);
	}
}
