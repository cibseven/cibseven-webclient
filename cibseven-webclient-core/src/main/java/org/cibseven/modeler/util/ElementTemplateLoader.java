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

import org.cibseven.webapp.config.SevenWebclientProperties;
import org.cibseven.webapp.config.WebclientJsonMapper;
import org.cibseven.webapp.providers.WebclientExtensions;
import org.cibseven.persistence.CibsevenJpa;
import org.cibseven.modeler.model.ElementTemplate;
import org.cibseven.modeler.model.ElementTemplateOrigin;
import org.cibseven.modeler.repository.ElementTemplateRepository;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnProperty(
    prefix = SevenWebclientProperties.MODELER_PREFIX,
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
@Slf4j
public class ElementTemplateLoader implements DisposableBean {

	private final JsonMapper mapper;
	private final ElementTemplateRepository elementTemplateRepository;
	private final SevenWebclientProperties.Modeler.Templates templates;
	private final TransactionTemplate transactionTemplate;
	private final ResourcePatternResolver resourcePatternResolver;
	private final WebclientExtensions webclientExtensions;
	private volatile Thread loading;

	/**
	 * @param transactionManager the modeler's own transaction manager. An unqualified
	 *        {@code TransactionTemplate} would be built on the primary transaction manager, which in
	 *        an embedding application belongs to the host and cannot run the modeler's JPA work.
	 */
	public ElementTemplateLoader(ElementTemplateRepository elementTemplateRepository,
			SevenWebclientProperties properties,
			@Qualifier(CibsevenJpa.TRANSACTION_MANAGER) PlatformTransactionManager transactionManager,
			ResourcePatternResolver resourcePatternResolver, WebclientExtensions webclientExtensions) {
		this.elementTemplateRepository = elementTemplateRepository;
		// The webclient's own mapper, which reads them like the uploads to its REST service
		this.mapper = WebclientJsonMapper.create(properties);
		this.templates = properties.getModeler().getTemplates();
		this.transactionTemplate = new TransactionTemplate(transactionManager);
		this.resourcePatternResolver = resourcePatternResolver;
		this.webclientExtensions = webclientExtensions;
	}

	/** What the wait for the table came to. */
	enum TableState { READY, SWITCHED_OFF, MISSING, STOPPED }

	/**
	 * Once the application is up, and in a thread of its own. The engine creates the table on its own
	 * schedule, and an engine-rest in the same server cannot even say whether it will before the
	 * webclient has started, so the start neither waits for the table nor depends on it.
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void loadInBackground() {
		if (templates.getPaths() == null || templates.getPaths().isEmpty()) {
			return;
		}
		Thread thread = new Thread(this::load, "cibseven-element-templates");
		thread.setDaemon(true);
		loading = thread;
		thread.start();
	}

	@Override
	public void destroy() {
		Thread thread = loading;
		if (thread != null) {
			thread.interrupt();
		}
	}

	/**
	 * Waits for the table, then loads the templates into it. If it never appears, this logs and skips
	 * rather than failing; an engine that reports the webclient tables as not there is not waited for.
	 */
	void load() {
		try {
			switch (awaitElementTemplatesTable()) {
				case READY -> populateElementTemplatesDatabase();
				case SWITCHED_OFF -> log.info("The engine has no webclient tables; no element templates are loaded");
				case MISSING -> log.error("Table MOD_ELEMENT_TEMPLATES is not available after {} attempt(s); "
						+ "skipping element template loading for this startup. The webclient runs "
						+ "without (re)loading element templates.", templates.getSchemaReadyMaxAttempts());
				case STOPPED -> log.debug("The webclient stops; element templates are not loaded");
			}
		} catch (RuntimeException e) {
			log.error("Loading the element templates failed", e);
		}
	}

	/**
	 * Polls for the existence of the {@code MOD_ELEMENT_TEMPLATES} table. Each probe runs in its
	 * own transaction so a failed probe (table missing) is rolled back cleanly and never poisons a
	 * subsequent attempt or the later load. Between probes the engine is asked again, as it may only
	 * answer once it has started.
	 */
	private TableState awaitElementTemplatesTable() {
		if (webclientExtensions.switchedOffNow()) {
			return TableState.SWITCHED_OFF;
		}
		int maxAttempts = Math.max(1, templates.getSchemaReadyMaxAttempts());
		long delayMs = Math.max(0, templates.getSchemaReadyRetryDelayMs());

		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				transactionTemplate.execute(status -> elementTemplateRepository.count());
				if (attempt > 1) {
					log.info("Table MOD_ELEMENT_TEMPLATES became available after {} attempt(s).", attempt);
				}
				return TableState.READY;
			} catch (Exception e) {
				log.info("Table MOD_ELEMENT_TEMPLATES not ready yet (attempt {}/{}): {}",
						attempt, maxAttempts, e.getMessage());
				if (webclientExtensions.switchedOffNow()) {
					return TableState.SWITCHED_OFF;
				}
				if (attempt == maxAttempts) {
					break;
				}
				try {
					Thread.sleep(delayMs);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					return TableState.STOPPED;
				}
			}
		}
		return TableState.MISSING;
	}

	private void populateElementTemplatesDatabase() {
		for (String path : templates.getPaths()) {
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
		} catch (IOException | JacksonException e) {
			log.error("Failed to read element templates JSON at '{}'", description, e);
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
