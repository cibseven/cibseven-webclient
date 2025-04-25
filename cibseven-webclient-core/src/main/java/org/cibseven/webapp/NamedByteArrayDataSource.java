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
