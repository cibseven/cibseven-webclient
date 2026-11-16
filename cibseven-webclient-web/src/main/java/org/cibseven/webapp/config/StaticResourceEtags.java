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
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.io.Resource;
import org.springframework.util.DigestUtils;
import org.springframework.web.servlet.handler.AbstractUrlHandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * Lets a browser tell a changed static file from an unchanged one by its content instead of its modification
 * time.
 * <p>
 * The bundles carry a content hash in their names, but the files a theme or a customer replaces under the same
 * name do not: the logo, the favicon, the translation files, {@code config.json}. Spring's resource handler
 * revalidates by modification time only, so a replaced logo whose time did not move forward (a reproducible
 * build, a copy that keeps the time, a file older than the cached one) was answered with "304 Not Modified",
 * and the browser kept showing the old logo until the user cleared the cache.
 * <p>
 * This post-processor therefore sets every {@link ResourceHttpRequestHandler} of the application (the static
 * files and the webjars Spring Boot serves) to answer with an ETag computed from the content
 * ({@link ContentEtag}) and to ignore the modification time. A changed file is sent, an unchanged one is still
 * answered "304 Not Modified", and a browser that cached a file before, with its time only, gets it once more.
 * Being the handler's own validator, the ETag is the same for GET, HEAD and range requests, and controllers are
 * not touched.
 */
@Slf4j
public class StaticResourceEtags implements BeanPostProcessor {

	private final ContentEtag etagGenerator = new ContentEtag();

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		if (bean instanceof AbstractUrlHandlerMapping mapping) {
			Stream.concat(mapping.getHandlerMap().values().stream(), mapping.getPathPatternHandlerMap().values().stream())
				.filter(ResourceHttpRequestHandler.class::isInstance)
				.map(ResourceHttpRequestHandler.class::cast)
				.forEach(this::configure);
		}
		return bean;
	}

	/** The handler validates by the ETag of the content alone. */
	void configure(ResourceHttpRequestHandler handler) {
		handler.setEtagGenerator(etagGenerator);
		handler.setUseLastModified(false);
	}

	/**
	 * The MD5 of a file's content. A small file is read on every request, so a replaced one always gets a new
	 * ETag. A large one (the bundles) is read once per version: its ETag is kept for as long as its modification
	 * time and length stay the same, which within one running application means the same file. A new
	 * deployment starts with nothing kept.
	 */
	static final class ContentEtag implements Function<Resource, String> {

		/** Up to this length a file is hashed on every request (bytes) */
		static final long HASH_EVERY_TIME_UP_TO = 256 * 1024;

		private record Version(long lastModified, long length, String etag) {}

		private final Map<String, Version> large = new ConcurrentHashMap<>();

		@Override
		public String apply(Resource resource) {
			try {
				long length = resource.contentLength();
				if (length <= HASH_EVERY_TIME_UP_TO) {
					return hash(resource);
				}
				String key = resource.getURL().toString();
				long lastModified = resource.lastModified();
				Version kept = large.get(key);
				if (kept != null && kept.lastModified() == lastModified && kept.length() == length) {
					return kept.etag();
				}
				String etag = hash(resource);
				large.put(key, new Version(lastModified, length, etag));
				return etag;
			} catch (IOException e) {
				// Without an ETag the file is simply sent in full
				log.debug("No ETag for {}", resource, e);
				return null;
			}
		}

		private static String hash(Resource resource) throws IOException {
			try (InputStream content = resource.getInputStream()) {
				return DigestUtils.md5DigestAsHex(content);
			}
		}
	}
}
