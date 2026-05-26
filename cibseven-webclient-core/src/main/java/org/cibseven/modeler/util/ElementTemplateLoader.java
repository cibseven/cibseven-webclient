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
package org.cibseven.modeler.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.cibseven.modeler.config.ElementTemplateProperties;
import org.cibseven.modeler.model.ElementTemplate;
import org.cibseven.modeler.model.ElementTemplateOrigin;
import org.cibseven.modeler.repository.ElementTemplateRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnProperty(
    prefix = "cibseven.webclient.modeler",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
@Slf4j
public class ElementTemplateLoader {

	private final ObjectMapper mapper;
	private final ElementTemplateRepository elementTemplateRepository;
	private final ElementTemplateProperties properties;
	private final TransactionTemplate transactionTemplate;
	private final ResourcePatternResolver resourcePatternResolver;

	public ElementTemplateLoader(ElementTemplateRepository elementTemplateRepository, ObjectMapper mapper,
			ElementTemplateProperties properties, TransactionTemplate transactionTemplate,
			ResourcePatternResolver resourcePatternResolver) {
		this.elementTemplateRepository = elementTemplateRepository;
		this.mapper = mapper;
		this.properties = properties;
		this.transactionTemplate = transactionTemplate;
		this.resourcePatternResolver = resourcePatternResolver;
	}

	// Defer to context refresh: the engine creates the MOD_* tables during ProcessEngine
	// bootstrap (ProcessEngineFactoryBean.getObject -> dbSchemaCreateModeler), which runs
	// while singletons are being instantiated. Querying them in afterPropertiesSet races
	// the engine and fails on a fresh database.
	@EventListener
	public void onContextRefreshed(ContextRefreshedEvent event) {
		if (event.getApplicationContext().getParent() != null) {
			return;
		}
		transactionTemplate.executeWithoutResult(status -> populateElementTemplatesDatabase());
	}

	private void populateElementTemplatesDatabase() {
		if (properties.getPaths() == null) {
			return;
		}
		for (String path : properties.getPaths()) {
			loadElementTemplatesFromPath(path);
		}
	}

	private void loadElementTemplatesFromPath(String path) {
		log.debug("Loading element templates from path: {}", path);
		assert (path != null && !path.isBlank());

		Resource[] resources;
		try {
			resources = resourcePatternResolver.getResources(path);
		} catch (IOException ioe) {
			log.error("Failed to resolve element templates location '{}'", path, ioe);
			return;
		}

		if (resources.length == 0) {
			log.warn("No element templates matched location '{}'. Skipping.", path);
			return;
		}

		for (Resource resource : resources) {
			loadElementTemplatesFromResource(resource);
		}
	}

	private void loadElementTemplatesFromResource(Resource resource) {
		String description = resource.getDescription();

		try (InputStream inputStream = resource.getInputStream()) {
			log.info("Loading element templates from JSON at '{}'...", description);

			JsonNode root = mapper.readTree(inputStream);

			List<JsonNode> templateNodes;
			if (root.isArray()) {
				templateNodes = new ArrayList<>(root.size());
				root.forEach(templateNodes::add);
			} else if (root.isObject()) {
				templateNodes = List.of(root);
			} else {
				log.error("Expected JSON object or array in '{}', but found: {}", description, root.getNodeType());
				return;
			}

			List<ElementTemplate> newTemplates = new ArrayList<>();
			int updated = 0;

			for (JsonNode templateNode : templateNodes) {
				try {
					ElementTemplate jsonTemplate = mapper.treeToValue(templateNode, ElementTemplate.class);
					jsonTemplate.setOrigin(ElementTemplateOrigin.DEFAULT_JSON);
					jsonTemplate.setContent(mapper.writeValueAsString(templateNode));

					ElementTemplate existing = elementTemplateRepository
							.findElementTemplateById(jsonTemplate.getTemplateId());

					if (existing != null) {
						jsonTemplate.setId(existing.getId());
						elementTemplateRepository.save(jsonTemplate);
						updated++;
					} else {
						newTemplates.add(jsonTemplate);
					}
				} catch (Exception e) {
					log.error("Error processing template from '{}': {}", description, e.getMessage(), e);
				}
			}

			if (!newTemplates.isEmpty()) {
				elementTemplateRepository.saveAll(newTemplates);
			}

			log.info("Successfully loaded {} element templates from '{}': {} updated, {} inserted",
					templateNodes.size(), description, updated, newTemplates.size());
		} catch (IOException ioe) {
			log.error("Failed to read element templates JSON at '{}'", description, ioe);
		}
	}

}
