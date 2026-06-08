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

import org.cibseven.webapp.auth.SevenUserProvider;
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

import jakarta.annotation.PostConstruct;

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
	
	@Value("${cibseven.webclient.theme:cib}") private String theme;
	@Value("${cibseven.webclient.sso.active:false}") private boolean ssoActive;
	@Value("${cibseven.webclient.sso.endpoints.authorization:}") private String authorizationEndpoint;
	@Value("${cibseven.webclient.sso.clientId:}") private String clientId;
	@Value("${cibseven.webclient.sso.scopes:}") private String scopes;
	/**
	 * Legacy fallback used only with engine-rest versions prior to 2.2.0.
	 *
	 * Since 2.2.0, the history level is exposed by the engine configuration endpoint,
	 * allowing it to be resolved dynamically. This property is therefore used only
	 * when communicating with older engine-rest versions that do not support that endpoint,
	 * keeping the "/info" endpoint backward-compatible without requiring the history level
	 * to be maintained in the application configuration.
	 */
	@Value("${cibseven.webclient.historyLevel:full}") private String camundaHistoryLevel;
	@Value("${cibseven.webclient.user.provider:org.cibseven.webapp.auth.SevenUserProvider}") private String userProvider;
	@Value("${cibseven.webclient.user.editable:#{null}}") private Boolean userEditable;
	@Value("${cibseven.webclient.user.userPasswordChangeEnabled:}") private Boolean userPasswordChangeEnabled;
	
	@Value("${cibseven.webclient.productNamePageTitle:CIB seven}") private String productNamePageTitle;
	
	@Value("${cibseven.webclient.services.basePath:services/v1}") private String servicesBasePath;
	
	@Value("${cibseven.webclient.link.terms:}") private String flowLinkTerms;
	@Value("${cibseven.webclient.link.privacy:}") private String flowLinkPrivacy;
	@Value("${cibseven.webclient.link.imprint:}") private String flowLinkImprint;
	@Value("${cibseven.webclient.link.accessibility:}") private String flowLinkAccessibility;
	@Value("${cibseven.webclient.link.help:}") private String flowLinkHelp;
	@Value("${cibseven.webclient.support-dialog:}") private String supportDialog;
	@Value("${cibseven.webclient.engineRest.path:/engine-rest}") private String engineRestPath;
	@Value("${cibseven.webclient.engineRest.url:./}") private String engineRestUrl;
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
	@Value("${cibseven.webclient.legacy.authorization.enabled:false}") private boolean legacyAuthorizationEnabled;
	@Value("${cibseven.webclient.modeler.enabled:false}") private boolean modelerEnabled;
	
	@Autowired
	InfoVersion infoVersion;

	@Autowired
	BpmProvider bpmProvider;

	@PostConstruct
	public void init() {
		// If userEditable is not set in yaml, set it based on userProvider
		if (userEditable == null) {
			userEditable = SevenUserProvider.class.getName().equals(userProvider);
		}
	}
	
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

		// Route to the correct engine-rest: default engine uses getDefaultEngineConfiguration(),
		// all other engines (named or external url|path|engineName) use getEngineConfiguration(engine)
		// which resolves the URL via getNamedEngineRestUrl(). If the remote engine does not support
		// the /configuration endpoint (404/401), getEngineConfiguration returns null and we fall back
		// to legacy configuration properties below.
		EngineConfiguration engineConfig =
			IEngineProvider.isDefaultEngine(engine)
				? bpmProvider.getDefaultEngineConfiguration()
				: bpmProvider.getEngineConfiguration(engine);
		if (engineConfig == null) {
			// when newer middleware is connected to old engine-rest,
			// the engine configuration endpoint may not exist yet (404).
			// In that case we fallback to legacy configuration properties.
			log.warn(
				"engine-rest does not support the configuration endpoint, falling back to legacy configuration"
			);

			engineConfig = new EngineConfiguration();
			engineConfig.setHistoryLevel(camundaHistoryLevel);
			engineConfig.setAuthorizationEnabled(authorizationEnabled);
			// Before 2.2.0 passwordPolicyEnabled was specified inside the config.json.
			// Since 2.2.0, it is available only in the engine configuration endpoint.
			// But since 2.2.0 it is not used anymore on webclient, so we can skip it here,
			// making the whole this "/info" endpoint compatible with both old and new versions of engine-rest without the need to maintain the password policy property in the configuration file.
			//
			// disabled (no need right now):
			// `engineConfig.setEnablePasswordPolicy(false);`
		}

		ObjectNode configJson = JsonNodeFactory.instance.objectNode();
		configJson.put("theme", theme);
		configJson.put("ssoActive", ssoActive);
		configJson.put("camundaHistoryLevel", engineConfig.getHistoryLevel());
		configJson.put("userProvider", userProvider);
		configJson.put("userEditable", userEditable);
		configJson.put("userPasswordChangeEnabled", userPasswordChangeEnabled);
		// disabled (no need right now):
		// `configJson.put("passwordPolicyEnabled", engineConfig.isEnablePasswordPolicy());`
		configJson.put("flowLinkTerms", flowLinkTerms);
		configJson.put("flowLinkPrivacy", flowLinkPrivacy);
		configJson.put("flowLinkImprint", flowLinkImprint);
		configJson.put("flowLinkAccessibility", flowLinkAccessibility);
		configJson.put("flowLinkHelp", flowLinkHelp);
		configJson.put("productNamePageTitle", productNamePageTitle);
		configJson.put("servicesBasePath", servicesBasePath);
		
		configJson.put("engineRestPath", engineRestPath);
		configJson.put("engineRestUrl", engineRestUrl);
		configJson.put("authorizationEnabled", engineConfig.isAuthorizationEnabled() || legacyAuthorizationEnabled);
		configJson.put("modelerEnabled", modelerEnabled);
		
        try {
            ObjectMapper mapper = new ObjectMapper();
        	JsonNode supportDialogJson = mapper.readTree(supportDialog);
			configJson.set("supportDialog", supportDialogJson);
		} catch (JsonProcessingException e) {
			log.warn("Property support dialog is not set or incorrect");
		}
       
		if (ssoActive) {
			configJson.put("authorizationEndpoint", authorizationEndpoint);
			configJson.put("clientId", clientId);
			configJson.put("scopes", scopes);
		}
		return configJson;
	}
	
}
