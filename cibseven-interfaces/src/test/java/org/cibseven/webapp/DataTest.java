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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.cibseven.webapp.exception.SystemException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;

public class DataTest {

	private static final byte[] CONTENT = "invoice-bytes".getBytes(StandardCharsets.UTF_8);

	private Data data(String name, String contentType) {
		ByteArrayResource resource = new ByteArrayResource(CONTENT);
		return new Data(name, contentType, resource, resource.contentLength());
	}

	@Test
	void exposesTheNameContentTypeAndSize() {
		Data data = data("invoice.bpmn", "application/bpmn+xml");

		assertThat(data.getName()).isEqualTo("invoice.bpmn");
		assertThat(data.getContentType()).isEqualTo("application/bpmn+xml");
		assertThat(data.getSize()).isEqualTo(CONTENT.length);
	}

	@Test
	void streamsTheUnderlyingContent() throws Exception {
		try (InputStream in = data("a.txt", "text/plain").getInputStream()) {
			assertThat(in.readAllBytes()).isEqualTo(CONTENT);
		}
	}

	@Test
	void isReadOnly_getOutputStreamIsUnsupported() {
		// unlike NamedByteArrayDataSource, which returns null, this one refuses properly
		assertThatThrownBy(() -> data("a.txt", "text/plain").getOutputStream())
			.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void setContentType_overridesTheDeclaredType() {
		Data data = data("a.bin", "application/octet-stream");

		data.setContentType("text/plain");

		assertThat(data.getContentType()).isEqualTo("text/plain");
	}

	@Test
	void streamTo_writesTheContentToAnOutputStream() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		ByteArrayOutputStream returned = data("a.txt", "text/plain").streamTo(out);

		assertThat(returned).isSameAs(out);
		assertThat(out.toByteArray()).isEqualTo(CONTENT);
	}

	@Test
	void streamTo_writesTheContentToAPath(@TempDir Path tempDir) throws Exception {
		Path target = tempDir.resolve("out.txt");

		Path returned = data("a.txt", "text/plain").streamTo(target);

		assertThat(returned).isEqualTo(target);
		assertThat(Files.readAllBytes(target)).isEqualTo(CONTENT);
	}

	@Test
	void streamTo_wrapsAnUnreadableSourceInSystemException() {
		InputStreamSource failing = () -> {
			throw new java.io.IOException("disk gone");
		};
		Data data = new Data("a.txt", "text/plain", failing, -1);

		assertThatThrownBy(() -> data.streamTo(new ByteArrayOutputStream()))
			.isInstanceOf(SystemException.class);
	}

	@Test
	void from_readsAFileFromDisk(@TempDir Path tempDir) throws Exception {
		Path source = Files.write(tempDir.resolve("invoice.bpmn"), CONTENT);

		Data data = Data.from(source, "application/bpmn+xml");

		assertThat(data.getName()).isEqualTo("invoice.bpmn");
		assertThat(data.getContentType()).isEqualTo("application/bpmn+xml");
		assertThat(data.getSize()).isEqualTo(CONTENT.length);
		try (InputStream in = data.getInputStream()) {
			assertThat(in.readAllBytes()).isEqualTo(CONTENT);
		}
	}

	@Test
	void from_wrapsAMissingFileInSystemException(@TempDir Path tempDir) {
		assertThatThrownBy(() -> Data.from(tempDir.resolve("absent.txt"), "text/plain"))
			.isInstanceOf(SystemException.class);
	}

	@Test
	void renamingConstructor_takesTheContentTypeFromTheSourceAndReportsAnUnknownSize() throws Exception {
		NamedByteArrayDataSource source =
			new NamedByteArrayDataSource("original.txt", "text/plain", CONTENT);

		Data data = new Data("renamed.txt", source);

		assertThat(data.getName()).isEqualTo("renamed.txt");
		assertThat(data.getContentType()).isEqualTo("text/plain");
		// the size of a DataSource is not known up front
		assertThat(data.getSize()).isEqualTo(-1);
		try (InputStream in = data.getInputStream()) {
			assertThat(in.readAllBytes()).isEqualTo(CONTENT);
		}
	}

	@Test
	void acceptsAStreamSourceDirectly() throws Exception {
		Data data = new Data("a.txt", "text/plain", () -> new ByteArrayInputStream(CONTENT), CONTENT.length);

		try (InputStream in = data.getInputStream()) {
			assertThat(in.readAllBytes()).isEqualTo(CONTENT);
		}
	}
}
