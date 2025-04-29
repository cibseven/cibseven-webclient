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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import jakarta.activation.DataSource;

import lombok.Getter;
import lombok.Setter;

public class NamedByteArrayDataSource implements DataSource, Serializable {
	private static final long serialVersionUID = 8390059640047573444L;

	@Getter @Setter
	private String name;
	
	@Getter @Setter
	private String contentType;
	
	@Getter @Setter
	private byte[] content;
	
	public NamedByteArrayDataSource(String name, String contentType, byte[] content) throws IOException {
		this.name = name;
		this.contentType = contentType;
		this.content = content;
	}
	
	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(content);
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return null;
	}

}
