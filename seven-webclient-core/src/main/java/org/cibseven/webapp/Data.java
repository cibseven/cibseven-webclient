package org.cibseven.webapp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.activation.DataSource;

import org.cibseven.webapp.exception.SystemException;
import org.springframework.core.io.InputStreamSource;
import org.springframework.util.StreamUtils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@AllArgsConstructor
public class Data implements org.springframework.core.io.InputStreamSource, DataSource {
	
	String name;
	@Setter String contentType;
	@Getter @NonNull InputStreamSource input;	
	@Getter long size;	
	
	public Data(String rename, DataSource ds) {
		this(rename, ds.getContentType(), ds::getInputStream, -1);
	}
	
	public static Data from(Path pth, String contentType) {
		try {
			return new Data(pth.getFileName().toString(), contentType, () -> Files.newInputStream(pth), Files.size(pth));
		} catch (IOException x) {
			throw new SystemException(x);
		}
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return input.getInputStream();
	}
	
	public Path streamTo(@NonNull Path path) {
		try (OutputStream out = Files.newOutputStream(path)) {
			streamTo(out);
			return path;
		} catch (IOException x) {
			throw new SystemException(x);
		}
	}
	
	public <T extends OutputStream> T streamTo(@NonNull T out) {
		try (InputStream in = getInputStream()) {
			StreamUtils.copy(in, out);
		} catch (IOException x) {
			throw new SystemException(x);
		}
		return out;
	}	

}
