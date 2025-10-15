package org.cibseven.webapp.rest;

import java.util.Map;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.MultiValueMap;
import org.cibseven.webapp.rest.model.Deployment;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.SevenResourceType;
import org.cibseven.webapp.providers.PermissionConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@ApiResponses({
    @ApiResponse(responseCode = "500", description = "An unexpected system error occurred"),
    @ApiResponse(responseCode = "401", description = "Unauthorized")
})
@RestController
@RequestMapping("${cibseven.webclient.services.basePath:/services/v1}" + "/deployment")
public class DeploymentService extends BaseService {

    /**
     * Creates a new deployment
     * 
     * @param data The deployment parameters (deployment-name, deployment-source, tenant-id, etc.)
     * @param files The files to deploy (DMN, BPMN, etc.)
     * @return The created deployment
     */
    @Operation(
        summary = "Create a new deployment",
        description = "Creates a new deployment."
                + "Requires CREATE permission on the DEPLOYMENT resource type.")
    @ApiResponse(responseCode = "200", description = "Deployment created successfully")
    @ApiResponse(responseCode = "400", description = "Bad request - invalid parameters or deployment failed")
    @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    @PostMapping("/create")
    public Deployment createDeployment(
            @RequestParam MultiValueMap<String, Object> data,
            @RequestParam(required = true) MultipartFile[] files,
            CIBUser user) {
        // Check permissions
        checkPermission(user, SevenResourceType.DEPLOYMENT, PermissionConstants.CREATE_ALL);

        return bpmProvider.createDeployment(data, files, user);
    }

    /**
     * Redeploys an existing deployment
     * 
     * @param id The deployment ID to redeploy
     * @param data The redeployment parameters (tenantId, source, resourceIds, resourceNames)
     * @param user The authenticated user
     * @return The newly created deployment
     */
    @Operation(
        summary = "Redeploy an existing deployment",
        description = "Re-deploys an existing deployment. The deployment resources to re-deploy can be restricted by using the properties resourceIds or resourceNames. "
                + "For every contained decision or process definition a new version will be created. "
                + "Requires CREATE permission on the DEPLOYMENT resource type.")
    @ApiResponse(responseCode = "200", description = "Deployment redeployed successfully")
    @ApiResponse(responseCode = "400", description = "Bad request - deployment not found or redeployment failed")
    @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    @ApiResponse(responseCode = "404", description = "Deployment not found")
    @PostMapping("/{id}/redeploy")
    public Deployment redeployDeployment(
            @Parameter(description = "The deployment ID", required = true)
            @PathVariable("id") String id,
            @RequestBody(required = false) Map<String, Object> data,
            CIBUser user) {
        // Check permissions
        checkPermission(user, SevenResourceType.DEPLOYMENT, PermissionConstants.CREATE_ALL);

        return bpmProvider.redeployDeployment(id, data, user);
    }
}