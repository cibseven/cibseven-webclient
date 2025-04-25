/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cibseven.webapp.rest;

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
@RestController @RequestMapping("${services.basePath:/services/v1}" + "/info") @Slf4j
public class InfoService extends BaseService {	
	
	@Value("${ui.element.template.url:}") private String uiElementTemplateUrl;
	@Value("${cockpit.url:}") private String cockpitUrl;
	@Value("${theme:}") private String theme;
	@Value("${sso.active:false}") private boolean ssoActive;
	@Value("${sso.endpoints.authorization:}") private String authorizationEndpoint;
	@Value("${sso.clientId:}") private String clientId;
	@Value("${sso.scopes:}") private String scopes;
	@Value("${camunda.historyLevel:}") private String camundaHistoryLevel;
	@Value("${user.provider:}") private String userProvider;
	
	@Value("${flow.link.terms:}") private String flowLinkTerms;
	@Value("${flow.link.privacy:}") private String flowLinkPrivacy;
	@Value("${flow.link.imprint:}") private String flowLinkImprint;
	@Value("${flow.link.accessibility:}") private String flowLinkAccessibility;
	@Value("${flow.link.help:}") private String flowLinkHelp;
	
	@Value("${productNamePageTitle:CIB seven}") private String productNamePageTitle;
	
	@Value("${support-dialog:}") private String supportDialog;
	@Value("${services.basePath:services/v1}") private String servicesBasePath;
	
	
	@Operation(
			summary = "Get info version",
			description = "<strong>Return: Info (SNAPSHOT) version")
	@GetMapping
	public String getImplementationVersion() {
		return InfoService.class.getPackage().getImplementationVersion();
	}
	
	@Operation(
			summary = "Get config JSON",
			description = "<strong>Return: Config JSON object")
	@GetMapping("/properties")
	public ObjectNode getConfig() {
		ObjectNode configJson = JsonNodeFactory.instance.objectNode();
		configJson.put("uiElementTemplateUrl", uiElementTemplateUrl);
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
