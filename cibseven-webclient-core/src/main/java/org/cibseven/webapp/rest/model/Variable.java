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
package org.cibseven.webapp.rest.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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

	public void deserializeValue() {
		if (value == null || isNull()) {
			return;
		}
		if (value instanceof String) {
			return; // already a string
		}

		if ("json".equalsIgnoreCase(type)) {
			try {
				// obj is your Java object
				ObjectMapper mapper = new ObjectMapper();

				// Convert to JsonNode (real JSON object)
				JsonNode jsonNode = mapper.valueToTree(value);

				// Now convert JsonNode to pretty or compact JSON string
				value = mapper.writeValueAsString(jsonNode);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		}
	}
}
