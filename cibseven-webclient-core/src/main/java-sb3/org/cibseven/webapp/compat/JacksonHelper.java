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
package org.cibseven.webapp.compat;

import org.cibseven.webapp.exception.SystemException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Jackson compatibility helper for Spring Boot 3 (Jackson 2 / com.fasterxml.jackson).
 * Provides version-independent JSON serialization/deserialization.
 */
public final class JacksonHelper {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private JacksonHelper() {}

	public static String toJson(Object obj) {
		try {
			return MAPPER.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			throw new SystemException(e);
		}
	}

	public static <T> T fromJson(String json, Class<T> clazz) {
		try {
			return MAPPER.readValue(json, clazz);
		} catch (JsonProcessingException e) {
			throw new SystemException(e);
		}
	}

	public static <T> T convertValue(Object fromValue, Class<T> toValueType) {
		return MAPPER.convertValue(fromValue, toValueType);
	}

	public static <T> T fromJsonWithMixin(String json, Class<T> clazz, Class<?> target, Class<?> mixin) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.addMixIn(target, mixin);
			return mapper.readValue(json, clazz);
		} catch (JsonProcessingException e) {
			throw new SystemException(e);
		}
	}

	public static String toJsonWithMixin(Object obj, Class<?> target, Class<?> mixin) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.addMixIn(target, mixin);
			return mapper.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			throw new SystemException(e);
		}
	}

}
