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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class NamedByteArrayDataSourceTest {

	@Test
	public void exposesTheNameContentTypeAndContentItWasBuiltWith() throws Exception {
		byte[] content = "hello".getBytes(StandardCharsets.UTF_8);

		NamedByteArrayDataSource dataSource = new NamedByteArrayDataSource("report.pdf", "application/pdf", content);

		assertEquals("report.pdf", dataSource.getName());
		assertEquals("application/pdf", dataSource.getContentType());
		assertArrayEquals(content, dataSource.getContent());
	}

	@Test
	public void streamsTheContentBackAndCanBeReadMoreThanOnce() throws Exception {
		byte[] content = "attachment-bytes".getBytes(StandardCharsets.UTF_8);
		NamedByteArrayDataSource dataSource = new NamedByteArrayDataSource("a.txt", "text/plain", content);

		// Each call must hand out a fresh stream - the attachment endpoints read the same
		// DataSource more than once (once to size it, once to write it out).
		try (InputStream first = dataSource.getInputStream(); InputStream second = dataSource.getInputStream()) {
			assertArrayEquals(content, first.readAllBytes());
			assertArrayEquals(content, second.readAllBytes());
		}
	}

	@Test
	public void isReadOnly_getOutputStreamReturnsNull() throws Exception {
		NamedByteArrayDataSource dataSource = new NamedByteArrayDataSource("a.txt", "text/plain", new byte[0]);

		// Pinning current behaviour: jakarta.activation.DataSource asks implementations to throw
		// IOException when writing is unsupported, but this one returns null instead. Callers that
		// follow the interface contract would NPE rather than see an IOException.
		assertNull(dataSource.getOutputStream());
	}

	@Test
	public void acceptsEmptyContent() throws Exception {
		NamedByteArrayDataSource dataSource = new NamedByteArrayDataSource("empty.bin", "application/octet-stream", new byte[0]);

		try (InputStream in = dataSource.getInputStream()) {
			assertEquals(0, in.readAllBytes().length);
		}
	}
}
