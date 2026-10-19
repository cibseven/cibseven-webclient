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
package org.cibseven.webapp.rest;

import java.util.Objects;

import org.cibseven.webapp.auth.SevenUserProvider;
import org.cibseven.webapp.config.SevenWebclientProperties;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.providers.IEngineProvider;
import org.cibseven.webapp.rest.model.EngineConfiguration;
import org.cibseven.webapp.rest.model.InfoVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;

@ApiResponses({ @ApiResponse(responseCode= "500", description = "An unexpected system error occured") })
@RestController @RequestMapping("/info") @Slf4j
public class InfoService extends BaseService {	
	
	/**
	 * Legacy fallback used only with engine-rest versions prior to 2.2.0.
	 *
	 * Since 2.2.0, the authorizationEnabled is exposed by the engine configuration endpoint,
	 * allowing it to be resolved dynamically. This property is therefore used only
	 * when communicating with older engine-rest versions that do not support that endpoint,
	 * keeping the "/info" endpoint backward-compatible without requiring the authorizationEnabled
	 * to be maintained in the application configuration.
	 */
	@Value("${camunda.bpm.authorization.enabled:true}") private boolean authorizationEnabled;
	
	@Autowired
	InfoVersion infoVersion;

	@Autowired
	BpmProvider bpmProvider;

	@Operation(
			summary = "Get info version",
			description = "<strong>Return: Info version")
	@GetMapping
	public String getImplementationVersion() {
		return infoVersion.getVersion();
	}
	
	@Operation(
			summary = "Get properties for webclient configuration and engine configuration",
			description = "<strong>Return: JSON object")
	@GetMapping("/properties")
	public ObjectNode getConfig(
		@Parameter(description = "Optional engine definition to get configuration for. If not provided, default engine configuration will be returned")
		@RequestHeader(value = "X-Process-Engine", required = false) String engine
	) {

		// when newer middleware is connected to old engine-rest,
		// the engine configuration endpoint might not be available (404).
		// In this case we will get properties from old configuration properties (history level, authorization enabled) and use them to create a default engine configuration object
		EngineConfiguration engineConfig =
			IEngineProvider.isEngineUnspecified(engine)
				? bpmProvider.getEffectiveDefaultEngineConfiguration()
				: bpmProvider.getEngineConfiguration(engine);
		if (engineConfig == null) {
			// when newer middleware is connected to old engine-rest,
			// the engine configuration endpoint may not exist yet (404).
			// In that case we fallback to legacy configuration properties.
			log.warn(
				"engine-rest does not support the configuration endpoint, falling back to legacy configuration"
			);

			engineConfig = new EngineConfiguration();
			engineConfig.setHistoryLevel(properties.getHistoryLevel());
			engineConfig.setAuthorizationEnabled(authorizationEnabled);
			// Before 2.2.0 passwordPolicyEnabled was specified inside the config.json.
			// Since 2.2.0, it is available only in the engine configuration endpoint.
			// But since 2.2.0 it is not used anymore on webclient, so we can skip it here,
			// making the whole this "/info" endpoint compatible with both old and new versions of engine-rest without the need to maintain the password policy property in the configuration file.
			//
			// disabled (no need right now):
			// `engineConfig.setEnablePasswordPolicy(false);`
		}

		SevenWebclientProperties.User user = properties.getUser();
		SevenWebclientProperties.Link link = properties.getLink();
		SevenWebclientProperties.Sso sso = properties.getSso();
		// If userEditable is not configured, set it based on userProvider
		Boolean userEditable = user.getEditable() != null
			? user.getEditable()
			: SevenUserProvider.class.getName().equals(user.getProvider());

		ObjectNode configJson = JsonNodeFactory.instance.objectNode();
		configJson.put("theme", properties.getTheme());
		configJson.put("ssoActive", sso.isActive());
		configJson.put("camundaHistoryLevel", engineConfig.getHistoryLevel());
		configJson.put("userProvider", user.getProvider());
		configJson.put("userEditable", userEditable);
		configJson.put("userPasswordChangeEnabled", user.getUserPasswordChangeEnabled());
		// disabled (no need right now):
		// `configJson.put("passwordPolicyEnabled", engineConfig.isEnablePasswordPolicy());`
		configJson.put("flowLinkTerms", link.getTerms());
		configJson.put("flowLinkPrivacy", link.getPrivacy());
		configJson.put("flowLinkImprint", link.getImprint());
		configJson.put("flowLinkAccessibility", link.getAccessibility());
		configJson.put("flowLinkHelp", link.getHelp());
		configJson.put("productNamePageTitle", properties.getProductNamePageTitle());
		configJson.put("servicesBasePath", properties.getServices().getBasePath());

		configJson.put("engineRestPath", properties.getEngineRest().getPath());
		configJson.put("engineRestUrl", properties.getEngineRest().getUrl());
		configJson.put("authorizationEnabled", engineConfig.isAuthorizationEnabled() || properties.getLegacy().getAuthorization().isEnabled());
		configJson.put("modelerEnabled", properties.getModeler().isEnabled());
		configJson.put("pluginsEnabled", properties.getPlugins().isEnabled());
		configJson.put("startProcessDefaultFormEnabled", properties.getStartProcess().isDefaultFormEnabled());
		
        try {
            ObjectMapper mapper = new ObjectMapper();
        	JsonNode supportDialogJson = mapper.readTree(properties.getSupportDialog());
			configJson.set("supportDialog", supportDialogJson);
		} catch (JsonProcessingException e) {
			log.warn("Property support dialog is not set or incorrect");
		}
       
		if (sso.isActive()) {
			configJson.put("authorizationEndpoint", sso.getEndpoints().getAuthorization());
			configJson.put("clientId", Objects.toString(sso.getClientId(), ""));
			configJson.put("scopes", sso.getScopes());
		}
		return configJson;
	}
	
}
