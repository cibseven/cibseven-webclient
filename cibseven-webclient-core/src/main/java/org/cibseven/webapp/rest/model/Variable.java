package org.cibseven.webapp.rest.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Variable {
	private String name;
	private String type;
	private Object value;

	private Map<String, String> valueInfo = new HashMap<>();
	
	//Used fields: filename, mimeType
	//private ValueInfo valueInfo = new ValueInfo();
	
	@JsonIgnore
	public String getFilename() {
		//return valueInfo.getFilename();
		return valueInfo.get("filename");
	}
	
	@JsonIgnore
	public String getMimeType() {
		return valueInfo.get("mimeType");
		//return valueInfo.getMimeType();
	}
	
	@JsonIgnore
	public String getObjectTypeName() {
		//return valueInfo.getObjectTypeName();
		return valueInfo.get("objectTypeName");
	}
	
	@JsonIgnore
	public void setFilename(String value) {
		valueInfo.put("filename", value);
	}
	
	@JsonIgnore
	public void setMimeType(String value) {
		valueInfo.put("mimeType", value);
	}
	
	@JsonIgnore
	public String setObjectTypeName(String value) {
		return valueInfo.put("objectTypeName", value);
	}
	
	@JsonIgnore
	public boolean isNull() {
		return "Null".equals(type);
	}
	
	@JsonIgnore
	public boolean isValueNull() {
		return value == null;
	}

	public String asJson() throws JsonProcessingException {
		return new ObjectMapper().writeValueAsString(this);
	}
	
	/*
	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	@NoArgsConstructor
	public static class ValueInfo {
		String filename;
		String mimeType;
		String objectTypeName;
	}
	*/
}
