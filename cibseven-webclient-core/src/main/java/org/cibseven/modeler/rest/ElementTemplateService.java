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
package org.cibseven.modeler.rest;

import org.cibseven.modeler.model.ElementTemplate;
import org.cibseven.modeler.model.ElementTemplateOrigin;
import org.cibseven.modeler.provider.ElementTemplateProvider;
import org.cibseven.modeler.rest.dto.ElementTemplateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.NoObjectException;
import org.cibseven.webapp.rest.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for managing Element Templates in the CIB Seven Modeler application.
 * <p>
 * This service provides comprehensive CRUD operations and advanced functionality for
 * Element Templates, including search, filtering, bulk operations, validation,
 * import/export capabilities, and statistics reporting.
 * </p>
 * <p>
 * All endpoints require authentication when enabled via the configuration property
 * {@code cibsevenmodeler.authentication.enabled}. The service integrates with the
 * ElementTemplateProvider for data persistence operations.
 * </p>
 * 
 * @author CIB Seven Modeler Team
 * @version 1.0
 * @since 1.0
 */
@SuppressWarnings("unused")
@Service
@Slf4j
@Tag(name = "Element Template Service", description = "Comprehensive REST API for managing Element Templates with CRUD operations, search, filtering, bulk operations, and import/export functionality")
@ApiResponses({
	@ApiResponse(responseCode = "500", description = "An unexpected system error occurred", 
		content = @Content(schema = @Schema(implementation = String.class))),
	@ApiResponse(responseCode = "401", description = "Unauthorized, invalid or missing authentication token",
		content = @Content(schema = @Schema(implementation = String.class)))
})
@RestController @RequestMapping("${cibseven.webclient.services.basePath:/services/v1}/element-templates")
public class ElementTemplateService extends BaseService {

    @Autowired
    private ElementTemplateProvider templateProvider;

    @Value("${cibsevenmodeler.authentication.enabled:true}")
    private boolean authenticationEnabled;

    /**
     * Ensures that an Element Template exists with the given ID.
     * <p>
     * This is a utility method used internally by other service methods to validate
     * that a template exists before performing operations on it.
     * </p>
     * 
     * @param id the unique identifier of the element template to validate
     * @return the ElementTemplate if it exists
     * @throws NoObjectException if no template is found with the given ID
     */
    private ElementTemplate ensureIdExists(String id) throws NoObjectException {
    	log.debug("Validating existence of template with ID: {}", id);
    	final Optional<ElementTemplate> template = templateProvider.findById(id);
    	
    	if (template.isPresent()) {
    		log.debug("Template found with ID: {}", id);
        	return template.get();
    	}
    	log.warn("Template not found with ID: {}", id);
    	throw new NoObjectException(id);
    }

    /**
     * Retrieves all element templates from the system.
     * <p>
     * This endpoint returns a complete list of all element templates, regardless of their
     * active status. Authentication is required if enabled in the application configuration.
     * </p>
     * 
     * @param rq the HTTP servlet request containing authentication information
     * @return a list of all element templates in the system
     */
    @Operation(
        summary = "Get all element templates",
        description = "Retrieves a complete list of all element templates in the system, including both active and inactive templates"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved all element templates",
            content = @Content(schema = @Schema(implementation = ElementTemplate[].class))
        )
    })
    @GetMapping
    public List<ElementTemplate> getAllElementTemplates(HttpServletRequest rq) {
    	log.debug("Retrieving all element templates");
		if (authenticationEnabled) {
			checkAuthorization(rq, true);
		}
		List<ElementTemplate> templates = templateProvider.getElementTemplates();
		log.debug("Retrieved {} element templates", templates.size());
		return templates;
    }
    
    /**
     * Retrieves a specific element template by its unique identifier.
     * <p>
     * This endpoint returns detailed information about a single element template
     * identified by the provided ID. Authentication is required if enabled.
     * </p>
     * 
     * @param rq the HTTP servlet request containing authentication information
     * @param id the unique identifier of the element template to retrieve
     * @return the element template with the specified ID
     * @throws NoObjectException if no template is found with the given ID
     */
    @Operation(
        summary = "Get element template by ID",
        description = "Retrieves a specific element template using its unique identifier"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved the element template",
            content = @Content(schema = @Schema(implementation = ElementTemplate.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Element template not found with the specified ID",
            content = @Content(schema = @Schema(implementation = String.class))
        )
    })
    @GetMapping("/{id}")
    public ElementTemplate getElementTemplateById(
    	HttpServletRequest rq,
    	@Parameter(description = "Unique identifier of the element template", required = true)
    	@PathVariable String id
    ) throws NoObjectException {
    	log.info("Retrieving element template with ID: {}", id);
    	if (authenticationEnabled) {
			checkAuthorization(rq, true);
		}
    	
    	ElementTemplate template = ensureIdExists(id);
    	log.info("Successfully retrieved element template: {} (ID: {})", template.getName(), id);
    	return template;
    }
    
    /**
     * Creates a new element template in the system.
     * <p>
     * This endpoint creates a new element template with the provided details.
     * The template will be assigned a unique ID and creation timestamp automatically.
     * The creator information is extracted from the authenticated user context.
     * </p>
     * 
     * @param rq the HTTP servlet request containing authentication information
     * @param element the element template request containing template details to create
     * @return the newly created element template with generated ID and timestamps
     */
    @Operation(
        summary = "Create new element template",
        description = "Creates a new element template with the provided details. Auto-generates unique ID and timestamps."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Element template created successfully",
            content = @Content(schema = @Schema(implementation = ElementTemplate.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data or validation errors",
            content = @Content(schema = @Schema(implementation = String.class))
        )
    })
    @PostMapping
    public ElementTemplate add(
    	HttpServletRequest rq,
    	@Parameter(description = "Element template data to create", required = true)
    	@RequestBody ElementTemplateRequest element
    ) {
    	log.info("Creating new element template: {}", element.getName());
    	CIBUser user = null;
		if (authenticationEnabled) {
			user = checkAuthorization(rq, true);
		}
    	
    	ElementTemplate entity = new ElementTemplate();
    	
    	entity.setName(element.getName());
    	entity.setTemplateId(element.getTemplateId());
    	entity.setDescription(element.getDescription());
    	entity.setContent(element.getContent());
    	entity.setOrigin(element.getOrigin() != null ? element.getOrigin() : ElementTemplateOrigin.MANUAL);
    	entity.setCreatedBy(user != null ? user.getUserID() : "anonymous");
    	
    	ElementTemplate savedTemplate = templateProvider.addTemplate(entity);
    	log.info("Successfully created element template: {} with ID: {}", savedTemplate.getName(), savedTemplate.getId());
    	return savedTemplate; 
    }
    
    /**
     * Partially updates an element template with the provided properties.
     * <p>
     * This endpoint allows for selective updates of template fields without
     * requiring the entire template object. Only the properties provided in
     * the request body will be updated.
     * </p>
     * 
     * @param rq the HTTP servlet request containing authentication information
     * @param id the unique identifier of the template to update
     * @param properties a map of properties to update with their new values
     * @return ResponseEntity with the updated template if changes were made, or no content if no changes occurred
     * @throws NoObjectException if no template is found with the given ID
     */
    @Operation(
        summary = "Partially update element template",
        description = "Updates specific properties of an element template without requiring the complete template object"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Template updated successfully",
            content = @Content(schema = @Schema(implementation = ElementTemplate.class))
        ),
        @ApiResponse(
            responseCode = "204",
            description = "No modifications were performed"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Template not found",
            content = @Content(schema = @Schema(implementation = String.class))
        )
    })
    @PatchMapping("/{id}")
    public ResponseEntity<ElementTemplate> partialUpdate(
        HttpServletRequest rq, 
        @Parameter(description = "Unique identifier of the template to update", required = true)
        @PathVariable String id, 
        @Parameter(description = "Map of properties to update", required = true)
        @RequestBody Map<String, Object> properties
    ) throws NoObjectException {
    	log.info("Performing partial update on template with ID: {}", id);
    	CIBUser user = null;
    	if (authenticationEnabled) {
			user = checkAuthorization(rq, true);
		}
    	
    	final ElementTemplate template = ensureIdExists(id);
    	
    	// Set updated_by field if any modifications will be made
    	template.setUpdatedBy(user != null ? user.getUserID() : "anonymous");
    	
    	final Optional<ElementTemplate> updatedTemplate = templateProvider.partialUpdate(template, properties);

        if (updatedTemplate.isPresent()) {
            log.info("Successfully updated template: {} (ID: {})", updatedTemplate.get().getName(), id);
            return ResponseEntity.ok(updatedTemplate.get());
        } else {
            log.info("No modifications performed for template with ID: {}", id);
            return ResponseEntity.noContent().build();
        }
    }
    
    /**
     * Completely updates an element template with new data.
     * <p>
     * This endpoint performs a full update of an element template, replacing
     * all modifiable fields with the provided data. Read-only fields like
     * creation timestamp and creator are preserved.
     * </p>
     * 
     * @param rq the HTTP servlet request containing authentication information
     * @param id the unique identifier of the template to update
     * @param element the complete template data for the update
     * @return the updated element template
     * @throws NoObjectException if no template is found with the given ID
     */
    @Operation(
        summary = "Fully update element template",
        description = "Performs a complete update of an element template, replacing all modifiable fields"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Template updated successfully",
            content = @Content(schema = @Schema(implementation = ElementTemplate.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Template not found",
            content = @Content(schema = @Schema(implementation = String.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content(schema = @Schema(implementation = String.class))
        )
    })
    @PutMapping("/{id}")
    public ElementTemplate update(
    	HttpServletRequest rq,
        @Parameter(description = "Unique identifier of the template to update", required = true)
        @PathVariable String id,
        @Parameter(description = "Complete template data for the update", required = true)
        @RequestBody ElementTemplateRequest element
    ) throws NoObjectException {
    	log.info("Performing full update on template with ID: {}", id);
    	CIBUser user = null;
        if (authenticationEnabled) {
			user = checkAuthorization(rq, true);
		}
    	
    	ElementTemplate existingTemplate = ensureIdExists(id);
    	
    	// Update the template with new data
    	existingTemplate.setName(element.getName());
    	existingTemplate.setTemplateId(element.getTemplateId());
    	existingTemplate.setDescription(element.getDescription());
    	existingTemplate.setContent(element.getContent());
    	existingTemplate.setUpdatedBy(user != null ? user.getUserID() : "anonymous");
    	// Note: origin and createdBy are not updated as they are read-only
    	
    	ElementTemplate updatedTemplate = templateProvider.updateTemplate(id, existingTemplate);
    	log.info("Successfully updated template: {} (ID: {})", updatedTemplate.getName(), id);
    	return updatedTemplate;
    }
    
    /**
     * Deletes an element template from the system.
     * <p>
     * This endpoint permanently removes an element template identified by the given ID.
     * This operation cannot be undone.
     * </p>
     * 
     * @param rq the HTTP servlet request containing authentication information
     * @param id the unique identifier of the template to delete
     */
    @Operation(
        summary = "Delete element template",
        description = "Permanently deletes an element template from the system"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Template deleted successfully"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Template not found",
            content = @Content(schema = @Schema(implementation = String.class))
        )
    })
    @DeleteMapping("/{id}")
    public void delete(
    	HttpServletRequest rq,
    	@Parameter(description = "Unique identifier of the template to delete", required = true)
    	@PathVariable String id
    ) {
    	log.info("Deleting template with ID: {}", id);
    	if (authenticationEnabled) {
			checkAuthorization(rq, true);
		}
    	templateProvider.deleteTemplateById(id);
    	log.info("Successfully deleted template with ID: {}", id);
    }
    
    /**
     * Creates a duplicate of an existing element template.
     * <p>
     * This endpoint creates a new template based on an existing one, with a modified
     * name and template ID to ensure uniqueness. The duplicated template will have
     * the same content and properties as the original but with new identifiers.
     * </p>
     * 
     * @param rq the HTTP servlet request containing authentication information
     * @param id the unique identifier of the template to duplicate
     * @return the newly created duplicate template
     * @throws NoObjectException if no template is found with the given ID
     */
    @Operation(
        summary = "Duplicate element template",
        description = "Creates a duplicate of an existing element template with modified identifiers"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Template duplicated successfully",
            content = @Content(schema = @Schema(implementation = ElementTemplate.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Original template not found",
            content = @Content(schema = @Schema(implementation = String.class))
        )
    })
    @PostMapping("/{id}/duplicate")
    public ElementTemplate duplicateTemplate(
        HttpServletRequest rq,
        @Parameter(description = "Unique identifier of the template to duplicate", required = true)
        @PathVariable String id
    ) throws NoObjectException {
        log.info("Duplicating template with ID: {}", id);
        CIBUser user = null;
        if (authenticationEnabled) {
            user = checkAuthorization(rq, true);
        }
        
        ElementTemplate originalTemplate = ensureIdExists(id);
        
        // Create a new template based on the original
        ElementTemplate duplicatedTemplate = new ElementTemplate();
        duplicatedTemplate.setName(originalTemplate.getName() + " (Copy)");
        duplicatedTemplate.setTemplateId(originalTemplate.getTemplateId() + "_copy_" + UUID.randomUUID().toString().substring(0, 8));
        duplicatedTemplate.setDescription(originalTemplate.getDescription());
        duplicatedTemplate.setContent(originalTemplate.getContent());
        duplicatedTemplate.setOrigin(originalTemplate.getOrigin());
        duplicatedTemplate.setCreatedBy(user != null ? user.getUserID() : "anonymous");
        
        ElementTemplate savedTemplate = templateProvider.addTemplate(duplicatedTemplate);
        log.info("Successfully duplicated template: {} -> {} (ID: {})", originalTemplate.getName(), savedTemplate.getName(), savedTemplate.getId());
        return savedTemplate;
    }
    
    /**
     * Deletes multiple element templates in a single operation.
     * <p>
     * This endpoint allows for bulk deletion of templates by providing a list of template IDs.
     * The operation continues processing all IDs even if some deletions fail, returning
     * detailed results about successful and failed deletions.
     * </p>
     * 
     * @param rq the HTTP servlet request containing authentication information
     * @param templateIds list of template IDs to delete
     * @return ResponseEntity containing deletion results including successful and failed IDs
     */
    @Operation(
        summary = "Bulk delete element templates",
        description = "Deletes multiple element templates in a single operation, returning detailed results"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Bulk deletion completed (some operations may have failed)",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content(schema = @Schema(implementation = String.class))
        )
    })
    @PostMapping("/bulk-delete")
    public ResponseEntity<Map<String, Object>> bulkDelete(
        HttpServletRequest rq,
        @Parameter(description = "List of template IDs to delete", required = true)
        @RequestBody List<String> templateIds
    ) {
        log.info("Performing bulk delete for {} templates", templateIds.size());
        if (authenticationEnabled) {
            checkAuthorization(rq, true);
        }
        
        List<String> deletedIds = new ArrayList<>();
        List<String> failedIds = new ArrayList<>();
        
        for (String id : templateIds) {
            try {
                templateProvider.deleteTemplateById(id);
                deletedIds.add(id);
                log.debug("Successfully deleted template with ID: {}", id);
            } catch (Exception e) {
                failedIds.add(id);
                log.warn("Failed to delete template with ID: {} - {}", id, e.getMessage());
            }
        }
        
        Map<String, Object> result = Map.of(
            "deleted", deletedIds,
            "failed", failedIds,
            "totalRequested", templateIds.size(),
            "totalDeleted", deletedIds.size()
        );
        
        log.info("Bulk delete completed: {}/{} templates deleted successfully", deletedIds.size(), templateIds.size());
        return ResponseEntity.ok(result);
    }
    
    /**
     * Updates the visibility status of multiple element templates in a single operation.
     * <p>
     * This endpoint allows for bulk updates of template active status by providing a list
     * of template IDs and the desired active state. The operation continues processing all
     * IDs even if some updates fail, returning detailed results.
     * </p>
     * 
     * @param rq the HTTP servlet request containing authentication information
     * @param bulkRequest map containing templateIds list and active boolean value
     * @return ResponseEntity containing update results including successful and failed IDs
     */
    @Operation(
        summary = "Bulk update element template visibility",
        description = "Updates the active status of multiple element templates in a single operation"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Bulk update completed (some operations may have failed)",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data or missing required parameters",
            content = @Content(schema = @Schema(implementation = String.class))
        )
    })
    @PatchMapping("/bulk-update-visibility")
    public ResponseEntity<Map<String, Object>> bulkUpdateVisibility(
        HttpServletRequest rq,
        @Parameter(description = "Bulk update request containing templateIds array and active boolean", required = true)
        @RequestBody Map<String, Object> bulkRequest
    ) {
        CIBUser user = null;
        if (authenticationEnabled) {
            user = checkAuthorization(rq, true);
        }
        
        @SuppressWarnings("unchecked")
        List<String> templateIds = (List<String>) bulkRequest.get("templateIds");
        Boolean active = (Boolean) bulkRequest.get("active");
        
        if (templateIds == null || active == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "templateIds and active parameters are required"));
        }
        
        log.info("Performing bulk visibility update for {} templates to active={}", templateIds.size(), active);
        
        List<String> updatedIds = new ArrayList<>();
        List<String> failedIds = new ArrayList<>();
        
        for (String id : templateIds) {
            try {
                Optional<ElementTemplate> template = templateProvider.findById(id);
                if (template.isPresent()) {
                    // Set updated_by field for audit trail
                    template.get().setUpdatedBy(user != null ? user.getUserID() : "anonymous");
                    Map<String, Object> updateProps = Map.of("active", active);
                    templateProvider.partialUpdate(template.get(), updateProps);
                    updatedIds.add(id);
                    log.debug("Successfully updated visibility for template with ID: {} to active={}", id, active);
                } else {
                    failedIds.add(id);
                    log.warn("Template not found with ID: {}", id);
                }
            } catch (Exception e) {
                failedIds.add(id);
                log.warn("Failed to update visibility for template with ID: {} - {}", id, e.getMessage());
            }
        }
        
        Map<String, Object> result = Map.of(
            "updated", updatedIds,
            "failed", failedIds,
            "totalRequested", templateIds.size(),
            "totalUpdated", updatedIds.size()
        );
        
        log.info("Bulk visibility update completed: {}/{} templates updated successfully", updatedIds.size(), templateIds.size());
        return ResponseEntity.ok(result);
    }
    
    /**
     * Searches for element templates using multiple criteria.
     * <p>
     * This endpoint provides advanced search functionality with multiple optional
     * parameters. All search criteria are case-insensitive and use partial matching.
     * Multiple criteria can be combined to refine search results.
     * </p>
     * 
     * @param rq the HTTP servlet request containing authentication information
     * @param name optional name filter (partial match, case-insensitive)
     * @param creator optional creator filter (partial match, case-insensitive)
     * @param active optional active status filter (exact match)
     * @param templateId optional template ID filter (partial match, case-insensitive)
     * @param description optional description filter (partial match, case-insensitive)
     * @return list of templates matching the search criteria
     */
    @Operation(
        summary = "Search element templates",
        description = "Searches for element templates using multiple optional criteria with partial matching"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Search completed successfully",
            content = @Content(schema = @Schema(implementation = ElementTemplate[].class))
        )
    })
    @GetMapping("/search")
    public List<ElementTemplate> searchTemplates(
        HttpServletRequest rq,
        @Parameter(description = "Filter by template name (partial match)")
        @RequestParam(required = false) String name,
        @Parameter(description = "Filter by template creator (partial match)")
        @RequestParam(required = false) String creator,
        @Parameter(description = "Filter by active status")
        @RequestParam(required = false) Boolean active,
        @Parameter(description = "Filter by template ID (partial match)")
        @RequestParam(required = false) String templateId,
        @Parameter(description = "Filter by template description (partial match)")
        @RequestParam(required = false) String description
    ) {
        log.info("Searching templates with criteria - name: {}, creator: {}, active: {}, templateId: {}, description: {}", 
                 name, creator, active, templateId, description);
        if (authenticationEnabled) {
            checkAuthorization(rq, true);
        }
        
        List<ElementTemplate> allTemplates = templateProvider.getElementTemplates();
        
        List<ElementTemplate> results = allTemplates.stream()
            .filter(template -> name == null || template.getName().toLowerCase().contains(name.toLowerCase()))
            .filter(template -> creator == null || (template.getCreatedBy() != null && template.getCreatedBy().toLowerCase().contains(creator.toLowerCase())))
            .filter(template -> active == null || template.getActive().equals(active))
            .filter(template -> templateId == null || template.getTemplateId().toLowerCase().contains(templateId.toLowerCase()))
            .filter(template -> description == null || (template.getDescription() != null && template.getDescription().toLowerCase().contains(description.toLowerCase())))
            .collect(Collectors.toList());
        
        log.info("Search completed: {} templates found matching criteria", results.size());
        return results;
    }
    
    /**
     * Filters element templates using basic criteria.
     * <p>
     * This endpoint provides simple filtering functionality with common use cases.
     * By default, it returns only active templates unless specified otherwise.
     * </p>
     * 
     * @param rq the HTTP servlet request containing authentication information
     * @param activeOnly whether to include only active templates (default: true)
     * @param createdBy optional filter by exact creator name (case-insensitive)
     * @return list of templates matching the filter criteria
     */
    @Operation(
        summary = "Filter element templates",
        description = "Filters element templates using basic criteria like active status and creator"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Filter applied successfully",
            content = @Content(schema = @Schema(implementation = ElementTemplate[].class))
        )
    })
    @GetMapping("/filter")
    public List<ElementTemplate> filterTemplates(
        HttpServletRequest rq,
        @Parameter(description = "Show only active templates (default: true)")
        @RequestParam(required = false, defaultValue = "true") Boolean activeOnly,
        @Parameter(description = "Filter by creator name (exact match, case-insensitive)")
        @RequestParam(required = false) String createdBy
    ) {
        log.info("Filtering templates with activeOnly: {}, createdBy: {}", activeOnly, createdBy);
        if (authenticationEnabled) {
            checkAuthorization(rq, true);
        }
        
        List<ElementTemplate> templates = templateProvider.getElementTemplates();
        
        if (activeOnly) {
            templates = templates.stream()
                .filter(ElementTemplate::getActive)
                .collect(Collectors.toList());
        }
        
        if (createdBy != null && !createdBy.trim().isEmpty()) {
            templates = templates.stream()
                .filter(template -> createdBy.equalsIgnoreCase(template.getCreatedBy()))
                .collect(Collectors.toList());
        }
        
        log.info("Filter completed: {} templates found matching criteria", templates.size());
        return templates;
    }
    
    /**
     * Validates an element template request before creation or update.
     * <p>
     * This endpoint validates template data against business rules and constraints
     * without actually persisting the template. It checks for required fields,
     * field length limits, and duplicate template IDs.
     * </p>
     * 
     * @param rq the HTTP servlet request containing authentication information
     * @param templateRequest the template data to validate
     * @return ResponseEntity containing validation results with any errors found
     */
    @Operation(
        summary = "Validate element template",
        description = "Validates template data against business rules without persisting it"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Validation completed",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateTemplate(
        HttpServletRequest rq,
        @Parameter(description = "Template data to validate", required = true)
        @RequestBody ElementTemplateRequest templateRequest
    ) {
        log.info("Validating template: {}", templateRequest.getName());
        if (authenticationEnabled) {
            checkAuthorization(rq, true);
        }
        
        List<String> errors = new ArrayList<>();
        
        // Validate required fields
        if (templateRequest.getName() == null || templateRequest.getName().trim().isEmpty()) {
            errors.add("Name is required");
        } else if (templateRequest.getName().length() > 200) {
            errors.add("Name cannot exceed 200 characters");
        }
        
        if (templateRequest.getTemplateId() == null || templateRequest.getTemplateId().trim().isEmpty()) {
            errors.add("Template ID is required");
        } else if (templateRequest.getTemplateId().length() > 100) {
            errors.add("Template ID cannot exceed 100 characters");
        }
        
        if (templateRequest.getContent() == null || templateRequest.getContent().trim().isEmpty()) {
            errors.add("Content is required");
        }
        
        // Check for duplicate template ID (excluding the current template if updating)
        List<ElementTemplate> existingTemplates = templateProvider.getElementTemplates();
        boolean duplicateTemplateId = existingTemplates.stream()
            .anyMatch(template -> templateRequest.getTemplateId().equals(template.getTemplateId()));
            
        if (duplicateTemplateId) {
            errors.add("Template ID already exists");
        }
        
        Map<String, Object> result = Map.of(
            "valid", errors.isEmpty(),
            "errors", errors
        );
        
        log.info("Validation completed for template '{}': {} (errors: {})", 
                 templateRequest.getName(), errors.isEmpty() ? "VALID" : "INVALID", errors.size());
        return ResponseEntity.ok(result);
    }
    
    /**
     * Imports multiple element templates from a JSON array.
     * <p>
     * This endpoint allows bulk import of templates from external sources.
     * Each template in the array is processed individually, and the operation
     * continues even if some imports fail. Detailed results are returned.
     * </p>
     * 
     * @param rq the HTTP servlet request containing authentication information
     * @param templateRequests list of template data to import
     * @return ResponseEntity containing import results with successful and failed imports
     */
    @Operation(
        summary = "Import multiple element templates",
        description = "Imports multiple element templates from a JSON array, processing each individually"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Import operation completed (some imports may have failed)",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content(schema = @Schema(implementation = String.class))
        )
    })
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importTemplates(
        HttpServletRequest rq,
        @Parameter(description = "Array of template data to import", required = true)
        @RequestBody List<ElementTemplateRequest> templateRequests
    ) {
        log.info("Importing {} templates", templateRequests.size());
        CIBUser user = null;
        if (authenticationEnabled) {
            user = checkAuthorization(rq, true);
        }
        
        List<ElementTemplate> importedTemplates = new ArrayList<>();
        List<String> failedTemplates = new ArrayList<>();
        
        for (ElementTemplateRequest request : templateRequests) {
            try {
                ElementTemplate template = new ElementTemplate();
                template.setName(request.getName());
                template.setTemplateId(request.getTemplateId());
                template.setDescription(request.getDescription());
                template.setContent(request.getContent());
                template.setOrigin(request.getOrigin() != null ? request.getOrigin() : ElementTemplateOrigin.MANUAL);
                template.setCreatedBy(user != null ? user.getUserID() : "anonymous");
                template.setUpdatedBy(user != null ? user.getUserID() : "anonymous");
                
                ElementTemplate savedTemplate = templateProvider.addTemplate(template);
                importedTemplates.add(savedTemplate);
                log.debug("Successfully imported template: {} (ID: {})", savedTemplate.getName(), savedTemplate.getId());
            } catch (Exception e) {
                String templateId = request.getTemplateId() != null ? request.getTemplateId() : "Unknown ID";
                failedTemplates.add(templateId);
                log.warn("Failed to import template with ID: {} - {}", templateId, e.getMessage());
            }
        }
        
        Map<String, Object> result = Map.of(
            "imported", importedTemplates,
            "failed", failedTemplates,
            "totalRequested", templateRequests.size(),
            "totalImported", importedTemplates.size()
        );
        
        log.info("Import completed: {}/{} templates imported successfully", importedTemplates.size(), templateRequests.size());
        return ResponseEntity.ok(result);
    }
    
    /**
     * Exports element templates based on specified criteria.
     * <p>
     * This endpoint allows exporting templates in JSON format for backup,
     * migration, or sharing purposes. Templates can be filtered by specific
     * IDs or active status before export.
     * </p>
     * 
     * @param rq the HTTP servlet request containing authentication information
     * @param templateIds optional list of specific template IDs to export
     * @param activeOnly whether to export only active templates (default: false)
     * @return ResponseEntity containing the list of templates matching export criteria
     */
    @Operation(
        summary = "Export element templates",
        description = "Exports element templates in JSON format based on specified filtering criteria"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Templates exported successfully",
            content = @Content(schema = @Schema(implementation = ElementTemplate[].class))
        )
    })
    @GetMapping("/export")
    public ResponseEntity<List<ElementTemplate>> exportTemplates(
        HttpServletRequest rq,
        @Parameter(description = "List of specific template IDs to export (optional)")
        @RequestParam(required = false) List<String> templateIds,
        @Parameter(description = "Export only active templates (default: false)")
        @RequestParam(required = false, defaultValue = "false") Boolean activeOnly
    ) {
        log.info("Exporting templates with filters - templateIds: {}, activeOnly: {}", 
                 templateIds != null ? templateIds.size() : "all", activeOnly);
        if (authenticationEnabled) {
            checkAuthorization(rq, true);
        }
        
        List<ElementTemplate> templates = templateProvider.getElementTemplates();
        
        // Filter by specific IDs if provided
        if (templateIds != null && !templateIds.isEmpty()) {
            templates = templates.stream()
                .filter(template -> templateIds.contains(template.getId()))
                .collect(Collectors.toList());
            log.debug("Filtered by template IDs: {} templates selected", templates.size());
        }
        
        // Filter by active status if requested
        if (activeOnly) {
            templates = templates.stream()
                .filter(ElementTemplate::getActive)
                .collect(Collectors.toList());
            log.debug("Filtered by active status: {} templates selected", templates.size());
        }
        
        log.info("Export completed: {} templates exported", templates.size());
        return ResponseEntity.ok(templates);
    }
    
    /**
     * Retrieves statistical information about element templates.
     * <p>
     * This endpoint provides comprehensive statistics about the template
     * repository including total counts, active/inactive breakdowns, and
     * statistics grouped by template creators.
     * </p>
     * 
     * @param rq the HTTP servlet request containing authentication information
     * @return ResponseEntity containing comprehensive template statistics
     */
    @Operation(
        summary = "Get element template statistics",
        description = "Retrieves comprehensive statistics about the element template repository"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Statistics retrieved successfully",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getTemplateStatistics(
        HttpServletRequest rq
    ) {
        log.info("Retrieving template statistics");
        if (authenticationEnabled) {
            checkAuthorization(rq, true);
        }
        
        List<ElementTemplate> allTemplates = templateProvider.getElementTemplates();
        
        long totalCount = allTemplates.size();
        long activeCount = allTemplates.stream().filter(ElementTemplate::getActive).count();
        long inactiveCount = totalCount - activeCount;
        
        Map<String, Long> creatorStats = allTemplates.stream()
            .collect(Collectors.groupingBy(
                template -> template.getCreatedBy() != null ? template.getCreatedBy() : "System",
                Collectors.counting()
            ));
        
        Map<String, Object> stats = Map.of(
            "total", totalCount,
            "active", activeCount,
            "inactive", inactiveCount,
            "byCreator", creatorStats
        );
        
        log.info("Statistics generated: {} total templates ({} active, {} inactive), {} unique creators", 
                 totalCount, activeCount, inactiveCount, creatorStats.size());
        return ResponseEntity.ok(stats);
    }
}
