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

import org.cibseven.webapp.rest.model.InfoVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;

@ApiResponses({ @ApiResponse(responseCode= "500", description = "An unexpected system error occured") })
@RestController @RequestMapping("/info") @Slf4j
public class InfoService extends BaseService {	
	
	@Value("${cibseven.webclient.cockpit.url:./camunda/app/cockpit/default/}") private String cockpitUrl;
	@Value("${cibseven.webclient.theme:cib}") private String theme;
	@Value("${cibseven.webclient.sso.active:false}") private boolean ssoActive;
	@Value("${cibseven.webclient.sso.endpoints.authorization:}") private String authorizationEndpoint;
	@Value("${cibseven.webclient.sso.clientId:}") private String clientId;
	@Value("${cibseven.webclient.sso.scopes:}") private String scopes;
	@Value("${cibseven.webclient.historyLevel:full}") private String camundaHistoryLevel;
	@Value("${cibseven.webclient.user.provider:org.cibseven.webapp.auth.SevenUserProvider}") private String userProvider;
	
	@Value("${cibseven.webclient.productNamePageTitle:CIB seven}") private String productNamePageTitle;
	
	@Value("${cibseven.webclient.services.basePath:services/v1}") private String servicesBasePath;
	
	@Value("${cibseven.webclient.link.terms:}") private String flowLinkTerms;
	@Value("${cibseven.webclient.link.privacy:}") private String flowLinkPrivacy;
	@Value("${cibseven.webclient.link.imprint:}") private String flowLinkImprint;
	@Value("${cibseven.webclient.link.accessibility:}") private String flowLinkAccessibility;
	@Value("${cibseven.webclient.link.help:}") private String flowLinkHelp;
	@Value("${cibseven.webclient.support-dialog:}") private String supportDialog;
	@Value("${cibseven.webclient.engineRest.path:/engine-rest}") private String engineRestPath;
	
	@Autowired
	InfoVersion infoVersion;
	
	@Operation(
			summary = "Get info version",
			description = "<strong>Return: Info version")
	@GetMapping
	public String getImplementationVersion() {
		return infoVersion.getVersion();
	}
	
	@Operation(
			summary = "Get config JSON",
			description = "<strong>Return: Config JSON object")
	@GetMapping("/properties")
	public ObjectNode getConfig() {
		ObjectNode configJson = JsonNodeFactory.instance.objectNode();
		configJson.put("cockpitUrl", cockpitUrl);
		configJson.put("theme", theme);
		configJson.put("ssoActive", ssoActive);
		configJson.put("camundaHistoryLevel", camundaHistoryLevel);
		configJson.put("userProvider", userProvider);
		configJson.put("flowLinkTerms", flowLinkTerms);
		configJson.put("flowLinkPrivacy", flowLinkPrivacy);
		configJson.put("flowLinkImprint", flowLinkImprint);
		configJson.put("flowLinkAccessibility", flowLinkAccessibility);
		configJson.put("flowLinkHelp", flowLinkHelp);
		configJson.put("productNamePageTitle", productNamePageTitle);
		configJson.put("servicesBasePath", servicesBasePath);
		
		configJson.put("engineRestPath", engineRestPath);
		
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
