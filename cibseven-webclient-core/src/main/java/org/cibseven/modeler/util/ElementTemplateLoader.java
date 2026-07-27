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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
public class ElementTemplateLoader implements InitializingBean {

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

	@Override
	public void afterPropertiesSet() throws Exception {
		if (properties.getPaths() == null || properties.getPaths().isEmpty()) {
			return;
		}

		// The MOD_ELEMENT_TEMPLATES table is created by the process engine (MyBatis), which
		// runs outside the Spring context and cannot be ordered against this deployment. Wait
		// for it to exist before loading; if it never appears, log and skip rather than failing
		// the whole webclient deployment.
		if (!awaitElementTemplatesTable()) {
			log.error("Table MOD_ELEMENT_TEMPLATES is not available after {} attempt(s); "
					+ "skipping element template loading for this startup. The webclient will start "
					+ "without (re)loading element templates.", properties.getSchemaReadyMaxAttempts());
			return;
		}

		populateElementTemplatesDatabase();
	}

	/**
	 * Polls for the existence of the {@code MOD_ELEMENT_TEMPLATES} table. Each probe runs in its
	 * own transaction so a failed probe (table missing) is rolled back cleanly and never poisons a
	 * subsequent attempt or the later load. Returns {@code true} once the table is queryable, or
	 * {@code false} if it is still unavailable after the configured number of attempts.
	 */
	private boolean awaitElementTemplatesTable() {
		int maxAttempts = Math.max(1, properties.getSchemaReadyMaxAttempts());
		long delayMs = Math.max(0, properties.getSchemaReadyRetryDelayMs());

		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				transactionTemplate.execute(status -> elementTemplateRepository.count());
				if (attempt > 1) {
					log.info("Table MOD_ELEMENT_TEMPLATES became available after {} attempt(s).", attempt);
				}
				return true;
			} catch (Exception e) {
				log.info("Table MOD_ELEMENT_TEMPLATES not ready yet (attempt {}/{}): {}",
						attempt, maxAttempts, e.getMessage());
				if (attempt == maxAttempts) {
					break;
				}
				try {
					Thread.sleep(delayMs);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					log.warn("Interrupted while waiting for MOD_ELEMENT_TEMPLATES table; aborting load.");
					return false;
				}
			}
		}
		return false;
	}

	private void populateElementTemplatesDatabase() {
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

		List<JsonNode> templateNodes;
		try (InputStream inputStream = resource.getInputStream()) {
			log.info("Loading element templates from JSON at '{}'...", description);

			JsonNode root = mapper.readTree(inputStream);

			if (root.isArray()) {
				templateNodes = new ArrayList<>(root.size());
				root.forEach(templateNodes::add);
			} else if (root.isObject()) {
				templateNodes = List.of(root);
			} else {
				log.error("Expected JSON object or array in '{}', but found: {}", description, root.getNodeType());
				return;
			}
		} catch (IOException ioe) {
			log.error("Failed to read element templates JSON at '{}'", description, ioe);
			return;
		}

		int updated = 0;
		int inserted = 0;
		int failed = 0;

		for (JsonNode templateNode : templateNodes) {
			// Parse outside the transaction; only the DB upsert is transactional.
			ElementTemplate parsed;
			try {
				parsed = mapper.treeToValue(templateNode, ElementTemplate.class);
				parsed.setOrigin(ElementTemplateOrigin.DEFAULT_JSON);
				parsed.setContent(mapper.writeValueAsString(templateNode));
			} catch (Exception e) {
				failed++;
				log.error("Error parsing element template from '{}': {}", description, e.getMessage(), e);
				continue;
			}

			// Each template is upserted in its own transaction, so a failure rolls back only that
			// template and never poisons the next one or the rest of the batch.
			try {
				if (Boolean.TRUE.equals(transactionTemplate.execute(status -> upsertTemplate(parsed)))) {
					updated++;
				} else {
					inserted++;
				}
			} catch (Exception e) {
				failed++;
				log.error("Error saving element template '{}' from '{}': {}",
						parsed.getTemplateId(), description, e.getMessage(), e);
			}
		}

		log.info("Processed {} element templates from '{}': {} updated, {} inserted, {} failed",
				templateNodes.size(), description, updated, inserted, failed);
	}

	/**
	 * Inserts or updates a single element template. Runs inside the caller-provided transaction.
	 *
	 * @return {@code true} if an existing template was updated, {@code false} if a new one was inserted.
	 */
	private boolean upsertTemplate(ElementTemplate template) {
		ElementTemplate existing = elementTemplateRepository.findElementTemplateById(template.getTemplateId());
		if (existing != null) {
			template.setId(existing.getId());
			elementTemplateRepository.save(template);
			return true;
		}
		elementTemplateRepository.save(template);
		return false;
	}

}
